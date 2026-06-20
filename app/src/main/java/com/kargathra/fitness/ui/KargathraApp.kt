package com.kargathra.fitness.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kargathra.fitness.data.repo.WorkoutRepository
import com.kargathra.fitness.ui.navigation.Destination
import com.kargathra.fitness.ui.navigation.SETTINGS_ROUTE
import com.kargathra.fitness.ui.screens.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KargathraApp(
    repo: WorkoutRepository,
    healthConnected: Boolean,
    healthStatusText: String,
    onConnectHealth: () -> Unit
) {
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val title = when {
        currentRoute == SETTINGS_ROUTE -> "Settings"
        currentRoute?.startsWith("log") == true -> "Log workout"
        else -> Destination.bottomBar.firstOrNull { it.route == currentRoute }?.label ?: "Kargathra"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
                actions = {
                    IconButton(onClick = { nav.navigate(SETTINGS_ROUTE) }) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                val dest = backStack?.destination
                Destination.bottomBar.forEach { d ->
                    val selected = dest?.hierarchy?.any { it.route == d.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            nav.navigate(d.route) {
                                popUpTo(Destination.TODAY.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(d.icon, contentDescription = d.label) },
                        label = { Text(d.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = Destination.TODAY.route,
            modifier = Modifier.padding(inner)
        ) {
            composable(Destination.TODAY.route) {
                TodayScreen(
                    healthConnected = healthConnected,
                    onConnectHealth = onConnectHealth,
                    onStart = {
                        scope.launch {
                            val id = repo.startWorkout("Upper Body A")
                            nav.navigate("log/$id")
                        }
                    }
                )
            }
            composable(Destination.ROUTINES.route) { RoutinesScreen() }
            composable(Destination.EXERCISES.route) { ExercisesScreen() }
            composable(Destination.PROGRESS.route) { ProgressScreen(repo) }
            composable(SETTINGS_ROUTE) { SettingsScreen(healthStatusText) }
            composable(
                route = "log/{workoutId}",
                arguments = listOf(navArgument("workoutId") { type = NavType.LongType })
            ) { entry ->
                val wid = entry.arguments?.getLong("workoutId") ?: 0L
                LogWorkoutScreen(
                    repo = repo,
                    workoutId = wid,
                    onFinish = {
                        nav.navigate(Destination.PROGRESS.route) {
                            popUpTo(Destination.TODAY.route)
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}
