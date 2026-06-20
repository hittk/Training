package com.kargathra.fitness.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Insert suspend fun insertWorkout(w: WorkoutEntity): Long
    @Update suspend fun updateWorkout(w: WorkoutEntity)
    @Query("SELECT * FROM workouts WHERE id = :id") suspend fun getWorkout(id: Long): WorkoutEntity?
    @Query("SELECT * FROM workouts WHERE id = :id") fun observeWorkout(id: Long): Flow<WorkoutEntity?>
    @Query("SELECT * FROM workouts ORDER BY startedAt DESC") fun observeWorkouts(): Flow<List<WorkoutEntity>>

    @Insert suspend fun insertSet(s: SetEntity): Long
    @Delete suspend fun deleteSet(s: SetEntity)
    @Query("SELECT * FROM sets WHERE workoutId = :wid ORDER BY id ASC")
    fun observeSets(wid: Long): Flow<List<SetEntity>>

    @Query("SELECT * FROM sets WHERE exerciseId = :exId ORDER BY performedAt ASC")
    fun observeSetsForExercise(exId: String): Flow<List<SetEntity>>

    @Query("SELECT DISTINCT exerciseId, exerciseName FROM sets ORDER BY exerciseName")
    fun observeLoggedExercises(): Flow<List<ExerciseRef>>
}
