package com.kargathra.fitness.data.anatomy


/**
 * Maps the fine-grained anatomical muscle names in the exercise data onto the
 * muscle-group ids used by the bundled SVG muscle map (the `data-group` values).
 *
 * SVG groups available:
 *   front: chest, shoulder, biceps, forearm, abs, obliques, quads, calves, trap
 *   back:  shoulder, triceps, forearm, trap, lats, lowerback, glutes, hamstrings, calves
 *
 * Folds (per product decision):
 *   rhomboids / upper-back  -> trap
 *   adductors / inner thigh -> quads
 */
object MuscleMap {

    /** Groups seen only on the back view. */
    private val backOnly = setOf("triceps", "lats", "lowerback", "glutes", "hamstrings")
    /** Groups seen only on the front view. */
    private val frontOnly = setOf("chest", "biceps", "abs", "obliques", "quads")
    /** Groups visible on both views (default to front when shown alone). */
    private val bothViews = setOf("shoulder", "forearm", "trap", "calves")

    fun groupFor(rawMuscle: String): String? = when (rawMuscle.trim().lowercase()) {
        // Chest
        "pectoralis major sternal head",
        "pectoralis major clavicular head",
        "pectoralis minor",
        "serratus anterior"                 -> "chest"

        // Shoulders (single shoulder region per view)
        "deltoid anterior",
        "deltoid lateral",
        "deltoid posterior",
        "rotator cuff", "teres minor", "teres major" -> "shoulder"

        // Arms
        "biceps brachii long head",
        "biceps brachii short head",
        "brachialis"                        -> "biceps"
        "triceps brachii long head",
        "triceps brachii lateral head",
        "triceps brachii medial head"       -> "triceps"
        "wrist flexors", "wrist extensors", "brachioradialis" -> "forearm"

        // Core
        "rectus abdominis", "transverse abdominis" -> "abs"
        "obliques external", "obliques internal"   -> "obliques"

        // Back
        "trapezius upper", "trapezius middle", "trapezius lower",
        "levator scapulae",
        "rhomboids"                         -> "trap"   // rhomboids folded into trap
        "latissimus dorsi"                  -> "lats"
        "erector spinae"                    -> "lowerback"

        // Hips / thighs
        "gluteus maximus", "gluteus medius", "gluteus minimus",
        "tensor fasciae latae"              -> "glutes"
        "rectus femoris", "vastus lateralis", "vastus medialis", "vastus intermedius",
        "adductor magnus", "adductor longus", "gracilis", "iliopsoas" -> "quads" // adductors folded in
        "biceps femoris", "semitendinosus", "semimembranosus" -> "hamstrings"

        // Lower leg
        "gastrocnemius", "soleus", "tibialis anterior" -> "calves"

        // Neck has no SVG region -> ignore
        "sternocleidomastoid", "neck extensors" -> null

        else -> null
    }

    /**
     * Builds the engagement map for the muscle view from an exercise's muscles.
     * Primary muscles win over secondary if a group appears in both.
     */
    fun engagementFor(
        primaryMuscles: List<String>,
        secondaryMuscles: List<String>
    ): Map<String, Engagement> {
        val map = HashMap<String, Engagement>()
        secondaryMuscles.forEach { raw ->
            groupFor(raw)?.let { map[it] = Engagement.SECONDARY }
        }
        primaryMuscles.forEach { raw ->
            groupFor(raw)?.let { map[it] = Engagement.PRIMARY }  // overrides secondary
        }
        return map
    }

    /** Show the front view if any front-only or both-view group is engaged. */
    fun needsFront(groups: Set<String>): Boolean =
        groups.any { it in frontOnly || it in bothViews }

    /** Show the back view only if a back-specific group is engaged. */
    fun needsBack(groups: Set<String>): Boolean =
        groups.any { it in backOnly }

    /** Coarse body regions for the exercise list filter. */
    enum class BodyRegion(val label: String) {
        CHEST("Chest"), CORE("Core"), ARMS("Arms"), LEGS("Legs")
    }

    private val regionGroups: Map<BodyRegion, Set<String>> = mapOf(
        BodyRegion.CHEST to setOf("chest", "shoulder", "trap"),
        BodyRegion.CORE  to setOf("abs", "obliques", "lats", "lowerback"),
        BodyRegion.ARMS  to setOf("biceps", "triceps", "forearm"),
        BodyRegion.LEGS  to setOf("quads", "hamstrings", "glutes", "calves")
    )

    private val neckNames = setOf("sternocleidomastoid", "neck extensors")

    /** True if an exercise (by primary muscle names) belongs to [region]. Neck counts as Chest. */
    fun inRegion(primaryMuscles: List<String>, region: BodyRegion): Boolean {
        val groups = regionGroups[region] ?: return false
        return primaryMuscles.any { raw ->
            val g = groupFor(raw)
            (g != null && g in groups) ||
            (region == BodyRegion.CHEST && raw.trim().lowercase() in neckNames)
        }
    }

}
