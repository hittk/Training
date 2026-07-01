package com.kargathra.fitness.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedRoutineDao {
    @Query("SELECT * FROM saved_routines ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<SavedRoutineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(routine: SavedRoutineEntity)

    @Query("DELETE FROM saved_routines WHERE id = :id")
    suspend fun delete(id: String)
}
