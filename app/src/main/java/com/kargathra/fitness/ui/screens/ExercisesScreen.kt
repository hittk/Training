package com.kargathra.fitness.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.Image
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.kargathra.fitness.data.model.Exercise
import com.kargathra.fitness.data.sample.SampleData
import com.kargathra.fitness.ui.components.KCard
import com.kargathra.fitness.ui.components.rememberAssetImage
import com.kargathra.fitness.ui.components.Tag

@Composable
fun ExercisesScreen(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(SampleData.exercises, key = { it.id }) { ex -> ExerciseCard(ex) }
    }
}

@Composable
private fun ExerciseCard(ex: Exercise) {
    KCard {
        Text(ex.name, style = MaterialTheme.typography.titleLarge)
        Text(
            ex.primary.display + (if (ex.secondary.isNotEmpty())
                " · " + ex.secondary.joinToString(", ") { it.display } else ""),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
        // Bundled illustration (free-exercise-db, public domain). First frame
        // shown; the second frame (end position) ships in assets for later use.
        Spacer(Modifier.height(12.dp))
        val illustration = rememberAssetImage("exercises/${ex.id}/0.jpg")
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium
        ) {
            if (illustration != null) {
                Image(
                    bitmap = illustration,
                    contentDescription = "How to perform ${ex.name}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Illustration",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (ex.cues.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            ex.cues.forEach { cue ->
                Row(Modifier.padding(vertical = 2.dp)) {
                    Text("•  ", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary)
                    Text(cue, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tag(ex.equipment.first().display)
            if (ex.repRange.first > 0) Tag("${ex.repRange.first}-${ex.repRange.last} reps")
        }
    }
}
