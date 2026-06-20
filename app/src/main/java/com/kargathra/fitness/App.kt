package com.kargathra.fitness

import android.app.Application
import androidx.room.Room
import com.kargathra.fitness.data.db.KargathraDatabase
import com.kargathra.fitness.data.repo.WorkoutRepository
import com.kargathra.fitness.health.HealthConnectManager

/** Minimal manual DI: one place that owns the database, Health Connect
 *  manager and repository for the whole process. */
class App : Application() {
    val database: KargathraDatabase by lazy {
        Room.databaseBuilder(this, KargathraDatabase::class.java, "kargathra.db")
            .fallbackToDestructiveMigration()
            .build()
    }
    val healthConnect: HealthConnectManager by lazy { HealthConnectManager(this) }
    val repository: WorkoutRepository by lazy { WorkoutRepository(database.workoutDao(), healthConnect) }
}
