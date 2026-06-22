package com.kargathra.fitness

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.kargathra.fitness.data.db.KargathraDatabase
import com.kargathra.fitness.data.repo.ExerciseRepository
import com.kargathra.fitness.data.repo.WorkoutRepository
import com.kargathra.fitness.health.HealthConnectManager

class App : Application() {

    val database: KargathraDatabase by lazy {
        Room.databaseBuilder(this, KargathraDatabase::class.java, "kargathra.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    val healthConnect: HealthConnectManager by lazy { HealthConnectManager(this) }

    val repository: WorkoutRepository by lazy {
        WorkoutRepository(database.workoutDao(), healthConnect)
    }

    val exerciseRepository: ExerciseRepository by lazy {
        // Key is stored in SharedPreferences and entered via Settings screen
        val prefs = getSharedPreferences("kargathra", Context.MODE_PRIVATE)
        val key = prefs.getString("exercise_api_key", "") ?: ""
        ExerciseRepository(database.exerciseDao(), key)
    }
}
