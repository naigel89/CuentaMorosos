package com.cuentamorosos.ui

import com.cuentamorosos.model.EventDebtItem
import com.cuentamorosos.model.EventState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for Fix 4: Multi-Debt Participant Checkboxes.
 *
 * Verifies that the checkbox toggle logic correctly handles profiles
 * with multiple debts by toggling ALL debts atomically.
 *
 * Tests the pure functions extracted from SettlementPanel.kt:
 * - [computeMultiDebtToggleActions]
 * - [computeProfileCheckState]
 * - [computeShouldShowCheckbox]
 */
class SettlementPanelMultiDebtTest {

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun makeDebt(
        profileId: String = "p1",
        amountEuros: Double = 10.0,
        paid: Boolean = false,
        creditorId: String? = "cred1",
    ) = EventDebtItem(
        eventId = "ev1",
        profileId = profileId,
        amountEuros = amountEuros,
        paid = paid,
        creditorId = creditorId,
    )

    // ── computeMultiDebtToggleActions tests ──────────────────────────────────

    @Test
    fun `multi-debt toggle sets ALL debts to paid when any are unpaid`() {
        // Charlie owes Alice €33.33 AND Bob €6.67
        val debts = listOf(
            makeDebt(profileId = "charlie", amountEuros = 33.33, creditorId = "alice"),
            makeDebt(profileId = "charlie", amountEuros = 6.67, creditorId = "bob"),
        )

        val result = computeMultiDebtToggleActions(debts)

        assertEquals(2, result.size, "Both debts must be returned")
        assertTrue(result.all { it.paid }, "All debts must become paid")
        assertEquals("alice", result[0].creditorId)
        assertEquals("bob", result[1].creditorId)
        assertEquals(33.33, result[0].amountEuros, 0.001)
        assertEquals(6.67, result[1].amountEuros, 0.001)
    }

    @Test
    fun `single-debt toggle sets that one debt to paid`() {
        val debts = listOf(makeDebt(profileId = "alice", amountEuros = 50.0))
        val result = computeMultiDebtToggleActions(debts)
        assertEquals(1, result.size)
        assertTrue(result[0].paid, "Single debt should become paid")
    }

    @Test
    fun `single-debt toggle from paid to unpaid`() {
        val debts = listOf(makeDebt(profileId = "alice", amountEuros = 50.0, paid = true))
        val result = computeMultiDebtToggleActions(debts)
        assertEquals(1, result.size)
        assertFalse(result[0].paid, "Single paid debt should become unpaid")
    }

    @Test
    fun `uncheck all sets ALL debts to unpaid when all are currently paid`() {
        val debts = listOf(
            makeDebt(profileId = "bob", amountEuros = 20.0, paid = true),
            makeDebt(profileId = "bob", amountEuros = 15.0, paid = true),
        )
        val result = computeMultiDebtToggleActions(debts)
        assertEquals(2, result.size)
        assertTrue(result.none { it.paid }, "All debts must become unpaid")
    }

    @Test
    fun `zero debts returns empty list`() {
        val result = computeMultiDebtToggleActions(emptyList())
        assertTrue(result.isEmpty(), "No debts should produce empty result")
    }

    @Test
    fun `toggle uses immutable snapshot of debts at click time`() {
        val debts = mutableListOf(makeDebt(profileId = "p1", amountEuros = 10.0, paid = false))
        val snapshot = debts.toList() // Simulates snapshot at click time
        debts.add(makeDebt(profileId = "p1", amountEuros = 20.0, paid = false)) // Concurrent add

        val result = computeMultiDebtToggleActions(snapshot)

        assertEquals(1, result.size, "Only debts at click time should be toggled")
        assertTrue(result[0].paid, "Snapshot debt should be toggled")
    }

    @Test
    fun `mixed paid state sets ALL to paid`() {
        val debts = listOf(
            makeDebt(amountEuros = 10.0, paid = true),
            makeDebt(amountEuros = 5.0, paid = false),
        )
        val result = computeMultiDebtToggleActions(debts)
        assertTrue(result.all { it.paid }, "All debts should become paid when mixed")
    }

    // ── computeProfileCheckState tests ───────────────────────────────────────

    @Test
    fun `isPaid is true only when ALL profile debts are paid`() {
        val allUnpaid = listOf(makeDebt(paid = false), makeDebt(paid = false))
        assertFalse(computeProfileCheckState(allUnpaid).isPaid)

        val allPaid = listOf(makeDebt(paid = true), makeDebt(paid = true))
        assertTrue(computeProfileCheckState(allPaid).isPaid)

        val mixed = listOf(makeDebt(paid = true), makeDebt(paid = false))
        assertFalse(computeProfileCheckState(mixed).isPaid)
    }

    @Test
    fun `isPaid is false when profile has no debts`() {
        assertFalse(computeProfileCheckState(emptyList()).isPaid)
    }

    @Test
    fun `hasDebt reflects non-empty debt list`() {
        assertTrue(computeProfileCheckState(listOf(makeDebt())).hasDebt)
        assertFalse(computeProfileCheckState(emptyList()).hasDebt)
    }

    // ── computeShouldShowCheckbox tests ──────────────────────────────────────

    @Test
    fun `checkbox visible when profile has debts and event is not OPEN`() {
        assertTrue(computeShouldShowCheckbox(listOf(makeDebt()), EventState.CALCULATED))
        assertTrue(computeShouldShowCheckbox(listOf(makeDebt()), EventState.CLOSED))
    }

    @Test
    fun `checkbox hidden when profile has debts but event is OPEN`() {
        assertFalse(computeShouldShowCheckbox(listOf(makeDebt()), EventState.OPEN))
    }

    @Test
    fun `checkbox hidden when profile has no debts regardless of state`() {
        assertFalse(computeShouldShowCheckbox(emptyList(), EventState.CALCULATED))
        assertFalse(computeShouldShowCheckbox(emptyList(), EventState.OPEN))
    }
}
