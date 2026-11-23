package com.freshr.admin.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freshr.admin.model.FreshrCardBorder
import com.freshr.admin.model.FreshrPrimaryBlue
import com.freshr.admin.viewmodel.FreshrViewModel

@Composable
fun SettingsScreen(vm: FreshrViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Preferences", "Notifications", "Security")

    Column(modifier = Modifier.fillMaxSize()) {
        // Admin Profile Header (Admin-Settings.png)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE5E7EB)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Person, contentDescription = "Admin Avatar", modifier = Modifier.size(40.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("ADMIN", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("ADMIN@gmail.com", color = Color.Gray, fontSize = 13.sp)
            Text("ADMIN", color = Color.Gray, fontSize = 12.sp)
        }

        // 3-Variant Settings TabRow
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = Color.Black
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, FreshrCardBorder),
            colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                when (selectedTab) {
                    0 -> PreferencesTabContent(vm)
                    1 -> NotificationsTabContent()
                    2 -> SecurityTabContent()
                }
            }
        }
    }
}

@Composable
private fun PreferencesTabContent(vm: FreshrViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Dark Theme", fontSize = 15.sp)
        Switch(
            checked = vm.isDarkTheme,
            onCheckedChange = { vm.isDarkTheme = it },
            colors = SwitchDefaults.colors(checkedTrackColor = FreshrPrimaryBlue)
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Animations", fontSize = 15.sp)
        Switch(
            checked = vm.animationsEnabled,
            onCheckedChange = { vm.animationsEnabled = it },
            colors = SwitchDefaults.colors(checkedTrackColor = FreshrPrimaryBlue)
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Font Size", fontSize = 15.sp)
        Text("${(vm.fontScale * 100).toInt()}%", color = Color.Gray)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Language", fontSize = 15.sp)
        Icon(Icons.Outlined.Language, contentDescription = "Language")
    }
}

@Composable
private fun NotificationsTabContent() {
    NotificationEntry("System", "New System update on 29/05/2026. New Features have been added.")
    NotificationEntry("System", "New System update on 29/05/2026. New Features have been added.")
    NotificationEntry("Job Application", "New jobs being reviewed. Click to view.")
    NotificationEntry("Job Application", "New jobs being reviewed. Click to view.")
}

@Composable
private fun NotificationEntry(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(body, color = Color.Gray, fontSize = 13.sp)
    }
}

@Composable
private fun SecurityTabContent() {
    listOf(
        "Update Password",
        "Privacy Policy",
        "Terms and Conditions",
        "Help",
        "Shutdown System"
    ).forEach { action ->
        Text(action, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}