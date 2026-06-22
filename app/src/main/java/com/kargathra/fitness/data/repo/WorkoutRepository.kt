package com.kargathra.fitness.data.repo

import com.kargathra.fitness.data.db.ExerciseRef
import com.kargathra.fitness.data.db.SetEntity
import com.kargathra.fitness.data.db.WorkoutDao
import com.kargathra.fitness.data.db.WorkoutEntity
import com.kargathra.fitness.data.model.Exercise
import com.kargathra.fitness.data.model.MuscleGroup
import com.kargathra.fitness.data.sample.SampleData
import com.kargathra.fitness.health.HealthConnectManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoField

data class TrendPoint(
    val timeMillis: Long,
    val estimated1Rm: Double,
    val volume: Double,
    val topWeight: Double
)

/** kg of volume attributed to each muscle group this week */
data class MuscleVolume(val group: MuscleGroup, val volumeKg: Double)

class WorkoutRepository(
    private val dao: WorkoutDao,
    private val health: HealthConnectManager
) {
    // ── Workout lifecycle ─────────────────────────────────────────────────────

    suspend fun startWorkout(title: String): Long =
        dao.insertWorkout(WorkoutEntity(title = title, startedAt = System.currentTimeMillis()))

    suspend fun addSet(workoutId: Long, exercise: Exercise, weightKg: Double, reps: Int) {
        dao.insertSet(
            SetEntity(
                workoutId    = workoutId,
                exerciseId   = exercise.id,
                exerciseName = exercise.name,
                weightKg     = weightKg,
                reps         = reps,
                performedAt  = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteSet(set: SetEntity) = dao.deleteSet(set)

    suspend fun finishWorkout(workoutId: Long) {
        val w = dao.getWorkout(workoutId) ?: return
        val end = System.currentTimeMillis()
        var avg: Long? = null
        var max: Long? = null
        var kcal: Double? = null
        var synced = false
        runCatching {
            if (health.isReady()) {
                val start = Instant.ofEpochMilli(w.startedAt)
                val endI  = Instant.ofEpochMilli(end)
                val zone  = ZoneId.systemDefault().rules.getOffset(start)
                health.writeStrengthSession(start, endI, w.title, zone)
                val v = health.readSessionVitals(start, endI)
                avg = v.avgBpm; max = v.maxBpm; kcal = v.activeKcal; synced = true
            }
        }
        dao.updateWorkout(
            w.copy(completedAt = end, avgBpm = avg, maxBpm = max, activeKcal = kcal, syncedToHc = synced)
        )
    }

    // ── Progressive overload ──────────────────────────────────────────────────

    /**
     * Returns the best (heaviest) set ever logged for this exercise in any
     * completed workout — used to surface "last session" suggestions.
     */
    suspend fun bestSetForExercise(exerciseId: String): SetEntity? =
        dao.bestSetForExercise(exerciseId)

    // ── Weekly muscle volume ──────────────────────────────────────────────────

    /**
     * Emits a list of [MuscleVolume] for the current calendar week (Mon 00:00
     * device time through now), aggregated by primary + secondary muscle groups.
     * Secondary muscles are counted at 50% of the set's volume.
     */
    fun weeklyMuscleVolume(): Flow<List<MuscleVolume>> {
        val weekStart = mondayMidnightMillis()
        // Build a lookup from exerciseId → exercise model (primary + secondary)
        val library = SampleData.allRoutines
            .flatMap { it.items.map { i -> i.exercise } }
            .distinctBy { it.id }
            .associateBy { it.id }

        return dao.observeSetsFrom(weekStart).map { sets ->
            val tally = mutableMapOf<MuscleGroup, Double>()
            sets.forEach { s ->
                val ex = library[s.exerciseId] ?: return@forEach
                val vol = s.weightKg * s.reps
                tally[ex.primary] = (tally[ex.primary] ?: 0.0) + vol
                ex.secondary.forEach { mg ->
                    tally[mg] = (tally[mg] ?: 0.0) + vol * 0.5
                }
            }
            tally.entries
                .filter { it.key != MuscleGroup.CARDIO }
                .map { MuscleVolume(it.key, it.value) }
                .sortedByDescending { it.volumeKg }
        }
    }

    // ── Observation ───────────────────────────────────────────────────────────

    fun observeWorkout(id: Long): Flow<WorkoutEntity?> = dao.observeWorkout(id)
    fun observeSets(workoutId: Long): Flow<List<SetEntity>> = dao.observeSets(workoutId)
    fun observeLoggedExercises(): Flow<List<ExerciseRef>> = dao.observeLoggedExercises()
    fun observeWorkouts(): Flow<List<WorkoutEntity>> = dao.observeWorkouts()

    // ── Trends ────────────────────────────────────────────────────────────────

    fun trendsFor(exerciseId: String): Flow<List<TrendPoint>> =
        dao.observeSetsForExercise(exerciseId).map { sets ->
            sets.groupBy { it.workoutId }
                .map { (_, grp) ->
                    TrendPoint(
                        timeMillis   = grp.minOf { it.performedAt },
                        estimated1Rm = grp.maxOf { epley(it.weightKg, it.reps) },
                        volume       = grp.sumOf { it.weightKg * it.reps },
                        topWeight    = grp.maxOf { it.weightKg }
                    )
                }
                .sortedBy { it.timeMillis }
        }

    // ── Helpers ───────────────────────────────────────────────────────────────

    companion object {
        fun epley(weightKg: Double, reps: Int): Double =
            if (reps <= 1) weightKg else weightKg * (1.0 + reps / 30.0)

        /** Epoch-millis for Monday 00:00:00 in the device's local timezone. */
        fun mondayMidnightMillis(): Long {
            val zone = ZoneId.systemDefault()
            val now  = java.time.LocalDate.now(zone)
            val mon  = now.with(ChronoField.DAY_OF_WEEK, 1L)
            return mon.atStartOfDay(zone).toInstant().toEpochMilli()
        }
    }
}
