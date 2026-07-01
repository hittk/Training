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
fun RoutinesScreen(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionLabel("Generate a session")
        KCard {
            Text("Build me a workout", style = MaterialTheme.typography.titleLarge)
            Text(
                "Pick target areas, days per week and time available — Kargathra assembles a balanced, progressive session from your equipment.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                Text("Generator — coming next")
            }
        }

        SectionLabel("Preset programs")
        val p = SampleData.starterProgram
        KCard {
            Text(p.title, style = MaterialTheme.typography.titleLarge)
            Text(
                p.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tag("${p.daysPerWeek} days/wk")
                Tag("No rack needed")
            }
        }
    }
}
