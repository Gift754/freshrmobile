package com.freshr.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.freshr.app.model.UserRole

enum class PublicRoute {
    LANDING,
    REGISTER,
    LOGIN,
    FORGOT_PASSWORD,
    CODE_VERIFICATION,
    RESET_PASSWORD,
    AUTHENTICATED_APP
}

class OnboardingViewModel : ViewModel() {

    var currentPublicRoute by mutableStateOf(PublicRoute.LANDING)
    var authenticatedRole by mutableStateOf(UserRole.STUDENT)
    var pendingEmail by mutableStateOf("")
    var loggedInEmail by mutableStateOf("")
    var loggedInName by mutableStateOf("")
    var heroSearchQuery by mutableStateOf("")
    var authError by mutableStateOf<String?>(null)

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")

        fun isValidEmail(email: String): Boolean {
            val trimmed = email.trim()
            if (trimmed.isBlank()) return false
            val parts = trimmed.split("@")
            if (parts.size != 2 || parts[0].isBlank() || parts[1].isBlank()) return false
            return EMAIL_REGEX.matches(trimmed)
        }
    }

    fun searchJobsFromLanding(query: String) {
        heroSearchQuery = query
        currentPublicRoute = PublicRoute.REGISTER
    }

    fun registerUser(name: String, email: String, password: String) {
        authError = null
        if (!isValidEmail(email)) {
            authError = "Please enter a valid email address (e.g. user@example.com)."
            return
        }
        if (password.length < 4) {
            authError = "Password must be at least 4 characters."
            return
        }
        loggedInName = name.ifBlank { email.substringBefore("@").replaceFirstChar { it.uppercase() } }
        loggedInEmail = email.trim()
        pendingEmail = email.trim()
        authenticatedRole = if (email.trim().equals("ADMIN@gmail.com", ignoreCase = true)) {
            UserRole.ADMIN
        } else {
            UserRole.STUDENT
        }
        currentPublicRoute = PublicRoute.AUTHENTICATED_APP
    }

    fun loginUser(email: String, password: String) {
        authError = null
        if (!isValidEmail(email)) {
            authError = "Please enter a valid email address (e.g. user@example.com)."
            return
        }
        if (password.isBlank()) {
            authError = "Please enter your password."
            return
        }
        loggedInEmail = email.trim()
        val usernamePart = email.substringBefore("@").replace(".", " ")
        loggedInName = usernamePart.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            .ifBlank { "User" }

        authenticatedRole = if (email.trim().equals("ADMIN@gmail.com", ignoreCase = true)) {
            UserRole.ADMIN
        } else {
            UserRole.STUDENT
        }
        currentPublicRoute = PublicRoute.AUTHENTICATED_APP
    }

    fun continueWithGoogle() {
        authError = null
        loggedInEmail = "student.user@gmail.com"
        loggedInName = "Student User"
        authenticatedRole = UserRole.STUDENT
        currentPublicRoute = PublicRoute.AUTHENTICATED_APP
    }

    fun sendRecoveryCode(email: String) {
        authError = null
        if (!isValidEmail(email)) {
            authError = "Please enter a valid email address (e.g. user@example.com)."
            return
        }
        pendingEmail = email
        currentPublicRoute = PublicRoute.CODE_VERIFICATION
    }

    fun logout() {
        loggedInEmail = ""
        loggedInName = ""
        authError = null
        currentPublicRoute = PublicRoute.LANDING
    }
}
