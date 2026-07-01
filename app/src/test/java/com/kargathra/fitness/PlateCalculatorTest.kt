package com.kargathra.fitness

import com.kargathra.fitness.ui.components.computePlates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the pure plate-loading logic in
 * [com.kargathra.fitness.ui.components.computePlates].
 *
 * The component's own PLATE_SIZES list is private, so these tests pass an
 * equivalent inventory explicitly (which is exactly what the public signature
 * `computePlates(target, bar, plates)` is designed for).
 */
class PlateCalculatorTest {

    // Mirror of the app's default plate inventory (kg, per plate).
    private val plates = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)

    @Test
    fun standard_100kg_on_20kg_bar() {
        // per side = (100 - 20) / 2 = 40 → greedy: 25 + 15
        val r = computePlates(100.0, 20.0, plates)
        assertEquals(listOf(25.0 to 1, 15.0 to 1), r.perSide)
        assertEquals(0.0, r.leftover, 1e-9)
    }

    @Test
    fun target_equals_bar_gives_no_plates() {
        val r = computePlates(20.0, 20.0, plates)
        assertTrue("expected no plates when target == bar", r.perSide.isEmpty())
        assertEquals(0.0, r.leftover, 1e-9)
    }

    @Test
    fun target_below_bar_is_impossible() {
        // per-side target is negative, so computePlates returns an empty result.
        // Note: the function collapses "below bar" and "equals bar" into the same
        // empty PlateResult (leftover 0.0); the UI distinguishes the two via a
        // separate `target < bar` check before rendering.
        val r = computePlates(15.0, 20.0, plates)
        assertTrue("expected no plates when target < bar", r.perSide.isEmpty())
        assertEquals(0.0, r.leftover, 1e-9)
    }

    @Test
    fun odd_remainder_reports_leftover() {
        // per side = (63 - 20) / 2 = 21.5 → 20 + 1.25, leaving 0.25 kg/side unmatched
        val r = computePlates(63.0, 20.0, plates)
        assertEquals(listOf(20.0 to 1, 1.25 to 1), r.perSide)
        assertEquals(0.25, r.leftover, 1e-9)
    }

    @Test
    fun large_target_breaks_down_correctly() {
        // per side = (220 - 20) / 2 = 100 → 4 × 25
        val r = computePlates(220.0, 20.0, plates)
        assertEquals(listOf(25.0 to 4), r.perSide)
        assertEquals(0.0, r.leftover, 1e-9)

        // sanity: the plates actually reconstruct the requested total
        val loaded = 20.0 + r.perSide.sumOf { it.first * it.second } * 2
        assertEquals(220.0, loaded, 1e-9)
    }
}
