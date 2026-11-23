"""Train the code -> skill-area model.

    python train_classifier.py --data data/train.jsonl --backend tfidf --out models/code_skill.joblib
    python train_classifier.py --data data/train.jsonl --backend embeddings   # needs torch + transformers

Evaluation splits by repo so files from one repo never sit in both train and test.
"""
import argparse
import json
from pathlib import Path

from sklearn.metrics import classification_report
from sklearn.model_selection import GroupShuffleSplit

from codemodel import CodeSkillModel


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--data", default="data/train.jsonl")
    ap.add_argument("--backend", choices=["tfidf", "embeddings"], default="tfidf")
    ap.add_argument("--encoder", default="microsoft/unixcoder-base")
    ap.add_argument("--out", default="models/code_skill.joblib")
    a = ap.parse_args()

    rows = [json.loads(l) for l in Path(a.data).read_text(encoding="utf-8").splitlines() if l.strip()]
    texts = [r["text"] for r in rows]
    labels = [r["label"] for r in rows]
    groups = [r["repo"] for r in rows]
    print(f"{len(rows)} files, {len(set(groups))} repos, {len(set(labels))} areas")

    tr, te = next(GroupShuffleSplit(n_splits=1, test_size=0.2, random_state=1).split(texts, labels, groups))
    model = CodeSkillModel(a.backend, a.encoder).fit([texts[i] for i in tr], [labels[i] for i in tr])
    pred = [model.labels[i] for i in model.predict_proba([texts[i] for i in te]).argmax(1)]
    print(classification_report([labels[i] for i in te], pred, zero_division=0))

    final = CodeSkillModel(a.backend, a.encoder).fit(texts, labels)   # refit on everything
    Path(a.out).parent.mkdir(parents=True, exist_ok=True)
    final.save(a.out)
    print("saved", a.out)


if __name__ == "__main__":
    main()
