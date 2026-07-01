package com.kargathra.fitness.data.repo

import com.kargathra.fitness.data.db.SavedRoutineDao
import com.kargathra.fitness.data.db.SavedRoutineEntity
import com.kargathra.fitness.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class SavedRoutineRepository(private val dao: SavedRoutineDao) {

    val savedRoutines: Flow<List<Routine>> =
        dao.observeAll().map { list -> list.mapNotNull { runCatching { fromJson(it.json) }.getOrNull() } }

    suspend fun save(routine: Routine) {
        // Give custom saves a stable, unique id if generated
        val id = if (routine.id.startsWith("custom_")) routine.id
                 else "custom_${System.currentTimeMillis()}"
        val stored = routine.copy(id = id)
        dao.upsert(
            SavedRoutineEntity(id = id, title = stored.title, json = toJson(stored))
        )
    }

    suspend fun delete(id: String) = dao.delete(id)

    /** Create a new empty custom program; returns its id. */
    suspend fun createEmpty(title: String): String {
        val id = "custom_${System.currentTimeMillis()}"
        val routine = Routine(
            id = id, title = title, focus = emptyList(),
            items = emptyList(), estimatedMinutes = 0, isGenerated = false
        )
        dao.upsert(SavedRoutineEntity(id = id, title = title, json = toJson(routine)))
        return id
    }

    /** Add a routine item to a saved program (by id). No-op if program missing. */
    suspend fun addItem(programId: String, item: RoutineItem) {
        val current = load(programId) ?: return
        val updated = current.copy(
            items = current.items + item,
            estimatedMinutes = estimateMinutes(current.items.size + 1)
        )
        dao.upsert(SavedRoutineEntity(programId, updated.title, toJson(updated)))
    }

    /** Remove the item at [index] from a saved program. */
    suspend fun removeItemAt(programId: String, index: Int) {
        val current = load(programId) ?: return
        if (index !in current.items.indices) return
        val newItems = current.items.toMutableList().apply { removeAt(index) }
        val updated = current.copy(
            items = newItems,
            estimatedMinutes = estimateMinutes(newItems.size)
        )
        dao.upsert(SavedRoutineEntity(programId, updated.title, toJson(updated)))
    }

    /** Rename a saved program. */
    suspend fun rename(programId: String, title: String) {
        val current = load(programId) ?: return
        val updated = current.copy(title = title)
        dao.upsert(SavedRoutineEntity(programId, title, toJson(updated)))
    }

    private suspend fun load(programId: String): Routine? =
        dao.getOnce(programId)?.let { runCatching { fromJson(it.json) }.getOrNull() }

    private fun estimateMinutes(itemCount: Int): Int = itemCount * 6

    // ── JSON (manual, to keep the nested Routine structure intact) ────────────

    private fun toJson(r: Routine): String {
        val o = JSONObject()
        o.put("id", r.id)
        o.put("title", r.title)
        o.put("estimatedMinutes", r.estimatedMinutes)
        o.put("isGenerated", r.isGenerated)
        o.put("focus", JSONArray(r.focus.map { it.name }))
        val items = JSONArray()
        r.items.forEach { it ->
            val io = JSONObject()
            io.put("sets", it.sets)
            io.put("repFrom", it.repTarget.first)
            io.put("repTo", it.repTarget.last)
            io.put("restSeconds", it.restSeconds)
            val e = it.exercise
            val eo = JSONObject()
            eo.put("id", e.id)
            eo.put("name", e.name)
            eo.put("primary", e.primary.name)
            eo.put("secondary", JSONArray(e.secondary.map { s -> s.name }))
            eo.put("equipment", JSONArray(e.equipment.map { q -> q.name }))
            eo.put("mechanic", e.mechanic.name)
            eo.put("pattern", e.pattern.name)
            eo.put("repFrom", e.repRange.first)
            eo.put("repTo", e.repRange.last)
            eo.put("cues", JSONArray(e.cues))
            eo.put("illustrationKey", e.illustrationKey ?: JSONObject.NULL)
            io.put("exercise", eo)
            items.put(io)
        }
        o.put("items", items)
        return o.toString()
    }

    private fun fromJson(s: String): Routine {
        val o = JSONObject(s)
        val focus = o.getJSONArray("focus").let { arr ->
            (0 until arr.length()).map { MuscleGroup.valueOf(arr.getString(it)) }
        }
        val itemsArr = o.getJSONArray("items")
        val items = (0 until itemsArr.length()).map { i ->
            val io = itemsArr.getJSONObject(i)
            val eo = io.getJSONObject("exercise")
            val exercise = Exercise(
                id = eo.getString("id"),
                name = eo.getString("name"),
                primary = MuscleGroup.valueOf(eo.getString("primary")),
                secondary = eo.getJSONArray("secondary").let { a ->
                    (0 until a.length()).map { MuscleGroup.valueOf(a.getString(it)) }
                },
                equipment = eo.getJSONArray("equipment").let { a ->
                    (0 until a.length()).map { Equipment.valueOf(a.getString(it)) }
                },
                mechanic = Mechanic.valueOf(eo.getString("mechanic")),
                pattern = Pattern.valueOf(eo.getString("pattern")),
                repRange = eo.getInt("repFrom")..eo.getInt("repTo"),
                cues = eo.getJSONArray("cues").let { a ->
                    (0 until a.length()).map { a.getString(it) }
                },
                illustrationKey = if (eo.isNull("illustrationKey")) null else eo.getString("illustrationKey")
            )
            RoutineItem(
                exercise = exercise,
                sets = io.getInt("sets"),
                repTarget = io.getInt("repFrom")..io.getInt("repTo"),
                restSeconds = io.getInt("restSeconds")
            )
        }
        return Routine(
            id = o.getString("id"),
            title = o.getString("title"),
            focus = focus,
            items = items,
            estimatedMinutes = o.getInt("estimatedMinutes"),
            isGenerated = o.optBoolean("isGenerated", false)
        )
    }
}
