package com.kargathra.fitness.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kargathra.fitness.data.db.ExerciseEntity
import com.kargathra.fitness.data.db.splitPipe
import com.kargathra.fitness.data.repo.ExerciseRepository
import com.kargathra.fitness.ui.components.KCard
import com.kargathra.fitness.ui.components.ExerciseVideo
import com.kargathra.fitness.ui.components.SectionLabel
import com.kargathra.fitness.ui.components.Tag
import com.kargathra.fitness.ui.theme.Gold

@Composable
fun ExercisesScreen(
    repo: ExerciseRepository,
    hasPunchBag: Boolean = false,
    modifier: Modifier = Modifier
) {

    var query      by remember { mutableStateOf("") }
    var equipment  by remember { mutableStateOf("") }
    var mechanic   by remember { mutableStateOf("") }
    var detailEx   by remember { mutableStateOf<ExerciseEntity?>(null) }

    val exercises by repo.search(
        query          = query,
        equipment      = equipment,
        mechanic       = mechanic,
        includePunchBag= hasPunchBag
    ).collectAsStateWithLifecycle(emptyList())

    val equipmentOptions by repo.equipmentList()
        .collectAsStateWithLifecycle(emptyList())

    // Ensure the bundled library is loaded
    LaunchedEffect(Unit) { repo.ensureLoaded() }

    // Detail bottom sheet
    detailEx?.let { ex ->
        ExerciseDetailSheet(ex = ex, onDismiss = { detailEx = null })
    }

    Column(modifier.fillMaxSize()) {
        // ── Search bar ─────────────────────────────────────────────────────────
        OutlinedTextField(
            value         = query,
            onValueChange = { query = it },
            placeholder   = { Text("Search exercises, muscles…") },
            leadingIcon   = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon  = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine    = true,
            modifier      = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        )

        // ── Filter chips ───────────────────────────────────────────────────────
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterPill("All", mechanic.isEmpty() && equipment.isEmpty()) {
                mechanic = ""; equipment = ""
            }
            FilterPill("Compound", mechanic == "compound") {
                mechanic = if (mechanic == "compound") "" else "compound"
            }
            FilterPill("Isolation", mechanic == "isolation") {
                mechanic = if (mechanic == "isolation") "" else "isolation"
            }
            equipmentOptions.forEach { eq ->
                FilterPill(eq.replaceFirstChar { it.uppercase() }, equipment == eq) {
                    equipment = if (equipment == eq) "" else eq
                }
            }
        }

        Spacer(Modifier.height(8.dp))


        // ── Exercise list ──────────────────────────────────────────────────────
        if (exercises.isEmpty()) {
            EmptyState()
        } else {
            val listState = rememberLazyListState()
            LazyColumn(
                state           = listState,
                contentPadding  = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier        = Modifier.fillMaxSize()
            ) {
                item {
                    Text(
                        "${exercises.size} exercises",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(exercises, key = { it.id }) { ex ->
                    ExerciseCard(ex = ex, onClick = { detailEx = ex })
                }
            }
        }
    }
}

// ── Exercise card (list item) ─────────────────────────────────────────────────

@Composable
private fun ExerciseCard(ex: ExerciseEntity, onClick: () -> Unit) {
    KCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(ex.name, style = MaterialTheme.typography.titleMedium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                val muscles = ex.primaryMuscles.splitPipe().take(2).joinToString(", ")
                if (muscles.isNotEmpty()) {
                    Text(
                        muscles,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            if (ex.videoUrl.isNotEmpty()) {
                Icon(
                    Icons.Outlined.FitnessCenter,
                    contentDescription = "Video available",
                    tint     = Gold,
                    modifier = Modifier.padding(start = 8.dp).size(18.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (ex.equipment.isNotEmpty()) Tag(ex.equipment.replaceFirstChar { it.uppercase() })
            if (ex.mechanic.isNotEmpty())  Tag(ex.mechanic.replaceFirstChar { it.uppercase() })
            if (ex.level.isNotEmpty())     Tag(ex.level.replaceFirstChar { it.uppercase() })
        }
    }
}

// ── Detail bottom sheet ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseDetailSheet(ex: ExerciseEntity, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (ex.videoUrl.isNotEmpty()) {
                item {
                    ExerciseVideo(
                        videoUrl = ex.videoUrl,
                        modifier = Modifier.clip(MaterialTheme.shapes.medium)
                    )
                }
            }
            item {
                Text(ex.name, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (ex.equipment.isNotEmpty()) Tag(ex.equipment.replaceFirstChar { it.uppercase() })
                    if (ex.mechanic.isNotEmpty())  Tag(ex.mechanic.replaceFirstChar { it.uppercase() })
                    if (ex.level.isNotEmpty())     Tag(ex.level.replaceFirstChar { it.uppercase() })
                }
            }

            if (ex.overview.isNotEmpty()) {
                item {
                    SectionLabel("Overview")
                    Text(ex.overview, style = MaterialTheme.typography.bodyMedium)
                }
            }

            val primary = ex.primaryMuscles.splitPipe()
            val secondary = ex.secondaryMuscles.splitPipe()
            if (primary.isNotEmpty()) {
                item {
                    SectionLabel("Muscles")
                    Text("Primary: ${primary.joinToString(", ")}",
                        style = MaterialTheme.typography.bodyMedium)
                    if (secondary.isNotEmpty()) {
                        Text(
                            "Secondary: ${secondary.joinToString(", ")}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            val instructions = ex.instructions.splitPipe()
            if (instructions.isNotEmpty()) {
                item {
                    SectionLabel("How to perform")
                    instructions.forEachIndexed { i, step ->
                        Row(Modifier.padding(vertical = 3.dp)) {
                            Text(
                                "${i + 1}.",
                                style    = MaterialTheme.typography.bodyMedium,
                                color    = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(24.dp)
                            )
                            Text(step, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            val tips = ex.exerciseTips.splitPipe()
            if (tips.isNotEmpty()) {
                item {
                    SectionLabel("Tips")
                    tips.forEach { tip ->
                        Row(Modifier.padding(vertical = 2.dp)) {
                            Text("✓  ", style = MaterialTheme.typography.bodyMedium,
                                color = Gold)
                            Text(tip, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            val mistakes = ex.commonMistakes.splitPipe()
            if (mistakes.isNotEmpty()) {
                item {
                    SectionLabel("Common mistakes")
                    mistakes.forEach { mistake ->
                        Row(Modifier.padding(vertical = 2.dp)) {
                            Text("✗  ", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error)
                            Text(mistake, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            if (ex.safetyInfo.isNotEmpty()) {
                item {
                    SectionLabel("Safety")
                    KCard {
                        Text(ex.safetyInfo, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            val variations = ex.variations.splitPipe()
            if (variations.isNotEmpty()) {
                item {
                    SectionLabel("Variations")
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        variations.forEach { Tag(it) }
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Outlined.FitnessCenter,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Loading exercise library…",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Filter pill ───────────────────────────────────────────────────────────────

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color    = if (selected) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.surfaceVariant,
        shape    = RoundedCornerShape(50),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text     = label,
            style    = MaterialTheme.typography.labelLarge,
            color    = if (selected) MaterialTheme.colorScheme.onPrimary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}
