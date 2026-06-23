package com.kargathra.fitness.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.kargathra.fitness.ui.components.KCard
import com.kargathra.fitness.ui.components.SectionLabel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    healthStatusText: String,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    anthropicKey: String,
    onAnthropicKeyChange: (String) -> Unit,
    hasPunchBag: Boolean,
    onPunchBagChange: (Boolean) -> Unit,
    exerciseCount: Int,
    syncStatus: String,
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    var keyVisible by remember { mutableStateOf(false) }
    var aKeyVisible by remember { mutableStateOf(false) }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ── Exercise library ───────────────────────────────────────────────────
        SectionLabel("Exercise library")
        KCard {
            Text("exerciseapi.dev", style = MaterialTheme.typography.titleLarge)
            Text(
                if (exerciseCount > 0) "$exerciseCount exercises cached"
                else "Not synced — add your API key below",
                style = MaterialTheme.typography.bodyMedium,
                color = if (exerciseCount > 0) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )
            OutlinedTextField(
                value         = apiKey,
                onValueChange = onApiKeyChange,
                label         = { Text("API key") },
                placeholder   = { Text("exlib_…") },
                singleLine    = true,
                visualTransformation = if (keyVisible) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon  = {
                    IconButton(onClick = { keyVisible = !keyVisible }) {
                        Icon(
                            if (keyVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (keyVisible) "Hide key" else "Show key"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onSyncNow, modifier = Modifier.fillMaxWidth()) {
                Text(if (exerciseCount > 0) "Re-sync library" else "Sync now")
            }
            if (syncStatus.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    syncStatus,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (syncStatus.startsWith("Error") || syncStatus.contains("failed"))
                                MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                )
            }
            Text(
                "Your key is stored on-device only, sent exclusively to api.exerciseapi.dev. " +
                "The library syncs once per 24 hours — the first sync uses ~25 API calls.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // ── AI generator key ─────────────────────────────────────────────────────
        SectionLabel("AI workout generator")
        KCard {
            Text("Anthropic API key", style = MaterialTheme.typography.titleLarge)
            Text(
                "Required for the \"Build my workout\" generator. Get one at console.anthropic.com.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )
            OutlinedTextField(
                value         = anthropicKey,
                onValueChange = onAnthropicKeyChange,
                label         = { Text("Anthropic key") },
                placeholder   = { Text("sk-ant-…") },
                singleLine    = true,
                visualTransformation = if (aKeyVisible) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon  = {
                    IconButton(onClick = { aKeyVisible = !aKeyVisible }) {
                        Icon(
                            if (aKeyVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (aKeyVisible) "Hide key" else "Show key"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Stored on-device only. Sent solely to api.anthropic.com.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // ── Equipment ──────────────────────────────────────────────────────────
        SectionLabel("My equipment")
        KCard {
            Text(
                "Owned equipment determines which exercises appear in the library and what the AI generator can recommend.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            // Static owned equipment (always on)
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
            // Punch bag — toggleable
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Punch bag", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (hasPunchBag) "Bag exercises visible in library"
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
        }

        // ── About ──────────────────────────────────────────────────────────────
        SectionLabel("About")
        KCard {
            Text("Kargathra", style = MaterialTheme.typography.titleLarge)
            Text(
                "Version 0.1.0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
