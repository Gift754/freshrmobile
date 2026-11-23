package com.freshr.app.data

import com.freshr.app.BuildConfig
import com.freshr.admin.model.JobPosting
import com.freshr.app.model.StudentJobMatch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Results returned to the ViewModels. Your own model classes are only built where the shape is known. */
data class JobAnalysis(
    val requiredSkills: List<String>,
    val topMatchedSkills: List<String>,
    val averageMatchScore: Int,
    val scoreLabel: String,
    val candidatesScored: Int,
)

data class ApplicantInput(val applicantId: String, val name: String = "", val cvText: String = "")

data class RankedApplicant(
    val applicantId: String,
    val name: String,
    val individualMatchScore: Int,
    val suggestedStatus: String,
    val matchedSkills: List<String>,
    val missingSkills: List<String>,
    val appliedAtEpochMs: Long?,
)

data class GitHubSyncResult(val coreSkills: List<String>)

/**
 * Single entry point for all AI/dataset calls. Every function returns kotlin.Result, never throws
 * (except CancellationException), so ViewModels can do `.onSuccess { } .onFailure { }`.
 */
class JobMatchRepository(
    private val api: FreshrApi,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private suspend fun <T> call(block: suspend () -> T): Result<T> = withContext(dispatcher) {
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---------------- Admin (FreshrViewModel) ----------------

    /** Hooks 1 + 2: createJob / updateJob. Upserts the job on the server and returns skills + average score. */
    suspend fun analyzeJob(job: JobPosting): Result<JobAnalysis> = call {
        api.putJob(job.id.toString(), job.toDto()).toDomain()
    }

    /**
     * Hook 3: rankApplicantsForJob. Pass an empty list to rank everyone who applied through the app.
     * Pass cvText per applicant to include skills claimed in their CV (counted at half weight vs GitHub-verified).
     * Result is sorted best first; suggestedStatus is Shortlisted / Under Review / Rejected.
     */
    suspend fun rankApplicants(
        jobId: String,
        applicants: List<ApplicantInput> = emptyList(),
    ): Result<List<RankedApplicant>> = call {
        api.rankApplicants(jobId, RankRequestDto(applicants.map { ApplicantDto(it.applicantId, it.name, it.cvText) }))
            .map { it.toDomain() }
    }

    /** Hook 4: dashboard counters (CVs Generated, Applied Jobs). Pass studentId for the student dashboard. */
    suspend fun dashboardStats(studentId: String? = null): Result<StatsDto> = call { api.stats(studentId) }

    /** Hook 5: placement rate per skill-match tier and CV performance lift. Values are null until real data exists. */
    suspend fun reports(): Result<ReportsDto> = call { api.reports() }

    /** Record whether an application ended in placement (feeds the placement-rate report). */
    suspend fun recordOutcome(applicationId: Int, placed: Boolean): Result<Unit> = call {
        api.outcome(applicationId, OutcomeDto(if (placed) "placed" else "not_placed"))
        Unit
    }

    /** Call when an employer opens an application's CV (feeds the AI-vs-manual CV lift report). */
    suspend fun logCvViewed(applicationId: Int): Result<Unit> = call {
        api.event(EventDto(kind = "cv_viewed", applicationId = applicationId))
        Unit
    }

    // ---------------- Student (StudentViewModel) ----------------

    /** Hook 11 helper: also usable from the student side after sign-up. */
    suspend fun registerUser(
        studentId: String,
        name: String?,
        skills: List<String> = emptyList(),
        interests: String = "",
    ): Result<List<String>> = call {
        api.register(RegisterDto(studentId, name, skills, interests)).initialSkills
    }

    /**
     * Hook 6: syncGitHubProfile. The server clones the student's public, non-fork repos in the background,
     * keeps only files they wrote, and returns verified skills. We poll because this can take minutes.
     * The student must be registered first (registerUser).
     */
    suspend fun syncGitHubProfile(
        studentId: String,
        githubUrl: String,
        authorEmails: List<String> = emptyList(),
        timeoutMs: Long = 180_000L,
        pollMs: Long = 2_000L,
    ): Result<GitHubSyncResult> = call {
        api.startGitHubSync(studentId, GitHubSyncRequestDto(githubUrl, authorEmails))
        val last = withTimeoutOrNull(timeoutMs) {
            var s = api.githubSyncStatus(studentId)
            while (s.status == "running") {
                delay(pollMs)
                s = api.githubSyncStatus(studentId)
            }
            s
        } ?: error("GitHub sync timed out, try again in a minute")
        if (last.status != "done") error(last.error ?: "GitHub sync failed")
        GitHubSyncResult(last.coreSkills)
    }

    /** Hook 7: topMatches. matchPercentage 0-100, sorted descending. */
    suspend fun topMatches(studentId: String, limit: Int = 20): Result<List<StudentJobMatch>> = call {
        api.matches(studentId, limit).map { it.toDomain() }
    }

    /** Hook 8: customizeResumeWithAI. Returns the text for generatedCvPreview. */
    suspend fun customizeResumeWithAI(studentId: String, jobId: String, prompt: String): Result<String> = call {
        api.tailorCv(TailorRequestDto(studentId, jobId, prompt)).generatedCvPreview
    }

    /** Hook 9: exportTailoredCvPdf. Returns PDF bytes; write them to a file/MediaStore. The export is logged server-side. */
    suspend fun exportTailoredCvPdf(
        studentId: String,
        jobId: String?,
        cvText: String,
        aiTailored: Boolean = true,
    ): Result<ByteArray> = call {
        api.cvPdf(PdfRequestDto(studentId, jobId, cvText, aiTailored)).use { it.bytes() }
    }

    /** Submit an application; the server stores the match score and tier used by the reports. */
    suspend fun applyToJob(studentId: String, jobId: String, aiTailored: Boolean): Result<ApplyResultDto> = call {
        api.apply(ApplyRequestDto(jobId, studentId, aiTailored))
    }

    // ---------------- Onboarding (OnboardingViewModel) ----------------

    /** Hook 10: searchJobsFromLanding. Accepts free text or a comma-separated skill list. */
    suspend fun searchJobsFromLanding(query: String, limit: Int = 10): Result<List<StudentJobMatch>> = call {
        api.search(query, limit).map { it.toDomain() }
    }
}

// ---------------- Mapping ----------------
// Assumes JobPosting / StudentJobMatch use exactly the field names you listed. If your id is Int or Long,
// change `id = id` in toDomain() to `id = id.toInt()` / `id.toLong()`.
private fun JobPosting.toDto() = JobInDto(
    position = position,
    company = company,
    description = description,
    salary = salary,
    location = location,
    employmentType = employmentType,
    experienceItems = experienceItems,
)

private fun JobAnalysisDto.toDomain() =
    JobAnalysis(requiredSkills, topMatchedSkills, averageMatchScore, scoreLabel, candidatesScored)

private fun RankedApplicantDto.toDomain() = RankedApplicant(
    applicantId, name, individualMatchScore, suggestedStatus, matchedSkills, missingSkills, appliedAtEpochMs,
)

private fun StudentMatchDto.toDomain() = StudentJobMatch(
    id = id,
    position = position,
    company = company,
    location = location,
    employmentType = employmentType,
    salary = salary,
    description = description,
    experience = experience,
    matchPercentage = matchPercentage,
)

/** App-wide singleton so ViewModels can share one OkHttp client. Set the two fields in build.gradle (see INTEGRATION.md). */
object FreshrServices {
    val repository: JobMatchRepository by lazy {
        JobMatchRepository(FreshrNetwork.create(BuildConfig.FRESHR_BASE_URL, BuildConfig.FRESHR_API_KEY))
    }
}
