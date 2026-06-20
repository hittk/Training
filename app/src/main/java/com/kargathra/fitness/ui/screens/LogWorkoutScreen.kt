package com.kargathra.fitness.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kargathra.fitness.data.db.SetEntity
import com.kargathra.fitness.data.model.Exercise
import com.kargathra.fitness.data.repo.WorkoutRepository
import com.kargathra.fitness.data.sample.SampleData
import com.kargathra.fitness.ui.components.KCard
import com.kargathra.fitness.ui.components.SectionLabel
import kotlinx.coroutines.launch

@Composable
fun LogWorkoutScreen(
    repo: WorkoutRepository,
    workoutId: Long,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val sets by repo.observeSets(workoutId).collectAsStateWithLifecycle(emptyList())
    val planned = SampleData.upperBodyDay.items

    val totalSets = sets.size
    val totalVolume = sets.sumOf { it.weightKg * it.reps }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            KCard {
                Text(SampleData.upperBodyDay.title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    "$totalSets sets logged  ·  ${fmt(totalVolume)} kg total volume",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        items(planned.size) { idx ->
            val item = planned[idx]
            val exercise = item.exercise
            val logged = sets.filter { it.exerciseId == exercise.id }
            ExerciseLogCard(
                exercise = exercise,
                target = "${item.sets} × ${item.repTarget.first}-${item.repTarget.last}",
                logged = logged,
                onAdd = { w, r -> scope.launch { repo.addSet(workoutId, exercise, w, r) } },
                onDelete = { s -> scope.launch { repo.deleteSet(s) } }
            )
        }

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

@Composable
private fun ExerciseLogCard(
    exercise: Exercise,
    target: String,
    logged: List<SetEntity>,
    onAdd: (Double, Int) -> Unit,
    onDelete: (SetEntity) -> Unit
) {
    KCard {
        Text(exercise.name, style = MaterialTheme.typography.titleLarge)
        Text(
            "Target: $target reps",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
        if (logged.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            logged.forEachIndexed { i, s -> LoggedSetRow(i + 1, s, onDelete) }
        }
        Spacer(Modifier.height(12.dp))
        SetEntryRow(
            initialWeight = logged.lastOrNull()?.weightKg ?: 20.0,
            initialReps = logged.lastOrNull()?.reps ?: exercise.repRange.first.coerceAtLeast(1),
            onAdd = onAdd
        )
    }
}

@Composable
private fun LoggedSetRow(index: Int, s: SetEntity, onDelete: (SetEntity) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Set $index",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(64.dp)
        )
        Text(
            "${fmt(s.weightKg)} kg × ${s.reps}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = { onDelete(s) }) {
            Icon(Icons.Filled.Close, contentDescription = "Delete set",
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetEntryRow(
    initialWeight: Double,
    initialReps: Int,
    onAdd: (Double, Int) -> Unit
) {
    var weight by remember { mutableStateOf(fmt(initialWeight)) }
    var reps by remember { mutableStateOf(initialReps.toString()) }

    fun adjustWeight(delta: Double) {
        val v = (weight.toDoubleOrNull() ?: 0.0) + delta
        weight = fmt(v.coerceAtLeast(0.0))
    }
    fun adjustReps(delta: Int) {
        val v = (reps.toIntOrNull() ?: 0) + delta
        reps = v.coerceAtLeast(0).toString()
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Stepper(
            label = "kg",
            value = weight,
            onValue = { weight = it },
            onMinus = { adjustWeight(-2.5) },
            onPlus = { adjustWeight(2.5) },
            keyboard = KeyboardType.Decimal,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(10.dp))
        Stepper(
            label = "reps",
            value = reps,
            onValue = { reps = it },
            onMinus = { adjustReps(-1) },
            onPlus = { adjustReps(1) },
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

@OptIn(ExperimentalMaterial3Api::class)
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
            value = value,
            onValueChange = onValue,
            label = { Text(label) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onPlus) {
            Icon(Icons.Filled.Add, contentDescription = "Increase $label")
        }
    }
}

/** Trim trailing .0 for whole numbers (e.g. 20.0 -> "20", 22.5 -> "22.5"). */
internal fun fmt(v: Double): String =
    if (v % 1.0 == 0.0) v.toLong().toString() else ((Math.round(v * 100) / 100.0).toString())
