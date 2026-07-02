package com.kargathra.fitness.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.kargathra.fitness.ui.components.PlateCalculatorCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kargathra.fitness.data.model.Routine
import com.kargathra.fitness.ui.components.KCard
import com.kargathra.fitness.ui.components.SectionLabel
import com.kargathra.fitness.ui.components.Tag

/**
 * "Workout" tab — shows the current routine queued up and lets the user start it.
 * [activeRoutine] is either the default upper-body day or whatever was loaded from
 * the Programs tab (preset or AI-generated).
 */
@Composable
fun WorkoutScreen(
    activeRoutine: Routine,
    onStart: () -> Unit,
    resumeTitle: String? = null,
    onResume: () -> Unit = {},
    onDiscardResume: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (resumeTitle != null) {
            SectionLabel("In progress")
            KCard {
                Text(resumeTitle, style = MaterialTheme.typography.titleMedium)
                Text(
                    "You have an unfinished session.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onResume, modifier = Modifier.weight(1f)) {
                        Text("Resume")
                    }
                    OutlinedButton(onClick = onDiscardResume) { Text("Discard") }
                }
            }
        }

        SectionLabel("Today's session")
        KCard {
            Text(activeRoutine.title, style = MaterialTheme.typography.headlineSmall)
            var expanded by remember { mutableStateOf(false) }
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${activeRoutine.items.size} exercises · ~${activeRoutine.estimatedMinutes} min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp).weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Show exercises",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    activeRoutine.items.forEach { item ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                activeRoutine.focus.take(3).forEach { Tag(it.display) }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Text("Start workout")
            }
        }

        PlateCalculatorCard()
    }
}
