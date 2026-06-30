package com.kargathra.fitness

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kargathra.fitness.data.db.KargathraDatabase
import com.kargathra.fitness.data.repo.ExerciseRepository
import com.kargathra.fitness.data.repo.FavouriteRepository
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
            .addMigrations(MIGRATION_2_3)
            .fallbackToDestructiveMigration()  // only used for unforeseen jumps
            .build()
    }

    val healthConnect: HealthConnectManager by lazy { HealthConnectManager(this) }

    val repository: WorkoutRepository by lazy {
        WorkoutRepository(database.workoutDao(), healthConnect)
    }

    val exerciseRepository: ExerciseRepository by lazy {
        ExerciseRepository(database.exerciseDao(), applicationContext)
    }

    val favouriteRepository: FavouriteRepository by lazy {
        FavouriteRepository(database.favouriteDao())
    }

    override fun onCreate() {
        super.onCreate()
        // Populate the exercise library from the bundled asset on first launch.
        appScope.launch { exerciseRepository.ensureLoaded() }
    }
}

/** v2 -> v3: add the favourites table without touching existing data. */
private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS favourites (" +
            "exerciseId TEXT NOT NULL PRIMARY KEY, " +
            "addedAt INTEGER NOT NULL)"
        )
    }
}
