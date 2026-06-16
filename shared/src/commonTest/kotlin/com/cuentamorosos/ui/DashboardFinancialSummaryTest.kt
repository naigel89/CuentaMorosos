package com.cuentamorosos.ui

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for the financial summary data computed from DashboardState.
 *
 * The `toFinancialSummary()` extension extracts the fields needed by
 * FinancialSummaryRow and NetBalanceCard composables.
 */
class DashboardFinancialSummaryTest {

    private fun createState(
        totalOwedToYou: Double = 0.0,
        totalYouOwe: Double = 0.0,
        owedToYouBreakdown: List<DebtBreakdownItem> = emptyList(),
        youOweBreakdown: List<DebtBreakdownItem> = emptyList(),
    ) = DashboardState(
        totalOwedToYou = totalOwedToYou,
        totalYouOwe = totalYouOwe,
        owedToYouBreakdown = owedToYouBreakdown,
        youOweBreakdown = youOweBreakdown,
    )

    @Test
    fun `net balance is teDeben minus debes when both positive`() {
        val state = createState(totalOwedToYou = 150.0, totalYouOwe = 50.0)
        val summary = state.toFinancialSummary()
        assertEquals(150.0, summary.teDeben)
        assertEquals(50.0, summary.debes)
        assertEquals(100.0, summary.netBalance) // positive → you're owed
    }

    @Test
    fun `net balance is negative when you owe more than you are owed`() {
        val state = createState(totalOwedToYou = 30.0, totalYouOwe = 80.0)
        val summary = state.toFinancialSummary()
        assertEquals(-50.0, summary.netBalance)
    }

    @Test
    fun `net balance is zero when all squared`() {
        val state = createState(totalOwedToYou = 100.0, totalYouOwe = 100.0)
        val summary = state.toFinancialSummary()
        assertEquals(0.0, summary.netBalance)
    }

    @Test
    fun `counts match breakdown sizes`() {
        val owedBreakdown = listOf(
            DebtBreakdownItem("a", "Alice", 10.0),
            DebtBreakdownItem("b", "Bob", 20.0),
        )
        val oweBreakdown = listOf(
            DebtBreakdownItem("c", "Charlie", 15.0),
        )
        val state = createState(
            totalOwedToYou = 30.0,
            totalYouOwe = 15.0,
            owedToYouBreakdown = owedBreakdown,
            youOweBreakdown = oweBreakdown,
        )
        val summary = state.toFinancialSummary()
        assertEquals(2, summary.teDebenCount)
        assertEquals(1, summary.debesCount)
    }

    @Test
    fun `empty breakdowns produce zero counts and zero amounts`() {
        val state = DashboardState()
        val summary = state.toFinancialSummary()
        assertEquals(0.0, summary.teDeben)
        assertEquals(0.0, summary.debes)
        assertEquals(0.0, summary.netBalance)
        assertEquals(0, summary.teDebenCount)
        assertEquals(0, summary.debesCount)
    }

    @Test
    fun `handles state with only owed-to-you`() {
        val state = createState(
            totalOwedToYou = 200.0,
            owedToYouBreakdown = listOf(DebtBreakdownItem("a", "Alice", 200.0)),
        )
        val summary = state.toFinancialSummary()
        assertEquals(200.0, summary.teDeben)
        assertEquals(0.0, summary.debes)
        assertEquals(200.0, summary.netBalance)
        assertEquals(1, summary.teDebenCount)
        assertEquals(0, summary.debesCount)
    }

    @Test
    fun `handles state with only you-owe`() {
        val state = createState(
            totalYouOwe = 75.0,
            youOweBreakdown = listOf(DebtBreakdownItem("a", "Alice", 75.0)),
        )
        val summary = state.toFinancialSummary()
        assertEquals(0.0, summary.teDeben)
        assertEquals(75.0, summary.debes)
        assertEquals(-75.0, summary.netBalance)
        assertEquals(0, summary.teDebenCount)
        assertEquals(1, summary.debesCount)
    }
}
