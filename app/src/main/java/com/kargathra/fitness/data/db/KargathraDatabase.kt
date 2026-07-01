package com.kargathra.fitness.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        WorkoutEntity::class,
        SetEntity::class,
        ExerciseEntity::class,  // added in v2
        FavouriteEntity::class,  // added in v3
        SavedRoutineEntity::class  // added in v4
    ],
    version = 5,
    exportSchema = false
)
abstract class KargathraDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun favouriteDao(): FavouriteDao
    abstract fun savedRoutineDao(): SavedRoutineDao
}
