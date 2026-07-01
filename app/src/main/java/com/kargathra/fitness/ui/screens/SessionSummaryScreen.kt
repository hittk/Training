package com.kargathra.fitness.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kargathra.fitness.data.anatomy.MuscleMap
import com.kargathra.fitness.data.repo.SessionSummary
import com.kargathra.fitness.data.repo.WorkoutRepository
import com.kargathra.fitness.ui.components.KCard
import com.kargathra.fitness.ui.components.MuscleMapView
import com.kargathra.fitness.ui.components.SectionLabel
import com.kargathra.fitness.ui.theme.Gold
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SessionSummaryScreen(
    repo: WorkoutRepository,
    workoutId: Long,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    var summary by remember(workoutId) { mutableStateOf<SessionSummary?>(null) }
    LaunchedEffect(workoutId) { summary = repo.sessionSummary(workoutId) }

    val s = summary
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (s == null) {
            Text("Loading summary…", style = MaterialTheme.typography.bodyMedium)
            return@Column
        }

        Text("Session complete", style = MaterialTheme.typography.headlineSmall, color = Gold)
        Text(s.title, style = MaterialTheme.typography.titleMedium)

        // ── Muscles worked (visual) ────────────────────────────────────────────
        val engagement = remember(s) {
            MuscleMap.engagementFromGroupVolumes(s.muscleVolume.map { it.group to it.volumeKg })
        }
        if (engagement.isNotEmpty()) {
            KCard {
                SectionLabel("Muscles worked")
                Spacer(Modifier.height(12.dp))
                MuscleMapView(
                    engagement = engagement,
                    showFront  = MuscleMap.needsFront(engagement.keys),
                    showBack   = MuscleMap.needsBack(engagement.keys),
                    modifier   = Modifier.fillMaxWidth()
                )
            }
        }

        // ── Stat grid ──────────────────────────────────────────────────────────
        KCard {
            SectionLabel("Summary")
            Spacer(Modifier.height(12.dp))
            StatRow("Duration", "${s.durationMin} min")
            StatRow("Exercises", "${s.exerciseCount}")
            StatRow("Sets", "${s.totalSets}")
            StatRow("Reps", "${s.totalReps}")
            StatRow("Total volume", "${fmt(s.totalVolumeKg)} kg")
            s.avgBpm?.let { StatRow("Avg HR", "$it bpm") }
            s.maxBpm?.let { StatRow("Max HR", "$it bpm") }
            s.activeKcal?.let { StatRow("Active energy", "${fmt(it)} kcal") }
        }

        // ── Muscle volume breakdown ────────────────────────────────────────────
        if (s.muscleVolume.isNotEmpty()) {
            KCard {
                SectionLabel("Volume by muscle")
                Spacer(Modifier.height(8.dp))
                s.muscleVolume.forEach { mv ->
                    StatRow(mv.group.display, "${fmt(mv.volumeKg)} kg")
                }
            }
        }

        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Done") }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
