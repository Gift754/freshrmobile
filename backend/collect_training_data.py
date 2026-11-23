"""Build a labelled code dataset from GitHub, one skill area per repo topic (weak supervision).

    export GITHUB_TOKEN=ghp_xxx        # optional but strongly recommended (rate limits)
    python collect_training_data.py --repos-per-area 40 --out data/train.jsonl

Repos that match topics from more than one area are dropped as ambiguous. Files go through the
same filtering as the scanner (no vendored/generated/minified/boilerplate code).
Output rows: {"text", "label", "repo", "path"}
"""
import argparse
import hashlib
import json
import os
import random
import shutil
import subprocess
import tempfile
import time
from collections import defaultdict
from pathlib import Path

import requests

import filtering as F

AREAS = {
    "frontend": ["react", "vue", "angular", "svelte", "nextjs", "tailwindcss", "frontend"],
    "backend": ["express", "django", "flask", "fastapi", "spring-boot", "laravel", "nestjs", "backend"],
    "mobile": ["android", "ios", "flutter", "react-native", "swiftui", "jetpack-compose"],
    "data_ml": ["machine-learning", "deep-learning", "data-science", "pytorch", "tensorflow", "nlp"],
    "devops": ["docker", "kubernetes", "terraform", "ansible", "github-actions", "ci-cd"],
    "systems": ["operating-system", "compiler", "embedded", "rust", "cpp", "linux-kernel"],
    "game_dev": ["unity", "godot", "game-development", "pygame", "unreal-engine"],
    "database": ["mysql", "postgresql", "mongodb", "sql", "database", "orm"],
}
API = "https://api.github.com/search/repositories"


def headers():
    h = {"Accept": "application/vnd.github+json"}
    if os.environ.get("GITHUB_TOKEN"):
        h["Authorization"] = f"Bearer {os.environ['GITHUB_TOKEN']}"
    return h


def search(topic, per_page=30, page=1):
    params = {"q": f"topic:{topic} fork:false stars:20..20000", "sort": "stars",
              "per_page": per_page, "page": page}
    for _ in range(5):
        r = requests.get(API, params=params, headers=headers(), timeout=30)
        if r.status_code in (403, 429):
            time.sleep(int(r.headers.get("Retry-After", 30)))
            continue
        r.raise_for_status()
        return r.json().get("items", [])
    return []


def pick_repos(per_area):
    seen = defaultdict(set)      # repo -> areas
    meta, by_area = {}, defaultdict(list)
    for area, topics in AREAS.items():
        for topic in topics:
            for repo in search(topic, per_page=max(10, per_area // len(topics) + 5)):
                seen[repo["full_name"]].add(area)
                meta[repo["full_name"]] = repo["clone_url"]
            time.sleep(2)   # search API is rate limited
    for name, areas in seen.items():
        if len(areas) == 1:
            by_area[next(iter(areas))].append(name)
    return {a: [(n, meta[n]) for n in names[:per_area]] for a, names in by_area.items()}, meta


def extract_files(root, files_per_repo, rng):
    cands = []
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = [d for d in dirnames if d not in F.VENDOR_DIRS]
        for fn in filenames:
            ext = os.path.splitext(fn)[1].lower()
            if ext not in F.LANG_EXT:
                continue
            rel = os.path.relpath(os.path.join(dirpath, fn), root)
            if F.skip_reason(rel):
                continue
            text = F.read_text(os.path.join(dirpath, fn), max_bytes=200_000)
            if not text or text.count("\n") < 15 or F.content_skip_reason(text):
                continue
            cands.append((rel, text[:4000]))
    rng.shuffle(cands)
    return cands[:files_per_repo]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--repos-per-area", type=int, default=40)
    ap.add_argument("--files-per-repo", type=int, default=25)
    ap.add_argument("--out", default="data/train.jsonl")
    ap.add_argument("--seed", type=int, default=7)
    a = ap.parse_args()
    rng = random.Random(a.seed)
    out = Path(a.out)
    out.parent.mkdir(parents=True, exist_ok=True)

    done = set()
    if out.exists():       # resumable
        for line in out.read_text().splitlines():
            done.add(json.loads(line)["repo"])
    plan, _ = pick_repos(a.repos_per_area)
    seen_hash = set()
    with out.open("a", encoding="utf-8") as fh:
        for area, repos in plan.items():
            for name, url in repos:
                if name in done:
                    continue
                tmp = tempfile.mkdtemp()
                try:
                    subprocess.run(["git", "clone", "--quiet", "--depth", "1", "--filter=blob:limit=1m",
                                    url, tmp], check=True, timeout=300, capture_output=True)
                    rows = 0
                    for rel, text in extract_files(tmp, a.files_per_repo, rng):
                        h = hashlib.sha1(text.encode("utf-8", "ignore")).hexdigest()
                        if h in seen_hash:
                            continue
                        seen_hash.add(h)
                        fh.write(json.dumps({"text": text, "label": area, "repo": name, "path": rel}) + "\n")
                        rows += 1
                    fh.flush()
                    print(f"{area:9s} {name:50s} {rows} files")
                except Exception as e:      # noqa: BLE001
                    print(f"skip {name}: {e}")
                finally:
                    shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    main()
