package com.kargathra.fitness

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kargathra.fitness.data.db.KargathraDatabase
import com.kargathra.fitness.data.repo.ExerciseRepository
import com.kargathra.fitness.data.repo.FavouriteRepository
import com.kargathra.fitness.data.repo.SavedRoutineRepository
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
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .fallbackToDestructiveMigration()  // only used for unforeseen jumps
            .build()
    }

    val healthConnect: HealthConnectManager by lazy { HealthConnectManager(this) }

    val repository: WorkoutRepository by lazy {
        WorkoutRepository(database.workoutDao(), healthConnect, database.exerciseDao())
    }

    val exerciseRepository: ExerciseRepository by lazy {
        ExerciseRepository(database.exerciseDao(), applicationContext)
    }

    val favouriteRepository: FavouriteRepository by lazy {
        FavouriteRepository(database.favouriteDao())
    }

    val savedRoutineRepository: SavedRoutineRepository by lazy {
        SavedRoutineRepository(database.savedRoutineDao())
    }

    override fun onCreate() {
        super.onCreate()
        // Populate the exercise library from the bundled asset on first launch.
        appScope.launch { exerciseRepository.ensureLoaded() }
    }
}

/** v4 -> v5: remap legacy preset exercise ids in logged sets to library ids. */
private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val remap = mapOf(
            "bb_bench" to "Barbell_Bench_Press_-_Medium_Grip",
            "incline_db_press" to "Incline_Dumbbell_Press",
            "db_row" to "Dumbbell_Single_Arm_Row_On_Bench",
            "db_shoulder_press" to "Dumbbell_Shoulder_Press",
            "preacher_curl" to "Barbell_Preacher_Curl_Seated",
            "db_rdl" to "Dumbbell_Romanian_Deadlift",
            "bulgarian_split" to "Bulgarian_Split_Squat",
            "goblet_squat" to "Goblet_Squat",
            "close_grip_press" to "Close-Grip_Barbell_Bench_Press",
            "db_lateral" to "Dumbbell_Lateral_Raise",
            "db_curl" to "Dumbbell_Bicep_Curl",
            "calf_raise" to "Standing_Barbell_Calf_Raise"
        )
        remap.forEach { (old, new) ->
            db.execSQL("UPDATE sets SET exerciseId = ? WHERE exerciseId = ?", arrayOf(new, old))
        }
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

/** v3 -> v4: add the saved_routines table (custom programs). */
private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS saved_routines (" +
            "id TEXT NOT NULL PRIMARY KEY, " +
            "title TEXT NOT NULL, " +
            "json TEXT NOT NULL, " +
            "savedAt INTEGER NOT NULL)"
        )
    }
}
