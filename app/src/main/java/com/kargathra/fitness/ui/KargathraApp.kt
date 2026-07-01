package com.kargathra.fitness.ui

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import com.kargathra.fitness.ui.screens.SessionSummaryScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kargathra.fitness.data.repo.ExerciseRepository
import com.kargathra.fitness.data.backup.BackupManager
import com.kargathra.fitness.data.db.WorkoutEntity
import com.kargathra.fitness.ui.components.SplashVideo
import com.kargathra.fitness.data.repo.FavouriteRepository
import com.kargathra.fitness.data.repo.SavedRoutineRepository
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
    favRepo: FavouriteRepository,
    savedRepo: SavedRoutineRepository,
    backup: BackupManager,
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
    val savedRoutines by savedRepo.savedRoutines.collectAsStateWithLifecycle(emptyList())
    val context       = LocalContext.current
    val prefs         = remember { context.getSharedPreferences("kargathra", Context.MODE_PRIVATE) }
    var hasPunchBag   by remember { mutableStateOf(prefs.getBoolean("has_punch_bag", false)) }
    var resumeWorkout by remember { mutableStateOf<WorkoutEntity?>(null) }
    LaunchedEffect(Unit) {
        val savedId = prefs.getLong("active_workout_id", -1L)
        if (savedId > 0) {
            val w = repo.getWorkout(savedId)
            if (w != null && w.completedAt == null) resumeWorkout = w
            else prefs.edit().remove("active_workout_id").remove("active_routine_json").apply()
        }
    }
    var exerciseCount by remember { mutableIntStateOf(0) }

    // Load the bundled library and report the count
    LaunchedEffect(Unit) {
        exerciseCount = exerciseRepo.ensureLoaded()
    }


    val title = when {
        currentRoute == SETTINGS_ROUTE  -> "Settings"
        currentRoute == GENERATOR_ROUTE -> "Build my workout"
        currentRoute?.startsWith("log") == true -> "Log workout"
        else -> Destination.bottomBar.firstOrNull { it.route == currentRoute }?.label ?: "Kargathra"
    }

    var showSplash by rememberSaveable { mutableStateOf(true) }
    if (showSplash) {
        SplashVideo(onDone = { showSplash = false })
        return
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
                    activeRoutine   = activeRoutine,
                    onStart         = {
                        scope.launch {
                            val id = repo.startWorkout(activeRoutine.title)
                            prefs.edit()
                                .putLong("active_workout_id", id)
                                .putString("active_routine_json", savedRepo.toJson(activeRoutine))
                                .apply()
                            resumeWorkout = null
                            nav.navigate("log/$id/${activeRoutine.id}")
                        }
                    },
                    resumeTitle     = resumeWorkout?.title,
                    onResume        = {
                        val w = resumeWorkout ?: return@WorkoutScreen
                        prefs.getString("active_routine_json", null)?.let { json ->
                            runCatching { savedRepo.fromJson(json) }.getOrNull()?.let { r ->
                                activeRoutine = r
                            }
                        }
                        nav.navigate("log/${w.id}/${activeRoutine.id}")
                    },
                    onDiscardResume = {
                        prefs.edit().remove("active_workout_id").remove("active_routine_json").apply()
                        resumeWorkout = null
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
                    },
                    savedRoutines   = savedRoutines,
                    onDeleteRoutine = { routine -> scope.launch { savedRepo.delete(routine.id) } },
                    onRenameProgram = { routine, name -> scope.launch { savedRepo.rename(routine.id, name) } },
                    onRemoveItem    = { routine, idx -> scope.launch { savedRepo.removeItemAt(routine.id, idx) } },
                    onEditItem      = { routine, idx, s, reps, rest ->
                        scope.launch { savedRepo.updateItemAt(routine.id, idx, s, reps, rest) }
                    },
                    onCreateProgram = { scope.launch { savedRepo.createEmpty("New Program") } }
                )
            }

            composable(Destination.EXERCISES.route) {
                ExercisesScreen(
                    repo        = exerciseRepo,
                    favRepo     = favRepo,
                    savedRepo   = savedRepo,
                    hasPunchBag = hasPunchBag
                )
            }

            composable(Destination.PROGRESS.route) {
                ProgressScreen(
                    repo = repo,
                    onOpenSession = { id -> nav.navigate("history_summary/$id") }
                )
            }

            composable(SETTINGS_ROUTE) {
                SettingsScreen(
                    backup           = backup,
                    healthStatusText = healthStatusText,
                    healthConnected  = healthConnected,
                    onConnectHealth  = onConnectHealth,
                    hasPunchBag      = hasPunchBag,
                    onPunchBagChange = {
                        hasPunchBag = it
                        prefs.edit().putBoolean("has_punch_bag", it).apply()
                    },
                    exerciseCount    = exerciseCount
                )
            }

            composable(GENERATOR_ROUTE) {
                WorkoutGeneratorScreen(
                    exerciseRepo    = exerciseRepo,
                    includePunchBag = hasPunchBag,
                    onBack          = { nav.popBackStack() },
                    onSaveRoutine   = { routine -> scope.launch { savedRepo.save(routine) } },
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
                    repo         = repo,
                    exerciseRepo = exerciseRepo,
                    workoutId = wid,
                    routine   = routine,
                    onFinish  = {
                        prefs.edit().remove("active_workout_id").remove("active_routine_json").apply()
                        resumeWorkout = null
                        nav.navigate("summary/$wid") {
                            popUpTo(Destination.WORKOUT.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                "summary/{workoutId}",
                arguments = listOf(navArgument("workoutId") { type = NavType.LongType })
            ) { backEntry ->
                val sid = backEntry.arguments?.getLong("workoutId") ?: 0L
                SessionSummaryScreen(
                    repo = repo,
                    workoutId = sid,
                    onDone = {
                        nav.navigate(Destination.WORKOUT.route) {
                            popUpTo(Destination.WORKOUT.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                "history_summary/{workoutId}",
                arguments = listOf(navArgument("workoutId") { type = NavType.LongType })
            ) { backEntry ->
                val sid = backEntry.arguments?.getLong("workoutId") ?: 0L
                SessionSummaryScreen(
                    repo = repo,
                    workoutId = sid,
                    onDone = { nav.popBackStack() }
                )
            }
        }
    }
}
