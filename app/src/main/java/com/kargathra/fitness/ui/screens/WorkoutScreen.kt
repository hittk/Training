package com.kargathra.fitness.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionLabel("Today's session")
        KCard {
            Text(activeRoutine.title, style = MaterialTheme.typography.headlineSmall)
            Text(
                "${activeRoutine.items.size} exercises · ~${activeRoutine.estimatedMinutes} min",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                activeRoutine.focus.take(3).forEach { Tag(it.display) }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Text("Start workout")
            }
        }
    }
}
