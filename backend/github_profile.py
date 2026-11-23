"""Build a skill profile for a GitHub user: clone their own (non-fork) repos, keep only files they
wrote, extract keyword/tool skills (skills.json) and, if a trained model is given, code skill areas.

    python github_profile.py --user octocat --author octocat@mail.com --model models/code_skill.joblib -o profiles/octocat.json

Commits made under a personal email need that email passed with --author; commits made with GitHub's
noreply address are matched from the username automatically.
"""
import argparse
import json
import os
import subprocess
import time
from collections import Counter, defaultdict
from pathlib import Path

import numpy as np
import requests

from scanner import load_taxonomy, scan_repo


def gh_headers():
    h = {"Accept": "application/vnd.github+json"}
    if os.environ.get("GITHUB_TOKEN"):
        h["Authorization"] = f"Bearer {os.environ['GITHUB_TOKEN']}"
    return h


def list_repos(user, max_repos=30):
    r = requests.get(f"https://api.github.com/users/{user}/repos",
                     params={"per_page": 100, "type": "owner", "sort": "pushed"},
                     headers=gh_headers(), timeout=30)
    r.raise_for_status()
    return [x for x in r.json() if not x["fork"] and x["size"] > 0][:max_repos]


def clone(url, dest):
    if (dest / ".git").exists():
        subprocess.run(["git", "-C", str(dest), "pull", "--quiet"], capture_output=True, timeout=300)
    else:
        dest.parent.mkdir(parents=True, exist_ok=True)
        subprocess.run(["git", "clone", "--quiet", url, str(dest)], check=True, capture_output=True,
                       timeout=300)


def repo_areas(model, samples):
    if not model or not samples:
        return {}
    probs = model.predict_proba([s["text"] for s in samples])
    w = np.array([s["lines"] for s in samples], float)
    avg = (probs * w[:, None]).sum(0) / w.sum()
    return dict(zip(map(str, model.labels), map(float, avg)))


def build_profile(user, authors=(), model=None, cache=".cache/repos", min_share=0.5,
                  template_hashes=None, max_repos=30):
    tax = load_taxonomy()
    authors = [x.lower() for x in authors] + [user.lower()]
    noisy_or = defaultdict(lambda: 1.0)
    repo_count, cats = Counter(), {}
    languages, area_acc, area_w, repo_out = Counter(), Counter(), 0.0, []
    for r in list_repos(user, max_repos):
        dest = Path(cache) / user / r["name"]
        try:
            clone(r["clone_url"], dest)
        except (subprocess.CalledProcessError, subprocess.TimeoutExpired):
            continue
        res = scan_repo(dest, authors, tax, min_share, template_hashes=template_hashes)
        if not res["files_analysed"]:
            continue
        for s, info in res["skills"].items():
            noisy_or[s] *= 1 - info["confidence"]
            repo_count[s] += 1
            cats[s] = info["category"]
        languages.update(res["languages"])
        areas = repo_areas(model, res["code_samples"])
        weight = sum(s["lines"] for s in res["code_samples"])
        for k, v in areas.items():
            area_acc[k] += v * weight
        area_w += weight if areas else 0
        repo_out.append({"repo": r["name"], "top_skills": list(res["skills"])[:8],
                         "areas": {k: round(v, 2) for k, v in sorted(areas.items(), key=lambda kv: -kv[1])[:3]},
                         "skipped": res["skipped"]})
    return {
        "user": user,
        "skills": {s: {"confidence": round(1 - noisy_or[s], 2), "repos": repo_count[s], "category": cats[s]}
                   for s in sorted(noisy_or, key=lambda k: noisy_or[k])},
        "areas": {k: round(v / area_w, 3) for k, v in area_acc.most_common()} if area_w else {},
        "languages": dict(languages.most_common()),
        "repos": repo_out,
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--user", required=True)
    ap.add_argument("--author", action="append", default=[])
    ap.add_argument("--model")
    ap.add_argument("--cache", default=".cache/repos")
    ap.add_argument("--min-share", type=float, default=0.5)
    ap.add_argument("--templates")
    ap.add_argument("-o", "--out")
    a = ap.parse_args()
    model = None
    if a.model:
        from codemodel import CodeSkillModel
        model = CodeSkillModel.load(a.model)
    hashes = set(Path(a.templates).read_text().split()) if a.templates else None
    out = json.dumps(build_profile(a.user, a.author, model, a.cache, a.min_share, hashes), indent=2)
    if a.out:
        Path(a.out).parent.mkdir(parents=True, exist_ok=True)
        Path(a.out).write_text(out)
        print("wrote", a.out)
    else:
        print(out)


if __name__ == "__main__":
    main()
