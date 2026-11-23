FROM python:3.12-slim
RUN apt-get update && apt-get install -y --no-install-recommends git && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY requirements.txt requirements-api.txt ./
RUN pip install --no-cache-dir -r requirements-api.txt
COPY . .
ENV FRESHR_DB=/data/freshr.db FRESHR_MODEL=/app/models/code_skill.joblib
VOLUME /data
CMD ["uvicorn", "api_server:app", "--host", "0.0.0.0", "--port", "8000"]
