package com.kargathra.fitness.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        WorkoutEntity::class,
        SetEntity::class,
        ExerciseEntity::class   // added in v2
    ],
    version = 2,
    exportSchema = false
)
abstract class KargathraDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun exerciseDao(): ExerciseDao
}
