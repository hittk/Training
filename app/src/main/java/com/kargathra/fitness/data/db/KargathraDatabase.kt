package com.kargathra.fitness.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [WorkoutEntity::class, SetEntity::class], version = 1, exportSchema = false)
abstract class KargathraDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
}
