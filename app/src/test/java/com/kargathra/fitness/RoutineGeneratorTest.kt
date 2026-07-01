package com.kargathra.fitness

import com.kargathra.fitness.data.db.ExerciseEntity
import com.kargathra.fitness.data.generator.LocalRoutineGenerator
import com.kargathra.fitness.data.model.Experience
import com.kargathra.fitness.data.model.Goal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [LocalRoutineGenerator].
 *
 * The exercise-count and prescription helpers are private and inlined inside
 * `generate()`, so they can't be called in isolation. Instead we drive the full
 * public `generate()` path with a controlled, minimal library and assert on the
 * resulting [com.kargathra.fitness.data.model.Routine]. Exercise selection uses
 * randomOrNull()/shuffled(), but the exercise *count* and the *prescription*
 * (sets/reps for a given goal + mechanic) are deterministic, so the assertions
 * below are stable.
 */
class RoutineGeneratorTest {

    /**
     * A minimal barbell "chest compound" entity. Only the fields the generator
     * reads (category, equipment, mechanic, primaryMuscles, movementFamily,
     * requiresPunchBag) carry meaning here; the rest are empty strings.
     * A distinct [movementFamily] per index stops the de-dup passes from
     * discarding them.
     */
    private fun compoundChest(idx: Int) = ExerciseEntity(
        id = "ex_$idx",
        name = "Bench Variant $idx",
        category = "strength",
        equipment = "barbell",
        mechanic = "compound",
        force = "push",
        level = "intermediate",
        primaryMuscles = "pectoralis major sternal head", // maps to MuscleGroup.CHEST
        secondaryMuscles = "",
        overview = "",
        instructions = "",
        exerciseTips = "",
        commonMistakes = "",
        safetyInfo = "",
        variations = "",
        videoUrl = "",
        movementFamily = "family_$idx"
    )

    private fun library(n: Int): List<ExerciseEntity> = (0 until n).map { compoundChest(it) }

    // ── Exercise count: (sessionMinutes / 9).coerceIn(3, 8) ───────────────────

    @Test
    fun exercise_count_scales_with_session_length() {
        val lib = library(10) // more than enough distinct compounds to fill any count

        fun countFor(mins: Int): Int = LocalRoutineGenerator.generate(
            library = lib,
            goal = Goal.STRENGTH,
            experience = Experience.INTERMEDIATE,
            sessionMinutes = mins,
            targetGroups = emptyList()
        ).items.size

        assertEquals(3, countFor(20)) // 20 / 9 = 2 → coerced up to floor 3
        assertEquals(5, countFor(45)) // 45 / 9 = 5
        assertEquals(8, countFor(90)) // 90 / 9 = 10 → coerced down to ceiling 8
    }

    // ── Prescription rules ────────────────────────────────────────────────────

    @Test
    fun strength_compound_prescribes_5_sets_and_3_to_5_reps() {
        val routine = LocalRoutineGenerator.generate(
            library = library(6),
            goal = Goal.STRENGTH,
            experience = Experience.INTERMEDIATE, // avoids the beginner isolation set reduction
            sessionMinutes = 45,
            targetGroups = emptyList()
        )
        assertTrue(routine.items.isNotEmpty())
        // Every chosen exercise here is a compound, so all items share the
        // STRENGTH + COMPOUND prescription.
        routine.items.forEach { item ->
            assertEquals(5, item.sets)
            assertEquals(3..5, item.repTarget)
        }
    }

    @Test
    fun fat_loss_prescribes_3_sets_and_12_to_20_reps() {
        val routine = LocalRoutineGenerator.generate(
            library = library(6),
            goal = Goal.FAT_LOSS,
            experience = Experience.INTERMEDIATE,
            sessionMinutes = 45,
            targetGroups = emptyList()
        )
        assertTrue(routine.items.isNotEmpty())
        // FAT_LOSS prescription is not gated on mechanic (non-cardio).
        routine.items.forEach { item ->
            assertEquals(3, item.sets)
            assertEquals(12..20, item.repTarget)
        }
    }

    // TODO: When an exercise library can be injected as a shared test fixture,
    // or the count / prescription helpers are lifted out of the private scope of
    // LocalRoutineGenerator, add finer-grained coverage for: pass-2 isolation
    // fill, movement-family de-duplication, cardio interval prescriptions, and
    // the beginner isolation set reduction. Those paths currently depend on the
    // full generate() flow and its random selection.
}
