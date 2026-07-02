package com.kargathra.fitness.data.model

import com.kargathra.fitness.data.anatomy.MuscleMap
import com.kargathra.fitness.data.db.ExerciseEntity

/**
 * Bridges a library ExerciseEntity (full 471-exercise library, muscle data as
 * anatomical strings) into the Routine model's Exercise/RoutineItem (coarse
 * MuscleGroup enum), so library exercises can be added to custom programs.
 */

/** Equipment where no external load is logged — reps only. */
private val WEIGHTLESS = setOf(
    "body only", "punching bag", "jump rope", "assault bike", "battle ropes", "plyo box"
)

private fun equipmentFor(entity: ExerciseEntity): List<Equipment> = when (val e = entity.equipment.lowercase()) {
    "barbell"        -> listOf(Equipment.BARBELL)
    "dumbbell"       -> listOf(Equipment.DUMBBELL)
    "kettlebell"     -> listOf(Equipment.KETTLEBELL)
    "medicine ball"  -> listOf(Equipment.MEDICINE_BALL)
    "machine"        -> listOf(Equipment.MACHINE)
    in WEIGHTLESS    -> listOf(Equipment.BODYWEIGHT)
    else             -> listOf(Equipment.OTHER)
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
    val primaryGroup = primaries.firstNotNullOfOrNull { MuscleMap.muscleGroupFor(it) }
        ?: MuscleGroup.CORE
    val secondaryGroups = secondaries.mapNotNull { MuscleMap.muscleGroupFor(it) }
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
