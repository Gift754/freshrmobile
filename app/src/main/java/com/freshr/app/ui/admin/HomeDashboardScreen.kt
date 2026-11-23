package com.freshr.admin.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freshr.admin.model.ActivityItem
import com.freshr.admin.model.FreshrCardBorder
import com.freshr.admin.model.FreshrSecondaryLavender
import com.freshr.admin.viewmodel.FreshrViewModel

@Composable
fun HomeDashboardScreen(vm: FreshrViewModel) {
    // =========================================================================
    // AI INTEGRATION POINT 4: REAL-TIME TELEMETRY & CV GENERATION METRICS
    // Bind `CVs Generated` and `Applied Jobs` counters directly to your AI
    // pipeline logs to track live automated CV tailoring and match events.
    // =========================================================================
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardStatCard(Icons.Outlined.People, "Active Users", "354", Modifier.weight(1f))
                DashboardStatCard(Icons.Outlined.Description, "CVs Generated", "354", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardStatCard(Icons.Outlined.Search, "Available Jobs", "354", Modifier.weight(1f))
                DashboardStatCard(Icons.Outlined.WorkOutline, "Applied Jobs", "354", Modifier.weight(1f))
            }
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Recent Activity",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        items(vm.recentActivities) { item ->
            ActivityMobileCard(item)
        }
    }
}

@Composable
private fun DashboardStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, FreshrCardBorder),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(28.dp))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ActivityMobileCard(item: ActivityItem) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, FreshrCardBorder),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = FreshrSecondaryLavender,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = item.activity,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text("${item.date} • ${item.time}", fontSize = 12.sp, color = Color.Gray)
            }
            Text(item.details, fontWeight = FontWeight.Medium, fontSize = 15.sp)
        }
    }
}