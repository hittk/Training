package com.kargathra.fitness.ui

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kargathra.fitness.data.repo.ExerciseRepository
import com.kargathra.fitness.data.repo.SyncState
import com.kargathra.fitness.data.repo.WorkoutRepository
import com.kargathra.fitness.data.model.Routine
import com.kargathra.fitness.data.sample.SampleData
import com.kargathra.fitness.ui.navigation.Destination
import com.kargathra.fitness.ui.navigation.SETTINGS_ROUTE
import com.kargathra.fitness.ui.screens.*
import kotlinx.coroutines.launch

private const val GENERATOR_ROUTE = "generator"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KargathraApp(
    repo: WorkoutRepository,
    exerciseRepo: ExerciseRepository,
    healthConnected: Boolean,
    healthStatusText: String,
    onConnectHealth: () -> Unit
) {
    val nav   = rememberNavController()
    val scope = rememberCoroutineScope()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    // Shared state
    var activeRoutine by remember { mutableStateOf<Routine>(SampleData.upperBodyDay) }
    val context       = LocalContext.current
    val prefs         = remember { context.getSharedPreferences("kargathra", Context.MODE_PRIVATE) }
    var apiKey        by remember { mutableStateOf(prefs.getString("exercise_api_key", "") ?: "") }
    var anthropicKey  by remember { mutableStateOf(prefs.getString("anthropic_api_key", "") ?: "") }
    var hasPunchBag   by remember { mutableStateOf(false) }
    var exerciseCount by remember { mutableIntStateOf(0) }
    var syncState     by remember { mutableStateOf<SyncState>(SyncState.Idle) }

    // Refresh exercise count when sync state changes
    LaunchedEffect(syncState) {
        exerciseCount = exerciseRepo.count()
    }

    val syncStatus = when (val s = syncState) {
        is SyncState.Idle        -> ""
        is SyncState.Syncing     -> "Syncing… batch ${s.batchDone}/${s.batchTotal} (${s.exerciseCount} so far)"
        is SyncState.Done        -> "Synced ${s.count} exercises"
        is SyncState.RateLimited -> "Rate limited — try again in a few minutes"
        is SyncState.Error       -> "Error: ${s.message}"
    }

    val title = when {
        currentRoute == SETTINGS_ROUTE  -> "Settings"
        currentRoute == GENERATOR_ROUTE -> "Build my workout"
        currentRoute?.startsWith("log") == true -> "Log workout"
        else -> Destination.bottomBar.firstOrNull { it.route == currentRoute }?.label ?: "Kargathra"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title   = { Text(title, style = MaterialTheme.typography.headlineSmall) },
                actions = {
                    IconButton(onClick = { nav.navigate(SETTINGS_ROUTE) }) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
                colors  = TopAppBarDefaults.topAppBarColors(
                    containerColor     = MaterialTheme.colorScheme.background,
                    titleContentColor  = MaterialTheme.colorScheme.onBackground,
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
                        selected  = selected,
                        onClick   = {
                            nav.navigate(d.route) {
                                popUpTo(Destination.WORKOUT.route) { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        },
                        icon  = { Icon(d.icon, contentDescription = d.label) },
                        label = { Text(d.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor   = MaterialTheme.colorScheme.onPrimary,
                            indicatorColor      = MaterialTheme.colorScheme.primary,
                            selectedTextColor   = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { inner ->
        NavHost(
            navController      = nav,
            startDestination   = Destination.WORKOUT.route,
            modifier           = Modifier.padding(inner)
        ) {
            composable(Destination.WORKOUT.route) {
                WorkoutScreen(
                    healthConnected = healthConnected,
                    onConnectHealth = onConnectHealth,
                    activeRoutine   = activeRoutine,
                    onStart         = {
                        scope.launch {
                            val id = repo.startWorkout(activeRoutine.title)
                            nav.navigate("log/$id/${activeRoutine.id}")
                        }
                    }
                )
            }

            composable(Destination.PROGRAMS.route) {
                ProgramsScreen(
                    onBuildWorkout  = { nav.navigate(GENERATOR_ROUTE) },
                    onLoadRoutine   = { routine ->
                        activeRoutine = routine
                        nav.navigate(Destination.WORKOUT.route) {
                            popUpTo(Destination.WORKOUT.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Destination.EXERCISES.route) {
                ExercisesScreen(
                    repo        = exerciseRepo,
                    hasPunchBag = hasPunchBag
                )
            }

            composable(Destination.PROGRESS.route) { ProgressScreen(repo) }

            composable(SETTINGS_ROUTE) {
                SettingsScreen(
                    healthStatusText = healthStatusText,
                    apiKey           = apiKey,
                    onApiKeyChange   = {
                        apiKey = it
                        prefs.edit().putString("exercise_api_key", it).apply()
                    },
                    anthropicKey         = anthropicKey,
                    onAnthropicKeyChange = {
                        anthropicKey = it
                        prefs.edit().putString("anthropic_api_key", it).apply()
                    },
                    hasPunchBag      = hasPunchBag,
                    onPunchBagChange = { hasPunchBag = it },
                    exerciseCount    = exerciseCount,
                    syncStatus       = syncStatus,
                    onSyncNow        = {
                        scope.launch {
                            exerciseRepo.updateApiKey(apiKey)
                            exerciseRepo.sync { syncState = it }
                            exerciseCount = exerciseRepo.count()
                        }
                    }
                )
            }

            composable(GENERATOR_ROUTE) {
                WorkoutGeneratorScreen(
                    anthropicApiKey = anthropicKey,
                    onBack          = { nav.popBackStack() },
                    onLoadRoutine = { routine ->
                        activeRoutine = routine
                        nav.navigate(Destination.WORKOUT.route) {
                            popUpTo(Destination.WORKOUT.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route     = "log/{workoutId}/{routineId}",
                arguments = listOf(
                    navArgument("workoutId") { type = NavType.LongType },
                    navArgument("routineId") { type = NavType.StringType }
                )
            ) { entry ->
                val wid     = entry.arguments?.getLong("workoutId") ?: 0L
                val rid     = entry.arguments?.getString("routineId") ?: activeRoutine.id
                val routine = if (rid == activeRoutine.id) activeRoutine
                              else SampleData.routineById(rid) ?: activeRoutine
                LogWorkoutScreen(
                    repo      = repo,
                    workoutId = wid,
                    routine   = routine,
                    onFinish  = {
                        nav.navigate(Destination.WORKOUT.route) {
                            popUpTo(Destination.WORKOUT.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}
