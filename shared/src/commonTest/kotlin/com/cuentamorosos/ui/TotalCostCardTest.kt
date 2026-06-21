package com.cuentamorosos.ui

import com.cuentamorosos.model.EventState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for Fix 5: Correct Total Display in TotalCostCard.
 *
 * After settlement, displayed totals must reflect two separate concepts:
 * total expense (immutable event cost) vs. net transfers (settlement amounts).
 *
 * Tests the pure functions extracted from TotalCostCard.kt:
 * - [computeTotalDisplayLabel] — returns the correct label for the pending row
 * - [computeTotalAmountForDisplay] — returns the correct amount to show as total
 */
class TotalCostCardTest {

    // ── computeTotalDisplayLabel tests ───────────────────────────────────────

    @Test
    fun `OPEN event shows Pendiente label`() {
        val label = computeTotalDisplayLabel(EventState.OPEN)
        assertEquals("Pendiente", label)
    }

    @Test
    fun `CALCULATED event shows Transferencias netas label`() {
        val label = computeTotalDisplayLabel(EventState.CALCULATED)
        assertEquals("Transferencias netas", label)
    }

    @Test
    fun `CLOSED event shows Transferencias netas label`() {
        val label = computeTotalDisplayLabel(EventState.CLOSED)
        assertEquals("Transferencias netas", label)
    }

    // ── computeTotalAmountForDisplay tests ───────────────────────────────────

    @Test
    fun `CALCULATED event shows calculationTotal as total`() {
        val amount = computeTotalAmountForDisplay(
            eventState = EventState.CALCULATED,
            totalExpenses = 100.0,
            calculationTotal = 66.67,
        )
        assertEquals(66.67, amount, 0.001,
            "CALCULATED should use calculationTotal, not totalExpenses")
    }

    @Test
    fun `OPEN event shows totalExpenses as total`() {
        val amount = computeTotalAmountForDisplay(
            eventState = EventState.OPEN,
            totalExpenses = 100.0,
            calculationTotal = 66.67,
        )
        assertEquals(100.0, amount, 0.001,
            "OPEN should use totalExpenses, not calculationTotal")
    }

    @Test
    fun `CALCULATED without calculationTotal falls back to totalExpenses`() {
        val amount = computeTotalAmountForDisplay(
            eventState = EventState.CALCULATED,
            totalExpenses = 100.0,
            calculationTotal = null,
        )
        assertEquals(100.0, amount, 0.001,
            "CALCULATED without snapshot total should fall back to totalExpenses")
    }

    @Test
    fun `CLOSED event shows totalExpenses as total`() {
        val amount = computeTotalAmountForDisplay(
            eventState = EventState.CLOSED,
            totalExpenses = 100.0,
            calculationTotal = 66.67,
        )
        assertEquals(100.0, amount, 0.001,
            "CLOSED should use totalExpenses")
    }
}
