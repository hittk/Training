package com.kargathra.fitness.data.api

import com.kargathra.fitness.data.db.ExerciseEntity
import com.kargathra.fitness.data.db.deriveMovementFamily
import com.kargathra.fitness.data.db.joinPipe
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Thin HTTP client for exerciseapi.dev.
 *
 * Responsibilities:
 *  - Execute all [ExerciseFetchPlan] batches with rate-limit back-off
 *  - Map API JSON → [ExerciseEntity]
 *  - Deduplicate by movement family within each batch
 *  - Never exceed the free-tier pagination depth (offset + limit ≤ 500)
 */
class ExerciseApiClient(private val apiKey: String) {

    companion object {
        private const val BASE = "https://api.exerciseapi.dev/v1"
        private const val PAGE_SIZE = 20       // free tier max
        private const val CONNECT_TIMEOUT = 15_000
        private const val READ_TIMEOUT = 20_000
        /** ms to wait between pages to stay well under 60 req/min */
        private const val PAGE_DELAY_MS = 1_200L
    }

    // ── Public ────────────────────────────────────────────────────────────────

    /**
     * Fetches all batches defined in [ExerciseFetchPlan].
     * Returns a deduplicated list of [ExerciseEntity] capped at 500 total.
     *
     * [onProgress] receives (batchIndex, totalBatches, exercisesSoFar).
     */
    suspend fun fetchAll(
        onProgress: (Int, Int, Int) -> Unit = { _, _, _ -> }
    ): List<ExerciseEntity> {
        val batches = ExerciseFetchPlan.batches
        val allEntities = mutableListOf<ExerciseEntity>()
        // Track movement families already seen to avoid cross-batch duplicates
        val seenFamilies = mutableSetOf<String>()
        val now = System.currentTimeMillis()

        batches.forEachIndexed { batchIdx, batch ->
            val batchEntities = fetchBatch(batch, seenFamilies, now)
            allEntities += batchEntities
            onProgress(batchIdx + 1, batches.size, allEntities.size)
        }

        return allEntities
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private suspend fun fetchBatch(
        batch: FetchBatch,
        seenFamilies: MutableSet<String>,
        fetchedAt: Long
    ): List<ExerciseEntity> {
        val result = mutableListOf<ExerciseEntity>()
        var offset = 0

        while (result.size < batch.slotLimit) {
            // Free-tier guard: offset + limit must not exceed 500
            val remaining = batch.slotLimit - result.size
            val limit = minOf(PAGE_SIZE, remaining)
            if (offset + limit > 500) break

            val url = buildUrl(batch, limit, offset)
            val page = fetchPage(url) ?: break

            if (page.isEmpty()) break

            for (item in page) {
                val family = deriveMovementFamily(item.name)
                if (family in seenFamilies) continue
                seenFamilies += family
                result += item.copy(
                    movementFamily   = family,
                    requiresPunchBag = batch.requiresPunchBag,
                    fetchedAt        = fetchedAt
                )
                if (result.size >= batch.slotLimit) break
            }

            offset += limit
            delay(PAGE_DELAY_MS)
        }

        return result
    }

    private fun buildUrl(batch: FetchBatch, limit: Int, offset: Int): String {
        val params = mutableListOf<String>()
        if (batch.category.isNotEmpty())  params += "category=${batch.category.encode()}"
        if (batch.equipment.isNotEmpty()) params += "equipment=${batch.equipment.encode()}"
        if (batch.mechanic.isNotEmpty())  params += "mechanic=${batch.mechanic.encode()}"
        if (batch.level.isNotEmpty())     params += "level=${batch.level.encode()}"
        params += "limit=$limit"
        params += "offset=$offset"
        return "$BASE/exercises?${params.joinToString("&")}"
    }

    private fun String.encode() = URLEncoder.encode(this, "UTF-8")

    private fun fetchPage(urlStr: String): List<ExerciseEntity>? {
        return try {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("X-API-Key", apiKey)
                connectTimeout = CONNECT_TIMEOUT
                readTimeout    = READ_TIMEOUT
            }
            when (conn.responseCode) {
                200 -> {
                    val body = conn.inputStream.bufferedReader().readText()
                    parseExerciseList(body)
                }
                429 -> {
                    // Rate limited — respect Retry-After if present
                    val retryAfter = conn.getHeaderField("Retry-After")?.toLongOrNull() ?: 60L
                    throw RateLimitException(retryAfter)
                }
                else -> null
            }
        } catch (e: RateLimitException) {
            throw e
        } catch (e: Exception) {
            null // Network error — caller gets empty result for this page
        }
    }

    private fun parseExerciseList(body: String): List<ExerciseEntity> {
        val root = JSONObject(body)
        val data = root.getJSONArray("data")
        val result = mutableListOf<ExerciseEntity>()
        for (i in 0 until data.length()) {
            parseExercise(data.getJSONObject(i))?.let { result += it }
        }
        return result
    }

    private fun parseExercise(obj: JSONObject): ExerciseEntity? {
        return try {
            val id   = obj.getString("id")
            val name = obj.getString("name")

            fun jsonArray(key: String): List<String> {
                val arr = obj.optJSONArray(key) ?: return emptyList()
                return (0 until arr.length()).map { arr.getString(it) }
            }

            // First video URL
            val videoUrl = obj.optJSONArray("videos")
                ?.let { if (it.length() > 0) it.getJSONObject(0).optString("url", "") else "" }
                ?: ""

            ExerciseEntity(
                id              = id,
                name            = name,
                category        = obj.optString("category", ""),
                equipment       = obj.optString("equipment", ""),
                mechanic        = obj.optString("mechanic", ""),
                force           = obj.optString("force", ""),
                level           = obj.optString("level", ""),
                primaryMuscles  = jsonArray("primaryMuscles").joinPipe(),
                secondaryMuscles= jsonArray("secondaryMuscles").joinPipe(),
                overview        = obj.optString("overview", ""),
                instructions    = jsonArray("instructions").joinPipe(),
                exerciseTips    = jsonArray("exerciseTips").joinPipe(),
                commonMistakes  = jsonArray("commonMistakes").joinPipe(),
                safetyInfo      = obj.optString("safetyInfo", ""),
                variations      = jsonArray("variations").joinPipe(),
                videoUrl        = videoUrl,
                movementFamily  = deriveMovementFamily(name), // overwritten by batch logic
                requiresPunchBag= false,                      // overwritten by batch logic
                fetchedAt       = 0L                          // overwritten by batch logic
            )
        } catch (e: Exception) {
            null
        }
    }
}

class RateLimitException(val retryAfterSeconds: Long) : Exception("Rate limited")
