package com.kargathra.fitness.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A favourited exercise, keyed by exercise id. Kept in its own table so it
 * survives library reloads (the exercises table is REPLACE-upserted on version
 * bumps, which would otherwise wipe a flag stored there).
 */
@Entity(tableName = "favourites")
data class FavouriteEntity(
    @PrimaryKey val exerciseId: String,
    val addedAt: Long = System.currentTimeMillis()
)
