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

data class RegisteredAccount(val name: String, val email: String, val passwordHash: String)

class OnboardingViewModel : ViewModel() {

    var currentPublicRoute by mutableStateOf(PublicRoute.LANDING)
    var authenticatedRole by mutableStateOf(UserRole.STUDENT)
    var pendingEmail by mutableStateOf("")
    var loggedInEmail by mutableStateOf("")
    var loggedInName by mutableStateOf("")
    var heroSearchQuery by mutableStateOf("")
    var authError by mutableStateOf<String?>(null)
    var authSuccessMessage by mutableStateOf<String?>(null)
    var prefilledEmail by mutableStateOf("")

    private val registeredAccounts = mutableMapOf<String, RegisteredAccount>()

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
        authSuccessMessage = null
        val cleanEmail = email.trim().lowercase()

        if (!isValidEmail(cleanEmail)) {
            authError = "Please enter a valid email address (e.g. user@example.com)."
            return
        }
        if (password.length < 4) {
            authError = "Password must be at least 4 characters long."
            return
        }

        val displayName = name.ifBlank { cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() } }
        registeredAccounts[cleanEmail] = RegisteredAccount(displayName, cleanEmail, password)

        prefilledEmail = cleanEmail
        authSuccessMessage = "Account created successfully! Please log in with your password."
        currentPublicRoute = PublicRoute.LOGIN
    }

    fun loginUser(email: String, password: String) {
        authError = null
        authSuccessMessage = null
        val cleanEmail = email.trim().lowercase()

        if (!isValidEmail(cleanEmail)) {
            authError = "Please enter a valid email address (e.g. user@example.com)."
            return
        }
        if (password.isBlank()) {
            authError = "Please enter your password."
            return
        }

        val registered = registeredAccounts[cleanEmail]
        if (registered != null && registered.passwordHash != password) {
            authError = "Incorrect password. Please verify your credentials."
            return
        }

        loggedInEmail = cleanEmail
        loggedInName = registered?.name ?: cleanEmail.substringBefore("@").replace(".", " ")
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            .ifBlank { "User" }

        authenticatedRole = if (cleanEmail.equals("admin@gmail.com", ignoreCase = true) || cleanEmail.contains("admin")) {
            UserRole.ADMIN
        } else {
            UserRole.STUDENT
        }
        currentPublicRoute = PublicRoute.AUTHENTICATED_APP
    }

    fun continueWithGoogle() {
        authError = null
        authSuccessMessage = null
        loggedInEmail = "student.user@gmail.com"
        loggedInName = "Student User"
        authenticatedRole = UserRole.STUDENT
        currentPublicRoute = PublicRoute.AUTHENTICATED_APP
    }

    fun sendRecoveryCode(email: String) {
        authError = null
        authSuccessMessage = null
        val cleanEmail = email.trim().lowercase()
        if (!isValidEmail(cleanEmail)) {
            authError = "Please enter a valid email address (e.g. user@example.com)."
            return
        }
        pendingEmail = cleanEmail
        authSuccessMessage = "Password reset instructions sent to $cleanEmail."
        currentPublicRoute = PublicRoute.LOGIN
    }

    fun logout() {
        loggedInEmail = ""
        loggedInName = ""
        authError = null
        authSuccessMessage = null
        currentPublicRoute = PublicRoute.LANDING
    }
}
