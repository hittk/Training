package com.kargathra.fitness.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouriteDao {

    @Query("SELECT exerciseId FROM favourites")
    fun favouriteIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favourites WHERE exerciseId = :id)")
    fun isFavourite(id: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(fav: FavouriteEntity)

    @Query("DELETE FROM favourites WHERE exerciseId = :id")
    suspend fun remove(id: String)

    @Query("SELECT exerciseId FROM favourites")
    suspend fun favouriteIdsOnce(): List<String>
}
