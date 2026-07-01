package com.kargathra.fitness.data.anatomy

import com.kargathra.fitness.data.model.MuscleGroup


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


    /** Maps the coarse MuscleGroup enum onto SVG data-group id(s). */
    fun svgGroupsFor(group: com.kargathra.fitness.data.model.MuscleGroup): Set<String> =
        when (group) {
            com.kargathra.fitness.data.model.MuscleGroup.CHEST      -> setOf("chest")
            com.kargathra.fitness.data.model.MuscleGroup.UPPER_BACK -> setOf("trap")
            com.kargathra.fitness.data.model.MuscleGroup.LATS       -> setOf("lats")
            com.kargathra.fitness.data.model.MuscleGroup.SHOULDERS  -> setOf("shoulder")
            com.kargathra.fitness.data.model.MuscleGroup.BICEPS     -> setOf("biceps")
            com.kargathra.fitness.data.model.MuscleGroup.TRICEPS    -> setOf("triceps")
            com.kargathra.fitness.data.model.MuscleGroup.QUADS      -> setOf("quads")
            com.kargathra.fitness.data.model.MuscleGroup.HAMSTRINGS -> setOf("hamstrings")
            com.kargathra.fitness.data.model.MuscleGroup.GLUTES     -> setOf("glutes")
            com.kargathra.fitness.data.model.MuscleGroup.CALVES     -> setOf("calves")
            com.kargathra.fitness.data.model.MuscleGroup.CORE       -> setOf("abs", "obliques")
            com.kargathra.fitness.data.model.MuscleGroup.CARDIO     -> emptySet()
        }

    /**
     * Convert a volume tally (SVG group -> volume) into graded engagement by
     * RELATIVE volume: the biggest movers are PRIMARY (gold), mid are SECONDARY,
     * the rest NONE. Threshold at 60% / 25% of the session's max group volume.
     */
    fun engagementByVolume(volumeByGroup: Map<String, Double>): Map<String, Engagement> {
        val max = volumeByGroup.values.maxOrNull() ?: return emptyMap()
        if (max <= 0.0) return emptyMap()
        return volumeByGroup.mapValues { (_, v) ->
            val frac = v / max
            when {
                frac >= 0.60 -> Engagement.PRIMARY
                frac >= 0.25 -> Engagement.SECONDARY
                else         -> Engagement.NONE
            }
        }.filterValues { it != Engagement.NONE }
    }


    /**
     * Convenience: fold a list of (MuscleGroup, volume) into a graded engagement
     * map keyed by SVG group. Used by both the weekly volume chart and the
     * session summary so the volume->figure logic lives in one place.
     */
    fun engagementFromGroupVolumes(volumes: List<Pair<com.kargathra.fitness.data.model.MuscleGroup, Double>>): Map<String, Engagement> {
        val bySvg = HashMap<String, Double>()
        volumes.forEach { (group, vol) ->
            svgGroupsFor(group).forEach { g -> bySvg[g] = (bySvg[g] ?: 0.0) + vol }
        }
        return engagementByVolume(bySvg)
    }


    /** SVG data-group id -> coarse MuscleGroup enum (single source of truth). */
    private fun svgGroupToEnum(g: String): MuscleGroup? = when (g) {
        "chest"              -> MuscleGroup.CHEST
        "trap", "upper_back" -> MuscleGroup.UPPER_BACK
        "lats"               -> MuscleGroup.LATS
        "shoulder"           -> MuscleGroup.SHOULDERS
        "biceps"             -> MuscleGroup.BICEPS
        "triceps"            -> MuscleGroup.TRICEPS
        "forearm"            -> MuscleGroup.BICEPS      // no forearm enum; fold to arms
        "abs", "obliques"    -> MuscleGroup.CORE
        "lowerback"          -> MuscleGroup.CORE
        "quads"              -> MuscleGroup.QUADS
        "hamstrings"         -> MuscleGroup.HAMSTRINGS
        "glutes"             -> MuscleGroup.GLUTES
        "calves"             -> MuscleGroup.CALVES
        else                 -> null
    }

    /**
     * THE canonical anatomical-name -> MuscleGroup mapping. Every consumer
     * (routine generator, exercise conversion, analytics) should use this so
     * classifications never drift apart. Routed through groupFor (the SVG map)
     * so the figure and the enums always agree. Neck folds to shoulders.
     */
    fun muscleGroupFor(anatomical: String): MuscleGroup? {
        val raw = anatomical.trim().lowercase()
        if (raw == "sternocleidomastoid" || raw == "neck extensors") return MuscleGroup.SHOULDERS
        return groupFor(raw)?.let { svgGroupToEnum(it) }
    }

}
