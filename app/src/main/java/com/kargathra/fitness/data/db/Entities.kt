package com.kargathra.fitness.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One logged workout instance. Vitals are filled from the Fitbit on finish. */
@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val startedAt: Long,
    val completedAt: Long? = null,
    val avgBpm: Long? = null,
    val maxBpm: Long? = null,
    val activeKcal: Double? = null,
    val syncedToHc: Boolean = false
)

/** A single working set: the weight lifted and the reps performed. */
@Entity(
    tableName = "sets",
    foreignKeys = [ForeignKey(
        entity = WorkoutEntity::class,
        parentColumns = ["id"],
        childColumns = ["workoutId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("workoutId"), Index("exerciseId")]
)
data class SetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val exerciseId: String,
    val exerciseName: String,
    val weightKg: Double,
    val reps: Int,
    val performedAt: Long
)

/** Lightweight projection for the "which exercises have data" list. */
data class ExerciseRef(val exerciseId: String, val exerciseName: String)
