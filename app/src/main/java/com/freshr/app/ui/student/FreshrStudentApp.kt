package com.freshr.app.ui.student

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freshr.app.model.StudentRoute
import com.freshr.app.viewmodel.StudentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreshrStudentApp(
    vm: StudentViewModel = viewModel(),
    onLoggedOut: () -> Unit = {}
) {
    val routes = StudentRoute.entries

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Freshr · ${vm.currentRoute.title}") },
                actions = {
                    IconButton(onClick = onLoggedOut) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = "Log out"
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                routes.forEach { route ->
                    val icon = when (route) {
                        StudentRoute.HOME -> Icons.Outlined.Home
                        StudentRoute.JOB_MATCHES -> Icons.Outlined.WorkOutline
                        StudentRoute.APPLICATIONS -> Icons.AutoMirrored.Outlined.Assignment
                        StudentRoute.CV_STUDIO -> Icons.Outlined.Description
                        StudentRoute.SETTINGS -> Icons.Outlined.Person
                    }
                    NavigationBarItem(
                        selected = vm.currentRoute == route,
                        onClick = { vm.currentRoute = route },
                        icon = { Icon(imageVector = icon, contentDescription = route.title) },
                        label = { Text(route.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(innerPadding)) {
            when (vm.currentRoute) {
                StudentRoute.HOME -> StudentHomeScreen(vm)
                StudentRoute.JOB_MATCHES -> StudentJobMatchesScreen(vm)
                StudentRoute.APPLICATIONS -> StudentApplicationsScreen(vm)
                StudentRoute.CV_STUDIO -> CvStudioScreen(vm)
                StudentRoute.SETTINGS -> StudentProfileHeader(vm, onLogout = onLoggedOut)
            }
        }
    }
}
