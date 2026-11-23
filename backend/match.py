"""Match candidate profiles with jobs.

Score = 0.6 * tool-skill coverage (confidence-weighted) + 0.4 * code-area similarity (learned model).
If the profile has no learned areas (no model used), the score is tool-skill coverage only.

    python match.py --profile profiles/octocat.json --jobs sample_jobs.jsonl --top 5
    python match.py --profile profiles/a.json --profile profiles/b.json --jobs sample_jobs.jsonl --job-id j1

Jobs: JSONL with id, title, description and optional required_skills (names or aliases).
Without required_skills they are extracted from the title + description using skills.json aliases.
"""
import argparse
import json
import math
import re
from collections import Counter
from pathlib import Path

from scanner import load_taxonomy

MIN_CONF = 0.25


def alias_index(tax):
    idx = {}
    for name, s in tax["skills"].items():
        for al in {name.lower(), *[x.lower() for x in s.get("aliases", [])]}:
            idx[al] = name
    return idx


def extract_skills(text, tax):
    found = set()
    for al, name in alias_index(tax).items():
        if re.search(r"(?<![\w+#])" + re.escape(al) + r"(?![\w+#])", text, re.I):
            found.add(name)
    return found


def normalise(names, tax):
    idx = alias_index(tax)
    return {idx[n.lower()] for n in names if n.lower() in idx}


def job_skills(job, tax):
    if job.get("required_skills"):
        return normalise(job["required_skills"], tax)
    return extract_skills(job.get("title", "") + "\n" + job.get("description", ""), tax)


def area_vector(skills, tax):
    c2a = tax["category_to_area"]
    c = Counter(c2a[tax["skills"][s]["category"]] for s in skills if tax["skills"][s]["category"] in c2a)
    n = sum(c.values())
    return {k: v / n for k, v in c.items()} if n else {}


def cosine(a, b):
    keys = set(a) | set(b)
    dot = sum(a.get(k, 0) * b.get(k, 0) for k in keys)
    na = math.sqrt(sum(v * v for v in a.values()))
    nb = math.sqrt(sum(v * v for v in b.values()))
    return dot / (na * nb) if na and nb else 0.0


def score(profile, job, tax):
    req = job_skills(job, tax)
    have = {s: v["confidence"] for s, v in profile["skills"].items() if v["confidence"] >= MIN_CONF}
    if not req:
        return None
    tool = sum(have.get(s, 0.0) for s in req) / len(req)
    jarea = area_vector(req, tax)
    if profile.get("areas") and jarea:
        total = 0.6 * tool + 0.4 * cosine(profile["areas"], jarea)
    else:
        total = tool
    return {"job_id": job["id"], "title": job.get("title", ""), "score": round(total, 3),
            "tool_coverage": round(tool, 3),
            "matched": sorted(s for s in req if s in have),
            "missing": sorted(s for s in req if s not in have)}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--profile", action="append", required=True)
    ap.add_argument("--jobs", required=True)
    ap.add_argument("--job-id", help="rank candidates for this job instead of jobs for each candidate")
    ap.add_argument("--top", type=int, default=5)
    a = ap.parse_args()

    tax = load_taxonomy()
    profiles = [json.loads(Path(p).read_text()) for p in a.profile]
    jobs = [json.loads(l) for l in Path(a.jobs).read_text(encoding="utf-8").splitlines() if l.strip()]

    if a.job_id:
        job = next(j for j in jobs if j["id"] == a.job_id)
        rows = [(p["user"], score(p, job, tax)) for p in profiles]
        for user, r in sorted((x for x in rows if x[1]), key=lambda x: -x[1]["score"])[:a.top]:
            print(f"{user:20s} {r['score']:.3f}  matched={r['matched']} missing={r['missing']}")
    else:
        for p in profiles:
            print(f"\n== {p['user']} ==")
            ranked = sorted((r for r in (score(p, j, tax) for j in jobs) if r), key=lambda r: -r["score"])
            for r in ranked[:a.top]:
                print(f"{r['score']:.3f}  {r['job_id']:6s} {r['title']}  matched={r['matched']} missing={r['missing']}")


if __name__ == "__main__":
    main()
