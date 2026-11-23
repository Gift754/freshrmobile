package com.freshr.admin.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freshr.admin.model.*
import com.freshr.admin.viewmodel.FreshrViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageJobsScreen(vm: FreshrViewModel) {
    var showCreateSheet by remember { mutableStateOf(false) }
    var selectedJobForEdit by remember { mutableStateOf<JobPosting?>(null) }
    var selectedJobForApplicants by remember { mutableStateOf<JobPosting?>(null) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateSheet = true },
                containerColor = FreshrPrimaryBlue,
                contentColor = Color.White,
                icon = { Icon(Icons.Outlined.Add, contentDescription = "Post Job") },
                text = { Text("New Job") }
            )
        },
        containerColor = Color.White
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(vm.jobs) { job ->
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, FreshrCardBorder),
                    colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(job.position, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                Text("${job.salary} • ${job.location}", color = Color.Gray, fontSize = 13.sp)
                            }
                            Surface(
                                color = FreshrSecondaryLavender,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${job.averageMatchScore}% Match",
                                    color = FreshrPrimaryBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { selectedJobForEdit = job },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Edit & Insights")
                            }
                            Button(
                                onClick = { selectedJobForApplicants = job },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FreshrPrimaryBlue)
                            ) {
                                Text("Applicants")
                            }
                        }
                    }
                }
            }
        }
    }

    // Frame 258: Add New Job Modal
    if (showCreateSheet) {
        ModalBottomSheet(onDismissRequest = { showCreateSheet = false }, containerColor = Color.White) {
            CreateJobForm(
                onDone = { pos, desc, sal, loc, exp ->
                    vm.createJob(pos, desc, sal, loc, exp)
                    showCreateSheet = false
                },
                onClose = { showCreateSheet = false }
            )
        }
    }

    // Frame 259: Edit Job & AI Match Insights Modal
    selectedJobForEdit?.let { job ->
        ModalBottomSheet(onDismissRequest = { selectedJobForEdit = null }, containerColor = Color.White) {
            JobMatchDetailsSheet(
                job = job,
                onUpdate = { updated ->
                    vm.updateJob(updated)
                    selectedJobForEdit = null
                },
                onClose = { selectedJobForEdit = null }
            )
        }
    }

    // Frame 311: Applicants Status Sheet
    selectedJobForApplicants?.let { job ->
        ModalBottomSheet(onDismissRequest = { selectedJobForApplicants = null }, containerColor = Color.White) {
            ApplicantsStatusSheet(
                job = job,
                applicants = vm.rankApplicantsForJob(job.id),
                onClose = { selectedJobForApplicants = null }
            )
        }
    }
}

@Composable
private fun CreateJobForm(
    onDone: (String, String, String, String, String) -> Unit,
    onClose: () -> Unit
) {
    var position by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("Remote") }
    var experience by remember { mutableStateOf("") }
    var locationExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = position,
            onValueChange = { position = it },
            placeholder = { Text("Position") },
            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            placeholder = { Text("+ Description") },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            shape = RoundedCornerShape(10.dp)
        )

        OutlinedTextField(
            value = salary,
            onValueChange = { salary = it },
            placeholder = { Text("Salary") },
            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        Box {
            OutlinedTextField(
                value = location,
                onValueChange = {},
                readOnly = true,
                label = { Text("Location") },
                trailingIcon = {
                    IconButton(onClick = { locationExpanded = true }) {
                        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Select Location")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
            DropdownMenu(expanded = locationExpanded, onDismissRequest = { locationExpanded = false }) {
                listOf("Remote", "Nairobi (On-site)", "Hybrid").forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            location = option
                            locationExpanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = experience,
            onValueChange = { experience = it },
            placeholder = { Text("+ Experience") },
            leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        Button(
            onClick = { onDone(position, description, salary, location, experience) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FreshrPrimaryBlue)
        ) {
            Text("Done", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Button(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = FreshrSecondaryLavender,
                contentColor = Color.DarkGray
            )
        ) {
            Text("Close", fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun JobMatchDetailsSheet(
    job: JobPosting,
    onUpdate: (JobPosting) -> Unit,
    onClose: () -> Unit
) {
    var position by remember { mutableStateOf(job.position) }
    var salary by remember { mutableStateOf(job.salary) }
    var location by remember { mutableStateOf(job.location) }
    var employmentType by remember { mutableStateOf(job.employmentType) }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // AI Match Score Card (Frame 259 Top-Right)
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, FreshrCardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // AI OUTPUT: Aggregate match score across candidate dataset
                    Text("${job.averageMatchScore}%", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(job.scoreLabel, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Match Score", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }

        // AI Match Insights Card (Frame 259 Middle-Right)
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, FreshrCardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Match Insights",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Text("Top Matched Skills", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    // AI OUTPUT: Highest-weighted overlapping skills from your dataset
                    job.topMatchedSkills.forEach { skill ->
                        Text(skill, fontSize = 14.sp)
                    }
                }
            }
        }

        // Editable Job Fields (Frame 259 Left Column)
        item {
            EditableFieldCard(label = "Position", value = position, onValueChange = { position = it })
        }
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, FreshrCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(job.description, fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Description", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }
        }
        item {
            EditableFieldCard(label = "Salary", value = salary, onValueChange = { salary = it })
        }
        item {
            EditableFieldCard(label = "Location", value = location, onValueChange = { location = it })
        }
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, FreshrCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Experience", fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit Experience", modifier = Modifier.size(18.dp))
                    }
                    job.experienceItems.forEach { exp ->
                        Text(exp, color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }
        }

        // Job Information Card (Frame 259 Bottom-Right)
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, FreshrCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Job Information",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Text("Posted On", fontWeight = FontWeight.SemiBold)
                    Text(job.postedDate, fontSize = 14.sp)
                    Text(job.postedTime, fontSize = 14.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Employment Type", fontWeight = FontWeight.SemiBold)
                            Text(employmentType, fontSize = 14.sp)
                        }
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit Type", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    onUpdate(
                        job.copy(
                            position = position,
                            salary = salary,
                            location = location,
                            employmentType = employmentType
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FreshrPrimaryBlue)
            ) {
                Text("Update", fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FreshrSecondaryLavender,
                    contentColor = Color.DarkGray
                )
            ) {
                Text("Close", fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun EditableFieldCard(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, FreshrCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(value, color = Color.Gray, fontSize = 14.sp)
            }
            Icon(Icons.Outlined.Edit, contentDescription = "Edit $label", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ApplicantsStatusSheet(
    job: JobPosting,
    applicants: List<Applicant>,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Blue Header Bar (Frame 311)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(FreshrPrimaryBlue)
                .padding(12.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.Close, contentDescription = "Close", tint = Color.White)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                border = BorderStroke(1.dp, Color.Gray),
                color = Color.White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Business, contentDescription = "Company Logo", tint = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("${job.position} (Python)", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(job.company, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(job.location, fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(job.employmentType, fontSize = 13.sp)
                }
            }
        }

        LazyColumn(modifier = Modifier.padding(horizontal = 20.dp)) {
            items(applicants) { applicant ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.4f)) {
                        Text(applicant.name, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Text("${applicant.appliedDate} • ${applicant.appliedTime}", fontSize = 12.sp, color = Color.Gray)
                    }
                    Surface(
                        color = FreshrRejectedRed,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = applicant.status,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
                HorizontalDivider(color = FreshrCardBorder)
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}