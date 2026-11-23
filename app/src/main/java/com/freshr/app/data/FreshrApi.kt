package com.freshr.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import java.util.concurrent.TimeUnit

// ---------- DTOs (field names match the FastAPI JSON exactly) ----------
@Serializable
data class JobInDto(
    val position: String,
    val company: String = "",
    val description: String = "",
    val salary: String = "",
    val location: String = "",
    val employmentType: String = "",
    val experienceItems: List<String> = emptyList(),
    val requiredSkills: List<String>? = null,
)

@Serializable
data class JobAnalysisDto(
    val id: String,
    val requiredSkills: List<String>,
    val topMatchedSkills: List<String>,
    val averageMatchScore: Int,
    val scoreLabel: String,
    val candidatesScored: Int,
)

@Serializable
data class RegisterDto(
    val studentId: String,
    val name: String? = null,
    val skills: List<String> = emptyList(),
    val interests: String = "",
)

@Serializable
data class RegisterResultDto(val ok: Boolean = true, val initialSkills: List<String> = emptyList())

@Serializable
data class GitHubSyncRequestDto(val githubUrl: String, val authorEmails: List<String> = emptyList())

@Serializable
data class SyncStatusDto(
    val status: String,                       // none | running | done | error
    val error: String? = null,
    val coreSkills: List<String> = emptyList(),
)

@Serializable
data class StudentMatchDto(
    val id: String,
    val position: String,
    val company: String,
    val location: String,
    val employmentType: String,
    val salary: String,
    val description: String,
    val experience: List<String>,
    val matchPercentage: Int,
)

@Serializable
data class ApplicantDto(val applicantId: String, val name: String = "", val cvText: String = "")

@Serializable
data class RankRequestDto(val applicants: List<ApplicantDto> = emptyList())

@Serializable
data class RankedApplicantDto(
    val applicantId: String,
    val name: String,
    val individualMatchScore: Int,
    val suggestedStatus: String,              // Shortlisted | Under Review | Rejected
    val matchedSkills: List<String>,
    val missingSkills: List<String>,
    val appliedAtEpochMs: Long? = null,
)

@Serializable
data class TailorRequestDto(val studentId: String, val jobId: String, val prompt: String = "")

@Serializable
data class TailorResponseDto(val generatedCvPreview: String)

@Serializable
data class PdfRequestDto(
    val studentId: String,
    val jobId: String? = null,
    val text: String,
    val aiTailored: Boolean = true,
)

@Serializable
data class ApplyRequestDto(val jobId: String, val studentId: String, val aiTailored: Boolean = false)

@Serializable
data class ApplyResultDto(val applicationId: Int, val matchPercentage: Int, val tier: String)

@Serializable
data class OutcomeDto(val outcome: String)   // placed | not_placed

@Serializable
data class EventDto(
    val kind: String,                         // cv_generated | cv_exported | cv_viewed
    val studentId: String? = null,
    val jobId: String? = null,
    val applicationId: Int? = null,
)

@Serializable
data class OkDto(val ok: Boolean = true)

@Serializable
data class StatsDto(
    val cvsGenerated: Int,
    val appliedJobs: Int,
    val activeJobs: Int,
    val registeredCandidates: Int,
)

@Serializable
data class TierReportDto(
    val tier: String,                         // "Met all required skills" | "Partial skill match" | "Low skill match"
    val applications: Int,
    val placed: Int,
    val decided: Int,
    val placementRate: Double? = null,        // null until at least one outcome is recorded
)

@Serializable
data class CvPerformanceDto(
    val aiTailoredAvgViews: Double? = null,
    val manualAvgViews: Double? = null,
    val liftPercent: Double? = null,
    val aiApplications: Int = 0,
    val manualApplications: Int = 0,
)

@Serializable
data class ReportsDto(val tiers: List<TierReportDto>, val cvPerformance: CvPerformanceDto)

// ---------- Retrofit interface ----------
interface FreshrApi {
    @POST("students/register") suspend fun register(@Body body: RegisterDto): RegisterResultDto
    @POST("students/{id}/github-sync")
    suspend fun startGitHubSync(@Path("id") id: String, @Body body: GitHubSyncRequestDto): SyncStatusDto
    @GET("students/{id}/github-sync") suspend fun githubSyncStatus(@Path("id") id: String): SyncStatusDto
    @GET("students/{id}/matches")
    suspend fun matches(@Path("id") id: String, @Query("limit") limit: Int = 20): List<StudentMatchDto>

    @PUT("jobs/{id}") suspend fun putJob(@Path("id") id: String, @Body job: JobInDto): JobAnalysisDto
    @POST("jobs/{id}/rank")
    suspend fun rankApplicants(@Path("id") id: String, @Body body: RankRequestDto): List<RankedApplicantDto>

    @GET("search") suspend fun search(@Query("q") q: String, @Query("limit") limit: Int = 10): List<StudentMatchDto>

    @POST("cv/tailor") suspend fun tailorCv(@Body body: TailorRequestDto): TailorResponseDto
    @Streaming @POST("cv/pdf") suspend fun cvPdf(@Body body: PdfRequestDto): ResponseBody

    @POST("applications") suspend fun apply(@Body body: ApplyRequestDto): ApplyResultDto
    @POST("applications/{id}/outcome") suspend fun outcome(@Path("id") id: Int, @Body body: OutcomeDto): OkDto
    @POST("events") suspend fun event(@Body body: EventDto): OkDto

    @GET("stats") suspend fun stats(@Query("studentId") studentId: String? = null): StatsDto
    @GET("reports") suspend fun reports(): ReportsDto
}

// ---------- Network factory ----------
object FreshrNetwork {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun create(baseUrl: String, apiKey: String): FreshrApi {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().header("X-API-Key", apiKey).build())
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)      // CV tailoring calls an LLM
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)                       // must end with "/"
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(FreshrApi::class.java)
    }
}
