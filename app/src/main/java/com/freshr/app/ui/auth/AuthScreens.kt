package com.freshr.app.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freshr.app.model.FreshrPrimaryBlue

@Composable
fun FreshrLogoMark(tint: Color = FreshrPrimaryBlue) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(Modifier.width(20.dp).height(3.dp).background(tint, RoundedCornerShape(2.dp)))
            Box(Modifier.width(14.dp).height(3.dp).background(tint, RoundedCornerShape(2.dp)))
            Box(Modifier.width(9.dp).height(3.dp).background(tint, RoundedCornerShape(2.dp)))
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "reshr",
            color = tint,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
    }
}

@Composable
fun FreshrTopPublicBar(
    onLogoClick: () -> Unit,
    onLogInClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    Surface(color = Color.White.copy(alpha = 0.92f), shadowElevation = 1.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.clickable { onLogoClick() }) {
                FreshrLogoMark()
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Log In",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onLogInClick() }
                )
                Text(
                    text = "Register",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onRegisterClick() }
                )
            }
        }
    }
}

@Composable
fun FreshrFooterSection(
    onLogInClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    var newsletterEmail by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FreshrPrimaryBlue)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Freshr", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newsletterEmail,
                onValueChange = { newsletterEmail = it },
                placeholder = { Text("Enter your E-mail", fontSize = 13.sp, color = Color.Gray) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(6.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )
            Button(
                onClick = { /* Subscribe email */ },
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B6CB0)),
                modifier = Modifier.height(48.dp)
            ) {
                Text("Connect", fontSize = 13.sp)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FooterLink("Contact Us")
                FooterLink("Privacy Policy")
                FooterLink("Terms & conditions")
                FooterLink("Help Centre")
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FooterLink("Features")
                FooterLink("Pricing")
                FooterLink("FAQS")
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Register", color = Color.White, fontSize = 12.sp, modifier = Modifier.clickable { onRegisterClick() })
                Text("Log In", color = Color.White, fontSize = 12.sp, modifier = Modifier.clickable { onLogInClick() })
            }
        }

        Text("©2026. Freshr.co.ke", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
    }
}

@Composable
private fun FooterLink(text: String) {
    Text(text = text, color = Color.White, fontSize = 12.sp)
}

@Composable
fun FreshrAuthShell(
    onLogoClick: () -> Unit,
    onLogInClick: () -> Unit,
    onRegisterClick: () -> Unit,
    cardContent: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        FreshrTopPublicBar(onLogoClick, onLogInClick, onRegisterClick)

        // Pastel Geometric Backdrop matching Create Account.jpg, Log in.jpg, Change-Password.jpg
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFCDEEF3),
                            Color(0xFFD5DDF7),
                            Color(0xFFF3D5E5)
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 36.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val poly1 = Path().apply {
                    moveTo(0f, size.height * 0.2f)
                    lineTo(size.width * 0.45f, 0f)
                    lineTo(size.width * 0.3f, size.height * 0.65f)
                    close()
                }
                drawPath(poly1, Color(0xFFB4E4EC).copy(alpha = 0.55f))

                val poly2 = Path().apply {
                    moveTo(size.width, size.height * 0.15f)
                    lineTo(size.width * 0.55f, size.height * 0.5f)
                    lineTo(size.width, size.height * 0.9f)
                    close()
                }
                drawPath(poly2, Color(0xFFEAC2D8).copy(alpha = 0.55f))
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 6.dp,
                color = Color.White
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    content = cardContent
                )
            }
        }

        FreshrFooterSection(onLogInClick, onRegisterClick)
    }
}

// 1. Create Account.jpg
@Composable
fun CreateAccountScreen(
    onRegisterSubmit: (String, String, String) -> Unit,
    onContinueWithGoogle: () -> Unit,
    onNavigateLogin: () -> Unit,
    onNavigateLanding: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    FreshrAuthShell(
        onLogoClick = onNavigateLanding,
        onLogInClick = onNavigateLogin,
        onRegisterClick = {}
    ) {
        Text("Create Account", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        AuthLabeledField(
            label = "Name",
            value = name,
            onValueChange = { name = it; errorMessage = null },
            placeholder = "Enter your Name"
        )
        AuthLabeledField(
            label = "E-mail",
            value = email,
            onValueChange = { email = it; errorMessage = null },
            placeholder = "Example@gmail.com"
        )
        AuthLabeledField(
            label = "Create Password",
            value = password,
            onValueChange = { password = it; errorMessage = null },
            placeholder = "Enter a Strong Password",
            isPassword = true
        )

        errorMessage?.let { err ->
            Text(
                text = err,
                color = Color(0xFFEF1C1C),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }

        Text(
            text = "Already have an account? Log In",
            fontSize = 12.sp,
            color = Color.DarkGray,
            modifier = Modifier.clickable { onNavigateLogin() }
        )

        GoogleAuthButton(onClick = onContinueWithGoogle)

        Button(
            onClick = {
                val trimmedEmail = email.trim()
                if (!com.freshr.app.viewmodel.OnboardingViewModel.isValidEmail(trimmedEmail)) {
                    errorMessage = "Please enter a valid email address (e.g. user@example.com)."
                } else if (password.length < 4) {
                    errorMessage = "Password must be at least 4 characters long."
                } else {
                    errorMessage = null
                    onRegisterSubmit(name, trimmedEmail, password)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FreshrPrimaryBlue)
        ) {
            Text("Create Account", fontWeight = FontWeight.SemiBold)
        }
    }
}

// 2. Log in.jpg
@Composable
fun LoginScreen(
    onLoginSubmit: (String, String) -> Unit,
    onContinueWithGoogle: () -> Unit,
    onNavigateForgotPassword: () -> Unit,
    onNavigateRegister: () -> Unit,
    onNavigateLanding: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    FreshrAuthShell(
        onLogoClick = onNavigateLanding,
        onLogInClick = {},
        onRegisterClick = onNavigateRegister
    ) {
        Text("Log In", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        AuthLabeledField(
            label = "E-mail",
            value = email,
            onValueChange = { email = it; errorMessage = null },
            placeholder = "Example@gmail.com"
        )

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AuthLabeledField(
                label = "Password",
                value = password,
                onValueChange = { password = it; errorMessage = null },
                placeholder = "Enter your Password",
                isPassword = true
            )
            Text(
                text = "Forgot Password?",
                fontSize = 12.sp,
                color = Color.DarkGray,
                modifier = Modifier.clickable { onNavigateForgotPassword() }
            )
        }

        errorMessage?.let { err ->
            Text(
                text = err,
                color = Color(0xFFEF1C1C),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }

        Text(
            text = "Don't have an account? Create Account",
            fontSize = 12.sp,
            color = Color.DarkGray,
            modifier = Modifier.clickable { onNavigateRegister() }
        )

        GoogleAuthButton(onClick = onContinueWithGoogle)

        Button(
            onClick = {
                val trimmedEmail = email.trim()
                if (!com.freshr.app.viewmodel.OnboardingViewModel.isValidEmail(trimmedEmail)) {
                    errorMessage = "Please enter a valid email address (e.g. user@example.com)."
                } else if (password.isBlank()) {
                    errorMessage = "Please enter your password."
                } else {
                    errorMessage = null
                    onLoginSubmit(trimmedEmail, password)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FreshrPrimaryBlue)
        ) {
            Text("Log In", fontWeight = FontWeight.SemiBold)
        }
    }
}

// 3. Change-Password.jpg (Forgot Password? Screen)
@Composable
fun ForgotPasswordScreen(
    onSendCode: (String) -> Unit,
    onContinueWithGoogle: () -> Unit,
    onNavigateRegister: () -> Unit,
    onNavigateLogin: () -> Unit,
    onNavigateLanding: () -> Unit
) {
    var email by remember { mutableStateOf("") }

    FreshrAuthShell(
        onLogoClick = onNavigateLanding,
        onLogInClick = onNavigateLogin,
        onRegisterClick = onNavigateRegister
    ) {
        Text("Forgot Password?", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        AuthLabeledField(
            label = "E-mail",
            value = email,
            onValueChange = { email = it },
            placeholder = "Example@gmail.com"
        )

        Text(
            text = "Don't have an account? Create Account",
            fontSize = 12.sp,
            color = Color.DarkGray,
            modifier = Modifier.clickable { onNavigateRegister() }
        )

        GoogleAuthButton(onClick = onContinueWithGoogle)

        Button(
            onClick = { onSendCode(email) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FreshrPrimaryBlue)
        ) {
            Text("Send Code", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun AuthLabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, fontSize = 13.sp, color = Color.Gray) },
            singleLine = true,
            visualTransformation = if (isPassword && !passwordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                            contentDescription = "Toggle password"
                        )
                    }
                }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
private fun GoogleAuthButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        shadowElevation = 2.dp,
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Text("Continue With Google", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}