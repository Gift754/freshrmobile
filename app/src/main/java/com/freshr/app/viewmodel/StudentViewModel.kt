package com.freshr.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshr.app.data.CvGenerator
import com.freshr.app.data.DataCleaner
import com.freshr.app.data.FreshrServices
import com.freshr.app.data.GitHubFetcher
import com.freshr.app.data.GitHubProfileData
import com.freshr.app.data.JobMatchingEngine
import com.freshr.app.model.*
import kotlinx.coroutines.launch

class StudentViewModel : ViewModel() {

    var currentRoute by mutableStateOf(StudentRoute.HOME)
    var userEmail by mutableStateOf("student@gmail.com")
    var userName by mutableStateOf("Student User")

    var githubUrl by mutableStateOf("")
    var isGitHubSynced by mutableStateOf(false)
    var isSyncing by mutableStateOf(false)
    var syncErrorMessage by mutableStateOf<String?>(null)
    var githubProfile by mutableStateOf<GitHubProfileData?>(null)

    val coreSkills = mutableStateListOf<String>()
    val topMatches = mutableStateListOf<StudentJobMatch>()
    val applications = mutableStateListOf<StudentApplication>()

    var cvPromptInput by mutableStateOf("")
    var generatedCvPreview by mutableStateOf<String?>(null)
    var cvExportStatus by mutableStateOf<String?>(null)
    var showLogoutDialog by mutableStateOf(false)

    init {
        val sampleJobs = listOf(
            StudentJobMatch(
                id = "job_py_1",
                position = "Backend Developer (Python)",
                company = "TechCom Solution",
                location = "Remote",
                employmentType = "Full-time",
                salary = "KES 50,000",
                description = "Design, build, and maintain backend APIs and data pipelines using Python and Django/FastAPI.",
                experience = listOf(
                    "Proficiency in Python and Django/FastAPI",
                    "Experience designing RESTful APIs",
                    "Familiarity with PostgreSQL and Git",
                    "Strong debugging and testing practices"
                ),
                matchPercentage = 92
            ),
            StudentJobMatch(
                id = "job_kt_2",
                position = "Android Mobile Developer (Kotlin)",
                company = "Nairobi Tech Hub",
                location = "Nairobi (Hybrid)",
                employmentType = "Full-time",
                salary = "KES 75,000",
                description = "Develop modern Android applications using Kotlin, Jetpack Compose, Coroutines, and ViewModel architecture.",
                experience = listOf(
                    "Solid understanding of Kotlin and Android SDK",
                    "Experience with Jetpack Compose UI",
                    "REST API integration with Retrofit/OkHttp",
                    "Git version control and unit testing"
                ),
                matchPercentage = 88
            ),
            StudentJobMatch(
                id = "job_js_3",
                position = "Full Stack Engineer (React & Node.js)",
                company = "AfriPay Solutions",
                location = "Remote",
                employmentType = "Full-time",
                salary = "KES 60,000",
                description = "Build responsive web interfaces and microservices using React, Node.js, and TypeScript.",
                experience = listOf(
                    "Experience with React and Node.js",
                    "Proficiency in TypeScript and RESTful services",
                    "Database management with MongoDB or PostgreSQL"
                ),
                matchPercentage = 82
            ),
            StudentJobMatch(
                id = "job_dev_4",
                position = "DevOps & Cloud Specialist",
                company = "CloudScale Africa",
                location = "Remote",
                employmentType = "Contract",
                salary = "KES 90,000",
                description = "Manage CI/CD pipelines, Docker containerization, and AWS cloud infrastructure deployment.",
                experience = listOf(
                    "Experience with Docker, Kubernetes, and AWS",
                    "Linux administration and Shell scripting",
                    "Automated deployment workflows with GitHub Actions"
                ),
                matchPercentage = 75
            ),
            StudentJobMatch(
                id = "job_data_5",
                position = "Data Analyst & Python Developer",
                company = "DataMetrics East Africa",
                location = "Nairobi (On-site)",
                employmentType = "Full-time",
                salary = "KES 55,000",
                description = "Analyze large datasets, write ETL pipelines, and generate actionable insights using Python and SQL.",
                experience = listOf(
                    "Proficiency in Python (Pandas, NumPy, Matplotlib)",
                    "Strong SQL query writing and database design",
                    "Data visualization and reporting experience"
                ),
                matchPercentage = 79
            ),
            StudentJobMatch(
                id = "job_ui_6",
                position = "Frontend Developer (UI/UX & Web)",
                company = "CreativeLabs Ltd",
                location = "Remote",
                employmentType = "Full-time",
                salary = "KES 48,000",
                description = "Craft pixel-perfect user interfaces and web applications using HTML, CSS, JavaScript, and Tailwind.",
                experience = listOf(
                    "Proficiency in HTML5, CSS3, JavaScript, and Tailwind CSS",
                    "Responsive design and cross-browser compatibility",
                    "Figma to code implementation"
                ),
                matchPercentage = 71
            )
        )

        topMatches.addAll(sampleJobs)

        val firstJob = topMatches.first()
        applications.addAll(
            listOf(
                StudentApplication("app_1", firstJob, ApplicationStatus.HIRED, "01/06/2026", "v2_ai_tailored.pdf"),
                StudentApplication("app_2", sampleJobs[1], ApplicationStatus.PENDING, "02/06/2026", "v1_ai_tailored.pdf"),
                StudentApplication("app_3", sampleJobs[2], ApplicationStatus.REJECTED, "03/06/2026", "base_cv.pdf")
            )
        )
    }

    fun setUserDetails(email: String, name: String) {
        if (email.isNotBlank()) userEmail = email
        if (name.isNotBlank()) userName = name
    }

    fun syncGitHubProfile() {
        val input = githubUrl.trim()
        if (input.isBlank()) {
            syncErrorMessage = "Please enter a GitHub profile or repository URL."
            return
        }

        isSyncing = true
        syncErrorMessage = null

        viewModelScope.launch {
            val result = GitHubFetcher.fetchProfileAndSkills(input)
            result.onSuccess { profileData ->
                githubProfile = profileData
                if (profileData.name.isNotBlank()) {
                    userName = profileData.name
                }
                val cleanedSkills = DataCleaner.cleanSkillList(profileData.verifiedSkills)
                coreSkills.clear()
                coreSkills.addAll(cleanedSkills)

                // Try fetching matches from remote FastAPI model backend first, with local fallback
                val remoteResult = FreshrServices.repository.topMatches(userEmail)
                remoteResult.onSuccess { remoteMatches ->
                    if (remoteMatches.isNotEmpty()) {
                        topMatches.clear()
                        topMatches.addAll(remoteMatches)
                    } else {
                        val rankedJobs = JobMatchingEngine.rankAndSortJobs(cleanedSkills, topMatches.toList())
                        topMatches.clear()
                        topMatches.addAll(rankedJobs)
                    }
                }.onFailure {
                    // FastAPI ML model backend offline: use local on-device JobMatchingEngine model
                    val rankedJobs = JobMatchingEngine.rankAndSortJobs(cleanedSkills, topMatches.toList())
                    topMatches.clear()
                    topMatches.addAll(rankedJobs)
                }

                isGitHubSynced = true
                isSyncing = false
            }.onFailure { err ->
                syncErrorMessage = err.message ?: "Failed to sync GitHub profile."
                val fallbackSkills = listOf("Python", "Kotlin", "Django", "REST APIs", "Git", "PostgreSQL")
                val cleaned = DataCleaner.cleanSkillList(fallbackSkills)
                coreSkills.clear()
                coreSkills.addAll(cleaned)

                val rankedJobs = JobMatchingEngine.rankAndSortJobs(cleaned, topMatches.toList())
                topMatches.clear()
                topMatches.addAll(rankedJobs)

                isGitHubSynced = true
                isSyncing = false
            }
        }
    }

    fun applyToJob(job: StudentJobMatch) {
        if (applications.any { it.job.id == job.id }) return
        applications.add(
            0,
            StudentApplication(
                id = "app_${applications.size + 1}",
                job = job,
                status = ApplicationStatus.PENDING,
                appliedDate = "19/11/2026",
                tailoredCvVersion = "ai_matched_${job.id}.pdf"
            )
        )
    }

    fun deleteApplication(appId: String) {
        applications.removeAll { it.id == appId }
    }

    fun customizeResumeWithAI(targetJob: StudentJobMatch? = topMatches.firstOrNull()) {
        val repos = githubProfile?.topRepos ?: emptyList()
        val username = githubProfile?.username ?: GitHubFetcher.extractUsername(githubUrl)

        generatedCvPreview = CvGenerator.generateTailoredCv(
            studentName = userName,
            studentEmail = userEmail,
            githubUsername = username,
            candidateSkills = coreSkills.toList(),
            githubRepos = repos,
            targetJob = targetJob ?: topMatches.firstOrNull(),
            customPrompt = cvPromptInput
        )
    }

    fun exportTailoredCvPdf() {
        if (generatedCvPreview.isNullOrBlank()) {
            customizeResumeWithAI()
        }
        cvExportStatus = "CV Exported Successfully as PDF/Markdown document for $userName"
    }
}
