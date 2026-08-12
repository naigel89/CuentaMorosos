package com.cuentamorosos.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettlementEngineRealConsumptionTest {

    private fun testEvent(
        name: String = "Test Event",
        ownerId: String = "owner1",
        memberIds: List<String> = listOf("owner1", "user2", "user3"),
        baseCurrency: String = "EUR",
    ) = EventItem(
        name = name,
        dateMillis = System.currentTimeMillis(),
        ownerId = ownerId,
        memberIds = memberIds,
        baseCurrency = baseCurrency,
    )

    private fun testExpense(
        id: String = "exp1",
        eventId: String = "evt1",
        name: String = "Test Expense",
        amountEuros: Double,
        payerContributions: Map<String, Double>,
        debtorIds: List<String>,
        splitMode: String = "SIMPLE_AVG",
        profileWeights: Map<String, Double> = emptyMap(),
    ) = EventExpenseItem(
        id = id,
        eventId = eventId,
        name = name,
        amountEuros = amountEuros,
        payerContributions = payerContributions,
        debtorIds = debtorIds,
        splitMode = splitMode,
        profileWeights = profileWeights,
    )

    // ── RC-01: REAL_CONSUMPTION with defined weights ─────────────────────────

    @Test
    fun `REAL_CONSUMPTION uses profileWeights as per-debtor amounts`() {
        // A pays 57.74€, REAL_CONSUMPTION with weights A:23.50, B:34.24
        // A: +57.74 - 23.50 = +34.24 (creditor)
        // B: -34.24 (debtor)
        // Transfer: B → A: 34.24
        val event = testEvent(memberIds = listOf("A", "B"))
        val expenses = listOf(
            testExpense(
                id = "rc1",
                eventId = "rc1",
                amountEuros = 57.74,
                payerContributions = mapOf("A" to 57.74),
                debtorIds = listOf("A", "B"),
                splitMode = "REAL_CONSUMPTION",
                profileWeights = mapOf("A" to 23.50, "B" to 34.24),
            )
        )

        val result = SettlementEngine.calculate(event, expenses)

        assertTrue(result.isSuccess, "Calculation should succeed")
        val transfers = result.snapshot!!.transfers
        assertEquals(1, transfers.size, "Should have exactly 1 transfer")
        assertEquals("B", transfers[0].fromProfileId)
        assertEquals("A", transfers[0].toProfileId)
        assertEquals(34.24, transfers[0].amount, 0.01)
    }

    // ── RC-02: REAL_CONSUMPTION with empty weights → equal split fallback ────

    @Test
    fun `REAL_CONSUMPTION empty weights falls back to equal split`() {
        // A pays 100€, REAL_CONSUMPTION with empty weights, 2 debtors (A, B)
        // Falls back to equal split: 50€ each
        // A: +100 - 50 = +50 (creditor)
        // B: -50 (debtor)
        // Transfer: B → A: 50
        val event = testEvent(memberIds = listOf("A", "B"))
        val expenses = listOf(
            testExpense(
                id = "rc2",
                eventId = "rc2",
                amountEuros = 100.0,
                payerContributions = mapOf("A" to 100.0),
                debtorIds = listOf("A", "B"),
                splitMode = "REAL_CONSUMPTION",
                profileWeights = emptyMap(),
            )
        )

        val result = SettlementEngine.calculate(event, expenses)

        assertTrue(result.isSuccess, "Calculation should succeed")
        val transfers = result.snapshot!!.transfers
        assertEquals(1, transfers.size, "Should have exactly 1 transfer")
        assertEquals("B", transfers[0].fromProfileId)
        assertEquals("A", transfers[0].toProfileId)
        assertEquals(50.0, transfers[0].amount, 0.01)
    }

    // ── RC-03: Weight sum mismatch > 0.02€ → weights used as-is ──────────────

    @Test
    fun `REAL_CONSUMPTION weight sum mismatch uses weights as-is`() {
        // Weights sum = 57.50, diff from total 57.74 = 0.24 > 0.02
        // computeDebtorAmounts MUST return declared weights as-is (with warning log)
        val expense = testExpense(
            id = "rc3",
            eventId = "rc3",
            amountEuros = 57.74,
            payerContributions = mapOf("A" to 57.74),
            debtorIds = listOf("A", "B"),
            splitMode = "REAL_CONSUMPTION",
            profileWeights = mapOf("A" to 23.50, "B" to 34.00),
        )

        val amounts = SettlementEngine.computeDebtorAmounts(expense)

        assertEquals(2, amounts.size, "Should have 2 debtors")
        assertEquals(23.50, amounts["A"] ?: 0.0, 0.001, "A should owe 23.50")
        assertEquals(34.00, amounts["B"] ?: 0.0, 0.001, "B should owe 34.00")
        // The sum does NOT match the total, but weights are returned as-is per spec
        val weightSum = amounts.values.sum()
        val diff = kotlin.math.abs(weightSum - expense.amountEuros)
        assertTrue(diff > 0.02, "Difference ${diff} should exceed 0.02€ tolerance")
    }

    // ── RC-04: REAL_CONSUMPTION does NOT use equal split when weights present

    @Test
    fun `REAL_CONSUMPTION with weights differs from equal split`() {
        // A pays 60€, 3 debtors A, B, C
        // REAL_CONSUMPTION weights: A:10, B:20, C:30
        // Equal split would be: 20, 20, 20
        // Verify it does NOT use equal split
        val event = testEvent(memberIds = listOf("A", "B", "C"))
        val expenses = listOf(
            testExpense(
                id = "rc4",
                eventId = "rc4",
                amountEuros = 60.0,
                payerContributions = mapOf("A" to 60.0),
                debtorIds = listOf("A", "B", "C"),
                splitMode = "REAL_CONSUMPTION",
                profileWeights = mapOf("A" to 10.0, "B" to 20.0, "C" to 30.0),
            )
        )

        val result = SettlementEngine.calculate(event, expenses)

        assertTrue(result.isSuccess, "Calculation should succeed")
        val balances = result.snapshot!!.participantBalances
        // A: +60 - 10 = +50
        // B: -20
        // C: -30
        // Transfers: B→A:20, C→A:30
        assertEquals(50.0, balances["A"] ?: 0.0, 0.01, "A balance should be +50 with REAL_CONSUMPTION, not +40 (equal split)")
        assertEquals(-20.0, balances["B"] ?: 0.0, 0.01, "B balance should be -20 with REAL_CONSUMPTION")
        assertEquals(-30.0, balances["C"] ?: 0.0, 0.01, "C balance should be -30 with REAL_CONSUMPTION")
    }
}
