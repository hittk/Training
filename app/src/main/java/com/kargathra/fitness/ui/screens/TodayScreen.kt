package com.kargathra.fitness.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kargathra.fitness.data.sample.SampleData
import com.kargathra.fitness.ui.components.KCard
import com.kargathra.fitness.ui.components.SectionLabel
import com.kargathra.fitness.ui.components.Tag

@Composable
fun TodayScreen(
    healthConnected: Boolean,
    onConnectHealth: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val routine = SampleData.upperBodyDay
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionLabel("Today's session")
        KCard {
            Text(routine.title, style = MaterialTheme.typography.headlineSmall)
            Text(
                "${routine.items.size} exercises · ~${routine.estimatedMinutes} min",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                routine.focus.take(3).forEach { Tag(it.display) }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Text("Start workout")
            }
        }

        SectionLabel("Health Connect")
        KCard {
            Text(
                if (healthConnected) "Connected" else "Not connected",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "Logged sessions sync to Health Connect on this device, and your Fitbit's heart rate is read back over each one. Nothing is uploaded.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            if (!healthConnected) {
                OutlinedButton(onClick = onConnectHealth, modifier = Modifier.fillMaxWidth()) {
                    Text("Grant access")
                }
            }
        }
    }
}
