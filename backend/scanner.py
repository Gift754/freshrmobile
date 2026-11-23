"""Scan one git repo and extract skills from the files a given user actually wrote.

    python scanner.py <path-or-git-url> --author you@mail.com --author your-github-name -o repo.json

Authorship: a file counts as the user's when they added >= --min-share of all lines ever added to it
(git log --numstat). Add --precise to confirm with git blame on the files that pass.
"""
import argparse
import fnmatch
import json
import os
import re
import subprocess
import sys
import tempfile
from collections import Counter, defaultdict
from pathlib import Path

import filtering as F

HERE = Path(__file__).parent
MANIFESTS = {
    "package.json", "requirements.txt", "pyproject.toml", "Pipfile", "setup.py", "setup.cfg",
    "pom.xml", "build.gradle", "build.gradle.kts", "composer.json", "Gemfile", "go.mod",
    "Cargo.toml", "pubspec.yaml", "environment.yml",
}
MANIFEST_SUFFIXES = (".csproj",)
CONFIG_EXT = {".json", ".yml", ".yaml", ".toml", ".xml", ".gradle", ".properties", ".ini",
              ".cfg", ".conf", ".tf"}


def load_taxonomy(path=HERE / "skills.json"):
    with open(path, encoding="utf-8") as fh:
        return json.load(fh)


# ---------- git helpers ----------
def git(repo, *args):
    return subprocess.run(["git", "-c", "core.quotepath=false", "-C", str(repo), *args],
                          capture_output=True, text=True, errors="ignore").stdout


def is_me(email, name, authors):
    email, name = email.lower(), name.lower()
    handle = None
    if email.endswith("@users.noreply.github.com"):
        handle = email.split("@")[0].split("+")[-1]
    return any(a in (email, name, handle) for a in authors)


def author_shares(repo, authors):
    """Return (lines added by user per path, lines added by anyone per path, user commit count)."""
    log = git(repo, "log", "--no-merges", "--no-renames", "--numstat", "--format=@@%ae%x1f%an")
    mine, total, commits, cur = Counter(), Counter(), 0, None
    for line in log.splitlines():
        if line.startswith("@@"):
            email, _, name = line[2:].partition("\x1f")
            cur = is_me(email, name, authors)
            commits += cur
        elif "\t" in line and cur is not None:
            added, _, rest = line.partition("\t")
            _, _, path = rest.partition("\t")
            if added == "-":
                continue
            n = int(added)
            total[path] += n
            if cur:
                mine[path] += n
    return mine, total, commits


def blame_share(repo, rel, authors):
    out = git(repo, "blame", "-w", "-M", "--line-porcelain", "HEAD", "--", rel)
    mine = total = 0
    email = name = ""
    for line in out.splitlines():
        if line.startswith("author-mail "):
            email = line[12:].strip("<>")
        elif line.startswith("author "):
            name = line[7:]
        elif line.startswith("\t"):
            total += 1
            mine += is_me(email, name, authors)
    return mine, total


# ---------- skill detection ----------
def manifest_tokens(text):
    return set(re.findall(r"[A-Za-z0-9_.@/\-]+", text.lower()))


def dep_match(deps, tokens):
    hits = []
    for d in deps:
        d = d.lower()
        if d.endswith("*"):
            if any(t.startswith(d[:-1]) for t in tokens):
                hits.append(d)
        elif d in tokens:
            hits.append(d)
    return hits


def scan_repo(repo, authors, taxonomy, min_share=0.5, precise=False, template_hashes=None,
              max_samples=60):
    repo = Path(repo)
    skills = taxonomy["skills"]
    files = [f for f in git(repo, "ls-files").splitlines() if f]
    mine = total = None
    if authors:
        mine, total, commits = author_shares(repo, authors)
    else:
        commits = None

    skipped = Counter()
    ext_lines = Counter()
    lang_lines = Counter()
    dep_tokens = set()
    file_hits = defaultdict(list)
    pattern_files = defaultdict(set)
    samples = []
    compiled = {n: [re.compile(p, re.M) for p in s.get("patterns", [])] for n, s in skills.items()}
    analysed = 0

    for rel in files:
        reason = F.skip_reason(rel)
        if reason:
            skipped[reason] += 1
            continue
        share = 1.0
        if authors:
            t = total.get(rel, 0)
            share = mine.get(rel, 0) / t if t else 0.0
            if share < min_share:
                skipped["not written by user"] += 1
                continue
            if precise:
                m, tt = blame_share(repo, rel, authors)
                share = m / tt if tt else 0.0
                if share < min_share:
                    skipped["not written by user (blame)"] += 1
                    continue
        name = os.path.basename(rel)
        ext = os.path.splitext(name)[1].lower()

        for sname, s in skills.items():
            for g in s.get("files", []):
                if fnmatch.fnmatch(rel, g) or fnmatch.fnmatch(name, g):
                    file_hits[sname].append(rel)
                    break

        text = F.read_text(repo / rel)
        if text is None:
            skipped["binary/too large"] += 1
            continue
        reason = F.content_skip_reason(text, template_hashes)
        if reason:
            skipped[reason] += 1
            continue
        analysed += 1
        n_lines = text.count("\n") + 1
        user_lines = int(n_lines * share)

        if ext in F.LANG_EXT:
            lang_lines[F.LANG_EXT[ext]] += user_lines
            ext_lines[ext] += user_lines
            if user_lines >= 10:
                samples.append({"path": rel, "text": text[:4000], "lines": user_lines})
        if name in MANIFESTS or name.endswith(MANIFEST_SUFFIXES):
            dep_tokens |= manifest_tokens(text)
        if ext in F.LANG_EXT or ext in CONFIG_EXT or name.startswith("Dockerfile"):
            body = text[:200_000]
            for sname, pats in compiled.items():
                for p in pats:
                    if p.search(body):
                        pattern_files[sname].add(rel)
                        break

    result = {}
    for sname, s in skills.items():
        score, evidence = 0.0, []
        lines = sum(ext_lines.get(e, 0) for e in s.get("extensions", []))
        if lines:
            score += min(4.0, lines / 100)
            evidence.append(f"{lines} authored lines")
        deps = dep_match(s.get("dependencies", []), dep_tokens)
        if deps:
            score += min(6.0, 3.0 * len(deps))
            evidence.append("dependency: " + ", ".join(deps[:4]))
        hits = file_hits.get(sname, [])
        if hits:
            score += min(4.0, 2.0 * len(hits))
            evidence.append("file: " + ", ".join(hits[:3]))
        pf = pattern_files.get(sname, set())
        if pf:
            score += min(3.0, float(len(pf)))
            evidence.append(f"code patterns in {len(pf)} file(s)")
        if score > 0:
            result[sname] = {"confidence": round(min(1.0, score / 4.0), 2), "category": s["category"],
                             "evidence": evidence}

    samples.sort(key=lambda x: -x["lines"])
    return {
        "repo": repo.name,
        "authored_commits": commits,
        "files_analysed": analysed,
        "skipped": dict(skipped),
        "languages": dict(lang_lines.most_common()),
        "skills": dict(sorted(result.items(), key=lambda kv: -kv[1]["confidence"])),
        "code_samples": samples[:max_samples],
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("repo", help="local path or git URL")
    ap.add_argument("--author", action="append", default=[], help="email, name or GitHub handle (repeatable)")
    ap.add_argument("--min-share", type=float, default=0.5)
    ap.add_argument("--precise", action="store_true", help="confirm authorship with git blame")
    ap.add_argument("--templates", help="file of known scaffold hashes (see make_template_hashes.py)")
    ap.add_argument("--taxonomy", default=str(HERE / "skills.json"))
    ap.add_argument("--keep-code", action="store_true", help="include code samples in the output")
    ap.add_argument("-o", "--out")
    a = ap.parse_args()

    tmp = None
    repo = a.repo
    if re.match(r"^(https?://|git@)", repo):
        tmp = tempfile.mkdtemp()
        subprocess.run(["git", "clone", "--quiet", repo, tmp], check=True)
        repo = tmp
    hashes = None
    if a.templates:
        hashes = set(Path(a.templates).read_text().split())
    res = scan_repo(repo, [x.lower() for x in a.author], load_taxonomy(a.taxonomy), a.min_share,
                    a.precise, hashes)
    if not a.keep_code:
        res.pop("code_samples")
    out = json.dumps(res, indent=2)
    if a.out:
        Path(a.out).write_text(out)
    else:
        print(out)


if __name__ == "__main__":
    sys.exit(main())
