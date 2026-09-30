package com.todoink.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.todoink.app.ui.candidates.CandidatesScreen
import com.todoink.app.ui.notifications.NotificationDetailScreen
import com.todoink.app.ui.notifications.NotificationsScreen
import com.todoink.app.ui.settings.AboutScreen
import com.todoink.app.ui.settings.RetentionScreen
import com.todoink.app.ui.settings.SettingsScreen
import com.todoink.app.ui.settings.SourcesScreen
import com.todoink.app.ui.status.StatusScreen
import com.todoink.app.ui.todo.TodoHomeScreen

private data class Destination(val route: String, val label: String, val icon: ImageVector)

/** 预览稿导航：待办 / 待确认 / 通知 / 设置；状态并入设置。 */
private val destinations = listOf(
    Destination("todo", "待办", Icons.Filled.Checklist),
    Destination("candidates", "待确认", Icons.Filled.Inbox),
    Destination("notifications", "通知", Icons.Filled.Notifications),
    Destination("settings", "设置", Icons.Filled.Settings),
)

private const val DETAIL_ROUTE = "notification/{snapshotId}"

@Composable
fun TodoInkApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevel = destinations.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (isTopLevel) {
                NavigationBar {
                    destinations.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "todo",
            modifier = Modifier.padding(padding),
        ) {
            composable("todo") { TodoHomeScreen() }
            composable("candidates") { CandidatesScreen() }
            composable("notifications") {
                NotificationsScreen(
                    onOpenSnapshot = { id -> navController.navigate("notification/$id") },
                )
            }
            composable(
                route = DETAIL_ROUTE,
                arguments = listOf(navArgument("snapshotId") { type = NavType.LongType }),
            ) {
                NotificationDetailScreen(onBack = { navController.popBackStack() })
            }
            composable("settings") {
                SettingsScreen(
                    onOpenSources = { navController.navigate("sources") },
                    onOpenStatus = { navController.navigate("status") },
                    onOpenRetention = { navController.navigate("retention") },
                    onOpenAbout = { navController.navigate("about") },
                )
            }
            composable("sources") { SourcesScreen(onBack = { navController.popBackStack() }) }
            composable("status") { StatusScreen(onBack = { navController.popBackStack() }) }
            composable("retention") { RetentionScreen(onBack = { navController.popBackStack() }) }
            composable("about") { AboutScreen(onBack = { navController.popBackStack() }) }
        }
    }
}
