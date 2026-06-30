package com.kargathra.fitness.data.generator

import com.kargathra.fitness.data.db.ExerciseEntity
import com.kargathra.fitness.data.db.splitPipe
import com.kargathra.fitness.data.model.*

/**
 * Builds a balanced workout routine entirely on-device from the cached exercise
 * library. No network, no API key, fully deterministic given the same inputs
 * (a small random seed varies exercise selection between generations).
 *
 * Strategy:
 *  1. Map each cached exercise's anatomical muscles → the app's MuscleGroup enum.
 *  2. Filter to the user's available equipment.
 *  3. Decide how many exercises fit the session length.
 *  4. Pick compounds first, balanced across the target muscle groups (or a
 *     full-body push/pull/legs spread when no targets are given), then fill
 *     remaining slots with isolation work.
 *  5. Assign goal-appropriate sets/reps/rest.
 */
object LocalRoutineGenerator {

    // ── Anatomical name → MuscleGroup mapping ─────────────────────────────────

    private val muscleMap: Map<String, MuscleGroup> = buildMap {
        // Chest
        listOf("pectoralis major sternal head", "pectoralis major clavicular head",
               "pectoralis minor", "pectoralis").forEach { put(it, MuscleGroup.CHEST) }
        // Upper back
        listOf("trapezius middle", "trapezius upper", "rhomboids", "levator scapulae",
               "erector spinae").forEach { put(it, MuscleGroup.UPPER_BACK) }
        // Lats
        listOf("latissimus dorsi", "serratus anterior").forEach { put(it, MuscleGroup.LATS) }
        // Shoulders
        listOf("deltoid anterior", "deltoid lateral", "deltoid posterior",
               "rotator cuff").forEach { put(it, MuscleGroup.SHOULDERS) }
        // Biceps
        listOf("biceps brachii short head", "biceps brachii long head", "brachialis",
               "wrist flexors").forEach { put(it, MuscleGroup.BICEPS) }
        // Triceps
        listOf("triceps brachii lateral head", "triceps brachii medial head",
               "triceps brachii long head", "wrist extensors").forEach { put(it, MuscleGroup.TRICEPS) }
        // Quads
        listOf("rectus femoris", "vastus lateralis", "vastus medialis", "vastus intermedius",
               "tensor fasciae latae").forEach { put(it, MuscleGroup.QUADS) }
        // Hamstrings
        listOf("biceps femoris", "semitendinosus", "semimembranosus").forEach {
            put(it, MuscleGroup.HAMSTRINGS) }
        // Glutes
        listOf("gluteus maximus", "gluteus medius", "gluteus minimus",
               "adductor longus", "adductor magnus", "gracilis", "iliopsoas").forEach {
            put(it, MuscleGroup.GLUTES) }
        // Calves
        listOf("gastrocnemius", "soleus").forEach { put(it, MuscleGroup.CALVES) }
        // Core
        listOf("rectus abdominis", "obliques external", "obliques internal",
               "transverse abdominis").forEach { put(it, MuscleGroup.CORE) }
        // Neck — fold into shoulders rather than dropping
        listOf("sternocleidomastoid", "neck extensors").forEach { put(it, MuscleGroup.SHOULDERS) }
    }

    private fun ExerciseEntity.muscleGroups(): List<MuscleGroup> {
        val primary = primaryMuscles.splitPipe().mapNotNull { muscleMap[it.lowercase()] }
        return if (category == "conditioning") listOf(MuscleGroup.CARDIO) else primary.distinct()
    }

    private fun ExerciseEntity.primaryGroup(): MuscleGroup? =
        muscleGroups().firstOrNull()

    // ── Equipment availability ────────────────────────────────────────────────

    /** Equipment strings (from the cache) the user can actually use. */
    private val ownedEquipment = setOf(
        "barbell", "dumbbell", "body only", "kettlebell",
        "medicine ball", "machine", "other", "punching bag"
        // note: battle ropes / sled / assault bike / jump rope / plyo box excluded —
        // user doesn't own these; conditioning still covered by body-only options
    )

    // ── Public entry point ────────────────────────────────────────────────────

    fun generate(
        library: List<ExerciseEntity>,
        goal: Goal,
        experience: Experience,
        sessionMinutes: Int,
        targetGroups: List<MuscleGroup>,
        includePunchBag: Boolean = false
    ): Routine {

        // Filter to usable equipment and known muscle mapping
        val usable = library.filter { ex ->
            (ex.equipment in ownedEquipment) &&
            (includePunchBag || !ex.requiresPunchBag) &&
            ex.muscleGroups().isNotEmpty()
        }

        // Determine the muscle groups to cover
        val groups: List<MuscleGroup> = when {
            targetGroups.isNotEmpty() -> targetGroups
            else -> listOf( // balanced full-body spread
                MuscleGroup.CHEST, MuscleGroup.LATS, MuscleGroup.QUADS,
                MuscleGroup.SHOULDERS, MuscleGroup.HAMSTRINGS, MuscleGroup.CORE
            )
        }

        // Exercise count from session length (~9 min per exercise incl. rest)
        val exerciseCount = (sessionMinutes / 9).coerceIn(3, 8)

        val compounds = usable.filter { it.mechanic == "compound" }
        val isolation = usable.filter { it.mechanic == "isolation" }

        val chosen = LinkedHashSet<ExerciseEntity>()
        val usedFamilies = HashSet<String>()

        // Pass 1 — one compound per target group, in priority order
        for (group in groups) {
            if (chosen.size >= exerciseCount) break
            val pick = compounds
                .filter { group in it.muscleGroups() && it.movementFamily !in usedFamilies }
                .randomOrNull()
            if (pick != null) {
                chosen += pick
                usedFamilies += pick.movementFamily
            }
        }

        // Pass 2 — fill remaining slots with isolation for the target groups
        var gi = 0
        while (chosen.size < exerciseCount && gi < groups.size * 2) {
            val group = groups[gi % groups.size]
            val pick = isolation
                .filter { group in it.muscleGroups() && it.movementFamily !in usedFamilies }
                .randomOrNull()
            if (pick != null) {
                chosen += pick
                usedFamilies += pick.movementFamily
            }
            gi++
        }

        // Pass 3 — if still short, top up with any usable compound
        if (chosen.size < exerciseCount) {
            compounds.filter { it.movementFamily !in usedFamilies }
                .shuffled()
                .take(exerciseCount - chosen.size)
                .forEach { chosen += it; usedFamilies += it.movementFamily }
        }

        // Build routine items with goal-appropriate prescription
        val items = chosen.map { ex -> ex.toRoutineItem(goal, experience) }

        val title = buildTitle(goal, targetGroups)
        val focus = (targetGroups.ifEmpty { groups }).distinct().take(4)
        val estMinutes = items.sumOf { estimateMinutes(it) }

        return Routine(
            id               = "ai_${System.currentTimeMillis()}",
            title            = title,
            focus            = focus,
            items            = items,
            estimatedMinutes = estMinutes,
            isGenerated      = true
        )
    }

    // ── Prescription ──────────────────────────────────────────────────────────

    private fun ExerciseEntity.toRoutineItem(goal: Goal, exp: Experience): RoutineItem {
        val isCompound = mechanic == "compound"
        val isCardio   = category == "conditioning"

        val (sets, reps, rest) = when {
            isCardio -> Triple(intervalSets(exp), 0..0, 75)
            goal == Goal.STRENGTH && isCompound -> Triple(5, 3..5, 180)
            goal == Goal.STRENGTH               -> Triple(4, 6..8, 120)
            goal == Goal.HYPERTROPHY && isCompound -> Triple(4, 6..10, 120)
            goal == Goal.HYPERTROPHY            -> Triple(3, 10..15, 75)
            goal == Goal.FAT_LOSS               -> Triple(3, 12..20, 45)
            else /* GENERAL */                  -> Triple(3, 8..12, 90)
        }

        // Beginners do one fewer set on isolation work
        val finalSets = if (exp == Experience.BEGINNER && !isCompound) (sets - 1).coerceAtLeast(2) else sets

        return RoutineItem(
            exercise   = toDomainExercise(),
            sets       = finalSets,
            repTarget  = reps,
            restSeconds= rest
        )
    }

    private fun intervalSets(exp: Experience) = when (exp) {
        Experience.BEGINNER     -> 4
        Experience.INTERMEDIATE -> 6
        Experience.ADVANCED     -> 8
    }

    private fun estimateMinutes(item: RoutineItem): Int {
        // ~40s per working set + rest, rounded up, min 4 min/exercise
        val perSet = 40 + item.restSeconds
        return ((item.sets * perSet) / 60).coerceAtLeast(4)
    }

    private fun buildTitle(goal: Goal, targets: List<MuscleGroup>): String {
        val focusPart = when {
            targets.isEmpty()      -> "Full Body"
            targets.size == 1      -> targets.first().display
            targets.size == 2      -> "${targets[0].display} & ${targets[1].display}"
            else                   -> "${targets[0].display} Focus"
        }
        val goalPart = when (goal) {
            Goal.STRENGTH    -> "Strength"
            Goal.HYPERTROPHY -> "Hypertrophy"
            Goal.FAT_LOSS    -> "Conditioning"
            Goal.GENERAL     -> "Session"
        }
        return "$focusPart $goalPart"
    }

    // ── ExerciseEntity → domain Exercise ──────────────────────────────────────

    private fun ExerciseEntity.toDomainExercise(): Exercise {
        val groups = muscleGroups()
        val primary = groups.firstOrNull() ?: MuscleGroup.CORE
        return Exercise(
            id         = id,
            name       = name,
            primary    = primary,
            secondary  = groups.drop(1),
            equipment  = mapEquipment(equipment),
            mechanic   = when (mechanic) {
                "compound"  -> Mechanic.COMPOUND
                "isolation" -> Mechanic.ISOLATION
                else        -> Mechanic.CONDITIONING
            },
            pattern    = inferPattern(this),
            repRange   = 8..12,
            cues       = exerciseTips.splitPipe().take(3)
        )
    }

    private fun mapEquipment(eq: String): List<Equipment> = when (eq) {
        "barbell"       -> listOf(Equipment.BARBELL)
        "dumbbell"      -> listOf(Equipment.DUMBBELL)
        "kettlebell"    -> listOf(Equipment.DUMBBELL) // closest owned analogue in enum
        "medicine ball" -> listOf(Equipment.MEDICINE_BALL)
        "body only"     -> listOf(Equipment.BODYWEIGHT)
        else            -> listOf(Equipment.BODYWEIGHT)
    }

    private fun inferPattern(ex: ExerciseEntity): Pattern {
        val n = ex.name.lowercase()
        return when {
            ex.category == "conditioning"                 -> Pattern.CARDIO
            "squat" in n                                  -> Pattern.SQUAT
            "deadlift" in n || "rdl" in n || "hinge" in n -> Pattern.HINGE
            "lunge" in n || "split squat" in n || "step" in n -> Pattern.LUNGE
            "curl" in n                                   -> Pattern.CURL
            "extension" in n || "pushdown" in n || "kickback" in n -> Pattern.EXTENSION
            "row" in n || "pull" in n                     -> Pattern.HORIZONTAL_PULL
            "press" in n && ("shoulder" in n || "overhead" in n) -> Pattern.VERTICAL_PUSH
            "press" in n || "bench" in n || "push" in n   -> Pattern.HORIZONTAL_PUSH
            ex.force == "pull"                            -> Pattern.HORIZONTAL_PULL
            ex.force == "push"                            -> Pattern.HORIZONTAL_PUSH
            ex.primaryMuscles.contains("abdominis")       -> Pattern.CORE
            else                                          -> Pattern.HORIZONTAL_PUSH
        }
    }
}
