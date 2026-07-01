package com.kargathra.fitness.data.backup

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.kargathra.fitness.data.db.FavouriteEntity
import com.kargathra.fitness.data.db.KargathraDatabase
import com.kargathra.fitness.data.db.SavedRoutineEntity
import com.kargathra.fitness.data.db.SetEntity
import com.kargathra.fitness.data.db.WorkoutEntity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exports/imports all user data (workouts, sets, favourites, custom programs)
 * as a single JSON file. Export writes to Downloads via MediaStore (no
 * permission needed). Import appends: workouts/sets get fresh ids, favourites
 * and programs upsert by id. Nothing is ever deleted by an import.
 */
class BackupManager(private val db: KargathraDatabase) {

    suspend fun export(context: Context): String {
        val root = JSONObject()
        root.put("format", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val workouts = JSONArray()
        db.workoutDao().allWorkouts().forEach { w ->
            workouts.put(JSONObject().apply {
                put("id", w.id); put("title", w.title)
                put("startedAt", w.startedAt)
                put("completedAt", w.completedAt ?: JSONObject.NULL)
                put("avgBpm", w.avgBpm ?: JSONObject.NULL)
                put("maxBpm", w.maxBpm ?: JSONObject.NULL)
                put("activeKcal", w.activeKcal ?: JSONObject.NULL)
            })
        }
        root.put("workouts", workouts)

        val sets = JSONArray()
        db.workoutDao().allSets().forEach { s ->
            sets.put(JSONObject().apply {
                put("workoutId", s.workoutId); put("exerciseId", s.exerciseId)
                put("exerciseName", s.exerciseName)
                put("weightKg", s.weightKg); put("reps", s.reps)
                put("performedAt", s.performedAt)
            })
        }
        root.put("sets", sets)

        root.put("favourites", JSONArray(db.favouriteDao().favouriteIdsOnce()))

        val programs = JSONArray()
        db.savedRoutineDao().allOnce().forEach { p ->
            programs.put(JSONObject().apply {
                put("id", p.id); put("title", p.title)
                put("json", p.json); put("savedAt", p.savedAt)
            })
        }
        root.put("programs", programs)

        val name = "kargathra_backup_" +
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".json"
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: return "Export failed: couldn't create file"
        resolver.openOutputStream(uri)?.use { out ->
            out.write(root.toString(2).toByteArray())
        } ?: return "Export failed: couldn't write file"

        return "Exported to Downloads/$name " +
            "(${workouts.length()} workouts, ${sets.length()} sets, " +
            "${programs.length()} programs)"
    }

    suspend fun import(context: Context, source: Uri): String {
        val text = try {
            context.contentResolver.openInputStream(source)?.use {
                it.readBytes().decodeToString()
            } ?: return "Import failed: couldn't open file"
        } catch (e: Exception) {
            return "Import failed: ${e.message}"
        }

        val root = try { JSONObject(text) } catch (e: Exception) {
            return "Import failed: not a valid backup file"
        }
        if (root.optInt("format", -1) != 1) return "Import failed: unknown backup format"

        var nWorkouts = 0; var nSets = 0; var nFavs = 0; var nProgs = 0

        // Workouts + sets: insert with fresh ids, remap set.workoutId
        val idMap = HashMap<Long, Long>()
        val workouts = root.optJSONArray("workouts") ?: JSONArray()
        for (i in 0 until workouts.length()) {
            val o = workouts.getJSONObject(i)
            val newId = db.workoutDao().insertWorkout(
                WorkoutEntity(
                    title = o.getString("title"),
                    startedAt = o.getLong("startedAt"),
                    completedAt = if (o.isNull("completedAt")) null else o.getLong("completedAt"),
                    avgBpm = if (o.isNull("avgBpm")) null else o.getLong("avgBpm"),
                    maxBpm = if (o.isNull("maxBpm")) null else o.getLong("maxBpm"),
                    activeKcal = if (o.isNull("activeKcal")) null else o.getDouble("activeKcal")
                )
            )
            idMap[o.getLong("id")] = newId
            nWorkouts++
        }
        val sets = root.optJSONArray("sets") ?: JSONArray()
        for (i in 0 until sets.length()) {
            val o = sets.getJSONObject(i)
            val wid = idMap[o.getLong("workoutId")] ?: continue
            db.workoutDao().insertSet(
                SetEntity(
                    workoutId = wid,
                    exerciseId = o.getString("exerciseId"),
                    exerciseName = o.optString("exerciseName", ""),
                    weightKg = o.getDouble("weightKg"),
                    reps = o.getInt("reps"),
                    performedAt = o.getLong("performedAt")
                )
            )
            nSets++
        }

        val favs = root.optJSONArray("favourites") ?: JSONArray()
        for (i in 0 until favs.length()) {
            db.favouriteDao().add(FavouriteEntity(favs.getString(i), System.currentTimeMillis()))
            nFavs++
        }

        val progs = root.optJSONArray("programs") ?: JSONArray()
        for (i in 0 until progs.length()) {
            val o = progs.getJSONObject(i)
            db.savedRoutineDao().upsert(
                SavedRoutineEntity(
                    id = o.getString("id"), title = o.getString("title"),
                    json = o.getString("json"), savedAt = o.getLong("savedAt")
                )
            )
            nProgs++
        }

        return "Imported $nWorkouts workouts, $nSets sets, $nFavs favourites, $nProgs programs"
    }
}
