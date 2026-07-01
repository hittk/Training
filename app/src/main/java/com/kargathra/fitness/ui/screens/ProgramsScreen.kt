package com.kargathra.fitness.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kargathra.fitness.data.model.Program
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import com.kargathra.fitness.data.model.Routine
import com.kargathra.fitness.data.sample.SampleData
import com.kargathra.fitness.ui.components.KCard
import com.kargathra.fitness.ui.components.SectionLabel
import com.kargathra.fitness.ui.components.Tag

/**
 * "Programs" tab.
 * – "Build me a workout" launches the AI generator.
 * – Preset programs are expandable; each day inside can be loaded onto the Workout tab.
 */
@Composable
fun ProgramsScreen(
    onBuildWorkout: () -> Unit,
    onLoadRoutine: (Routine) -> Unit,
    savedRoutines: List<Routine> = emptyList(),
    onDeleteRoutine: (Routine) -> Unit = {},
    onRenameProgram: (Routine, String) -> Unit = { _, _ -> },
    onRemoveItem: (Routine, Int) -> Unit = { _, _ -> },
    onEditItem: (Routine, Int, Int, IntRange, Int) -> Unit = { _, _, _, _, _ -> },
    onCreateProgram: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionLabel("AI Generator")
        KCard {
            Text("Build me a workout", style = MaterialTheme.typography.titleLarge)
            Text(
                "Tell Kargathra your goals, available time and focus areas — it assembles a balanced session tailored to your equipment.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            Button(onClick = onBuildWorkout, modifier = Modifier.fillMaxWidth()) {
                Text("Build me a workout")
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionLabel("My Programs")
            TextButton(onClick = onCreateProgram) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("New")
            }
        }
        if (savedRoutines.isEmpty()) {
            KCard {
                Text(
                    "No custom programs yet. Tap “New” to create one, generate a workout and save it, or add exercises from the Exercises tab.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            savedRoutines.forEach { routine ->
                KCard {
                    SavedRoutineRow(
                        routine    = routine,
                        onLoad     = { onLoadRoutine(routine) },
                        onDelete   = { onDeleteRoutine(routine) },
                        onRename   = { newName -> onRenameProgram(routine, newName) },
                        onRemoveItem = { idx -> onRemoveItem(routine, idx) },
                        onEditItem   = { idx, s, r, rest -> onEditItem(routine, idx, s, r, rest) }
                    )
                }
            }
        }

        SectionLabel("Preset programs")
        SampleData.allPrograms.forEach { program ->
            ProgramCard(program = program, onLoadRoutine = onLoadRoutine)
        }
    }
}

@Composable
private fun ProgramCard(
    program: Program,
    onLoadRoutine: (Routine) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    KCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(program.title, style = MaterialTheme.typography.titleLarge)
                Text(
                    program.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tag("${program.daysPerWeek} days/wk")
                    Tag("${program.days.size} sessions")
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(2.dp))
                program.days.distinctBy { it.id }.forEach { routine ->
                    RoutineRow(routine = routine, onLoad = { onLoadRoutine(routine) })
                }
            }
        }
    }
}

@Composable
private fun RoutineRow(routine: Routine, onLoad: () -> Unit) {
    var showDetail by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDetail = !showDetail }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(routine.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${routine.items.size} exercises · ~${routine.estimatedMinutes} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = if (showDetail) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(visible = showDetail) {
            Column(Modifier.padding(bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                routine.items.forEach { item ->
                    Row(Modifier.padding(vertical = 2.dp)) {
                        Text(
                            "• ${item.exercise.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${item.sets} × ${item.repTarget.first}–${item.repTarget.last}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onLoad, modifier = Modifier.fillMaxWidth()) {
                    Text("Load onto Workout page")
                }
            }
        }
    }
}

@Composable
private fun SavedRoutineRow(
    routine: Routine,
    onLoad: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onEditItem: (Int, Int, IntRange, Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var editingIdx by remember { mutableStateOf<Int?>(null) }

    Column {
        Row(
            Modifier.fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(routine.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${routine.items.size} exercises · ~${routine.estimatedMinutes} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (routine.items.isEmpty()) {
                    Text(
                        "No exercises yet — add some from the Exercises tab.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    routine.items.forEachIndexed { idx, item ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable { editingIdx = idx }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "• ${item.exercise.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${item.sets} × ${item.repTarget.first}–${item.repTarget.last}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            IconButton(onClick = { onRemoveItem(idx) }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    Icons.Filled.RemoveCircleOutline,
                                    contentDescription = "Remove ${item.exercise.name}",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onLoad,
                        enabled = routine.items.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) { Text("Load") }
                    OutlinedButton(onClick = { renaming = true }) { Text("Rename") }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete program",
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    editingIdx?.let { idx ->
        val item = routine.items.getOrNull(idx)
        if (item == null) { editingIdx = null } else {
            var setsText by remember(idx) { mutableStateOf(item.sets.toString()) }
            var repFromText by remember(idx) { mutableStateOf(item.repTarget.first.toString()) }
            var repToText by remember(idx) { mutableStateOf(item.repTarget.last.toString()) }
            var restText by remember(idx) { mutableStateOf(item.restSeconds.toString()) }
            AlertDialog(
                onDismissRequest = { editingIdx = null },
                title = { Text(item.exercise.name) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = setsText,
                            onValueChange = { setsText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Sets") }, singleLine = true
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = repFromText,
                                onValueChange = { repFromText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Reps from") }, singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = repToText,
                                onValueChange = { repToText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Reps to") }, singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = restText,
                            onValueChange = { restText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Rest (seconds)") }, singleLine = true
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val s = setsText.toIntOrNull() ?: item.sets
                        val rf = repFromText.toIntOrNull() ?: item.repTarget.first
                        val rt = repToText.toIntOrNull() ?: item.repTarget.last
                        val rest = restText.toIntOrNull() ?: item.restSeconds
                        val lo = minOf(rf, rt).coerceAtLeast(1)
                        val hi = maxOf(rf, rt).coerceAtLeast(1)
                        onEditItem(idx, s.coerceIn(1, 20), lo..hi, rest.coerceIn(0, 600))
                        editingIdx = null
                    }) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = { editingIdx = null }) { Text("Cancel") } }
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete program?") },
            text = { Text("Remove \"${routine.title}\" from My Programs? This can't be undone.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }

    if (renaming) {
        var text by remember { mutableStateOf(routine.title) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text("Rename program") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    label = { Text("Program name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (text.isNotBlank()) onRename(text.trim())
                    renaming = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text("Cancel") } }
        )
    }
}
