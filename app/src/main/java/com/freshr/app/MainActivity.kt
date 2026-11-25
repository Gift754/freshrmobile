package com.freshr.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freshr.admin.ui.FreshrAdminApp
import com.freshr.app.model.UserRole
import com.freshr.app.ui.auth.*
import com.freshr.app.ui.landing.LandingScreen
import com.freshr.app.ui.shared.ChangePasswordScreen
import com.freshr.app.ui.shared.CodeVerificationScreen
import com.freshr.app.ui.student.FreshrStudentApp
import com.freshr.app.viewmodel.OnboardingViewModel
import com.freshr.app.viewmodel.PublicRoute
import com.freshr.app.viewmodel.StudentViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                FreshrRootNavigation()
            }
        }
    }
}

@Composable
fun FreshrRootNavigation(onboardingVm: OnboardingViewModel = viewModel()) {
    when (onboardingVm.currentPublicRoute) {
        PublicRoute.LANDING -> LandingScreen(
            onSearchHero = { onboardingVm.searchJobsFromLanding(it) },
            onStartNow = { onboardingVm.currentPublicRoute = PublicRoute.REGISTER },
            onLogInClick = { onboardingVm.currentPublicRoute = PublicRoute.LOGIN },
            onRegisterClick = { onboardingVm.currentPublicRoute = PublicRoute.REGISTER }
        )

        PublicRoute.REGISTER -> CreateAccountScreen(
            onRegisterSubmit = { name, email, pass -> onboardingVm.registerUser(name, email, pass) },
            onContinueWithGoogle = { onboardingVm.continueWithGoogle() },
            onNavigateLogin = { onboardingVm.currentPublicRoute = PublicRoute.LOGIN },
            onNavigateLanding = { onboardingVm.currentPublicRoute = PublicRoute.LANDING }
        )

        PublicRoute.LOGIN -> LoginScreen(
            initialEmail = onboardingVm.prefilledEmail,
            successMessage = onboardingVm.authSuccessMessage,
            onLoginSubmit = { email, pass -> onboardingVm.loginUser(email, pass) },
            onContinueWithGoogle = { onboardingVm.continueWithGoogle() },
            onNavigateForgotPassword = { onboardingVm.currentPublicRoute = PublicRoute.FORGOT_PASSWORD },
            onNavigateRegister = { onboardingVm.currentPublicRoute = PublicRoute.REGISTER },
            onNavigateLanding = { onboardingVm.currentPublicRoute = PublicRoute.LANDING }
        )

        PublicRoute.FORGOT_PASSWORD -> ForgotPasswordScreen(
            onSendCode = { email -> onboardingVm.sendRecoveryCode(email) },
            onContinueWithGoogle = { onboardingVm.continueWithGoogle() },
            onNavigateRegister = { onboardingVm.currentPublicRoute = PublicRoute.REGISTER },
            onNavigateLogin = { onboardingVm.currentPublicRoute = PublicRoute.LOGIN },
            onNavigateLanding = { onboardingVm.currentPublicRoute = PublicRoute.LANDING }
        )

        PublicRoute.CODE_VERIFICATION -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CodeVerificationScreen(
                onConfirmCode = { onboardingVm.currentPublicRoute = PublicRoute.RESET_PASSWORD },
                onContinueWithGoogle = { onboardingVm.continueWithGoogle() },
                onNavigateCreateAccount = { onboardingVm.currentPublicRoute = PublicRoute.REGISTER }
            )
        }

        PublicRoute.RESET_PASSWORD -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            ChangePasswordScreen(
                onChangePassword = { _, _ -> onboardingVm.currentPublicRoute = PublicRoute.LOGIN },
                onNavigateCreateAccount = { onboardingVm.currentPublicRoute = PublicRoute.REGISTER }
            )
        }

        PublicRoute.AUTHENTICATED_APP -> {
            if (onboardingVm.authenticatedRole == UserRole.ADMIN) {
                FreshrAdminApp()
            } else {
                val studentVm: StudentViewModel = viewModel()
                if (onboardingVm.loggedInEmail.isNotBlank()) {
                    studentVm.setUserDetails(onboardingVm.loggedInEmail, onboardingVm.loggedInName)
                }
                FreshrStudentApp(
                    vm = studentVm,
                    onLoggedOut = { onboardingVm.logout() }
                )
            }
        }
    }
}
