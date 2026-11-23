"""Freshr API.  Run:  uvicorn api_server:app --host 0.0.0.0 --port 8000

Env: FRESHR_API_KEY (shared secret, header X-API-Key), FRESHR_MODEL (default models/code_skill.joblib),
     FRESHR_DB, GITHUB_TOKEN, ANTHROPIC_API_KEY, FRESHR_LLM_MODEL, FRESHR_SHORTLIST_AT, FRESHR_REJECT_BELOW
"""
import io
import os
import re
from contextlib import asynccontextmanager
from typing import Optional

from fastapi import BackgroundTasks, Depends, FastAPI, Header, HTTPException, Response
from pydantic import BaseModel, Field

import api_core as core
import match as M

MODEL = None


@asynccontextmanager
async def lifespan(_app):
    global MODEL
    core.init()
    path = os.environ.get("FRESHR_MODEL", "models/code_skill.joblib")
    if os.path.exists(path):
        from codemodel import CodeSkillModel
        MODEL = CodeSkillModel.load(path)
    yield


def auth(x_api_key: str = Header(default="")):
    expected = os.environ.get("FRESHR_API_KEY")
    if expected and x_api_key != expected:
        raise HTTPException(401, "bad api key")


app = FastAPI(title="Freshr API", lifespan=lifespan, dependencies=[Depends(auth)])


# ---------------- schemas ----------------
class JobIn(BaseModel):
    position: str
    company: str = ""
    description: str = ""
    salary: str = ""
    location: str = ""
    employmentType: str = ""
    experienceItems: list[str] = Field(default_factory=list)
    requiredSkills: Optional[list[str]] = None


class RegisterIn(BaseModel):
    studentId: str
    name: Optional[str] = None
    skills: list[str] = Field(default_factory=list)     # self-declared at sign-up
    interests: str = ""                                 # free text, skills extracted from it


class SyncIn(BaseModel):
    githubUrl: str
    authorEmails: list[str] = Field(default_factory=list)


class ApplicantIn(BaseModel):
    applicantId: str
    name: str = ""
    cvText: str = ""


class RankIn(BaseModel):
    applicants: list[ApplicantIn] = Field(default_factory=list)   # empty -> everyone who applied


class TailorIn(BaseModel):
    studentId: str
    jobId: str
    prompt: str = ""


class PdfIn(BaseModel):
    studentId: str
    jobId: Optional[str] = None
    text: str
    aiTailored: bool = True


class ApplyIn(BaseModel):
    jobId: str
    studentId: str
    aiTailored: bool = False


class OutcomeIn(BaseModel):
    outcome: str      # placed | not_placed


class EventIn(BaseModel):
    kind: str
    studentId: Optional[str] = None
    jobId: Optional[str] = None
    applicationId: Optional[int] = None


# ---------------- helpers ----------------
def gh_user(url):
    m = re.search(r"github\.com/([A-Za-z0-9-]+)", url)
    user = m.group(1) if m else url.strip().lstrip("@")
    if not re.fullmatch(r"[A-Za-z0-9-]{1,39}", user):
        raise HTTPException(422, "could not read a GitHub username from that URL")
    return user


def run_sync(sid, user, authors):
    try:
        from github_profile import build_profile
        core.save_profile(sid, build_profile(user, authors, MODEL, max_repos=20), user)
    except Exception as e:      # noqa: BLE001
        core.set_sync(sid, "error", str(e)[:300])


def need_student(sid):
    row = core.get_student(sid)
    if row is None:
        raise HTTPException(404, "unknown student, call /students/register first")
    return row


# ---------------- 11: cold start / 6: GitHub sync / 7: matches ----------------
@app.post("/students/register")
def register(body: RegisterIn):
    claimed = M.normalise(body.skills, core.TAX) | M.extract_skills(body.interests, core.TAX)
    core.upsert_student(body.studentId, body.name, claimed)
    return {"ok": True, "initialSkills": sorted(claimed)}


@app.post("/students/{sid}/github-sync", status_code=202)
def start_sync(sid: str, body: SyncIn, bg: BackgroundTasks):
    row = need_student(sid)
    user = gh_user(body.githubUrl)
    if row["sync_status"] != "running":
        core.upsert_student(sid, github_user=user)
        core.set_sync(sid, "running")
        bg.add_task(run_sync, sid, user, body.authorEmails)
    return {"status": "running", "coreSkills": []}


@app.get("/students/{sid}/github-sync")
def sync_status(sid: str):
    row = need_student(sid)
    return {"status": row["sync_status"], "error": row["sync_error"],
            "coreSkills": core.core_skills(row) if row["sync_status"] == "done" else []}


@app.get("/students/{sid}/matches")
def matches(sid: str, limit: int = 20):
    out = core.matches_for_student(sid, limit)
    if out is None:
        raise HTTPException(404, "unknown student")
    return out


# ---------------- 1+2: job parsing / re-scoring ----------------
@app.put("/jobs/{jid}")
def put_job(jid: str, body: JobIn):
    job = {"id": jid, **body.model_dump()}
    core.save_job(job)
    return core.analyze_job(job)


@app.get("/jobs")
def list_jobs():
    return [core.analyze_job(j) for j in core.all_jobs()]


# ---------------- 3: ranking ----------------
@app.post("/jobs/{jid}/rank")
def rank(jid: str, body: RankIn):
    job = core.get_job(jid)
    if job is None:
        raise HTTPException(404, "unknown job, PUT /jobs/{id} first")
    req = core.job_requirements(job)
    applicants = body.applicants
    if not applicants:
        with core.conn() as c:
            applicants = [ApplicantIn(applicantId=r["student_id"], name=(core.get_student(r["student_id"]) or {"name": ""})["name"] or "")
                          for r in c.execute("SELECT student_id FROM applications WHERE job_id=?", (jid,))]
    with core.conn() as c:
        applied_at = {r["student_id"]: int(r["created"] * 1000)
                      for r in c.execute("SELECT student_id, created FROM applications WHERE job_id=?", (jid,))}
    out = []
    for a in applicants:
        cv_skills = M.extract_skills(a.cvText, core.TAX) if a.cvText else set()
        row = core.get_student(a.applicantId)
        if row is not None:
            if cv_skills:
                core.upsert_student(a.applicantId, claimed=cv_skills)
                row = core.get_student(a.applicantId)
            skills, areas, _ = core.effective(row)
        else:
            skills, areas = {s: core.CLAIMED_WEIGHT for s in cv_skills}, {}
        s = core.score_pair(skills, areas, req)
        with core.conn() as c:
            c.execute("UPDATE applications SET score=?, tier=? WHERE job_id=? AND student_id=?",
                      (s["pct"], s["tier"], jid, a.applicantId))
        out.append({"applicantId": a.applicantId, "name": a.name, "individualMatchScore": s["pct"],
                    "suggestedStatus": core.status_for(s["pct"]), "matchedSkills": s["matched"],
                    "missingSkills": s["missing"], "appliedAtEpochMs": applied_at.get(a.applicantId)})
    return sorted(out, key=lambda x: -x["individualMatchScore"])


# ---------------- 10: landing search ----------------
@app.get("/search")
def search(q: str, limit: int = 10):
    return core.search_jobs(q, limit)


# ---------------- 8: CV tailoring ----------------
CV_SYSTEM = (
    "You tailor resumes for students. Use ONLY the facts provided. Never invent employers, degrees, dates, "
    "metrics or skills. Only list a skill as a strength if it appears under VERIFIED or CLAIMED skills; "
    "never claim the MISSING skills (you may suggest learning them in a short note at the end). "
    "Output plain text with sections: SUMMARY, SKILLS, PROJECTS, and TAILORING NOTES. Be concise."
)


def tailor_text(row, job, prompt):
    skills, _, prof = core.effective(row)
    req = core.job_requirements(job)
    s = core.score_pair(skills, {}, req)
    verified = sorted(prof.get("skills", {}))
    claimed = sorted(set(skills) - set(verified))
    repos = "\n".join(f"- {r['repo']}: {', '.join(r['top_skills'][:5])}" for r in prof.get("repos", [])[:8]) or "(none)"
    user_msg = (f"STUDENT: {row['name'] or 'Student'}\nVERIFIED skills (from GitHub): {', '.join(verified) or 'none'}\n"
                f"CLAIMED skills: {', '.join(claimed) or 'none'}\nPROJECTS:\n{repos}\n\n"
                f"TARGET JOB: {job['position']} at {job.get('company', '')}\n{job.get('description', '')}\n"
                f"Requirements: {'; '.join(job.get('experienceItems', []))}\nRequired skills: {', '.join(req)}\n"
                f"MISSING skills: {', '.join(s['missing']) or 'none'}\n\nStudent instructions: {prompt or 'none'}")
    if os.environ.get("ANTHROPIC_API_KEY"):
        import anthropic
        msg = anthropic.Anthropic().messages.create(
            model=os.environ.get("FRESHR_LLM_MODEL", "claude-sonnet-5-5"), max_tokens=1500,
            system=CV_SYSTEM, messages=[{"role": "user", "content": user_msg}])
        return "".join(b.text for b in msg.content if b.type == "text")
    have = [k for k in req if k in skills]
    return (f"SUMMARY\n{row['name'] or 'Student'} applying for {job['position']}. "
            f"Matches {len(have)} of {len(req)} required skills.\n\nSKILLS\n{', '.join(have) or 'n/a'}\n"
            f"Other: {', '.join(k for k in sorted(skills) if k not in have) or 'n/a'}\n\nPROJECTS\n{repos}\n\n"
            f"TAILORING NOTES\nSet ANTHROPIC_API_KEY on the server for full AI rewriting. "
            f"Skills to build for this role: {', '.join(s['missing']) or 'none'}.")


@app.post("/cv/tailor")
def cv_tailor(body: TailorIn):
    row, job = need_student(body.studentId), core.get_job(body.jobId)
    if job is None:
        raise HTTPException(404, "unknown job")
    text = tailor_text(row, job, body.prompt)
    core.log_event("cv_generated", body.studentId, body.jobId)
    return {"generatedCvPreview": text}


# ---------------- 9: PDF export ----------------
@app.post("/cv/pdf")
def cv_pdf(body: PdfIn):
    try:
        from reportlab.lib.pagesizes import A4
        from reportlab.lib.styles import getSampleStyleSheet
        from reportlab.platypus import Paragraph, SimpleDocTemplate, Spacer
    except ImportError:
        raise HTTPException(501, "pip install reportlab")
    from xml.sax.saxutils import escape
    st = getSampleStyleSheet()
    buf, flow = io.BytesIO(), []
    for line in body.text.splitlines():
        if not line.strip():
            flow.append(Spacer(1, 8))
        elif line.strip().isupper():
            flow.append(Paragraph(f"<b>{escape(line.strip())}</b>", st["Heading3"]))
        else:
            flow.append(Paragraph(escape(line), st["BodyText"]))
    SimpleDocTemplate(buf, pagesize=A4, title="Tailored CV").build(flow)
    core.log_event("cv_exported", body.studentId, body.jobId)
    return Response(buf.getvalue(), media_type="application/pdf")


# ---------------- applications, events, 4: dashboard, 5: reports ----------------
@app.post("/applications")
def apply(body: ApplyIn):
    out = core.apply_to_job(body.jobId, body.studentId, body.aiTailored)
    if out is None:
        raise HTTPException(404, "unknown job or student")
    return out


@app.post("/applications/{aid}/outcome")
def outcome(aid: int, body: OutcomeIn):
    if body.outcome not in ("placed", "not_placed"):
        raise HTTPException(422, "outcome must be placed or not_placed")
    with core.conn() as c:
        c.execute("UPDATE applications SET outcome=? WHERE id=?", (body.outcome, aid))
    return {"ok": True}


@app.post("/events")
def event(body: EventIn):
    if body.kind not in core.EVENT_KINDS:
        raise HTTPException(422, f"kind must be one of {sorted(core.EVENT_KINDS)}")
    core.log_event(body.kind, body.studentId, body.jobId, body.applicationId)
    return {"ok": True}


@app.get("/stats")
def stats(studentId: Optional[str] = None):
    return core.stats(studentId)


@app.get("/reports")
def reports():
    return core.reports()
