package com.kargathra.fitness.data.repo

import android.content.Context
import com.kargathra.fitness.R
import com.kargathra.fitness.data.db.ExerciseDao
import com.kargathra.fitness.data.db.ExerciseEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Exercise library backed entirely by a bundled JSON asset (res/raw/exercises_cache.json).
 * No network, no API key — the 461-exercise library ships inside the app and is
 * loaded into Room once on first launch.
 */
class ExerciseRepository(
    private val dao: ExerciseDao,
    private val appContext: Context
) {

    /** True if Room has not yet been populated from the bundled asset. */
    suspend fun needsLoad(): Boolean = dao.count() == 0

    /**
     * Loads the bundled exercise library into Room if it isn't already there.
     * Idempotent and cheap to call on every launch — returns early once populated.
     * Returns the number of exercises now in the database.
     */
    suspend fun ensureLoaded(): Int = withContext(Dispatchers.IO) {
        val raw = appContext.resources.openRawResource(R.raw.exercises_cache)
            .bufferedReader().use { it.readText() }
        val root = JSONObject(raw)
        val bundledVersion = root.optInt("libraryVersion", 1)

        val prefs = appContext.getSharedPreferences("kargathra", Context.MODE_PRIVATE)
        val loadedVersion = prefs.getInt("library_version", 0)

        // Load when the DB is empty OR the bundled library is newer than what we
        // last loaded. upsert (REPLACE) merges by id, so this updates existing
        // exercises and inserts new ones without wiping anything.
        if (dao.count() > 0 && loadedVersion >= bundledVersion) {
            return@withContext dao.count()
        }

        val arr: JSONArray = root.getJSONArray("exercises")
        val now = System.currentTimeMillis()
        val entities = ArrayList<ExerciseEntity>(arr.length())
        for (i in 0 until arr.length()) {
            parseEntity(arr.getJSONObject(i), now)?.let { entities += it }
        }
        if (entities.isNotEmpty()) dao.upsertAll(entities)
        prefs.edit().putInt("library_version", bundledVersion).apply()
        dao.count()
    }

    private fun parseEntity(o: JSONObject, fetchedAt: Long): ExerciseEntity? {
        return try {
            // Lists are already pipe-joined? No — the cache stores JSON arrays.
            // Convert arrays → pipe-delimited strings to match the entity schema.
            fun pipe(key: String): String {
                val a = o.optJSONArray(key) ?: return ""
                return (0 until a.length()).joinToString("|") { a.getString(it).trim() }
            }
            ExerciseEntity(
                id               = o.getString("id"),
                name             = o.getString("name"),
                category         = o.optString("category", ""),
                equipment        = o.optString("equipment", ""),
                mechanic         = o.optString("mechanic", ""),
                force            = o.optString("force", ""),
                level            = o.optString("level", ""),
                primaryMuscles   = pipe("primaryMuscles"),
                secondaryMuscles = pipe("secondaryMuscles"),
                overview         = o.optString("overview", ""),
                instructions     = pipe("instructions"),
                exerciseTips     = pipe("exerciseTips"),
                commonMistakes   = pipe("commonMistakes"),
                safetyInfo       = o.optString("safetyInfo", ""),
                variations       = pipe("variations"),
                videoUrl         = o.optString("videoUrl", ""),
                movementFamily   = o.optString("movementFamily", o.getString("name")),
                requiresPunchBag = o.optBoolean("requiresPunchBag", false),
                fetchedAt        = fetchedAt
            )
        } catch (e: Exception) {
            null
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
        query           = query.trim(),
        equipment       = equipment,
        category        = category,
        mechanic        = mechanic,
        includePunchBag = includePunchBag,
        limit           = limit,
        offset          = offset
    )

    fun equipmentList(): Flow<List<String>> = dao.equipmentList()

    suspend fun getById(id: String): ExerciseEntity? = dao.getById(id)

    suspend fun findByName(name: String): ExerciseEntity? = dao.findByName(name.trim())

    /** One exercise per movement family, compound-first — used by the local generator. */
    suspend fun generatorLibrary(includePunchBag: Boolean = false): List<ExerciseEntity> =
        dao.generatorLibrary(includePunchBag = includePunchBag)

    suspend fun count(): Int = dao.count()
}
