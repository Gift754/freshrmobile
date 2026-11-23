"""Shared logic for the Freshr API: storage, scoring, job analysis, search and reports."""
import json
import os
import sqlite3
import time
from collections import Counter
from contextlib import contextmanager

from sklearn.feature_extraction.text import TfidfVectorizer

import match as M
from scanner import load_taxonomy

TAX = load_taxonomy()
DB_PATH = os.environ.get("FRESHR_DB", "freshr.db")
SHORTLIST_AT = int(os.environ.get("FRESHR_SHORTLIST_AT", 70))
REJECT_BELOW = int(os.environ.get("FRESHR_REJECT_BELOW", 40))
CLAIMED_WEIGHT = 0.5            # CV/self-declared skills count half as much as GitHub-verified ones
TIERS = ["Met all required skills", "Partial skill match", "Low skill match"]
EVENT_KINDS = {"cv_generated", "cv_exported", "cv_viewed"}

SCHEMA = """
CREATE TABLE IF NOT EXISTS jobs(id TEXT PRIMARY KEY, data TEXT NOT NULL, updated REAL);
CREATE TABLE IF NOT EXISTS students(
  id TEXT PRIMARY KEY, name TEXT, github_user TEXT, profile TEXT, claimed TEXT DEFAULT '[]',
  sync_status TEXT DEFAULT 'none', sync_error TEXT, updated REAL);
CREATE TABLE IF NOT EXISTS applications(
  id INTEGER PRIMARY KEY AUTOINCREMENT, job_id TEXT, student_id TEXT, status TEXT DEFAULT 'Applied',
  score INTEGER, tier TEXT, outcome TEXT, ai_cv INTEGER DEFAULT 0, created REAL,
  UNIQUE(job_id, student_id));
CREATE TABLE IF NOT EXISTS events(
  id INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT, student_id TEXT, job_id TEXT,
  application_id INTEGER, ts REAL);
"""


@contextmanager
def conn():
    c = sqlite3.connect(DB_PATH)
    c.row_factory = sqlite3.Row
    try:
        yield c
        c.commit()
    finally:
        c.close()


def init():
    with conn() as c:
        c.executescript(SCHEMA)


# ---------------- students ----------------
def upsert_student(sid, name=None, claimed=None, github_user=None):
    with conn() as c:
        row = c.execute("SELECT * FROM students WHERE id=?", (sid,)).fetchone()
        if row is None:
            c.execute("INSERT INTO students(id,name,github_user,claimed,updated) VALUES(?,?,?,?,?)",
                      (sid, name, github_user, json.dumps(sorted(set(claimed or []))), time.time()))
        else:
            merged = sorted(set(json.loads(row["claimed"] or "[]")) | set(claimed or []))
            c.execute("UPDATE students SET name=COALESCE(?,name), github_user=COALESCE(?,github_user),"
                      " claimed=?, updated=? WHERE id=?",
                      (name, github_user, json.dumps(merged), time.time(), sid))


def get_student(sid):
    with conn() as c:
        return c.execute("SELECT * FROM students WHERE id=?", (sid,)).fetchone()


def set_sync(sid, status, error=None):
    with conn() as c:
        c.execute("UPDATE students SET sync_status=?, sync_error=?, updated=? WHERE id=?",
                  (status, error, time.time(), sid))


def save_profile(sid, profile, github_user=None):
    with conn() as c:
        c.execute("UPDATE students SET profile=?, github_user=COALESCE(?,github_user),"
                  " sync_status='done', sync_error=NULL, updated=? WHERE id=?",
                  (json.dumps(profile), github_user, time.time(), sid))


def effective(row):
    """(skill -> confidence, areas, raw profile) combining verified GitHub skills and claimed ones."""
    prof = json.loads(row["profile"]) if row["profile"] else {}
    skills = {s: v["confidence"] for s, v in prof.get("skills", {}).items()}
    for s in json.loads(row["claimed"] or "[]"):
        skills[s] = max(skills.get(s, 0.0), CLAIMED_WEIGHT)
    return skills, prof.get("areas", {}), prof


def core_skills(row, limit=25):
    skills, _, prof = effective(row)
    verified = prof.get("skills", {})
    ordered = sorted(skills, key=lambda s: (-(verified.get(s, {}).get("confidence", 0)), s))
    return [s for s in ordered if skills[s] >= M.MIN_CONF][:limit]


# ---------------- jobs ----------------
def save_job(job):
    with conn() as c:
        c.execute("INSERT INTO jobs(id,data,updated) VALUES(?,?,?) ON CONFLICT(id) DO UPDATE SET"
                  " data=excluded.data, updated=excluded.updated",
                  (job["id"], json.dumps(job), time.time()))


def get_job(jid):
    with conn() as c:
        r = c.execute("SELECT data FROM jobs WHERE id=?", (jid,)).fetchone()
    return json.loads(r["data"]) if r else None


def all_jobs():
    with conn() as c:
        return [json.loads(r["data"]) for r in c.execute("SELECT data FROM jobs")]


def job_requirements(job):
    if job.get("requiredSkills"):
        return sorted(M.normalise(job["requiredSkills"], TAX))
    text = "\n".join([job.get("position", ""), job.get("description", ""), *job.get("experienceItems", [])])
    return sorted(M.extract_skills(text, TAX))


# ---------------- scoring ----------------
def tier_for(frac):
    return TIERS[0] if frac >= 0.999 else TIERS[1] if frac >= 0.5 else TIERS[2]


def label_for(pct):
    return "Strong Match" if pct >= 75 else "Good Match" if pct >= 55 else "Fair Match" if pct >= 35 else "Low Match"


def status_for(pct):
    return "Shortlisted" if pct >= SHORTLIST_AT else "Rejected" if pct < REJECT_BELOW else "Under Review"


def score_pair(skills, areas, req):
    have = {s: c for s, c in skills.items() if c >= M.MIN_CONF}
    if not req:
        return {"pct": 0, "matched": [], "missing": [], "tier": TIERS[2]}
    tool = sum(have.get(s, 0.0) for s in req) / len(req)
    jarea = M.area_vector(req, TAX)
    total = 0.6 * tool + 0.4 * M.cosine(areas, jarea) if areas and jarea else tool
    matched = [s for s in req if s in have]
    return {"pct": round(min(1.0, total) * 100), "matched": matched,
            "missing": [s for s in req if s not in have], "tier": tier_for(len(matched) / len(req))}


def analyze_job(job):
    req = job_requirements(job)
    with conn() as c:
        rows = c.execute("SELECT * FROM students").fetchall()
    scores, counts = [], Counter()
    for r in rows:
        skills, areas, _ = effective(r)
        if not skills:
            continue
        s = score_pair(skills, areas, req)
        scores.append(s["pct"])
        counts.update(s["matched"])
    avg = round(sum(scores) / len(scores)) if scores else 0
    return {"id": job["id"], "requiredSkills": req,
            "topMatchedSkills": sorted(req, key=lambda k: (-counts[k], k))[:5],
            "averageMatchScore": avg,
            "scoreLabel": label_for(avg) if scores else "No candidates yet",
            "candidatesScored": len(scores)}


def as_match(job, pct):
    return {"id": job["id"], "position": job.get("position", ""), "company": job.get("company", ""),
            "location": job.get("location", ""), "employmentType": job.get("employmentType", ""),
            "salary": job.get("salary", ""), "description": job.get("description", ""),
            "experience": job.get("experienceItems", []), "matchPercentage": pct}


def matches_for_student(sid, limit=20):
    row = get_student(sid)
    if row is None:
        return None
    skills, areas, _ = effective(row)
    out = [as_match(j, score_pair(skills, areas, job_requirements(j))["pct"]) for j in all_jobs()]
    return sorted(out, key=lambda m: -m["matchPercentage"])[:limit]


def search_jobs(query, limit=10):
    jobs = all_jobs()
    if not jobs or not query.strip():
        return []
    docs = [" ".join([j.get("position", ""), j.get("description", ""), *j.get("experienceItems", [])])
            for j in jobs]
    X = TfidfVectorizer(stop_words="english").fit_transform(docs + [query])
    sims = (X[:-1] @ X[-1].T).toarray().ravel()
    qskills = set(M.extract_skills(query, TAX))
    out = []
    for j, sim in zip(jobs, sims):
        if qskills:
            cov = len(qskills & set(job_requirements(j))) / len(qskills)
            s = 0.6 * cov + 0.4 * float(sim)
        else:
            s = float(sim)
        if s > 0:
            out.append(as_match(j, round(min(1.0, s) * 100)))
    return sorted(out, key=lambda m: -m["matchPercentage"])[:limit]


# ---------------- applications, events, reports ----------------
def apply_to_job(job_id, student_id, ai_tailored):
    job, row = get_job(job_id), get_student(student_id)
    if job is None or row is None:
        return None
    skills, areas, _ = effective(row)
    s = score_pair(skills, areas, job_requirements(job))
    with conn() as c:
        c.execute("INSERT INTO applications(job_id,student_id,score,tier,ai_cv,created) VALUES(?,?,?,?,?,?)"
                  " ON CONFLICT(job_id,student_id) DO UPDATE SET score=excluded.score, tier=excluded.tier,"
                  " ai_cv=excluded.ai_cv", (job_id, student_id, s["pct"], s["tier"], int(ai_tailored), time.time()))
        aid = c.execute("SELECT id FROM applications WHERE job_id=? AND student_id=?",
                        (job_id, student_id)).fetchone()["id"]
    return {"applicationId": aid, "matchPercentage": s["pct"], "tier": s["tier"]}


def log_event(kind, student_id=None, job_id=None, application_id=None):
    with conn() as c:
        c.execute("INSERT INTO events(kind,student_id,job_id,application_id,ts) VALUES(?,?,?,?,?)",
                  (kind, student_id, job_id, application_id, time.time()))


def stats(student_id=None):
    with conn() as c:
        def n(sql, *a):
            return c.execute(sql, a).fetchone()[0]
        if student_id:
            return {"cvsGenerated": n("SELECT COUNT(*) FROM events WHERE kind='cv_generated' AND student_id=?", student_id),
                    "appliedJobs": n("SELECT COUNT(*) FROM applications WHERE student_id=?", student_id),
                    "activeJobs": n("SELECT COUNT(*) FROM jobs"),
                    "registeredCandidates": n("SELECT COUNT(*) FROM students")}
        return {"cvsGenerated": n("SELECT COUNT(*) FROM events WHERE kind='cv_generated'"),
                "appliedJobs": n("SELECT COUNT(*) FROM applications"),
                "activeJobs": n("SELECT COUNT(*) FROM jobs"),
                "registeredCandidates": n("SELECT COUNT(*) FROM students")}


def reports():
    with conn() as c:
        tiers = []
        for t in TIERS:
            outs = [r["outcome"] for r in c.execute("SELECT outcome FROM applications WHERE tier=?", (t,))]
            placed, lost = outs.count("placed"), outs.count("not_placed")
            tiers.append({"tier": t, "applications": len(outs), "placed": placed, "decided": placed + lost,
                          "placementRate": round(placed / (placed + lost), 3) if placed + lost else None})
        views = {0: [], 1: []}
        for r in c.execute("SELECT a.ai_cv AS ai, COUNT(e.id) AS v FROM applications a LEFT JOIN events e"
                           " ON e.application_id=a.id AND e.kind='cv_viewed' GROUP BY a.id"):
            views[r["ai"]].append(r["v"])
    avg = lambda xs: round(sum(xs) / len(xs), 2) if xs else None   # noqa: E731
    ai, man = avg(views[1]), avg(views[0])
    return {"tiers": tiers,
            "cvPerformance": {"aiTailoredAvgViews": ai, "manualAvgViews": man,
                              "liftPercent": round((ai - man) / man * 100, 1) if ai is not None and man else None,
                              "aiApplications": len(views[1]), "manualApplications": len(views[0])}}
