package com.freshr.admin.model

import androidx.compose.ui.graphics.Color

val FreshrPrimaryBlue = Color(0xFF1849D6)
val FreshrSecondaryLavender = Color(0xFFE8EDFA)
val FreshrCardBorder = Color(0xFFD0D5DD)
val FreshrRejectedRed = Color(0xFFEF1C1C)
val FreshrActiveGreen = Color(0xFF22C55E)

enum class FreshrRoute(val title: String) {
    HOME("Home"),
    MANAGE_JOBS("Manage Jobs"),
    REPORTS("Reports"),
    SETTINGS("Settings")
}

data class JobPosting(
    val id: String,
    val position: String,
    val company: String = "TechCom Solution",
    val description: String,
    val salary: String,
    val location: String,
    val employmentType: String = "Full-time",
    val experienceItems: List<String>,
    val postedDate: String,
    val postedTime: String,
    val averageMatchScore: Int,
    val scoreLabel: String,
    val topMatchedSkills: List<String>
)

data class Applicant(
    val name: String,
    val appliedDate: String,
    val appliedTime: String,
    val status: String,
    val individualMatchScore: Int = 0
)

data class ActivityItem(
    val activity: String,
    val details: String,
    val date: String,
    val time: String
)