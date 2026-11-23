"""Code -> skill-area classifier.

backend="tfidf"       fast, CPU only, learns identifier/API vocabulary from code
backend="embeddings"  code-model embeddings (default microsoft/unixcoder-base) + logistic regression;
                      needs torch + transformers, better on code without obvious keywords
"""
import joblib
import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression


class CodeSkillModel:
    def __init__(self, backend="tfidf", encoder="microsoft/unixcoder-base"):
        self.backend = backend
        self.encoder = encoder
        self.vec = None
        self.clf = None
        self.labels = []
        self._tok = None
        self._enc = None

    def _load_encoder(self):
        if self._enc is None:
            import torch
            from transformers import AutoModel, AutoTokenizer
            self._torch = torch
            self._dev = "cuda" if torch.cuda.is_available() else "cpu"
            self._tok = AutoTokenizer.from_pretrained(self.encoder)
            self._enc = AutoModel.from_pretrained(self.encoder).to(self._dev).eval()

    def _embed(self, texts, batch=16):
        self._load_encoder()
        torch, out = self._torch, []
        for i in range(0, len(texts), batch):
            enc = self._tok(texts[i:i + batch], padding=True, truncation=True, max_length=512,
                            return_tensors="pt").to(self._dev)
            with torch.no_grad():
                h = self._enc(**enc).last_hidden_state
            m = enc["attention_mask"].unsqueeze(-1).float()
            v = (h * m).sum(1) / m.sum(1)
            out.append(torch.nn.functional.normalize(v, dim=1).cpu().numpy())
        return np.vstack(out)

    def _features(self, texts, fit=False):
        if self.backend == "tfidf":
            if fit:
                self.vec = TfidfVectorizer(token_pattern=r"[A-Za-z_][A-Za-z0-9_]+", lowercase=False,
                                           ngram_range=(1, 2), min_df=2, max_features=200_000,
                                           sublinear_tf=True)
                return self.vec.fit_transform(texts)
            return self.vec.transform(texts)
        return self._embed(texts)

    def fit(self, texts, labels):
        X = self._features(texts, fit=True)
        self.clf = LogisticRegression(max_iter=2000, class_weight="balanced", C=4.0).fit(X, labels)
        self.labels = list(self.clf.classes_)
        return self

    def predict_proba(self, texts):
        return self.clf.predict_proba(self._features(texts))

    def save(self, path):
        joblib.dump({"backend": self.backend, "encoder": self.encoder, "vec": self.vec,
                     "clf": self.clf, "labels": self.labels}, path)

    @classmethod
    def load(cls, path):
        d = joblib.load(path)
        m = cls(d["backend"], d["encoder"])
        m.vec, m.clf, m.labels = d["vec"], d["clf"], d["labels"]
        return m
