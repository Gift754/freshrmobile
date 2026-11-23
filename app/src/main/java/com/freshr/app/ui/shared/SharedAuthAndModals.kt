package com.freshr.app.ui.shared

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freshr.app.model.*

// 6. Admin-Manage Jobs.png (Full List + Filter, Add Job, Search)
@Composable
fun AdminManageJobsListScreen(
    jobs: List<StudentJobMatch>,
    onOpenFilter: () -> Unit,
    onAddJob: () -> Unit,
    onViewApplications: (StudentJobMatch) -> Unit,
    onUpdateDetails: (StudentJobMatch) -> Unit,
    onDeleteJob: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Filter, + Add Job, and Search Pill Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onOpenFilter,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Icon(Icons.Outlined.FilterAlt, contentDescription = "Filter", modifier = Modifier.size(16.dp), tint = Color.Black)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Filter", fontSize = 12.sp, color = Color.Black)
            }
            OutlinedButton(
                onClick = onAddJob,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add Job", modifier = Modifier.size(16.dp), tint = Color.Black)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Job", fontSize = 12.sp, color = Color.Black)
            }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(50),
                singleLine = true
            )
        }

        val filteredJobs = jobs.filter { it.position.contains(searchQuery, ignoreCase = true) }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(filteredJobs) { job ->
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, FreshrCardBorder),
                    colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                border = BorderStroke(1.dp, Color.Gray),
                                color = Color.White
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.Business, contentDescription = null, tint = Color.Gray)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(job.position, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(job.company, fontSize = 13.sp)
                                Text("📍 ${job.location}   🕒 ${job.employmentType}", fontSize = 12.sp, color = Color.Gray)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onViewApplications(job) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FreshrSecondaryLavender,
                                    contentColor = Color.Black
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Text("Applications", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { onUpdateDetails(job) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Text("Update Details", fontSize = 12.sp, color = Color.Black)
                            }
                            Button(
                                onClick = { onDeleteJob(job.id) },
                                modifier = Modifier.weight(0.9f),
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FreshrDangerRed),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Text("Delete", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 7. Frame 1.png (Code Verification + Continue With Google)
@Composable
fun CodeVerificationScreen(
    onConfirmCode: (String) -> Unit,
    onContinueWithGoogle: () -> Unit,
    onNavigateCreateAccount: () -> Unit
) {
    val digits = remember { mutableStateListOf("", "", "", "", "", "") }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 6.dp,
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Code Verification", fontSize = 26.sp, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                digits.forEachIndexed { index, value ->
                    OutlinedTextField(
                        value = value,
                        onValueChange = { if (it.length <= 1) digits[index] = it },
                        placeholder = { Text("*", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                        modifier = Modifier.width(44.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }

            TextButton(onClick = onNavigateCreateAccount) {
                Text("Don't have an account? Create Account", color = Color.Gray, fontSize = 13.sp)
            }

            ElevatedButton(
                onClick = onContinueWithGoogle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.elevatedButtonColors(containerColor = Color.White)
            ) {
                Text("G   Continue With Google", color = Color.Black, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = { onConfirmCode(digits.joinToString("")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FreshrPrimaryBlue)
            ) {
                Text("Confirm", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// 8. Frame 130.png (Change Your Password)
@Composable
fun ChangePasswordScreen(
    onChangePassword: (String, String) -> Unit,
    onNavigateCreateAccount: () -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 6.dp,
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Change Your\nPassword",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                placeholder = { Text("New Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                placeholder = { Text("Confirm Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            TextButton(onClick = onNavigateCreateAccount) {
                Text("Don't have an account? Create Account", color = Color.Gray, fontSize = 13.sp)
            }

            Button(
                onClick = { onChangePassword(newPassword, confirmPassword) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FreshrPrimaryBlue)
            ) {
                Text("Change Password", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// 9. Log Out.png (Confirmation Dialog)
@Composable
fun LogoutConfirmationDialog(
    onConfirmLogout: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                border = BorderStroke(3.dp, FreshrDangerRed),
                color = Color.White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("!", color = FreshrDangerRed, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        title = {
            Text(
                text = "Are You Sure You Want To Log Out?",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "You will need to log in again to access your account",
                    fontSize = 13.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onConfirmLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FreshrDangerRed)
                ) {
                    Text("Log Out", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FreshrSecondaryLavender,
                        contentColor = Color.DarkGray
                    )
                ) {
                    Text("Cancel")
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}