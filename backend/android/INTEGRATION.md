# Connecting the Freshr backend to the Android app

## 1. Architecture decision: Python backend (FastAPI), not on-device
Run inference on a server. Reasons specific to Freshr:
- **GitHub sync (hook 6)** needs `git clone`, full commit history (to tell which files the student wrote) and minutes of CPU. Not feasible on a phone.
- **CV tailoring (hook 8)** needs an LLM and a secret API key. A key shipped inside an APK can be extracted.
- **Admin hooks (1-3, 5)** compare one job against *all* candidates and aggregate across users. That needs one shared database.
- The trained code classifier is small (TF-IDF + logistic regression), so a `.onnx/.tflite` export would work, but it would only ever be a piece of hook 6, which must run on the server anyway.
Optional later: ship `skills.json` in `assets/` so the landing-page search (hook 10) can work offline.

## 2. Data and model pipeline (run once, on the server machine)
    pip install -r requirements-api.txt
    export GITHUB_TOKEN=...                                   # for collection and GitHub sync rate limits
    python collect_training_data.py --repos-per-area 40 --out data/train.jsonl
    python train_classifier.py --data data/train.jsonl --out models/code_skill.joblib
    export FRESHR_API_KEY=<long random string>
    export ANTHROPIC_API_KEY=<key>                            # enables real CV tailoring (else a plain template is returned)
    uvicorn api_server:app --host 0.0.0.0 --port 8000
Or build the included `Dockerfile` (mount a volume at `/data` so the SQLite file survives restarts).
Interactive docs for every endpoint: `http://<server>:8000/docs`.

| Hook | Repository function | Endpoint |
|---|---|---|
| 1, 2 createJob / updateJob | `analyzeJob(job)` | `PUT /jobs/{id}` |
| 3 rankApplicantsForJob | `rankApplicants(jobId)` | `POST /jobs/{id}/rank` |
| 4 dashboard | `dashboardStats(studentId?)` | `GET /stats` |
| 5 reports | `reports()` | `GET /reports` |
| 6 syncGitHubProfile | `syncGitHubProfile(...)` | `POST/GET /students/{id}/github-sync` |
| 7 topMatches | `topMatches(studentId)` | `GET /students/{id}/matches` |
| 8 customizeResumeWithAI | `customizeResumeWithAI(...)` | `POST /cv/tailor` |
| 9 exportTailoredCvPdf | `exportTailoredCvPdf(...)` | `POST /cv/pdf` |
| 10 searchJobsFromLanding | `searchJobsFromLanding(q)` | `GET /search` |
| 11 registerUser | `registerUser(...)` | `POST /students/register` |

The server is the store of record for jobs, students and applications. Hooks 4 and 5 only show real numbers if the app also calls `applyToJob`, `recordOutcome` and `logCvViewed`. Until then they return zeros/nulls (no mock values).

## 3. Gradle setup
`app/build.gradle.kts`:
    plugins { id("org.jetbrains.kotlin.plugin.serialization") }   // same version as your Kotlin plugin
    android {
        buildFeatures { buildConfig = true }
        defaultConfig {
            buildConfigField("String", "FRESHR_BASE_URL", "\"${project.findProperty("FRESHR_BASE_URL") ?: "http://10.0.2.2:8000/"}\"")
            buildConfigField("String", "FRESHR_API_KEY", "\"${project.findProperty("FRESHR_API_KEY") ?: ""}\"")
        }
    }
    dependencies {
        implementation("com.squareup.retrofit2:retrofit:2.11.0")
        implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")
        implementation("com.squareup.okhttp3:okhttp:4.12.0")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    }
Put `FRESHR_BASE_URL` and `FRESHR_API_KEY` in `~/.gradle/gradle.properties` (not in git). `10.0.2.2` is the emulator's alias for your laptop; it needs cleartext HTTP in debug builds only (use HTTPS in production). Add `<uses-permission android:name="android.permission.INTERNET"/>` to the manifest.

Copy `FreshrApi.kt` and `JobMatchRepository.kt` into `app/src/main/java/com/freshr/app/data/`.

## 4. Wiring the ViewModels (no Compose screen changes)
I have not seen your ViewModel code, so treat these as patterns; adapt names.

    private val repo = FreshrServices.repository

**FreshrViewModel**
    // hooks 1 & 2: replace the mock scoring inside createJob / updateJob
    viewModelScope.launch {
        repo.analyzeJob(job).onSuccess { a ->
            val scored = job.copy(topMatchedSkills = a.topMatchedSkills,
                                  averageMatchScore = a.averageMatchScore, scoreLabel = a.scoreLabel)
            // save `scored` into your jobs state
        }.onFailure { /* keep job with defaults, surface an error */ }
    }
    // hook 3
    repo.rankApplicants(job.id.toString()).onSuccess { ranked ->
        // map to Applicant(name = r.name, status = r.suggestedStatus, individualMatchScore = r.individualMatchScore,
        //                  appliedDate/appliedTime formatted from r.appliedAtEpochMs)
    }
    // hooks 4 & 5
    repo.dashboardStats()   // StatsDto: cvsGenerated, appliedJobs
    repo.reports()          // tiers[].placementRate, cvPerformance.liftPercent (nullable)
Every job that already exists in the app must be sent once through `analyzeJob` (loop over them on first launch), because matching runs against the server's copy.

**StudentViewModel**
    repo.syncGitHubProfile(studentId, githubUrl).onSuccess { coreSkills = it.coreSkills }   // hook 6
    repo.topMatches(studentId).onSuccess { topMatches = it }                                // hook 7 (re-run after sync)
    repo.customizeResumeWithAI(studentId, job.id, cvPromptInput).onSuccess { generatedCvPreview = it }   // hook 8
    repo.exportTailoredCvPdf(studentId, job.id, generatedCvPreview).onSuccess { bytes -> /* save to file / MediaStore */ }  // hook 9
    repo.applyToJob(studentId, job.id, aiTailored = true)

**OnboardingViewModel**
    repo.searchJobsFromLanding(query).onSuccess { results = it }                           // hook 10
    repo.registerUser(newUserId, name, skills = selectedSkills, interests = freeText)       // hook 11

## 5. Things to know before launch
- **Auth:** the shared `X-API-Key` is fine for development only. Before real users, verify a per-user token (for example Firebase Auth ID tokens) on the server and derive `studentId` from it.
- **Privacy:** GitHub scanning reads public repos, and CV tailoring sends the student's skills and projects to an LLM provider. Get explicit consent in the UI and review the Kenya Data Protection Act requirements.
- **Cold start:** a brand-new student has only the skills they declared at sign-up (counted at half weight). Matches improve after GitHub sync.
- **Scores are skill-overlap based, not CV embeddings.** The CV is used by extracting skills from its text. If you want true embedding similarity later, add it inside `api_core.score_pair`.
- **Thresholds:** `Shortlisted` at 70+, `Rejected` below 40 (`FRESHR_SHORTLIST_AT`, `FRESHR_REJECT_BELOW`). Match your own status names in the ViewModel mapping.
- **Not compiled:** I could not build the Kotlin here. Fix any import/name mismatch against your `model` package.
