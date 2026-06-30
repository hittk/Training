package com.kargathra.fitness.data.video

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Manages user-added exercise videos stored in app-internal storage
 * (filesDir/uservideos/<exerciseId>.mp4). These persist across app updates,
 * are private to the app, and are per-device (not in the repo).
 */
object UserVideoStore {

    private fun dir(context: Context): File =
        File(context.filesDir, "uservideos").apply { if (!exists()) mkdirs() }

    /** The file where a given exercise's user video lives (may not exist yet). */
    fun fileFor(context: Context, exerciseId: String): File =
        File(dir(context), "${safe(exerciseId)}.mp4")

    /** True if the user has added a video for this exercise. */
    fun has(context: Context, exerciseId: String): Boolean =
        fileFor(context, exerciseId).let { it.exists() && it.length() > 0 }

    /**
     * Copies the picked video into internal storage for this exercise.
     * Returns true on success. Overwrites any existing user video for the id.
     */
    fun save(context: Context, exerciseId: String, source: Uri): Boolean {
        return try {
            val target = fileFor(context, exerciseId)
            context.contentResolver.openInputStream(source)?.use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return false
            target.exists() && target.length() > 0
        } catch (e: Exception) {
            false
        }
    }

    private fun safe(id: String): String =
        id.lowercase().replace(Regex("[^a-z0-9_]"), "_")
}
