package com.kargathra.fitness.data.anatomy

import com.kargathra.fitness.data.model.MuscleGroup
import kotlin.math.ceil

/**
 * One muscle group's share of a single session's training load.
 * [volumeKg] should already be engagement-weighted by the caller
 * (primary at 100%, secondary at 50% — same convention as the
 * weekly volume tally in WorkoutRepository).
 */
data class GroupLoad(
    val sessionId: Long,
    val group: MuscleGroup,
    val volumeKg: Double,
    val atMillis: Long
)

/** Recovery state for one muscle group. */
data class GroupRecovery(
    val group: MuscleGroup,
    /** 0f = fully fatigued, 1f = fully recovered. */
    val recovered: Float,
    /** Whole days until ~fully recovered (0 when already fresh). */
    val daysToFull: Int
)

/**
 * Deterministic per-muscle-group recovery model. Entirely on-device maths,
 * no network, no schema changes — derived purely from logged set history.
 *
 * Approach (Fitbod-style, self-calibrating):
 *  - A session's fatigue for a group is its volume relative to the user's own
 *    heaviest session for that group in the lookback window ("reference").
 *    Training at your usual max => ~100% fatigue; a light session => partial.
 *  - Fatigue decays linearly to zero over [RECOVERY_MS] (6 days).
 *  - Multiple recent sessions stack, capped at 100% fatigue.
 *
 * Because the reference is the user's own history, the model needs no
 * per-muscle constants and adapts as they get stronger.
 */
object RecoveryModel {

    /** Full recovery horizon: 6 days, matching common training practice. */
    const val RECOVERY_MS: Long = 6L * 24 * 60 * 60 * 1000

    /** Floor for the reference volume so tiny histories don't divide by ~0. */
    private const val MIN_REFERENCE_KG = 1.0

    /**
     * Compute recovery for every group present in [loads].
     * [loads] should span a longer window than [RECOVERY_MS] (e.g. 60 days)
     * so the reference volume is meaningful; only loads younger than
     * [RECOVERY_MS] contribute fatigue.
     */
    fun recovery(loads: List<GroupLoad>, nowMillis: Long): List<GroupRecovery> {
        if (loads.isEmpty()) return emptyList()

        // Session volume per (group, session) over the whole lookback.
        val sessionVol = HashMap<MuscleGroup, HashMap<Long, Double>>()
        val sessionAt  = HashMap<Long, Long>()
        for (l in loads) {
            if (l.volumeKg <= 0.0 || l.group == MuscleGroup.CARDIO) continue
            val byId = sessionVol.getOrPut(l.group) { HashMap() }
            byId[l.sessionId] = (byId[l.sessionId] ?: 0.0) + l.volumeKg
            // A session's time = earliest load seen for it.
            sessionAt[l.sessionId] = minOf(sessionAt[l.sessionId] ?: Long.MAX_VALUE, l.atMillis)
        }

        return sessionVol.map { (group, byId) ->
            val reference = maxOf(byId.values.max(), MIN_REFERENCE_KG)
            var fatigue = 0.0
            for ((sessionId, vol) in byId) {
                val age = nowMillis - (sessionAt[sessionId] ?: continue)
                if (age < 0 || age >= RECOVERY_MS) continue
                val initial = (vol / reference).coerceAtMost(1.0)
                fatigue += initial * (1.0 - age.toDouble() / RECOVERY_MS)
            }
            fatigue = fatigue.coerceIn(0.0, 1.0)
            val recovered = (1.0 - fatigue).toFloat()
            GroupRecovery(
                group = group,
                recovered = recovered,
                daysToFull = ceil(fatigue * RECOVERY_MS / (24.0 * 60 * 60 * 1000)).toInt()
            )
        }.sortedBy { it.recovered }
    }
}
