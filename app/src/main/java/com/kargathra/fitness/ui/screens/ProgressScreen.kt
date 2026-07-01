package com.kargathra.fitness.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kargathra.fitness.data.db.ExerciseRef
import com.kargathra.fitness.data.repo.MuscleVolume
import com.kargathra.fitness.data.repo.TrendPoint
import com.kargathra.fitness.data.repo.WorkoutRepository
import com.kargathra.fitness.ui.components.KCard
import com.kargathra.fitness.ui.components.MuscleVolumeChart
import com.kargathra.fitness.ui.components.PlateCalculatorCard
import com.kargathra.fitness.ui.components.SectionLabel
import com.kargathra.fitness.ui.components.TrendChart
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ProgressScreen(repo: WorkoutRepository, modifier: Modifier = Modifier) {
    val exercises    by repo.observeLoggedExercises().collectAsStateWithLifecycle(emptyList())
    val weeklyVolume by repo.weeklyMuscleVolume().collectAsStateWithLifecycle(emptyList())
    var selected     by remember { mutableStateOf<ExerciseRef?>(null) }

    LaunchedEffect(exercises) {
        if (selected == null || exercises.none { it.exerciseId == selected!!.exerciseId }) {
            selected = exercises.firstOrNull()
        }
    }

    val trends by produceState(initialValue = emptyList<TrendPoint>(), key1 = selected) {
        val s = selected
        if (s == null) value = emptyList()
        else repo.trendsFor(s.exerciseId).collect { value = it }
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Plate calculator ───────────────────────────────────────────────────
        PlateCalculatorCard()

        // ── Weekly muscle volume ───────────────────────────────────────────────
        if (weeklyVolume.isNotEmpty()) {
            MuscleVolumeChart(volumes = weeklyVolume)
        } else {
            SectionLabel("This week — muscle volume")
            KCard {
                Text(
                    "Complete a workout this week and your muscle volume breakdown will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ── Strength trends ────────────────────────────────────────────────────
        if (exercises.isEmpty()) {
            SectionLabel("Strength trends")
            KCard {
                Text(
                    "Log a few sets and your strength trends will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            return@Column
        }

        SectionLabel("Exercise")
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            exercises.forEach { ex ->
                SelectableChip(
                    text     = ex.exerciseName,
                    selected = ex.exerciseId == selected?.exerciseId,
                    onClick  = { selected = ex }
                )
            }
        }

        if (trends.isEmpty()) {
            Text(
                "No completed sessions yet for this exercise.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }

        val latest = trends.last()
        TrendChart(
            title       = "Total volume",
            values      = trends.map { it.volume.toFloat() },
            latestLabel = "${fmt(latest.volume)} kg",
            footer      = "${trends.size} sessions · reps × weight, summed per session"
        )
        TrendChart(
            title       = "Estimated 1RM",
            values      = trends.map { it.estimated1Rm.toFloat() },
            latestLabel = "${fmt(latest.estimated1Rm)} kg",
            footer      = "Epley estimate from your top set"
        )

        SectionLabel("Recent sessions")
        KCard {
            trends.reversed().take(8).forEachIndexed { i, p ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        fmtDate(p.timeMillis),
                        style    = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.width(96.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text("Top set ${fmt(p.topWeight)} kg",
                            style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "1RM ${fmt(p.estimated1Rm)} · vol ${fmt(p.volume)} kg",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectableChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color    = if (selected) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.surfaceVariant,
        shape    = RoundedCornerShape(50),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text     = text,
            style    = MaterialTheme.typography.labelLarge,
            color    = if (selected) MaterialTheme.colorScheme.onPrimary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
        )
    }
}

private val dateFmt = DateTimeFormatter.ofPattern("d MMM")
private fun fmtDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFmt)
