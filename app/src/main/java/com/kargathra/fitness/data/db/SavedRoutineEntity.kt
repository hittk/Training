package com.kargathra.fitness.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A user-saved custom routine, stored as JSON so the nested structure survives. */
@Entity(tableName = "saved_routines")
data class SavedRoutineEntity(
    @PrimaryKey val id: String,
    val title: String,
    val json: String,
    val savedAt: Long = System.currentTimeMillis()
)
