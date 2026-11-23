package com.freshr.app.ui.student

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freshr.app.model.*
import com.freshr.app.viewmodel.StudentViewModel

// 1. Student-Dashboard.png
@Composable
fun StudentHomeScreen(vm: StudentViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Welcome back, ${vm.userName}!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Connected Account: ${vm.userEmail}",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }

        // Connect GitHub Card
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, FreshrCardBorder),
                colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Code, contentDescription = "GitHub", modifier = Modifier.size(30.dp), tint = FreshrPrimaryBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connect Your GitHub Profile", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Text(
                        text = "Enter any active GitHub username or URL (e.g. torvalds or https://github.com/yourusername). We will analyze public repos & extract verified skills.",
                        fontSize = 13.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center
                    )
                    OutlinedTextField(
                        value = vm.githubUrl,
                        onValueChange = { vm.githubUrl = it },
                        placeholder = { Text("https://github.com/username", fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    vm.syncErrorMessage?.let { err ->
                        Text(err, color = FreshrDangerRed, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                    }
                    Button(
                        onClick = { vm.syncGitHubProfile() },
                        enabled = !vm.isSyncing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FreshrPrimaryBlue)
                    ) {
                        if (vm.isSyncing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Fetching Real GitHub Data...")
                        } else {
                            Text("Sync GitHub Profile", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (vm.isGitHubSynced) {
                        vm.githubProfile?.let { prof ->
                            Surface(
                                color = FreshrSecondaryLavender,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Connected GitHub: @${prof.username} (${prof.publicReposCount} Public Repos)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    if (prof.bio.isNotBlank()) Text(prof.bio, fontSize = 12.sp, color = Color.DarkGray)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Core Skills & Highest Rated Matches Cards
        item {
            SkillAndMatchSummaryCards(vm)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SkillAndMatchSummaryCards(vm: StudentViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, FreshrCardBorder),
            colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Your Verified Core Skills", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (!vm.isGitHubSynced && vm.coreSkills.isEmpty()) {
                    Icon(Icons.Outlined.Image, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(36.dp))
                    Text("No data yet. Sync your GitHub profile to extract skills.", fontSize = 12.sp, color = Color.Gray)
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        vm.coreSkills.forEach { skill ->
                            AssistChip(onClick = {}, label = { Text(skill, fontWeight = FontWeight.Medium) })
                        }
                    }
                }
            }
        }

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, FreshrCardBorder),
            colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Highest Rated Job Matches", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                vm.topMatches.take(3).forEach { match ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(match.position, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("${match.company} • ${match.salary}", fontSize = 12.sp, color = Color.Gray)
                        }
                        Text("${match.matchPercentage}% Match", color = FreshrPrimaryBlue, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                }
            }
        }
    }
}

// 2. Student-Job Matches.png
@Composable
fun StudentJobMatchesScreen(vm: StudentViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Top Job Matches For You", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Ranked by matching your skills against role requirements", fontSize = 13.sp, color = Color.Gray)
            }
        }
        items(vm.topMatches) { match ->
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, FreshrCardBorder),
                colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(52.dp),
                            border = BorderStroke(1.dp, Color.Gray),
                            color = Color.White,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Business, contentDescription = null, tint = FreshrPrimaryBlue)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(match.position, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("${match.company} • ${match.salary}", fontSize = 13.sp, color = Color.DarkGray)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("📍 ${match.location}", fontSize = 12.sp, color = Color.Gray)
                                Text("🕒 ${match.employmentType}", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = FreshrSecondaryLavender,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("${match.matchPercentage}%", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FreshrPrimaryBlue)
                                Text("Match", fontSize = 11.sp, color = FreshrPrimaryBlue)
                            }
                        }
                        val isApplied = vm.applications.any { it.job.id == match.id }
                        Button(
                            onClick = { vm.applyToJob(match) },
                            enabled = !isApplied,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isApplied) Color.Gray else FreshrPrimaryBlue),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(if (isApplied) "Applied" else "Apply", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// 3. Student-Applications.png & Student-Job details.png
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentApplicationsScreen(vm: StudentViewModel) {
    var selectedApplication by remember { mutableStateOf<StudentApplication?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("My Job Applications", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        if (vm.applications.isEmpty()) {
            item {
                Text("No applications submitted yet. Browse Job Matches and click Apply!", color = Color.Gray, fontSize = 14.sp)
            }
        }
        items(vm.applications) { app ->
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, FreshrCardBorder),
                colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            border = BorderStroke(1.dp, Color.Gray),
                            color = Color.White,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Business, contentDescription = null, tint = FreshrPrimaryBlue)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(app.job.position, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(app.job.company, fontSize = 13.sp)
                            Text("${app.job.location} • ${app.job.employmentType}", fontSize = 12.sp, color = Color.Gray)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { selectedApplication = app },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("View Details", fontSize = 12.sp, color = Color.Black)
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            color = app.status.containerColor,
                            border = BorderStroke(1.dp, app.status.borderColor),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = app.status.label,
                                color = app.status.contentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        Button(
                            onClick = { vm.deleteApplication(app.id) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FreshrDangerRed)
                        ) {
                            Text("Delete", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    selectedApplication?.let { app ->
        ModalBottomSheet(onDismissRequest = { selectedApplication = null }, containerColor = Color.White) {
            StudentJobDetailsSheet(application = app)
        }
    }
}

@Composable
private fun StudentJobDetailsSheet(application: StudentApplication) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Job Details", "Application Details")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Job Application Details",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            modifier = Modifier.size(56.dp),
            border = BorderStroke(1.dp, Color.Gray),
            color = Color.White,
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Business, contentDescription = null, tint = FreshrPrimaryBlue)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(application.job.position, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text(application.job.company, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            color = application.status.containerColor,
            border = BorderStroke(1.dp, application.status.borderColor),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = application.status.label,
                color = application.status.contentColor,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        TabRow(selectedTabIndex = selectedTab, containerColor = Color.White) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal)
                    }
                )
            }
        }

        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, FreshrCardBorder),
            colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (selectedTab == 0) {
                    Text("Description", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(application.job.description, fontSize = 13.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Salary", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(application.job.salary, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Location", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(application.job.location, fontSize = 13.sp)
                        }
                        Column {
                            Text("Job Type", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("• ${application.job.employmentType}", fontSize = 13.sp)
                        }
                    }
                    Text("Experience Requirements", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    application.job.experience.forEach { bullet ->
                        Text("• $bullet", fontSize = 13.sp)
                    }
                } else {
                    Text("Applied On: ${application.appliedDate}", fontSize = 14.sp)
                    Text("Skill Match Rating: ${application.job.matchPercentage}%", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("Submitted Resume: ${application.tailoredCvVersion}", fontSize = 14.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

// 4. CV-Studio.png
@Composable
fun CvStudioScreen(vm: StudentViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("AI CV Studio & Resume Tailor", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        SkillAndMatchSummaryCards(vm)

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, FreshrCardBorder),
            colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = "AI Assistant", modifier = Modifier.size(36.dp), tint = FreshrPrimaryBlue)
                Text("Generate & Tailor Your CV", fontSize = 16.sp, fontWeight = FontWeight.Bold)

                vm.generatedCvPreview?.let { preview ->
                    Surface(
                        color = FreshrSecondaryLavender,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = preview,
                            fontSize = 12.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                vm.cvExportStatus?.let { status ->
                    Text(status, color = FreshrDownloadGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = vm.cvPromptInput,
                onValueChange = { vm.cvPromptInput = it },
                placeholder = { Text("Special notes or focus areas (e.g. Highlight FastAPI & Kotlin experience)", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { vm.customizeResumeWithAI() },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FreshrPrimaryBlue)
                ) {
                    Text("Generate CV", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = { vm.exportTailoredCvPdf() },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FreshrDownloadGreen)
                ) {
                    Text("Download PDF", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// 5. Settings.png (Student Profile Header)
@Composable
fun StudentProfileHeader(vm: StudentViewModel, onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(FreshrSecondaryLavender),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Person, contentDescription = "Student Avatar", modifier = Modifier.size(44.dp), tint = FreshrPrimaryBlue)
        }

        Text(vm.userName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(vm.userEmail, color = Color.Gray, fontSize = 14.sp)

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, FreshrCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Account Overview", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                HorizontalDivider()
                Text("Role: Candidate / Student", fontSize = 13.sp)
                Text("Connected GitHub: ${if (vm.isGitHubSynced) "@${vm.githubProfile?.username}" else "Not Synced"}", fontSize = 13.sp)
                Text("Verified Skills: ${if (vm.coreSkills.isNotEmpty()) vm.coreSkills.joinToString(", ") else "None"}", fontSize = 13.sp)
                Text("Total Applications: ${vm.applications.size}", fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FreshrDangerRed)
        ) {
            Icon(Icons.Outlined.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Log Out", fontWeight = FontWeight.Bold)
        }
    }
}
