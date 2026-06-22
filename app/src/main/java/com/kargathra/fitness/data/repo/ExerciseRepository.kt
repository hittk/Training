package com.kargathra.fitness.data.repo

import com.kargathra.fitness.data.api.ExerciseApiClient
import com.kargathra.fitness.data.api.RateLimitException
import com.kargathra.fitness.data.db.ExerciseDao
import com.kargathra.fitness.data.db.ExerciseEntity
import kotlinx.coroutines.flow.Flow

/** How old the cache must be (ms) before a background refresh is triggered */
private const val CACHE_TTL_MS = 24L * 60 * 60 * 1_000 // 24 hours

sealed class SyncState {
    object Idle : SyncState()
    data class Syncing(val batchDone: Int, val batchTotal: Int, val exerciseCount: Int) : SyncState()
    data class Done(val count: Int) : SyncState()
    data class Error(val message: String) : SyncState()
    object RateLimited : SyncState()
}

class ExerciseRepository(
    val dao: ExerciseDao,
    private val apiKey: String
) {
    private val client = ExerciseApiClient(apiKey)

    // ── Cache management ──────────────────────────────────────────────────────

    /** Returns true if the cache is empty or older than [CACHE_TTL_MS]. */
    suspend fun needsSync(): Boolean {
        val count = dao.count()
        if (count == 0) return true
        val oldest = dao.oldestFetchMs() ?: return true
        return (System.currentTimeMillis() - oldest) > CACHE_TTL_MS
    }

    /**
     * Fetches all exercise batches from the API and stores them in Room.
     * Calls [onState] with progress updates throughout.
     * Safe to call even if cache is fresh — caller should check [needsSync] first.
     */
    suspend fun sync(onState: (SyncState) -> Unit) {
        onState(SyncState.Syncing(0, 10, 0))
        try {
            val exercises = client.fetchAll { batchDone, batchTotal, count ->
                onState(SyncState.Syncing(batchDone, batchTotal, count))
            }
            if (exercises.isNotEmpty()) {
                dao.upsertAll(exercises)
                onState(SyncState.Done(exercises.size))
            } else {
                onState(SyncState.Error("No exercises returned — check API key in settings"))
            }
        } catch (e: RateLimitException) {
            onState(SyncState.RateLimited)
        } catch (e: Exception) {
            onState(SyncState.Error(e.message ?: "Unknown error"))
        }
    }

    // ── Search & browse ───────────────────────────────────────────────────────

    fun search(
        query: String = "",
        equipment: String = "",
        category: String = "",
        mechanic: String = "",
        includePunchBag: Boolean = false,
        limit: Int = 40,
        offset: Int = 0
    ): Flow<List<ExerciseEntity>> = dao.search(
        query          = query.trim(),
        equipment      = equipment,
        category       = category,
        mechanic       = mechanic,
        includePunchBag= includePunchBag,
        limit          = limit,
        offset         = offset
    )

    fun equipmentList(): Flow<List<String>> = dao.equipmentList()

    suspend fun getById(id: String): ExerciseEntity? = dao.getById(id)

    // ── AI generator library ──────────────────────────────────────────────────

    /**
     * Returns one exercise per movement family, compound-first.
     * Used by the AI workout generator to build its exercise name list.
     */
    suspend fun generatorLibrary(
        includePunchBag: Boolean = false
    ): List<ExerciseEntity> = dao.generatorLibrary(includePunchBag = includePunchBag)

    suspend fun count(): Int = dao.count()
}
