package com.freshr.app.ui.landing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freshr.app.model.FreshrCardBorder
import com.freshr.app.model.FreshrPrimaryBlue
import com.freshr.app.ui.auth.FreshrFooterSection
import com.freshr.app.ui.auth.FreshrTopPublicBar

// 4. Landing Page.jpg
@Composable
fun LandingScreen(
    onSearchHero: (String) -> Unit,
    onStartNow: () -> Unit,
    onLogInClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    var heroQuery by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top Public Navigation Bar
        item {
            FreshrTopPublicBar(
                onLogoClick = {},
                onLogInClick = onLogInClick,
                onRegisterClick = onRegisterClick
            )
        }

        // Hero Section ("Find Your Perfect Job Match With AI")
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF4A453E), Color(0xFF8C7A6B))
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Find Your Perfect Job Match With AI",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Connect your GitHub profile, discover personalized job opportunities, and tailor your CV with AI.",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = heroQuery,
                            onValueChange = { heroQuery = it },
                            placeholder = { Text("Search roles or paste GitHub URL...", fontSize = 12.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                        Button(
                            onClick = { onSearchHero(heroQuery) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FreshrPrimaryBlue),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Match", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // How It Works Section
        item {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("How It Works", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Connect your GitHub, receive AI job matches, customize your CV, and track applications in one place.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                val steps = listOf(
                    Triple(Icons.Outlined.Code, "Connect Your GitHub", "Sync your public repositories so our AI can analyze your verified coding stack."),
                    Triple(Icons.Outlined.Psychology, "Get Smart Job Matches", "Receive ranked job recommendations scored against your actual technical skills."),
                    Triple(Icons.Outlined.DashboardCustomize, "Tailor Your CV", "Generate ATS-ready, role-specific resumes in seconds inside CV Studio."),
                    Triple(Icons.Outlined.TrackChanges, "Apply & Track", "Submit applications and monitor Hired, Pending, and Reviewed statuses.")
                )

                steps.forEach { (icon, title, desc) ->
                    LandingFeatureCard(icon = icon, title = title, description = desc)
                }

                Button(
                    onClick = onStartNow,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FreshrPrimaryBlue),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Start Now")
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Key Features Section
        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Key Features", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Explore AI-powered career tools built for students, graduates, and hiring teams.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                val features = listOf(
                    Triple(Icons.Outlined.AutoAwesome, "AI Job Matching", "Semantic matching beyond keywords to surface roles aligned with your real projects."),
                    Triple(Icons.Outlined.Description, "AI CV Studio", "Interactive resume assistant with instant PDF export tailored to each job post."),
                    Triple(Icons.Outlined.WorkOutline, "Application Tracking", "Real-time status pills and detailed role breakdowns for every submitted application."),
                    Triple(Icons.Outlined.Analytics, "GitHub Skill Analysis", "Automated repository parsing to highlight your top core languages and frameworks."),
                    Triple(Icons.Outlined.Insights, "Match Insights", "Transparent match percentage scores and top overlapping skill breakdowns."),
                    Triple(Icons.Outlined.Security, "Secure & Private", "Full control over profile visibility, notifications, and account security.")
                )

                features.forEach { (icon, title, desc) ->
                    LandingFeatureCard(icon = icon, title = title, description = desc)
                }

                Button(
                    onClick = onStartNow,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FreshrPrimaryBlue),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Explore")
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Testimonials Horizontal Carousel
        item {
            Column(
                modifier = Modifier.padding(vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text("Testimonials", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("What students and recruiters say about Freshr.", fontSize = 12.sp, color = Color.Gray)
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(4) {
                        OutlinedCard(
                            modifier = Modifier.width(270.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, FreshrCardBorder),
                            colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Syncing my GitHub immediately matched me with backend Python roles at 92% fit and tailored my CV for each interview.",
                                    fontSize = 12.sp,
                                    color = Color.DarkGray
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFE5E7EB)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("John Doe", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("Software Developer", fontSize = 10.sp, color = Color.Gray)
                                        }
                                    }
                                    Text("★★★★★", color = Color(0xFFFACC15), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Brand Footer
        item {
            FreshrFooterSection(
                onLogInClick = onLogInClick,
                onRegisterClick = onRegisterClick
            )
        }
    }
}

@Composable
private fun LandingFeatureCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, FreshrCardBorder),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = title, modifier = Modifier.size(28.dp), tint = Color.Black)
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.Center)
            Text(description, fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
        }
    }
}