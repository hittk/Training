package com.kargathra.fitness.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kargathra.fitness.data.generator.LocalRoutineGenerator
import com.kargathra.fitness.data.model.*
import com.kargathra.fitness.data.repo.ExerciseRepository
import com.kargathra.fitness.ui.components.KCard
import com.kargathra.fitness.ui.components.SectionLabel
import com.kargathra.fitness.ui.components.Tag
import kotlinx.coroutines.launch

/**
 * Workout generator. Collects goal, experience, session length and target muscles,
 * then builds a balanced routine locally from the cached exercise library —
 * no network, no API key. Result is previewed and can be loaded onto the Workout page.
 */
@Composable
fun WorkoutGeneratorScreen(
    exerciseRepo: ExerciseRepository,
    includePunchBag: Boolean = false,
    onBack: () -> Unit,
    onLoadRoutine: (Routine) -> Unit,
    onSaveRoutine: (Routine) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    // Form state
    var goal by remember { mutableStateOf(Goal.HYPERTROPHY) }
    var experience by remember { mutableStateOf(Experience.INTERMEDIATE) }
    var daysPerWeek by remember { mutableIntStateOf(4) }
    var sessionMinutes by remember { mutableIntStateOf(45) }
    val focusAreas = remember { mutableStateListOf<MuscleGroup>() }

    // Result state
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var generatedRoutine by remember { mutableStateOf<Routine?>(null) }

    val availableFocusAreas = MuscleGroup.entries.filter { it != MuscleGroup.CARDIO }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ── Goal ──────────────────────────────────────────────────────────────
        SectionLabel("Your goal")
        KCard {
            Goal.entries.forEach { g ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = goal == g, onClick = { goal = g })
                    Spacer(Modifier.width(8.dp))
                    Text(g.display, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        // ── Experience ────────────────────────────────────────────────────────
        SectionLabel("Experience level")
        KCard {
            Experience.entries.forEach { e ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = experience == e, onClick = { experience = e })
                    Spacer(Modifier.width(8.dp))
                    Text(e.display, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        // ── Days / time ───────────────────────────────────────────────────────
        SectionLabel("Schedule")
        KCard {
            Text("Days per week: $daysPerWeek", style = MaterialTheme.typography.titleMedium)
            Slider(
                value = daysPerWeek.toFloat(),
                onValueChange = { daysPerWeek = it.toInt() },
                valueRange = 2f..6f,
                steps = 3
            )
            Spacer(Modifier.height(8.dp))
            Text("Session length: ~$sessionMinutes min", style = MaterialTheme.typography.titleMedium)
            Slider(
                value = sessionMinutes.toFloat(),
                onValueChange = { sessionMinutes = (it / 5).toInt() * 5 },
                valueRange = 20f..90f,
                steps = 13
            )
        }

        // ── Focus areas ───────────────────────────────────────────────────────
        SectionLabel("Target muscles (optional)")
        KCard {
            Text(
                "Leave blank for a balanced full-body session.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            availableFocusAreas.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { mg ->
                        val selected = mg in focusAreas
                        FilterChip(
                            selected = selected,
                            onClick = {
                                if (selected) focusAreas.remove(mg)
                                else focusAreas.add(mg)
                            },
                            label = { Text(mg.display) },
                            leadingIcon = if (selected) {
                                { Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Pad odd row
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        // ── Generate button ───────────────────────────────────────────────────
        errorMsg?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Button(
            onClick = {
                errorMsg = null
                isLoading = true
                generatedRoutine = null
                scope.launch {
                    try {
                        val library = exerciseRepo.generatorLibrary(includePunchBag)
                        if (library.isEmpty()) {
                            errorMsg = "Exercise library is still loading — try again in a moment."
                        } else {
                            generatedRoutine = LocalRoutineGenerator.generate(
                                library        = library,
                                goal           = goal,
                                experience     = experience,
                                sessionMinutes = sessionMinutes,
                                targetGroups   = focusAreas.toList(),
                                includePunchBag= includePunchBag
                            )
                        }
                    } catch (e: Exception) {
                        errorMsg = "Couldn't build a routine: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp).padding(end = 8.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Text(if (isLoading) "Building your workout…" else "Build my workout")
        }

        // ── Result preview ────────────────────────────────────────────────────
        AnimatedVisibility(visible = generatedRoutine != null) {
            generatedRoutine?.let { routine ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionLabel("Your workout")
                    KCard {
                        Text(routine.title, style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "${routine.items.size} exercises · ~${routine.estimatedMinutes} min",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            routine.focus.take(3).forEach { Tag(it.display) }
                        }
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(8.dp))
                        routine.items.forEach { item ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
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
                        Spacer(Modifier.height(12.dp))
                        var saved by remember(routine.id) { mutableStateOf(false) }
                        Button(
                            onClick = {
                                onLoadRoutine(routine)
                                onBack()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Load onto Workout page")
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { onSaveRoutine(routine); saved = true },
                            enabled = !saved,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (saved) "Saved to My Programs ✓" else "Save to My Programs")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back to Programs")
        }
    }
}
