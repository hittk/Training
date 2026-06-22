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
