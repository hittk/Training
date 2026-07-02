package com.kargathra.fitness

import com.kargathra.fitness.data.anatomy.GroupLoad
import com.kargathra.fitness.data.anatomy.RecoveryModel
import com.kargathra.fitness.data.model.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryModelTest {

    private val now = 1_700_000_000_000L
    private val day = 24L * 60 * 60 * 1000

    private fun load(session: Long, group: MuscleGroup, vol: Double, agoMs: Long) =
        GroupLoad(sessionId = session, group = group, volumeKg = vol, atMillis = now - agoMs)

    @Test
    fun `max session just trained is fully fatigued`() {
        val r = RecoveryModel.recovery(
            listOf(load(1, MuscleGroup.CHEST, 1000.0, 0)), now
        ).single()
        assertEquals(MuscleGroup.CHEST, r.group)
        assertEquals(0f, r.recovered, 0.01f)
        assertEquals(6, r.daysToFull)
    }

    @Test
    fun `fatigue decays linearly over six days`() {
        val r = RecoveryModel.recovery(
            listOf(load(1, MuscleGroup.QUADS, 1000.0, 3 * day)), now
        ).single()
        assertEquals(0.5f, r.recovered, 0.01f)
        assertEquals(3, r.daysToFull)
    }

    @Test
    fun `older than six days is fully recovered`() {
        val r = RecoveryModel.recovery(
            listOf(load(1, MuscleGroup.LATS, 1000.0, 7 * day)), now
        ).single()
        assertEquals(1f, r.recovered, 0.001f)
        assertEquals(0, r.daysToFull)
    }

    @Test
    fun `light session relative to reference fatigues proportionally`() {
        // Reference set by an old heavy session (1000kg, outside fatigue window);
        // a fresh 250kg session should cost ~25% fatigue.
        val r = RecoveryModel.recovery(
            listOf(
                load(1, MuscleGroup.BICEPS, 1000.0, 30 * day),
                load(2, MuscleGroup.BICEPS, 250.0, 0)
            ), now
        ).single()
        assertEquals(0.75f, r.recovered, 0.01f)
    }

    @Test
    fun `stacked recent sessions cap at full fatigue`() {
        val r = RecoveryModel.recovery(
            listOf(
                load(1, MuscleGroup.CHEST, 1000.0, 1 * day),
                load(2, MuscleGroup.CHEST, 1000.0, 0)
            ), now
        ).single()
        assertEquals(0f, r.recovered, 0.001f)
    }

    @Test
    fun `groups are independent and sorted most fatigued first`() {
        val out = RecoveryModel.recovery(
            listOf(
                load(1, MuscleGroup.CHEST, 1000.0, 0),
                load(1, MuscleGroup.QUADS, 1000.0, 5 * day)
            ), now
        )
        assertEquals(2, out.size)
        assertEquals(MuscleGroup.CHEST, out[0].group)
        assertTrue(out[0].recovered < out[1].recovered)
    }

    @Test
    fun `cardio and zero volume are ignored`() {
        val out = RecoveryModel.recovery(
            listOf(
                load(1, MuscleGroup.CARDIO, 1000.0, 0),
                load(1, MuscleGroup.CHEST, 0.0, 0)
            ), now
        )
        assertTrue(out.isEmpty())
    }
}
