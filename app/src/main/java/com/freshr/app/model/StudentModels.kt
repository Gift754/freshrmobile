package com.freshr.app.model

import androidx.compose.ui.graphics.Color

val FreshrPrimaryBlue = Color(0xFF1849D6)
val FreshrSecondaryLavender = Color(0xFFE8EDFA)
val FreshrCardBorder = Color(0xFFD0D5DD)
val FreshrDownloadGreen = Color(0xFF28A745)
val FreshrDangerRed = Color(0xFFEF1C1C)

enum class UserRole { ADMIN, STUDENT }

enum class StudentRoute(val title: String) {
    HOME("Home"),
    JOB_MATCHES("Job Matches"),
    APPLICATIONS("Applications"),
    CV_STUDIO("CV Studio"),
    SETTINGS("Settings")
}

enum class ApplicationStatus(
    val label: String,
    val containerColor: Color,
    val contentColor: Color,
    val borderColor: Color
) {
    HIRED("Hired", Color(0xFFDDF5E4), Color(0xFF1E7E34), Color(0xFF8FD19E)),
    PENDING("Pending", Color(0xFFCFE2FF), Color(0xFF1849D6), Color(0xFF8BB9FE)),
    REJECTED("Rejected", Color(0xFFF8D7DA), Color(0xFFDC3545), Color(0xFFF1AEB5))
}

data class StudentJobMatch(
    val id: String,
    val position: String,
    val company: String,
    val location: String,
    val employmentType: String,
    val salary: String,
    val description: String,
    val experience: List<String>,
    val matchPercentage: Int
)

data class StudentApplication(
    val id: String,
    val job: StudentJobMatch,
    val status: ApplicationStatus,
    val appliedDate: String,
    val tailoredCvVersion: String
)