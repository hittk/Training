package com.kargathra.fitness.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.layout.Arrangement
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
import com.kargathra.fitness.data.repo.SavedRoutineRepository
import com.kargathra.fitness.data.model.toRoutineItem
import com.kargathra.fitness.data.model.Routine
import com.kargathra.fitness.data.repo.FavouriteRepository
import com.kargathra.fitness.ui.components.KCard
import com.kargathra.fitness.data.anatomy.MuscleMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.mutableIntStateOf
import com.kargathra.fitness.data.video.UserVideoStore
import com.kargathra.fitness.ui.components.hasAnyVideo
import com.kargathra.fitness.ui.components.ExerciseVideo
import com.kargathra.fitness.ui.components.sanitiseResName
import com.kargathra.fitness.ui.components.MuscleMapView
import com.kargathra.fitness.ui.components.SectionLabel
import com.kargathra.fitness.ui.components.Tag
import com.kargathra.fitness.ui.theme.Gold

@Composable
fun ExercisesScreen(
    repo: ExerciseRepository,
    favRepo: FavouriteRepository,
    savedRepo: SavedRoutineRepository,
    hasPunchBag: Boolean = false,
    modifier: Modifier = Modifier
) {

    var query      by remember { mutableStateOf("") }
    var equipment  by remember { mutableStateOf("") }
    var mechanic   by remember { mutableStateOf("") }
    var favsOnly   by remember { mutableStateOf(false) }
    var region     by remember { mutableStateOf<MuscleMap.BodyRegion?>(null) }
    var detailEx   by remember { mutableStateOf<ExerciseEntity?>(null) }

    val exercises by repo.search(
        query          = query,
        equipment      = equipment,
        mechanic       = mechanic,
        includePunchBag= hasPunchBag,
        limit          = 2000   // show the entire library (461 total) — no practical cap
    ).collectAsStateWithLifecycle(emptyList())

    val favouriteIds by favRepo.favouriteIds().collectAsStateWithLifecycle(emptyList())
    val equipmentOptions by repo.equipmentList()
        .collectAsStateWithLifecycle(emptyList())

    // Ensure the bundled library is loaded
    LaunchedEffect(Unit) { repo.ensureLoaded() }

    // Detail bottom sheet
    detailEx?.let { ex ->
        ExerciseDetailSheet(
            ex = ex,
            repo = repo,
            favRepo = favRepo,
            savedRepo = savedRepo,
            onOpenExercise = { detailEx = it },
            onDismiss = { detailEx = null }
        )
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

        // ── Filter chips: type / favourites ──────────────────────────────────
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterPill("★ Favourites", favsOnly) {
                favsOnly = !favsOnly
            }
            FilterPill("All", !favsOnly && mechanic.isEmpty() && equipment.isEmpty() && region == null) {
                favsOnly = false; mechanic = ""; equipment = ""; region = null
            }
            FilterPill("Compound", mechanic == "compound") {
                mechanic = if (mechanic == "compound") "" else "compound"
            }
            FilterPill("Isolation", mechanic == "isolation") {
                mechanic = if (mechanic == "isolation") "" else "isolation"
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Body region row ──────────────────────────────────────────────────
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MuscleMap.BodyRegion.entries.forEach { r ->
                FilterPill(r.label, region == r) {
                    region = if (region == r) null else r
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Equipment row ────────────────────────────────────────────────────
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            equipmentOptions.forEach { eq ->
                FilterPill(eq.replaceFirstChar { it.uppercase() }, equipment == eq) {
                    equipment = if (equipment == eq) "" else eq
                }
            }
        }

        Spacer(Modifier.height(8.dp))


        // ── Exercise list ──────────────────────────────────────────────────────
        val favSet = favouriteIds.toSet()
        val shown = exercises
            .let { if (favsOnly) it.filter { ex -> ex.id in favSet } else it }
            .let { list ->
                val r = region
                if (r != null) list.filter { ex ->
                    MuscleMap.inRegion(ex.primaryMuscles.splitPipe(), r)
                } else list
            }
        if (shown.isEmpty()) {
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
                        "${shown.size} exercises",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(shown, key = { it.id }) { ex ->
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
            if (hasAnyVideo(ex.videoUrl, ex.id)) {
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ExerciseDetailSheet(
    ex: ExerciseEntity,
    repo: ExerciseRepository,
    favRepo: FavouriteRepository,
    savedRepo: SavedRoutineRepository,
    onOpenExercise: (ExerciseEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ctx = androidx.compose.ui.platform.LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = MaterialTheme.colorScheme.surface
    ) {
        // Bumped after a successful upload to force the video to re-resolve.
        var videoRefresh by remember(ex.id) { mutableIntStateOf(0) }
        val videoExists = hasAnyVideo(ex.videoUrl, ex.id, videoRefresh)

        // System video picker — copies the chosen clip into internal storage.
        val pickVideo = rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                if (UserVideoStore.save(ctx, ex.id, uri)) videoRefresh++
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (videoExists) {
                item {
                    ExerciseVideo(
                        videoUrl   = ex.videoUrl,
                        fallbackId = ex.id,
                        refreshKey = videoRefresh,
                        modifier   = Modifier.clip(MaterialTheme.shapes.medium)
                    )
                }
            } else {
                item {
                    OutlinedButton(
                        onClick = { pickVideo.launch("video/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add video")
                    }
                }
            }
            item {
                val isFav by favRepo.isFavourite(ex.id).collectAsStateWithLifecycle(false)
                val favScope = rememberCoroutineScope()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        ex.name,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        favScope.launch { favRepo.toggle(ex.id, !isFav) }
                    }) {
                        Icon(
                            imageVector = if (isFav) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (isFav) "Remove from favourites" else "Add to favourites",
                            tint = if (isFav) Gold else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (ex.equipment.isNotEmpty()) Tag(ex.equipment.replaceFirstChar { it.uppercase() })
                    if (ex.mechanic.isNotEmpty())  Tag(ex.mechanic.replaceFirstChar { it.uppercase() })
                    if (ex.level.isNotEmpty())     Tag(ex.level.replaceFirstChar { it.uppercase() })
                }
            }

            item {
                AddToProgramButton(ex = ex, savedRepo = savedRepo)
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
                    SectionLabel("Muscles worked")
                    val engagement = MuscleMap.engagementFor(primary, secondary)
                    val groups = engagement.keys
                    val wantFront = MuscleMap.needsFront(groups)
                    val wantBack  = MuscleMap.needsBack(groups)
                    MuscleMapView(
                        engagement = engagement,
                        // default to front if nothing mapped, so the figure is never blank
                        showFront  = wantFront || !wantBack,
                        showBack   = wantBack,
                        modifier   = Modifier.padding(vertical = 8.dp)
                    )
                }
                item {
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
                    var links by remember(ex.id) {
                        mutableStateOf<Map<String, ExerciseEntity>>(emptyMap())
                    }
                    LaunchedEffect(ex.id) {
                        val resolved = mutableMapOf<String, ExerciseEntity>()
                        variations.forEach { name ->
                            repo.findByName(name)?.let { resolved[name] = it }
                        }
                        links = resolved
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        variations.forEach { name ->
                            val target = links[name]
                            if (target != null) Tag(name) { onOpenExercise(target) }
                            else Tag(name)
                        }
                    }
                    if (links.isNotEmpty()) {
                        Text(
                            "Tap a highlighted variation to open it",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
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

@Composable
private fun AddToProgramButton(
    ex: ExerciseEntity,
    savedRepo: SavedRoutineRepository
) {
    val scope = rememberCoroutineScope()
    val programs by savedRepo.savedRoutines.collectAsStateWithLifecycle(emptyList())
    var showDialog by remember { mutableStateOf(false) }
    var confirmation by remember { mutableStateOf<String?>(null) }

    OutlinedButton(
        onClick = { showDialog = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Filled.Add, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Add to program")
    }
    confirmation?.let { msg ->
        Text(
            msg,
            style = MaterialTheme.typography.labelMedium,
            color = Gold,
            modifier = Modifier.padding(top = 6.dp)
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Add to program") },
            text = {
                Column {
                    // New program (auto-named from the exercise's region)
                    TextButton(onClick = {
                        scope.launch {
                            val name = defaultProgramName(ex)
                            val id = savedRepo.createEmpty(name)
                            savedRepo.addItem(id, ex.toRoutineItem())
                            confirmation = "Added to new program \"$name\""
                            showDialog = false
                        }
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("New program")
                    }
                    if (programs.isNotEmpty()) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        programs.forEach { p ->
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        savedRepo.addItem(p.id, ex.toRoutineItem())
                                        confirmation = "Added to \"${p.title}\""
                                        showDialog = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(p.title, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
        )
    }
}

/** Auto name a new program from the exercise's primary region. */
private fun defaultProgramName(ex: ExerciseEntity): String {
    val first = ex.primaryMuscles.splitPipe().firstOrNull()?.lowercase() ?: ""
    val region = when {
        listOf("pectoralis", "deltoid", "trapezius").any { first.contains(it) } -> "Chest & Shoulders"
        listOf("bicep", "tricep", "brachi", "forearm").any { first.contains(it) } -> "Arms"
        listOf("latissimus", "rhomboid", "erector").any { first.contains(it) } -> "Back"
        listOf("rectus abdominis", "oblique", "transverse").any { first.contains(it) } -> "Core"
        listOf("quadricep", "rectus femoris", "vastus", "hamstring", "gluteus", "gastrocnemius", "soleus", "adductor").any { first.contains(it) } -> "Legs"
        else -> "My"
    }
    return "$region Program"
}
