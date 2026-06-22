package com.kargathra.fitness

import android.app.Application
import androidx.room.Room
import com.kargathra.fitness.data.db.KargathraDatabase
import com.kargathra.fitness.data.repo.ExerciseRepository
import com.kargathra.fitness.data.repo.WorkoutRepository
import com.kargathra.fitness.health.HealthConnectManager

/**
 * Manual DI root.
 *
 * The exerciseapi.dev API key is read from BuildConfig.EXERCISE_API_KEY —
 * set it in local.properties as:
 *   EXERCISE_API_KEY=exlib_your32characterkeyhere
 * and add to build.gradle.kts:
 *   buildConfigField("String", "EXERCISE_API_KEY", "\"${properties["EXERCISE_API_KEY"]}\"")
 *
 * The key is stored on-device only; it is never transmitted except to
 * api.exerciseapi.dev as the X-API-Key header.
 */
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
        // Read key from BuildConfig; falls back to empty string if not configured
        val key = try { BuildConfig.EXERCISE_API_KEY } catch (e: Exception) { "" }
        ExerciseRepository(database.exerciseDao(), key)
    }
}
