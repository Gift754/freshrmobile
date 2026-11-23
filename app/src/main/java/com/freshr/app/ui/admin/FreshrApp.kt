package com.freshr.admin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freshr.admin.model.*
import com.freshr.admin.viewmodel.FreshrViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreshrAdminApp(vm: FreshrViewModel = viewModel()) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = FreshrPrimaryBlue,
                drawerContentColor = Color.White,
                modifier = Modifier.width(280.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Freshr",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = { scope.launch { drawerState.close() } }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close Menu", tint = Color.White)
                    }
                }

                DrawerNavItem(
                    icon = Icons.Outlined.Home,
                    label = "Home",
                    selected = vm.currentRoute == FreshrRoute.HOME
                ) {
                    vm.currentRoute = FreshrRoute.HOME
                    scope.launch { drawerState.close() }
                }
                DrawerNavItem(
                    icon = Icons.Outlined.WorkOutline,
                    label = "Manage Jobs",
                    selected = vm.currentRoute == FreshrRoute.MANAGE_JOBS
                ) {
                    vm.currentRoute = FreshrRoute.MANAGE_JOBS
                    scope.launch { drawerState.close() }
                }
                DrawerNavItem(
                    icon = Icons.Outlined.Description,
                    label = "Reports",
                    selected = vm.currentRoute == FreshrRoute.REPORTS
                ) {
                    vm.currentRoute = FreshrRoute.REPORTS
                    scope.launch { drawerState.close() }
                }

                Spacer(modifier = Modifier.weight(1f))

                DrawerNavItem(
                    icon = Icons.Outlined.Settings,
                    label = "Settings",
                    selected = vm.currentRoute == FreshrRoute.SETTINGS
                ) {
                    vm.currentRoute = FreshrRoute.SETTINGS
                    scope.launch { drawerState.close() }
                }
                DrawerNavItem(
                    icon = Icons.Outlined.Logout,
                    label = "Log Out",
                    selected = false
                ) {
                    // Handle session logout
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Outlined.Menu, contentDescription = "Open Menu")
                            }
                        },
                        title = {
                            Text(
                                text = vm.currentRoute.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        actions = {
                            IconButton(onClick = { vm.currentRoute = FreshrRoute.SETTINGS }) {
                                Icon(Icons.Outlined.Notifications, contentDescription = "Notifications")
                            }
                            Text(
                                text = "Admin",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(end = 16.dp)
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                    )
                    HorizontalDivider(color = FreshrCardBorder)
                }
            },
            containerColor = Color.White
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (vm.currentRoute) {
                    FreshrRoute.HOME -> HomeDashboardScreen(vm)
                    FreshrRoute.MANAGE_JOBS -> ManageJobsScreen(vm)
                    FreshrRoute.REPORTS -> ReportsScreen(vm)
                    FreshrRoute.SETTINGS -> SettingsScreen(vm)
                }
            }
        }
    }
}

@Composable
private fun DrawerNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = label, tint = Color.White) },
        label = { Text(label, color = Color.White, fontSize = 16.sp) },
        selected = selected,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = Color.White.copy(alpha = 0.16f),
            unselectedContainerColor = Color.Transparent
        ),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
    )
}