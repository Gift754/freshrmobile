# Freshr skills pipeline

Two complementary detectors, one matcher.

| Part | File(s) | What it does |
|---|---|---|
| Clean data | `filtering.py`, `scanner.py` | Keeps only files the candidate wrote: drops forks, `node_modules`/`vendor`/`dist`, lockfiles, minified/generated code, bundled libraries, scaffold boilerplate, and files authored by other people (git history). |
| Word/signal skills | `skills.json`, `scanner.py` | ONE file holds every tool/tech skill (MySQL, Docker, React...). Detected from dependency manifests, file names, code patterns and authored line counts, never from README prose. |
| Learned code skills | `collect_training_data.py`, `train_classifier.py`, `codemodel.py` | Classifier that maps code to a skill area (frontend, backend, mobile, data_ml, devops, systems, game_dev, database) without relying on keywords. |
| Candidate profile | `github_profile.py` | GitHub username -> clone own repos -> filtered scan -> aggregated skills + area scores. |
| Matching | `match.py` | Scores jobs against profiles: 0.6 tool-skill coverage + 0.4 code-area similarity. |

## Setup
    pip install -r requirements.txt        # add torch transformers for --backend embeddings
    export GITHUB_TOKEN=...                # needed for training-data collection, helps with rate limits

## Workflow
1. Train the model (once):
       python collect_training_data.py --repos-per-area 40 --out data/train.jsonl
       python train_classifier.py --data data/train.jsonl --backend tfidf --out models/code_skill.joblib
   Use `--backend embeddings` (UniXcoder) once you have a GPU or time; it handles code with no obvious keywords better.
2. (Optional) Ignore scaffold files users never edited:
       python make_template_hashes.py <fresh-scaffold-dirs> > templates.txt
3. Profile a candidate:
       python github_profile.py --user <github-name> --author <their@email> --model models/code_skill.joblib -o profiles/<name>.json
4. Match:
       python match.py --profile profiles/<name>.json --jobs sample_jobs.jsonl
       python match.py --profile profiles/a.json --profile profiles/b.json --jobs sample_jobs.jsonl --job-id j1

Single repo check: `python scanner.py <path-or-url> --author <email-or-handle>`

## Extending
- New skill: add one entry to `skills.json` (category, aliases, files, dependencies, patterns). Nothing else changes.
- Job text is matched with the same `aliases`, so adding aliases improves job parsing too.
- Seed more skills from ESCO, O*NET technology skills or Lightcast Open Skills; map them into the same shape.

## Known limits (be upfront about these in the project)
- Training labels come from repo topics, so they are noisy (weak supervision). Hand-label 200-300 files from real student repos and use them as your test set before trusting scores.
- Authorship uses lines added in git history (`--precise` confirms with `git blame`). Commits under an email you did not pass count as someone else's. Squashed or imported repos lose authorship.
- Dependency detection is token-based on manifests; dependency names in `skills.json` should be reviewed per ecosystem.
- Respect licenses and terms: GitHub API terms for collection, and BrighterMonday/MyJobMag terms before scraping job posts.

## Android app backend
`api_server.py` (FastAPI) + `api_core.py` serve the 11 Freshr app hooks; see `android/INTEGRATION.md` for the Kotlin repository, Gradle setup and ViewModel wiring.
    pip install -r requirements-api.txt && uvicorn api_server:app --port 8000
