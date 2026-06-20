package com.kargathra.fitness.data.repo

import com.kargathra.fitness.data.db.ExerciseRef
import com.kargathra.fitness.data.db.SetEntity
import com.kargathra.fitness.data.db.WorkoutDao
import com.kargathra.fitness.data.db.WorkoutEntity
import com.kargathra.fitness.data.model.Exercise
import com.kargathra.fitness.health.HealthConnectManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

/** One plotted session point for an exercise's trend over time. */
data class TrendPoint(
    val timeMillis: Long,
    val estimated1Rm: Double,
    val volume: Double,
    val topWeight: Double
)

class WorkoutRepository(
    private val dao: WorkoutDao,
    private val health: HealthConnectManager
) {
    // ----- workout lifecycle -----
    suspend fun startWorkout(title: String): Long =
        dao.insertWorkout(WorkoutEntity(title = title, startedAt = System.currentTimeMillis()))

    suspend fun addSet(workoutId: Long, exercise: Exercise, weightKg: Double, reps: Int) {
        dao.insertSet(
            SetEntity(
                workoutId = workoutId,
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                weightKg = weightKg,
                reps = reps,
                performedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteSet(set: SetEntity) = dao.deleteSet(set)

    /** Marks the workout complete, writes the session to Health Connect, and
     *  reads the Fitbit's HR/calories back over the window. Best-effort: if
     *  Health Connect isn't ready, the workout still saves locally. */
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
                val endI = Instant.ofEpochMilli(end)
                health.writeStrengthSession(start, endI, w.title)
                val v = health.readSessionVitals(start, endI)
                avg = v.avgBpm; max = v.maxBpm; kcal = v.activeKcal; synced = true
            }
        }
        dao.updateWorkout(
            w.copy(completedAt = end, avgBpm = avg, maxBpm = max, activeKcal = kcal, syncedToHc = synced)
        )
    }

    // ----- observation -----
    fun observeWorkout(id: Long): Flow<WorkoutEntity?> = dao.observeWorkout(id)
    fun observeSets(workoutId: Long): Flow<List<SetEntity>> = dao.observeSets(workoutId)
    fun observeLoggedExercises(): Flow<List<ExerciseRef>> = dao.observeLoggedExercises()
    fun observeWorkouts(): Flow<List<WorkoutEntity>> = dao.observeWorkouts()

    // ----- trends -----
    /** Groups an exercise's sets by workout and derives per-session metrics. */
    fun trendsFor(exerciseId: String): Flow<List<TrendPoint>> =
        dao.observeSetsForExercise(exerciseId).map { sets ->
            sets.groupBy { it.workoutId }
                .map { (_, grp) ->
                    TrendPoint(
                        timeMillis = grp.minOf { it.performedAt },
                        estimated1Rm = grp.maxOf { epley(it.weightKg, it.reps) },
                        volume = grp.sumOf { it.weightKg * it.reps },
                        topWeight = grp.maxOf { it.weightKg }
                    )
                }
                .sortedBy { it.timeMillis }
        }

    companion object {
        /** Epley estimated one-rep max. */
        fun epley(weightKg: Double, reps: Int): Double =
            if (reps <= 1) weightKg else weightKg * (1.0 + reps / 30.0)
    }
}
