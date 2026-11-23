package com.freshr.admin.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.freshr.admin.model.*

class FreshrViewModel : ViewModel() {

    var currentRoute by mutableStateOf(FreshrRoute.HOME)
    var isDarkTheme by mutableStateOf(true)
    var animationsEnabled by mutableStateOf(true)
    var fontScale by mutableStateOf(1.0f)
    var selectedLanguage by mutableStateOf("English")

    val jobs = mutableStateListOf(
        JobPosting(
            id = "job_1",
            position = "Backend Developer",
            description = "Build and maintain scalable REST APIs, database schemas, and server-side services.",
            salary = "Kes 30,000",
            location = "Remote",
            employmentType = "Full-time",
            experienceItems = listOf(
                "2+ years building Python web services",
                "REST API design and relational databases",
                "Version control workflows with Git",
                "Unit testing and cloud deployment"
            ),
            postedDate = "01/06/2026",
            postedTime = "10:07 pm",
            averageMatchScore = 78,
            scoreLabel = "Average",
            topMatchedSkills = listOf("Django", "Python", "Java Script", "HTML")
        )
    )

    val applicants = mutableStateListOf(
        Applicant("James Ochieng", "03/06/2026", "12:09 pm", "Rejected", 42),
        Applicant("Jane Wambua", "03/06/2026", "12:09 pm", "Rejected", 48),
        Applicant("Marryannne Mueni", "03/06/2026", "12:09 pm", "Rejected", 51),
        Applicant("Kevin Richard", "03/06/2026", "12:09 pm", "Rejected", 45)
    )

    val recentActivities = listOf(
        ActivityItem("New Login", "James Logged In", "01/06/2026", "10:07 am"),
        ActivityItem("CV Generated", "Software Engineer CV", "01/06/2026", "10:07 am"),
        ActivityItem("Job Posted", "Backend Developer", "01/06/2026", "10:07 am"),
        ActivityItem("Job Application", "Backend Developer", "01/06/2026", "10:07 am")
    )

    fun createJob(
        position: String,
        description: String,
        salary: String,
        location: String,
        experience: String
    ) {
        // =========================================================================
        // AI INTEGRATION POINT 1: JOB DESCRIPTION PARSING & SKILL EXTRACTION
        // 1. Pass `position`, `description`, and `experience` into your trained NLP
        //    entity-extraction model or dataset vectorizer.
        // 2. Extract canonical technical/soft skills (e.g., ["Django", "Python", ...])
        //    and normalize salary/experience tiers against your historical job dataset.
        // 3. Query your candidate pool embeddings to compute the initial
        //    `averageMatchScore` across registered students/applicants.
        // =========================================================================
        val extractedSkillsFromAI = listOf("Django", "Python", "Java Script", "HTML")
        val predictedAvgScore = 78

        jobs.add(
            0,
            JobPosting(
                id = "job_${jobs.size + 1}",
                position = position,
                description = description,
                salary = salary,
                location = location,
                experienceItems = experience.split("\n").filter { it.isNotBlank() },
                postedDate = "19/11/2026",
                postedTime = "09:39 pm",
                averageMatchScore = predictedAvgScore,
                scoreLabel = if (predictedAvgScore >= 75) "Average" else "Low",
                topMatchedSkills = extractedSkillsFromAI
            )
        )
    }

    fun updateJob(updated: JobPosting) {
        // =========================================================================
        // AI INTEGRATION POINT 2: RE-SCORING ON JOB EDIT
        // 1. Whenever an admin edits `description`, `experienceItems`, or `employmentType`,
        //    re-run cosine similarity / ranking inference against your dataset.
        // 2. Refresh `averageMatchScore` and `topMatchedSkills` dynamically so the
        //    Match Insights card reflects the updated role requirements.
        // =========================================================================
        val index = jobs.indexOfFirst { it.id == updated.id }
        if (index != -1) {
            jobs[index] = updated
        }
    }

    fun rankApplicantsForJob(jobId: String): List<Applicant> {
        // =========================================================================
        // AI INTEGRATION POINT 3: CANDIDATE RANKING & AUTO-SCREENING
        // 1. Feed candidate CV embeddings + linked GitHub repo metrics from your
        //    dataset against `jobId` requirements.
        // 2. Compute `individualMatchScore` (0-100) per applicant and sort descending.
        // 3. Optionally flag threshold cutoffs (e.g., auto-suggest "Shortlisted" vs
        //    "Rejected") based on your placement equity model weights.
        // =========================================================================
        return applicants.sortedByDescending { it.individualMatchScore }
    }
}