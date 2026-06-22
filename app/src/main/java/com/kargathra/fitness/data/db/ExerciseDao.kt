package com.kargathra.fitness.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(exercises: List<ExerciseEntity>)

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    /** Oldest fetchedAt across all records — used to decide if cache is stale */
    @Query("SELECT MIN(fetchedAt) FROM exercises")
    suspend fun oldestFetchMs(): Long?

    // ── Search & filter ───────────────────────────────────────────────────────

    /**
     * Full search: text match on name/muscles + optional equipment/category
     * filters. Punch bag exercises are hidden unless [includePunchBag] is true.
     */
    @Query("""
        SELECT * FROM exercises
        WHERE (:query = '' OR name LIKE '%' || :query || '%'
                           OR primaryMuscles LIKE '%' || :query || '%'
                           OR secondaryMuscles LIKE '%' || :query || '%')
          AND (:equipment = '' OR equipment = :equipment)
          AND (:category  = '' OR category  = :category)
          AND (:mechanic  = '' OR mechanic  = :mechanic)
          AND (requiresPunchBag = 0 OR :includePunchBag = 1)
        ORDER BY
            CASE WHEN name LIKE :query || '%' THEN 0 ELSE 1 END,
            name ASC
        LIMIT :limit OFFSET :offset
    """)
    fun search(
        query: String = "",
        equipment: String = "",
        category: String = "",
        mechanic: String = "",
        includePunchBag: Boolean = false,
        limit: Int = 40,
        offset: Int = 0
    ): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: String): ExerciseEntity?

    /** All distinct equipment values present in the cache */
    @Query("SELECT DISTINCT equipment FROM exercises WHERE equipment != '' ORDER BY equipment")
    fun equipmentList(): Flow<List<String>>

    /** Exercises usable by the AI generator: one per movement family, compound-first */
    @Query("""
        SELECT * FROM exercises
        WHERE (requiresPunchBag = 0 OR :includePunchBag = 1)
          AND (:equipment = '' OR equipment = :equipment)
        GROUP BY movementFamily
        ORDER BY
            CASE mechanic WHEN 'compound' THEN 0 ELSE 1 END,
            name ASC
    """)
    suspend fun generatorLibrary(
        equipment: String = "",
        includePunchBag: Boolean = false
    ): List<ExerciseEntity>
}
