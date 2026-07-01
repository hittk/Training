package com.kargathra.fitness.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A bundled exercise from the local library (res/raw/exercises_cache.json).
 * All rich fields are stored as pipe-delimited strings to avoid
 * a JSON dependency in the DB layer — they're split on read.
 */
@Entity(
    tableName = "exercises",
    indices = [
        Index("equipment"),
        Index("category"),
        Index("mechanic"),
        Index("movementFamily"),
        Index("requiresPunchBag")
    ]
)
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,           // strength / conditioning / stretching / calisthenics
    val equipment: String,          // barbell / dumbbell / body only / kettlebell / punching bag
    val mechanic: String,           // compound / isolation / ""
    val force: String,              // push / pull / static / ""
    val level: String,              // beginner / intermediate / advanced
    val primaryMuscles: String,     // pipe-delimited anatomical names
    val secondaryMuscles: String,   // pipe-delimited anatomical names
    val overview: String,
    val instructions: String,       // pipe-delimited steps
    val exerciseTips: String,       // pipe-delimited tips
    val commonMistakes: String,     // pipe-delimited mistakes
    val safetyInfo: String,
    val variations: String,         // pipe-delimited variation names
    val videoUrl: String,           // first video URL or ""
    /** Derived: base movement name stripped of grip/width modifiers */
    val movementFamily: String,
    /** True if this exercise requires a punch bag — hidden until bag is owned */
    val requiresPunchBag: Boolean = false,
    /** Unix ms when this record was last fetched */
    val fetchedAt: Long = 0L
)

// ── Helpers ───────────────────────────────────────────────────────────────────

fun String.splitPipe(): List<String> =
    if (isBlank()) emptyList() else split("|").map { it.trim() }.filter { it.isNotEmpty() }

fun List<String>.joinPipe(): String = joinToString("|")

/**
 * Derives a movement family name from an exercise name by stripping common
 * grip/width/position qualifiers. Used to prevent duplicate movements in
 * AI-generated routines.
 *
 * "Barbell Bench Press - Wide Grip"  → "Barbell Bench Press"
 * "Incline Dumbbell Press"           → "Incline Dumbbell Press"
 */
fun deriveMovementFamily(name: String): String {
    val stripPatterns = listOf(
        " - Wide Grip", " - Medium Grip", " - Close Grip", " - Narrow Grip",
        " - Reverse Grip", " - Underhand Grip", " - Overhand Grip",
        " - Pronated", " - Supinated", " - Neutral Grip",
        " - Low Cable", " - High Cable",
        " (Barbell)", " (Dumbbell)", " (Machine)",
        " - Single Arm", " - One Arm",
        " - Alternate", " - Both Arms"
    )
    var family = name
    for (pattern in stripPatterns) {
        family = family.replace(pattern, "", ignoreCase = true)
    }
    return family.trim()
}
