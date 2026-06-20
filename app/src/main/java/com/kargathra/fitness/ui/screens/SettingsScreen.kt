package com.kargathra.fitness.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    healthStatusText: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionLabel("My equipment")
        KCard {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SampleData.ownedEquipment.forEach { Tag(it.display) }
            }
        }
        SectionLabel("Health Connect")
        KCard {
            Text(healthStatusText, style = MaterialTheme.typography.titleMedium)
            Text(
                "Strength sessions are written locally to Health Connect; heart rate and calories are read from your Fitbit. No account, no cloud, no telemetry.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        SectionLabel("About")
        KCard {
            Text("Kargathra", style = MaterialTheme.typography.titleLarge)
            Text("Version 0.1.0 — foundation build",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
