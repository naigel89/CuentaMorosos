package com.cuentamorosos.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NeoFintechAnimationsTest {

    // ── AnimatedCounter: formatAmount (pure function) ──────────────────

    @Test
    fun `formatAmount with default params formats euros with 2 decimals`() {
        val result = formatAmount(42.5)
        assertEquals("42,50€", result)
    }

    @Test
    fun `formatAmount with custom prefix and suffix`() {
        val result = formatAmount(99.99, prefix = "$", suffix = "", decimals = 2)
        assertEquals("$99,99", result)
    }

    @Test
    fun `formatAmount with 0 decimals`() {
        val result = formatAmount(100.7, decimals = 0)
        assertEquals("101€", result)
    }

    @Test
    fun `formatAmount handles zero`() {
        val result = formatAmount(0.0)
        assertEquals("0,00€", result)
    }

    @Test
    fun `formatAmount handles negative value`() {
        val result = formatAmount(-15.5)
        assertEquals("-15,50€", result)
    }

    @Test
    fun `formatAmount handles large value with thousand separators`() {
        val result = formatAmount(1234567.89)
        // Spanish locale: 1.234.567,89€ with thousands separator
        assertEquals("1.234.567,89€", result)
    }

    @Test
    fun `formatAmount handles value with many decimals rounding to 2`() {
        val result = formatAmount(3.14159)
        assertEquals("3,14€", result)
    }

    @Test
    fun `formatAmount handles empty prefix and suffix`() {
        val result = formatAmount(10.0, prefix = "", suffix = "")
        assertEquals("10,00", result)
    }

    // ── AnimationCoordinator ──────────────────────────────────────────

    @Test
    fun `AnimationCoordinator allocates sequential slots up to max`() {
        val coordinator = AnimationCoordinator(maxSimultaneous = 4)
        assertEquals(0, coordinator.requestSlot())
        assertEquals(1, coordinator.requestSlot())
        assertEquals(2, coordinator.requestSlot())
        assertEquals(3, coordinator.requestSlot())
    }

    @Test
    fun `AnimationCoordinator returns -1 when all slots full`() {
        val coordinator = AnimationCoordinator(maxSimultaneous = 4)
        for (i in 0 until 4) {
            coordinator.requestSlot()
        }
        assertEquals(-1, coordinator.requestSlot())
    }

    @Test
    fun `AnimationCoordinator reuse released slots`() {
        val coordinator = AnimationCoordinator(maxSimultaneous = 3)
        coordinator.requestSlot() // 0
        coordinator.requestSlot() // 1
        coordinator.releaseSlot(0)
        assertEquals(0, coordinator.requestSlot())
    }

    @Test
    fun `AnimationCoordinator hasAvailableSlot reports correctly`() {
        val coordinator = AnimationCoordinator(maxSimultaneous = 2)
        assertTrue(coordinator.hasAvailableSlot())
        coordinator.requestSlot() // 0
        assertTrue(coordinator.hasAvailableSlot())
        coordinator.requestSlot() // 1
        assertFalse(coordinator.hasAvailableSlot())
    }

    @Test
    fun `AnimationCoordinator release out of bounds is no-op`() {
        val coordinator = AnimationCoordinator(maxSimultaneous = 2)
        coordinator.releaseSlot(-1) // should not crash
        coordinator.releaseSlot(5)  // should not crash
        // slots should still be available
        assertEquals(0, coordinator.requestSlot())
    }

    @Test
    fun `AnimationCoordinator with custom max capacity`() {
        val coordinator = AnimationCoordinator(maxSimultaneous = 1)
        assertEquals(0, coordinator.requestSlot())
        assertEquals(-1, coordinator.requestSlot())
    }

    @Test
    fun `AnimationCoordinator releasing already released slot is no-op`() {
        val coordinator = AnimationCoordinator(maxSimultaneous = 2)
        coordinator.requestSlot() // 0
        coordinator.releaseSlot(0)
        coordinator.releaseSlot(0) // double release — should be no-op
        assertEquals(0, coordinator.requestSlot()) // should still get slot 0
    }

    // ── shouldAnimate (pure function) ─────────────────────────────────

    @Test
    fun `shouldAnimate returns true when all flags enabled`() {
        assertTrue(shouldAnimate(systemAnimationsEnabled = true, appAnimationsEnabled = true))
    }

    @Test
    fun `shouldAnimate returns false when system animations disabled`() {
        assertFalse(shouldAnimate(systemAnimationsEnabled = false, appAnimationsEnabled = true))
    }

    @Test
    fun `shouldAnimate returns false when app animations disabled`() {
        assertFalse(shouldAnimate(systemAnimationsEnabled = true, appAnimationsEnabled = false))
    }

    @Test
    fun `shouldAnimate returns false when both disabled`() {
        assertFalse(shouldAnimate(systemAnimationsEnabled = false, appAnimationsEnabled = false))
    }
}
