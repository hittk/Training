package com.kargathra.fitness.ui.screens

import com.kargathra.fitness.BuildConfig

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kargathra.fitness.ui.components.KCard
import com.kargathra.fitness.ui.components.SectionLabel

@Composable
fun SettingsScreen(
    healthStatusText: String,
    healthConnected: Boolean,
    onConnectHealth: () -> Unit,
    hasPunchBag: Boolean,
    onPunchBagChange: (Boolean) -> Unit,
    exerciseCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ── Exercise library ───────────────────────────────────────────────────
        SectionLabel("Exercise library")
        KCard {
            Text("Bundled library", style = MaterialTheme.typography.titleLarge)
            Text(
                if (exerciseCount > 0) "$exerciseCount exercises available offline"
                else "Loading…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                "The full exercise library ships inside the app — no account, no network, no API keys. Everything works offline.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // ── Equipment ──────────────────────────────────────────────────────────
        SectionLabel("My equipment")
        KCard {
            Text(
                "Owned equipment determines which exercises the workout generator can recommend.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            listOf(
                "Barbell", "Dumbbells", "Weight plates",
                "Flat bench", "Incline bench", "Preacher curl station",
                "Medicine ball", "Spin bike", "Incline treadmill",
                "Kettlebell", "EZ curl bar"
            ).forEach { item ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(checked = true, onCheckedChange = null, enabled = false)
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color    = MaterialTheme.colorScheme.outlineVariant
            )
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Punch bag", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (hasPunchBag) "Bag exercises included"
                        else "Toggle when you get yours",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = hasPunchBag, onCheckedChange = onPunchBagChange)
            }
        }

        // ── Health Connect ─────────────────────────────────────────────────────
        SectionLabel("Health Connect")
        KCard {
            Text(healthStatusText, style = MaterialTheme.typography.titleMedium)
            Text(
                "Strength sessions are written locally to Health Connect; heart rate and calories are read from your Fitbit. No account, no cloud, no telemetry.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (!healthConnected) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onConnectHealth, modifier = Modifier.fillMaxWidth()) {
                    Text("Grant access")
                }
            }
        }

        // ── About ──────────────────────────────────────────────────────────────
        SectionLabel("About")
        KCard {
            Text("Kargathra", style = MaterialTheme.typography.titleLarge)
            Text(
                "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
