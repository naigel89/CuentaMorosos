package com.cuentamorosos.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IntegrityGuardTest {

    // ── canDeleteExpense ─────────────────────────────────────────────────────

    @Test
    fun `canDeleteExpense allows when OPEN`() {
        assertTrue(IntegrityGuard.canDeleteExpense(EventState.OPEN, "exp1", emptyList()))
    }

    @Test
    fun `canDeleteExpense blocks when CALCULATED`() {
        assertFalse(IntegrityGuard.canDeleteExpense(EventState.CALCULATED, "exp1", emptyList()))
    }

    @Test
    fun `canDeleteExpense blocks when CLOSED`() {
        assertFalse(IntegrityGuard.canDeleteExpense(EventState.CLOSED, "exp1", emptyList()))
    }

    // ── canDeleteEvent ───────────────────────────────────────────────────────

    @Test
    fun `canDeleteEvent always allows`() {
        val summary = CascadeSummary(eventId = "evt1", itemCount = 5, calculationCount = 2, profilePreservationNote = "test")
        assertTrue(IntegrityGuard.canDeleteEvent(summary))
    }

    // ── canModifyPaidDebt ────────────────────────────────────────────────────

    @Test
    fun `canModifyPaidDebt blocks when paid is true`() {
        val debt = EventDebtItem(id = "d1", eventId = "evt1", profileId = "p1", amountEuros = 50.0, paid = true)
        assertFalse(IntegrityGuard.canModifyPaidDebt(debt))
    }

    @Test
    fun `canModifyPaidDebt allows when paid is false`() {
        val debt = EventDebtItem(id = "d1", eventId = "evt1", profileId = "p1", amountEuros = 50.0, paid = false)
        assertTrue(IntegrityGuard.canModifyPaidDebt(debt))
    }

    // ── canCreateAdjustment ──────────────────────────────────────────────────

    @Test
    fun `canCreateAdjustment allows when paid is true`() {
        val debt = EventDebtItem(id = "d1", eventId = "evt1", profileId = "p1", amountEuros = 50.0, paid = true)
        assertTrue(IntegrityGuard.canCreateAdjustment(debt))
    }

    @Test
    fun `canCreateAdjustment blocks when paid is false`() {
        val debt = EventDebtItem(id = "d1", eventId = "evt1", profileId = "p1", amountEuros = 50.0, paid = false)
        assertFalse(IntegrityGuard.canCreateAdjustment(debt))
    }

    // ── buildCascadeSummary ──────────────────────────────────────────────────

    @Test
    fun `buildCascadeSummary returns correct counts`() {
        val expenses = listOf(
            EventExpenseItem(id = "e1", eventId = "evt1", name = "A", amountEuros = 10.0),
            EventExpenseItem(id = "e2", eventId = "evt1", name = "B", amountEuros = 20.0),
            EventExpenseItem(id = "e3", eventId = "evt1", name = "C", amountEuros = 30.0),
        )
        val calculations = listOf(
            CalculationSnapshot(emptyList(), 60.0, 1000L),
        )

        val summary = buildCascadeSummary("evt1", expenses, calculations)

        assertEquals("evt1", summary.eventId)
        assertEquals(3, summary.itemCount)
        assertEquals(1, summary.calculationCount)
        assertTrue(summary.profilePreservationNote.contains("preserved", ignoreCase = true))
    }

    @Test
    fun `buildCascadeSummary with empty lists`() {
        val summary = buildCascadeSummary("evt1", emptyList(), emptyList())

        assertEquals(0, summary.itemCount)
        assertEquals(0, summary.calculationCount)
    }
}
