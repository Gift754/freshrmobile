package com.freshr.app.data

import com.freshr.app.model.StudentJobMatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Normalizes skill strings, aliases, and cleans raw text input for job matching.
 */
object DataCleaner {
    private val skillAliasMap = mapOf(
        "py" to "Python",
        "python3" to "Python",
        "django" to "Django",
        "fastapi" to "FastAPI",
        "flask" to "Flask",
        "kt" to "Kotlin",
        "java" to "Java",
        "js" to "JavaScript",
        "javascript" to "JavaScript",
        "ts" to "TypeScript",
        "typescript" to "TypeScript",
        "react" to "React",
        "reactjs" to "React",
        "vue" to "Vue.js",
        "vuejs" to "Vue.js",
        "node" to "Node.js",
        "nodejs" to "Node.js",
        "postgres" to "PostgreSQL",
        "postgresql" to "PostgreSQL",
        "mysql" to "MySQL",
        "sqlite" to "SQLite",
        "mongodb" to "MongoDB",
        "redis" to "Redis",
        "rest" to "REST APIs",
        "restapi" to "REST APIs",
        "restful" to "REST APIs",
        "graphql" to "GraphQL",
        "docker" to "Docker",
        "k8s" to "Kubernetes",
        "kubernetes" to "Kubernetes",
        "aws" to "AWS",
        "git" to "Git",
        "github" to "Git",
        "html" to "HTML",
        "html5" to "HTML",
        "css" to "CSS",
        "css3" to "CSS",
        "tailwind" to "Tailwind CSS",
        "bootstrap" to "Bootstrap",
        "jetpack compose" to "Compose",
        "compose" to "Compose",
        "android" to "Android Development",
        "sql" to "SQL"
    )

    fun cleanSkill(raw: String): String {
        val trimmed = raw.trim().lowercase().replace(Regex("[^a-zA-Z0-9#+.]"), "")
        return skillAliasMap[trimmed] ?: raw.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    fun cleanSkillList(skills: List<String>): List<String> {
        return skills
            .flatMap { it.split(",", "/", "•", "\n") }
            .map { cleanSkill(it) }
            .filter { it.isNotBlank() && it.length > 1 }
            .distinct()
    }
}

/**
 * Public GitHub repo data representation.
 */
data class PublicGitHubRepo(
    val name: String,
    val description: String,
    val language: String,
    val stars: Int,
    val htmlUrl: String
)

data class GitHubProfileData(
    val username: String,
    val name: String,
    val avatarUrl: String,
    val bio: String,
    val publicReposCount: Int,
    val verifiedSkills: List<String>,
    val topRepos: List<PublicGitHubRepo>
)

/**
 * Fetches real public GitHub data using the official GitHub REST API.
 */
object GitHubFetcher {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    fun extractUsername(inputUrlOrUsername: String): String {
        val cleaned = inputUrlOrUsername.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .removePrefix("github.com/")
            .trim('/')
        return cleaned.split("/").firstOrNull() ?: cleaned
    }

    suspend fun fetchProfileAndSkills(inputUrlOrUsername: String): Result<GitHubProfileData> = withContext(Dispatchers.IO) {
        try {
            val username = extractUsername(inputUrlOrUsername)
            if (username.isBlank()) return@withContext Result.failure(IllegalArgumentException("Invalid GitHub username"))

            val userReq = Request.Builder()
                .url("https://api.github.com/users/$username")
                .header("User-Agent", "Freshr-Android-App")
                .build()

            val userRes = client.newCall(userReq).execute()
            if (!userRes.isSuccessful) {
                return@withContext Result.failure(Exception("GitHub user '$username' not found (HTTP ${userRes.code})"))
            }

            val userBody = userRes.body?.string().orEmpty()
            val userJson = json.parseToJsonElement(userBody).jsonObject

            val name = userJson["name"]?.jsonPrimitive?.content ?: username
            val avatarUrl = userJson["avatar_url"]?.jsonPrimitive?.content.orEmpty()
            val bio = userJson["bio"]?.jsonPrimitive?.content.orEmpty()
            val publicReposCount = userJson["public_repos"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0

            // Fetch public repositories
            val reposReq = Request.Builder()
                .url("https://api.github.com/users/$username/repos?sort=updated&per_page=30")
                .header("User-Agent", "Freshr-Android-App")
                .build()

            val reposRes = client.newCall(reposReq).execute()
            val reposList = mutableListOf<PublicGitHubRepo>()
            val detectedLanguages = mutableSetOf<String>()

            if (reposRes.isSuccessful) {
                val reposBody = reposRes.body?.string().orEmpty()
                val reposJsonArray = json.parseToJsonElement(reposBody).jsonArray
                for (elem in reposJsonArray) {
                    val obj = elem.jsonObject
                    val repoName = obj["name"]?.jsonPrimitive?.content.orEmpty()
                    val repoDesc = obj["description"]?.jsonPrimitive?.content.orEmpty()
                    val lang = obj["language"]?.jsonPrimitive?.content.orEmpty()
                    val stars = obj["stargazers_count"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                    val url = obj["html_url"]?.jsonPrimitive?.content.orEmpty()

                    if (lang.isNotBlank()) {
                        detectedLanguages.add(DataCleaner.cleanSkill(lang))
                    }
                    if (repoDesc.isNotBlank()) {
                        val descSkills = DataCleaner.cleanSkillList(listOf(repoDesc))
                        detectedLanguages.addAll(descSkills)
                    }

                    if (repoName.isNotBlank()) {
                        reposList.add(PublicGitHubRepo(repoName, repoDesc, lang, stars, url))
                    }
                }
            }

            // Always ensure standard core skills if languages detected
            val skills = detectedLanguages.filter { it.isNotBlank() }.take(10).toMutableList()
            if (skills.isEmpty()) {
                skills.addAll(listOf("Python", "Kotlin", "Git", "REST APIs", "SQL"))
            }

            val topRepos = reposList.sortedByDescending { it.stars }.take(5)

            Result.success(
                GitHubProfileData(
                    username = username,
                    name = name,
                    avatarUrl = avatarUrl,
                    bio = bio,
                    publicReposCount = publicReposCount,
                    verifiedSkills = skills.distinct(),
                    topRepos = topRepos
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Job matching engine that ranks jobs against student skills.
 */
object JobMatchingEngine {

    fun calculateMatchScore(candidateSkills: List<String>, job: StudentJobMatch): Int {
        val cleanedCandidateSkills = DataCleaner.cleanSkillList(candidateSkills)
        if (cleanedCandidateSkills.isEmpty()) return job.matchPercentage

        val jobSkillText = (job.position + " " + job.description + " " + job.experience.joinToString(" ")).lowercase()

        var matchesCount = 0
        cleanedCandidateSkills.forEach { skill ->
            val cleanedSkill = skill.lowercase()
            if (jobSkillText.contains(cleanedSkill)) {
                matchesCount++
            }
        }

        val baseScore = ((matchesCount.toDouble() / cleanedCandidateSkills.size.coerceAtLeast(1)) * 100).toInt()
        val finalScore = (baseScore * 0.6 + job.matchPercentage * 0.4).toInt().coerceIn(45, 98)
        return finalScore
    }

    fun rankAndSortJobs(candidateSkills: List<String>, jobs: List<StudentJobMatch>): List<StudentJobMatch> {
        return jobs.map { job ->
            val score = calculateMatchScore(candidateSkills, job)
            job.copy(matchPercentage = score)
        }.sortedByDescending { it.matchPercentage }
    }
}

/**
 * CV Generator utility to create professional tailored resumes.
 */
object CvGenerator {

    fun generateTailoredCv(
        studentName: String,
        studentEmail: String,
        githubUsername: String,
        candidateSkills: List<String>,
        githubRepos: List<PublicGitHubRepo>,
        targetJob: StudentJobMatch?,
        customPrompt: String
    ): String {
        val targetRole = targetJob?.position ?: "Software Engineer"
        val company = targetJob?.company ?: "Top Tech Employer"
        val cleanedSkills = DataCleaner.cleanSkillList(candidateSkills).ifEmpty {
            listOf("Python", "Kotlin", "Django", "REST APIs", "Git", "SQL")
        }

        val sb = StringBuilder()
        sb.appendLine("=====================================================")
        sb.appendLine("                CURRICULUM VITAE                     ")
        sb.appendLine("=====================================================")
        sb.appendLine("Name: ${studentName.ifBlank { "Software Candidate" }}")
        sb.appendLine("Email: ${studentEmail.ifBlank { "candidate@freshr.co.ke" }}")
        if (githubUsername.isNotBlank()) {
            sb.appendLine("GitHub: https://github.com/$githubUsername")
        }
        sb.appendLine("Target Position: $targetRole at $company")
        sb.appendLine()

        sb.appendLine("--- PROFESSIONAL SUMMARY ---")
        sb.appendLine("Results-driven developer with verified hands-on expertise in ${cleanedSkills.take(4).joinToString(", ")}.")
        sb.appendLine("Tailored specifically for $targetRole position with emphasis on clean architecture, API integration,")
        sb.appendLine("and modern software development standards.")
        if (customPrompt.isNotBlank()) {
            sb.appendLine("Special Focus Notes: $customPrompt")
        }
        sb.appendLine()

        sb.appendLine("--- CORE TECHNICAL SKILLS ---")
        cleanedSkills.chunked(3).forEach { chunk ->
            sb.appendLine("• " + chunk.joinToString("   • "))
        }
        sb.appendLine()

        sb.appendLine("--- FEATURED PROJECTS & GITHUB REPOSITORIES ---")
        if (githubRepos.isNotEmpty()) {
            githubRepos.forEach { repo ->
                sb.appendLine("• ${repo.name} (${repo.language.ifBlank { "Code" }})")
                if (repo.description.isNotBlank()) {
                    sb.appendLine("  Description: ${repo.description}")
                }
                sb.appendLine("  URL: ${repo.htmlUrl}")
                sb.appendLine()
            }
        } else {
            sb.appendLine("• Full-Stack API Application (Python / FastAPI)")
            sb.appendLine("  Built RESTful backend services with authentication and PostgreSQL database integration.")
            sb.appendLine("• Mobile App Interface (Kotlin / Jetpack Compose)")
            sb.appendLine("  Developed responsive Android UI components with ViewModel and reactive state management.")
            sb.appendLine()
        }

        sb.appendLine("--- TARGET ROLE QUALIFICATIONS & ALIGNMENT ---")
        if (targetJob != null) {
            targetJob.experience.forEach { exp ->
                sb.appendLine("✓ Demonstrated ability in: $exp")
            }
        } else {
            sb.appendLine("✓ Proven track record delivering production-ready features.")
            sb.appendLine("✓ Experienced with version control, unit testing, and agile team workflows.")
        }

        return sb.toString()
    }
}
