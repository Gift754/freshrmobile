package com.freshr.admin.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freshr.admin.model.FreshrActiveGreen
import com.freshr.admin.model.FreshrCardBorder
import com.freshr.admin.model.FreshrPrimaryBlue
import com.freshr.admin.viewmodel.FreshrViewModel

@Composable
fun ReportsScreen(vm: FreshrViewModel) {
    // =========================================================================
    // AI INTEGRATION POINT 5: PLACEMENT EQUITY & SKILL-GAP ANALYTICS
    // Populate `Placement Equity` placement rates (71% / 52% / 36%) and
    // `CV Performance` lift (3.1 vs 1.8 views) directly from your evaluation
    // dataset comparing AI-tailored CVs against baseline manual submissions.
    // =========================================================================
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Horizontal Scrollable KPI Row (Reports.png Top)
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(3) {
                    OutlinedCard(
                        modifier = Modifier.width(170.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, FreshrCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Total Students", fontSize = 12.sp, color = Color.Gray)
                            Text("4,198", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("↑ 12% Increase", fontSize = 12.sp, color = FreshrActiveGreen)
                        }
                    }
                }
            }
        }

        // Registrations by Field
        item {
            ReportSectionCard("Registrations by Field") {
                val fields = listOf(
                    "Software Engineering" to 0.72f,
                    "Backend Development" to 0.55f,
                    "UI/UX Designing" to 0.40f,
                    "Front-end Development" to 0.28f,
                    "Database Management" to 0.16f
                )
                fields.forEach { (field, progress) ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(field, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = FreshrPrimaryBlue
                        )
                    }
                }
            }
        }

        // GitHub Integration & CV Performance
        item {
            ReportSectionCard("GitHub Integration & CV Performance") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("GitHub Linked", fontWeight = FontWeight.Medium)
                    Text("61%", fontWeight = FontWeight.Bold, color = FreshrPrimaryBlue, fontSize = 18.sp)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                MetricRow("CVs Generated", "1506")
                MetricRow("AVG. Employer Views", "3.1 Vs 1.8 Manual")
                MetricRow("Completion Rate", "76%")
            }
        }

        // Top Hiring Employers
        item {
            ReportSectionCard("Top Hiring Employers") {
                repeat(3) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Safaricom", fontWeight = FontWeight.Medium)
                        Text("42 Hires", color = Color.Gray)
                        Text("Active", color = FreshrActiveGreen, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Placement Equity Report (Skills Gap vs Hired)
        item {
            ReportSectionCard("Placement Equity Report — Skills Gap vs Hired") {
                val equityMetrics = listOf(
                    Triple("Met all required skills", 0.71f, "71% Placed"),
                    Triple("Partial skill match", 0.52f, "52% Placed"),
                    Triple("Low skill match", 0.36f, "36% Placed")
                )
                equityMetrics.forEach { (label, ratio, badge) ->
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, fontSize = 13.sp)
                            Text(badge, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = FreshrPrimaryBlue
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, FreshrCardBorder),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            content()
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}