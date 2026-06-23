package com.kargathra.fitness

import android.app.Application
import androidx.room.Room
import com.kargathra.fitness.data.db.KargathraDatabase
import com.kargathra.fitness.data.repo.ExerciseRepository
import com.kargathra.fitness.data.repo.WorkoutRepository
import com.kargathra.fitness.health.HealthConnectManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class App : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
        ExerciseRepository(database.exerciseDao(), applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
        // Populate the exercise library from the bundled asset on first launch.
        appScope.launch { exerciseRepository.ensureLoaded() }
    }
}
