package com.kargathra.fitness.data.model

import com.kargathra.fitness.data.anatomy.MuscleMap
import com.kargathra.fitness.data.db.ExerciseEntity

/**
 * Bridges a library ExerciseEntity (full 471-exercise library, muscle data as
 * anatomical strings) into the Routine model's Exercise/RoutineItem (coarse
 * MuscleGroup enum), so library exercises can be added to custom programs.
 */

/** SVG data-group id -> coarse MuscleGroup enum. */
private fun svgGroupToMuscleGroup(g: String): MuscleGroup? = when (g) {
    "chest"                 -> MuscleGroup.CHEST
    "trap", "upper_back"    -> MuscleGroup.UPPER_BACK
    "lats"                  -> MuscleGroup.LATS
    "shoulder"              -> MuscleGroup.SHOULDERS
    "biceps"                -> MuscleGroup.BICEPS
    "triceps"               -> MuscleGroup.TRICEPS
    "forearm"               -> MuscleGroup.BICEPS      // no forearm enum; fold to arms
    "abs", "obliques"       -> MuscleGroup.CORE
    "lowerback"             -> MuscleGroup.CORE
    "quads"                 -> MuscleGroup.QUADS
    "hamstrings"            -> MuscleGroup.HAMSTRINGS
    "glutes"                -> MuscleGroup.GLUTES
    "calves"                -> MuscleGroup.CALVES
    else                    -> null
}

/** Map one anatomical muscle name -> MuscleGroup enum (via the SVG group). */
private fun muscleGroupForAnatomical(name: String): MuscleGroup? =
    MuscleMap.groupFor(name)?.let { svgGroupToMuscleGroup(it) }

private fun equipmentFor(entity: ExerciseEntity): List<Equipment> = when (entity.equipment.lowercase()) {
    "barbell"        -> listOf(Equipment.BARBELL)
    "dumbbell"       -> listOf(Equipment.DUMBBELL)
    "kettlebell"     -> listOf(Equipment.DUMBBELL)
    "medicine ball"  -> listOf(Equipment.MEDICINE_BALL)
    "body only"      -> listOf(Equipment.BODYWEIGHT)
    else             -> listOf(Equipment.BODYWEIGHT)
}

private fun patternFor(entity: ExerciseEntity): Pattern = when (entity.force.lowercase()) {
    "push" -> Pattern.HORIZONTAL_PUSH
    "pull" -> Pattern.HORIZONTAL_PULL
    else   -> Pattern.CORE
}

/** Convert a library exercise into the Routine model's Exercise. */
fun ExerciseEntity.toModelExercise(): Exercise {
    val primaries = primaryMuscles.split("|").map { it.trim() }.filter { it.isNotEmpty() }
    val secondaries = secondaryMuscles.split("|").map { it.trim() }.filter { it.isNotEmpty() }
    val primaryGroup = primaries.firstNotNullOfOrNull { muscleGroupForAnatomical(it) }
        ?: MuscleGroup.CORE
    val secondaryGroups = secondaries.mapNotNull { muscleGroupForAnatomical(it) }
        .distinct().filter { it != primaryGroup }
    return Exercise(
        id = id,
        name = name,
        primary = primaryGroup,
        secondary = secondaryGroups,
        equipment = equipmentFor(this),
        mechanic = if (mechanic.equals("isolation", true)) Mechanic.ISOLATION else Mechanic.COMPOUND,
        pattern = patternFor(this),
        repRange = 8..12,
        cues = emptyList(),
        illustrationKey = null
    )
}

/** Convert a library exercise into a RoutineItem with default programming. */
fun ExerciseEntity.toRoutineItem(
    sets: Int = 3,
    repTarget: IntRange = 8..12,
    restSeconds: Int = 90
): RoutineItem = RoutineItem(
    exercise = toModelExercise(),
    sets = sets,
    repTarget = repTarget,
    restSeconds = restSeconds
)
