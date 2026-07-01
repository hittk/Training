package com.kargathra.fitness.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kargathra.fitness.data.db.SetEntity
import com.kargathra.fitness.data.model.Exercise
import com.kargathra.fitness.data.model.Routine
import com.kargathra.fitness.data.repo.ExerciseRepository
import com.kargathra.fitness.data.db.ExerciseEntity
import com.kargathra.fitness.ui.components.ExerciseVideo
import com.kargathra.fitness.ui.components.hasAnyVideo
import androidx.compose.material.icons.outlined.Info
import com.kargathra.fitness.data.repo.WorkoutRepository
import com.kargathra.fitness.ui.components.KCard
import com.kargathra.fitness.ui.components.SectionLabel
import com.kargathra.fitness.ui.theme.Gold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LogWorkoutScreen(
    repo: WorkoutRepository,
    exerciseRepo: ExerciseRepository,
    workoutId: Long,
    routine: Routine,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val sets by repo.observeSets(workoutId).collectAsStateWithLifecycle(emptyList())

    // Rest timer state — shared across all exercise cards
    var restSecondsTotal by remember { mutableIntStateOf(0) }
    var restSecondsLeft  by remember { mutableIntStateOf(0) }
    var timerActive      by remember { mutableStateOf(false) }

    // Tick the timer down every second
    LaunchedEffect(timerActive) {
        if (!timerActive) return@LaunchedEffect
        while (restSecondsLeft > 0) {
            delay(1_000L)
            restSecondsLeft--
        }
        timerActive = false
    }

    fun startTimer(seconds: Int) {
        restSecondsTotal = seconds
        restSecondsLeft  = seconds
        timerActive      = true
    }

    val totalSets   = sets.size
    val totalVolume = sets.sumOf { it.weightKg * it.reps }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        item {
            KCard {
                Text(routine.title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    "$totalSets sets logged  ·  ${fmt(totalVolume)} kg total volume",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // ── Rest timer banner ─────────────────────────────────────────────────
        item {
            AnimatedVisibility(
                visible = timerActive || restSecondsLeft > 0,
                enter = fadeIn(),
                exit  = fadeOut()
            ) {
                RestTimerBanner(
                    secondsLeft  = restSecondsLeft,
                    secondsTotal = restSecondsTotal,
                    onDismiss    = { timerActive = false; restSecondsLeft = 0 }
                )
            }
        }

        // ── Exercise cards ────────────────────────────────────────────────────
        items(routine.items.size) { idx ->
            val item     = routine.items[idx]
            val exercise = item.exercise
            val logged   = sets.filter { it.exerciseId == exercise.id }

            // Load the personal best for this exercise once
            var pb by remember { mutableStateOf<SetEntity?>(null) }
            LaunchedEffect(exercise.id) {
                pb = repo.bestSetForExercise(exercise.id)
            }

            ExerciseLogCard(
                exercise   = exercise,
                target     = "${item.sets} × ${item.repTarget.first}–${item.repTarget.last}",
                logged     = logged,
                personalBest = pb,
                exerciseRepo = exerciseRepo,
                onAdd      = { w, r ->
                    scope.launch {
                        repo.addSet(workoutId, exercise, w, r)
                        startTimer(item.restSeconds)
                    }
                },
                onDelete   = { s -> scope.launch { repo.deleteSet(s) } }
            )
        }

        // ── Finish button ─────────────────────────────────────────────────────
        item {
            Button(
                onClick = { scope.launch { repo.finishWorkout(workoutId); onFinish() } },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            ) { Text("Finish workout") }
            Spacer(Modifier.height(8.dp))
            Text(
                "Finishing writes the session to Health Connect and pulls your Fitbit heart rate over the window.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Rest timer banner ─────────────────────────────────────────────────────────

@Composable
private fun RestTimerBanner(
    secondsLeft: Int,
    secondsTotal: Int,
    onDismiss: () -> Unit
) {
    val progress = if (secondsTotal > 0) secondsLeft.toFloat() / secondsTotal else 0f
    val mins     = secondsLeft / 60
    val secs     = secondsLeft % 60
    val label    = if (mins > 0) "${mins}m ${secs.toString().padStart(2, '0')}s"
                   else "${secs}s"

    Surface(
        color  = MaterialTheme.colorScheme.primaryContainer,
        shape  = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Timer,
                    contentDescription = null,
                    tint   = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Rest  $label",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDismiss) {
                    Text("Skip", color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress       = { progress },
                modifier       = Modifier.fillMaxWidth(),
                color          = Gold,
                trackColor     = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
            )
        }
    }
}

// ── Exercise log card ─────────────────────────────────────────────────────────

@Composable
private fun ExerciseLogCard(
    exercise: Exercise,
    target: String,
    logged: List<SetEntity>,
    personalBest: SetEntity?,
    exerciseRepo: ExerciseRepository,
    onAdd: (Double, Int) -> Unit,
    onDelete: (SetEntity) -> Unit
) {
    var showInfo by remember { mutableStateOf(false) }
    KCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                exercise.name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showInfo = true }) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = "Exercise details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            "Target: $target reps",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )

        if (showInfo) {
            ExerciseInfoDialog(
                exercise = exercise,
                target = target,
                exerciseRepo = exerciseRepo,
                onDismiss = { showInfo = false }
            )
        }

        // Progressive overload suggestion
        if (personalBest != null) {
            val suggestedWeight = nextWeight(personalBest.weightKg, personalBest.reps, exercise.repRange)
            val suggestion = buildString {
                append("Last PB: ${fmt(personalBest.weightKg)} kg × ${personalBest.reps}")
                if (suggestedWeight != null) append("  →  try ${fmt(suggestedWeight)} kg")
            }
            Text(
                suggestion,
                style = MaterialTheme.typography.labelMedium,
                color = Gold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (logged.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            logged.forEachIndexed { i, s -> LoggedSetRow(i + 1, s, onDelete) }
        }

        Spacer(Modifier.height(12.dp))
        SetEntryRow(
            initialWeight = personalBest?.weightKg ?: logged.lastOrNull()?.weightKg ?: 20.0,
            initialReps   = logged.lastOrNull()?.reps ?: exercise.repRange.first.coerceAtLeast(1),
            onAdd         = onAdd
        )
    }
}

/**
 * Suggests the next weight increment given the best historical set.
 * If reps were at or above the top of the target range, nudge weight up 2.5 kg.
 * If reps were in range, keep the same weight. Below range, no suggestion.
 */
private fun nextWeight(bestWeight: Double, bestReps: Int, targetRange: IntRange): Double? {
    return when {
        bestReps >= targetRange.last -> bestWeight + 2.5
        bestReps >= targetRange.first -> bestWeight
        else -> null
    }
}

// ── Logged set row ────────────────────────────────────────────────────────────

@Composable
private fun LoggedSetRow(index: Int, s: SetEntity, onDelete: (SetEntity) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Set $index",
            style    = MaterialTheme.typography.labelMedium,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(48.dp)
        )
        Text(
            "${fmt(s.weightKg)} kg × ${s.reps}",
            style    = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = { onDelete(s) }) {
            Icon(Icons.Filled.Close, contentDescription = "Delete set",
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Set entry row ─────────────────────────────────────────────────────────────

@Composable
private fun SetEntryRow(
    initialWeight: Double,
    initialReps: Int,
    onAdd: (Double, Int) -> Unit
) {
    var weight by remember { mutableStateOf(fmt(initialWeight)) }
    var reps   by remember { mutableStateOf(initialReps.toString()) }

    fun adjustWeight(delta: Double) {
        weight = fmt(((weight.toDoubleOrNull() ?: 0.0) + delta).coerceAtLeast(0.0))
    }
    fun adjustReps(delta: Int) {
        reps = ((reps.toIntOrNull() ?: 0) + delta).coerceAtLeast(0).toString()
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Stepper(
            label    = "kg",
            value    = weight,
            onValue  = { weight = it },
            onMinus  = { adjustWeight(-2.5) },
            onPlus   = { adjustWeight(2.5) },
            keyboard = KeyboardType.Decimal,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(10.dp))
        Stepper(
            label    = "reps",
            value    = reps,
            onValue  = { reps = it },
            onMinus  = { adjustReps(-1) },
            onPlus   = { adjustReps(1) },
            keyboard = KeyboardType.Number,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(10.dp))
        FilledIconButton(onClick = {
            val w = weight.toDoubleOrNull()
            val r = reps.toIntOrNull()
            if (w != null && r != null && r > 0) onAdd(w, r)
        }) { Icon(Icons.Filled.Add, contentDescription = "Add set") }
    }
}

// ── Stepper ───────────────────────────────────────────────────────────────────

@Composable
private fun Stepper(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    keyboard: KeyboardType,
    modifier: Modifier = Modifier
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onMinus) {
            Icon(Icons.Filled.Remove, contentDescription = "Decrease $label")
        }
        OutlinedTextField(
            value          = value,
            onValueChange  = onValue,
            label          = { Text(label) },
            singleLine     = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            modifier       = Modifier.weight(1f)
        )
        IconButton(onClick = onPlus) {
            Icon(Icons.Filled.Add, contentDescription = "Increase $label")
        }
    }
}

internal fun fmt(v: Double): String =
    if (v % 1.0 == 0.0) v.toLong().toString()
    else (Math.round(v * 100) / 100.0).toString()

@Composable
private fun ExerciseInfoDialog(
    exercise: Exercise,
    target: String,
    exerciseRepo: ExerciseRepository,
    onDismiss: () -> Unit
) {
    // Try to resolve the full library entry by id (works for exercises added
    // from the Exercises tab; preset/generated ids may not match the library).
    var entity by remember(exercise.id) { mutableStateOf<ExerciseEntity?>(null) }
    LaunchedEffect(exercise.id) { entity = exerciseRepo.getById(exercise.id) }

    val instructions: List<String> = entity?.instructions
        ?.split("|")?.map { it.trim() }?.filter { it.isNotEmpty() }
        ?: exercise.cues

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text(exercise.name) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Target: $target reps",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                // Video, if one exists (library url or bundled/user clip)
                val vUrl = entity?.videoUrl ?: ""
                if (hasAnyVideo(vUrl, exercise.id)) {
                    ExerciseVideo(
                        videoUrl = vUrl,
                        fallbackId = exercise.id,
                        modifier = Modifier.clip(MaterialTheme.shapes.medium)
                    )
                }

                // How to perform
                if (instructions.isNotEmpty()) {
                    Text("How to perform",
                        style = MaterialTheme.typography.titleSmall)
                    instructions.forEachIndexed { i, step ->
                        Text("${i + 1}. $step",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    Text("No detailed instructions available for this exercise.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Tips if available
                val tips = entity?.exerciseTips
                    ?.split("|")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                if (tips.isNotEmpty()) {
                    Text("Tips", style = MaterialTheme.typography.titleSmall)
                    tips.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
    )
}
