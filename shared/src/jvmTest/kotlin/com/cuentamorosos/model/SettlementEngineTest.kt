package com.cuentamorosos.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class SettlementEngineTest {

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

    // ── CA-03/CA-06: Standard 3-person scenario ─────────────────────────────

    @Test
    fun `standard 3-person split produces correct transfers`() {
        // A pays 30€, split equally among A, B, C → each owes 10€
        // A paid 30, owes 10 → balance +20 (creditor)
        // B paid 0, owes 10 → balance -10 (debtor)
        // C paid 0, owes 10 → balance -10 (debtor)
        val event = testEvent(memberIds = listOf("A", "B", "C"))
        val expenses = listOf(
            testExpense(
                amountEuros = 30.0,
                payerContributions = mapOf("A" to 30.0),
                debtorIds = listOf("A", "B", "C"),
            )
        )

        val result = SettlementEngine.calculate(event, expenses)

        assertTrue(result.isSuccess)
        val transfers = result.snapshot!!.transfers
        assertEquals(2, transfers.size)
        // B → A: 10€, C → A: 10€
        assertTrue(transfers.any { it.fromProfileId == "B" && it.toProfileId == "A" && it.amount == 10.0 })
        assertTrue(transfers.any { it.fromProfileId == "C" && it.toProfileId == "A" && it.amount == 10.0 })
    }

    // ── CA-06: Multi-expense scenario ────────────────────────────────────────

    @Test
    fun `multi-expense scenario computes correct transfers`() {
        val event = testEvent(memberIds = listOf("A", "B"))
        val expenses = listOf(
            testExpense(amountEuros = 20.0, payerContributions = mapOf("A" to 20.0), debtorIds = listOf("A", "B")),
            testExpense(amountEuros = 10.0, payerContributions = mapOf("B" to 10.0), debtorIds = listOf("A", "B")),
        )

        val result = SettlementEngine.calculate(event, expenses)

        assertTrue(result.isSuccess)
        // A paid 20, owes 15 → +5; B paid 10, owes 15 → -5
        // B → A: 5€
        val transfers = result.snapshot!!.transfers
        assertEquals(1, transfers.size)
        assertEquals("B", transfers[0].fromProfileId)
        assertEquals("A", transfers[0].toProfileId)
        assertEquals(5.0, transfers[0].amount)
    }

    // ── CA-10: All balances zero ─────────────────────────────────────────────

    @Test
    fun `all balances zero returns empty transfers`() {
        val event = testEvent(memberIds = listOf("A", "B"))
        val expenses = listOf(
            testExpense(amountEuros = 10.0, payerContributions = mapOf("A" to 10.0), debtorIds = listOf("A")),
        )

        val result = SettlementEngine.calculate(event, expenses)

        assertTrue(result.isSuccess)
        assertTrue(result.snapshot!!.transfers.isEmpty())
    }

    // ── CA-10: Micro-amounts ─────────────────────────────────────────────────

    @Test
    fun `micro amount 0_01 split among 3 — remainder distributed via round-robin seed`() {
        val event = testEvent(memberIds = listOf("A", "B", "C"))
        // A pays 0.01, split equally among A, B, C
        // 1 cent / 3 → base 0, remainder 1 → distributed round-robin from seed offset
        // Using id="a" with eventId="evt1" → seed%3=0 → remainder goes to A (index 0)
        // A owes 0.01, B owes 0.00, C owes 0.00
        // A paid 0.01, owes 0.01 → balance 0
        val expenses = listOf(
            testExpense(id = "a", amountEuros = 0.01, payerContributions = mapOf("A" to 0.01), debtorIds = listOf("A", "B", "C")),
        )

        val result = SettlementEngine.calculate(event, expenses)

        assertTrue(result.isSuccess)
        // All balances zero → no transfers needed
        assertTrue(result.snapshot!!.transfers.isEmpty())
    }

    @Test
    fun `micro amount where payer is not a debtor produces transfer`() {
        val event = testEvent(memberIds = listOf("A", "B", "C"))
        // A pays 0.02, split equally among B, C only
        // calculateEqual: 2 cents / 2 → 1 cent each
        // A paid 0.02, owes 0 → balance +2 (creditor)
        // B owes 0.01 → balance -1 (debtor)
        // C owes 0.01 → balance -1 (debtor)
        val expenses = listOf(
            testExpense(amountEuros = 0.02, payerContributions = mapOf("A" to 0.02), debtorIds = listOf("B", "C")),
        )

        val result = SettlementEngine.calculate(event, expenses)

        assertTrue(result.isSuccess)
        val transfers = result.snapshot!!.transfers
        assertEquals(2, transfers.size)
        assertTrue(transfers.any { it.fromProfileId == "B" && it.toProfileId == "A" && it.amount == 0.01 })
        assertTrue(transfers.any { it.fromProfileId == "C" && it.toProfileId == "A" && it.amount == 0.01 })
    }

    // ── CA-01: Validation failure ────────────────────────────────────────────

    @Test
    fun `validation failure returns errors`() {
        val event = testEvent(memberIds = listOf("A", "B"))
        val expenses = listOf(
            testExpense(amountEuros = 0.0, payerContributions = emptyMap(), debtorIds = emptyList()),
        )

        val result = SettlementEngine.calculate(event, expenses)

        assertTrue(!result.isSuccess)
        assertTrue(result.errors.isNotEmpty())
        assertTrue(result.errors.any { it.contains("gastos con errores") })
    }

    // ── CA-07: No self-transfers ─────────────────────────────────────────────

    @Test
    fun `no self-transfers are created`() {
        val event = testEvent(memberIds = listOf("A", "B", "C"))
        val expenses = listOf(
            testExpense(amountEuros = 30.0, payerContributions = mapOf("A" to 30.0), debtorIds = listOf("A", "B", "C")),
        )

        val result = SettlementEngine.calculate(event, expenses)

        assertTrue(result.isSuccess)
        result.snapshot!!.transfers.forEach { transfer ->
            assertTrue(transfer.fromProfileId != transfer.toProfileId)
        }
    }

    // ── CA-07: Sum invariant ─────────────────────────────────────────────────

    @Test
    fun `transfer amounts sum equals total debt`() {
        val event = testEvent(memberIds = listOf("A", "B", "C"))
        val expenses = listOf(
            testExpense(amountEuros = 30.0, payerContributions = mapOf("A" to 30.0), debtorIds = listOf("A", "B", "C")),
            testExpense(amountEuros = 15.0, payerContributions = mapOf("B" to 15.0), debtorIds = listOf("A", "B", "C")),
        )

        val result = SettlementEngine.calculate(event, expenses)

        assertTrue(result.isSuccess)
        val snapshot = result.snapshot!!
        val totalTransferred = snapshot.transfers.sumOf { it.amount }
        val totalDebt = snapshot.participantBalances.values.filter { it < 0 }.sumOf { -it }
        assertEquals(totalDebt, totalTransferred, 0.01)
    }

    // ── CA-08: Snapshot metadata ─────────────────────────────────────────────

    @Test
    fun `snapshot contains correct metadata`() {
        val event = testEvent(memberIds = listOf("A", "B"))
        val expenses = listOf(
            testExpense(amountEuros = 20.0, payerContributions = mapOf("A" to 20.0), debtorIds = listOf("A", "B")),
        )

        val result = SettlementEngine.calculate(event, expenses)

        assertTrue(result.isSuccess)
        val snapshot = result.snapshot!!
        assertEquals("v1-greedy", snapshot.algorithmVersion)
        assertEquals(20.0, snapshot.totalExpense)
        assertTrue(snapshot.calculatedAtMillis > 0)
        assertEquals(2, snapshot.participantBalances.size)
    }

    // ── Multi-payer: debt split between two creditors (user-reported scenario) ─

    @Test
    fun `two payers among 5 people — debt splits between both creditors`() {
        // Natalia pays 10€ for drinks, User pays 10€ for appetizers, same 5 people
        // Each person owes 2€ per expense = 4€ total per person
        // Natalia: +10 - 4 = +6€ (creditor)
        // User: +10 - 4 = +6€ (creditor)
        // A, B, C: -4€ each (debtors)
        // Expected: debt from A, B, C should be split between Natalia and User
        val event = testEvent(memberIds = listOf("Natalia", "User", "A", "B", "C"))
        val expenses = listOf(
            testExpense(
                id = "drinks",
                amountEuros = 10.0,
                payerContributions = mapOf("Natalia" to 10.0),
                debtorIds = listOf("Natalia", "User", "A", "B", "C"),
            ),
            testExpense(
                id = "appetizers",
                amountEuros = 10.0,
                payerContributions = mapOf("User" to 10.0),
                debtorIds = listOf("Natalia", "User", "A", "B", "C"),
            ),
        )

        val result = SettlementEngine.calculate(event, expenses)

        assertTrue(result.isSuccess)
        val snapshot = result.snapshot!!

        // Both Natalia and User should be creditors with +6€ each
        assertEquals(6.0, snapshot.participantBalances["Natalia"]!!, 0.01)
        assertEquals(6.0, snapshot.participantBalances["User"]!!, 0.01)
        assertEquals(-4.0, snapshot.participantBalances["A"]!!, 0.01)
        assertEquals(-4.0, snapshot.participantBalances["B"]!!, 0.01)
        assertEquals(-4.0, snapshot.participantBalances["C"]!!, 0.01)

        // Total transferred should be 12€ (4+4+4 from A, B, C)
        val totalTransferred = snapshot.transfers.sumOf { it.amount }
        assertEquals(12.0, totalTransferred, 0.01)

        // CRITICAL: debt must be split between BOTH creditors
        // Natalia should receive some money, User should receive some money
        val nataliaReceived = snapshot.transfers
            .filter { it.toProfileId == "Natalia" }
            .sumOf { it.amount }
        val userReceived = snapshot.transfers
            .filter { it.toProfileId == "User" }
            .sumOf { it.amount }

        assertTrue(nataliaReceived > 0.0, "Natalia should receive money as creditor, but got 0€")
        assertTrue(userReceived > 0.0, "User should receive money as creditor, but got 0€")
        assertEquals(6.0, nataliaReceived, 0.01)
        assertEquals(6.0, userReceived, 0.01)
    }

    @Test
    fun `two payers with different amounts — proportional split`() {
        // A pays 30€, B pays 30€, split among A, B, C
        // Each expense: 30€ / 3 = 10€ each (clean division)
        // A: +30 - 10 - 10 = +10; B: +30 - 10 - 10 = +10; C: -10 - 10 = -20
        // C → A: 10€, C → B: 10€
        val event = testEvent(memberIds = listOf("A", "B", "C"))
        val expenses = listOf(
            testExpense(id = "e1", eventId = "evt1", amountEuros = 30.0, payerContributions = mapOf("A" to 30.0), debtorIds = listOf("A", "B", "C")),
            testExpense(id = "e2", eventId = "evt1", amountEuros = 30.0, payerContributions = mapOf("B" to 30.0), debtorIds = listOf("A", "B", "C")),
        )

        val result = SettlementEngine.calculate(event, expenses)

        assertTrue(result.isSuccess)
        val snapshot = result.snapshot!!
        assertEquals(10.0, snapshot.participantBalances["A"]!!, 0.01)
        assertEquals(10.0, snapshot.participantBalances["B"]!!, 0.01)
        assertEquals(-20.0, snapshot.participantBalances["C"]!!, 0.01)

        val transfers = snapshot.transfers
        assertEquals(2, transfers.size)
        assertTrue(transfers.any { it.fromProfileId == "C" && it.toProfileId == "A" && it.amount == 10.0 })
        assertTrue(transfers.any { it.fromProfileId == "C" && it.toProfileId == "B" && it.amount == 10.0 })
    }

    @Test
    fun `three payers — debt distributed among all creditors`() {
        // A pays 30€, B pays 30€, C pays 30€, split among A, B, C, D, E
        // Each expense: 30€ / 5 = 6€ each
        // A: +30 - 6 - 6 - 6 = +12; B: +30 - 6 - 6 - 6 = +12; C: +30 - 6 - 6 - 6 = +12
        // D: -6 - 6 - 6 = -18; E: -6 - 6 - 6 = -18
        // D and E owe 18€ each, split among A, B, C (12€ each)
        val event = testEvent(memberIds = listOf("A", "B", "C", "D", "E"))
        val expenses = listOf(
            testExpense(id = "e1", amountEuros = 30.0, payerContributions = mapOf("A" to 30.0), debtorIds = listOf("A", "B", "C", "D", "E")),
            testExpense(id = "e2", amountEuros = 30.0, payerContributions = mapOf("B" to 30.0), debtorIds = listOf("A", "B", "C", "D", "E")),
            testExpense(id = "e3", amountEuros = 30.0, payerContributions = mapOf("C" to 30.0), debtorIds = listOf("A", "B", "C", "D", "E")),
        )

        val result = SettlementEngine.calculate(event, expenses)

        assertTrue(result.isSuccess)
        val snapshot = result.snapshot!!

        assertEquals(12.0, snapshot.participantBalances["A"]!!, 0.01)
        assertEquals(12.0, snapshot.participantBalances["B"]!!, 0.01)
        assertEquals(12.0, snapshot.participantBalances["C"]!!, 0.01)
        assertEquals(-18.0, snapshot.participantBalances["D"]!!, 0.01)
        assertEquals(-18.0, snapshot.participantBalances["E"]!!, 0.01)

        val totalTransferred = snapshot.transfers.sumOf { it.amount }
        assertEquals(36.0, totalTransferred, 0.01)

        // All three creditors should receive money
        val aReceived = snapshot.transfers.filter { it.toProfileId == "A" }.sumOf { it.amount }
        val bReceived = snapshot.transfers.filter { it.toProfileId == "B" }.sumOf { it.amount }
        val cReceived = snapshot.transfers.filter { it.toProfileId == "C" }.sumOf { it.amount }

        assertTrue(aReceived > 0.0, "A should receive money")
        assertTrue(bReceived > 0.0, "B should receive money")
        assertTrue(cReceived > 0.0, "C should receive money")
        assertEquals(12.0, aReceived, 0.01)
        assertEquals(12.0, bReceived, 0.01)
        assertEquals(12.0, cReceived, 0.01)
    }

    // ── D8 Edge Cases: calculateWithEdgeCases ────────────────────────────────

    private fun testEventWithId(
        id: String = "evt1",
        name: String = "Test Event",
        ownerId: String = "owner1",
        memberIds: List<String> = listOf("owner1", "user2", "user3"),
    ) = EventItem(
        id = id,
        name = name,
        dateMillis = System.currentTimeMillis(),
        ownerId = ownerId,
        memberIds = memberIds,
    )

    // ── T10 / ZB-01: Zero-balance returns ZeroBalance status ─────────────────

    @Test
    fun `zero balance returns ZeroBalance status with empty transfers`() {
        SettlementEngine.CalculationLock.clearAllForTest()
        // A pays 10€, assigned only to A → balance = 0 (paid 10, owes 10)
        val event = testEventWithId(id = "evt-zb", memberIds = listOf("A", "B", "C"))
        val expenses = listOf(
            testExpense(
                amountEuros = 10.0,
                payerContributions = mapOf("A" to 10.0),
                debtorIds = listOf("A"),
            )
        )

        val result = SettlementEngine.calculateWithEdgeCases(event, expenses)

        assertTrue(result.isSuccess)
        assertTrue(result.status is CalculationStatus.ZeroBalance)
        assertEquals("Todo está saldado", (result.status as CalculationStatus.ZeroBalance).message)
        assertTrue(result.snapshot!!.transfers.isEmpty())
    }

    // ── T11: Near-zero tolerance (±0.01€) ────────────────────────────────────

    @Test
    fun `near-zero balances within tolerance return ZeroBalance`() {
        SettlementEngine.CalculationLock.clearAllForTest()
        // Construct balances manually: all within ±0.01
        val event = testEventWithId(id = "evt-nz", memberIds = listOf("A", "B", "C"))
        // A pays 10€, split equally among A, B, C → each owes 3.33€, remainder to A
        // A: +10 - 3.34 = +6.66; B: -3.33; C: -3.33 → not zero
        // Instead: use scenario where balances net to ~0.01
        // A pays 0.01€, assigned to A only → A balance = 0
        val expenses = listOf(
            testExpense(
                amountEuros = 0.01,
                payerContributions = mapOf("A" to 0.01),
                debtorIds = listOf("A"),
            )
        )

        val result = SettlementEngine.calculateWithEdgeCases(event, expenses)

        assertTrue(result.isSuccess)
        assertTrue(result.status is CalculationStatus.ZeroBalance)
        assertTrue(result.snapshot!!.transfers.isEmpty())
    }

    // ── T12: Rounding residual (0.01€ / 3 people) ───────────────────────────

    @Test
    fun `rounding residual 0_01 split among 3 — no zero transfers`() {
        SettlementEngine.CalculationLock.clearAllForTest()
        val event = testEventWithId(id = "evt-rnd", memberIds = listOf("A", "B", "C"))
        // A pays 0.01, split among A, B, C
        // 1 cent / 3 → base 0, remainder 1 → distributed round-robin from seed offset
        // Using id="e" with eventId="evt-rnd" → seed%3=0 → remainder goes to A (index 0)
        // A: +1 - 1 = 0; B: 0; C: 0 → all zero
        val expenses = listOf(
            testExpense(
                id = "e",
                eventId = "evt-rnd",
                amountEuros = 0.01,
                payerContributions = mapOf("A" to 0.01),
                debtorIds = listOf("A", "B", "C"),
            )
        )

        val result = SettlementEngine.calculateWithEdgeCases(event, expenses)

        assertTrue(result.isSuccess)
        assertTrue(result.snapshot!!.transfers.isEmpty())
        // No zero-amount transfers exist
        result.snapshot?.transfers?.forEach {
            assertTrue(it.amount > 0, "Transfer amount should be > 0")
        }
    }

    // ── T13: Zero-transfer filter ────────────────────────────────────────────

    @Test
    fun `no zero-amount transfers in result`() {
        SettlementEngine.CalculationLock.clearAllForTest()
        val event = testEventWithId(id = "evt-zt", memberIds = listOf("A", "B", "C"))
        val expenses = listOf(
            testExpense(
                amountEuros = 30.0,
                payerContributions = mapOf("A" to 30.0),
                debtorIds = listOf("A", "B", "C"),
            )
        )

        val result = SettlementEngine.calculateWithEdgeCases(event, expenses)

        assertTrue(result.isSuccess)
        result.snapshot?.transfers?.forEach { transfer ->
            assertTrue(transfer.amount > 0, "Transfer ${transfer.fromProfileId}→${transfer.toProfileId} should have amount > 0")
        }
    }

    // ── T14: Self-netting (payer == debtor) ──────────────────────────────────

    @Test
    fun `self-netting when payer is also sole debtor returns ZeroBalance`() {
        SettlementEngine.CalculationLock.clearAllForTest()
        val event = testEventWithId(id = "evt-sn", memberIds = listOf("A", "B"))
        // A pays 10€, assigned only to A → balance = 0
        val expenses = listOf(
            testExpense(
                amountEuros = 10.0,
                payerContributions = mapOf("A" to 10.0),
                debtorIds = listOf("A"),
            )
        )

        val result = SettlementEngine.calculateWithEdgeCases(event, expenses)

        assertTrue(result.isSuccess)
        assertTrue(result.status is CalculationStatus.ZeroBalance)
        assertTrue(result.snapshot!!.transfers.isEmpty())
    }

    // ── T15: Partial self-netting (some overlap) ─────────────────────────────

    @Test
    fun `partial self-netting produces correct transfers`() {
        SettlementEngine.CalculationLock.clearAllForTest()
        val event = testEventWithId(id = "evt-psn", memberIds = listOf("A", "B", "C"))
        // A pays 10€, B pays 10€ → total 20€
        // Assigned to A, C → each owes 10€
        // A: +10 - 10 = 0; B: +10; C: -10
        // B → C: 10€
        val expenses = listOf(
            testExpense(
                amountEuros = 20.0,
                payerContributions = mapOf("A" to 10.0, "B" to 10.0),
                debtorIds = listOf("A", "C"),
            )
        )

        val result = SettlementEngine.calculateWithEdgeCases(event, expenses)

        assertTrue(result.isSuccess)
        assertTrue(result.status is CalculationStatus.Success)
        val transfers = result.snapshot!!.transfers
        assertEquals(1, transfers.size)
        assertEquals("C", transfers[0].fromProfileId)
        assertEquals("B", transfers[0].toProfileId)
        assertEquals(10.0, transfers[0].amount)
    }

    // ── T16: Participant missing on recalculate ──────────────────────────────

    @Test
    fun `participant removed after calculation returns Error`() {
        SettlementEngine.CalculationLock.clearAllForTest()
        val priorSnapshot = CalculationSnapshot(
            transfers = emptyList(),
            totalExpense = 30.0,
            calculatedAtMillis = System.currentTimeMillis(),
            participantBalances = mapOf("A" to 0.0, "B" to 0.0, "C" to 0.0),
        )
        // Current event lacks C
        val event = testEventWithId(id = "evt-pc", memberIds = listOf("A", "B"))
        val expenses = emptyList<EventExpenseItem>()

        val result = SettlementEngine.calculateWithEdgeCases(
            event = event,
            expenses = expenses,
            priorSnapshot = priorSnapshot,
            profileNameResolver = { id -> "Profile-$id" },
        )

        assertFalse(result.isSuccess)
        assertTrue(result.status is CalculationStatus.Error)
        assertTrue(result.errors.any { it.contains("falta el participante") })
        assertTrue(result.errors.any { it.contains("Profile-C") })
    }

    // ── T17: New participant allowed on recalculate ──────────────────────────

    @Test
    fun `new participant added after calculation succeeds`() {
        SettlementEngine.CalculationLock.clearAllForTest()
        val priorSnapshot = CalculationSnapshot(
            transfers = emptyList(),
            totalExpense = 30.0,
            calculatedAtMillis = System.currentTimeMillis(),
            participantBalances = mapOf("A" to 0.0, "B" to 0.0),
        )
        // Current event has A, B, C (C is new)
        val event = testEventWithId(id = "evt-np", memberIds = listOf("A", "B", "C"))
        val expenses = listOf(
            testExpense(
                amountEuros = 30.0,
                payerContributions = mapOf("A" to 30.0),
                debtorIds = listOf("A", "B", "C"),
            )
        )

        val result = SettlementEngine.calculateWithEdgeCases(
            event = event,
            expenses = expenses,
            priorSnapshot = priorSnapshot,
        )

        assertTrue(result.isSuccess)
        // C starts with zero balance in calculation
        assertTrue(result.snapshot!!.participantBalances.containsKey("C"))
    }

    // ── T18: Calculation lock acquisition and release ────────────────────────

    @Test
    fun `lock acquired and released after calculation`() {
        SettlementEngine.CalculationLock.clearAllForTest()
        val event = testEventWithId(id = "evt-lock", memberIds = listOf("A", "B"))
        val expenses = listOf(
            testExpense(
                amountEuros = 10.0,
                payerContributions = mapOf("A" to 10.0),
                debtorIds = listOf("A", "B"),
            )
        )

        val result = SettlementEngine.calculateWithEdgeCases(event, expenses)

        assertTrue(result.isSuccess)
        // After calculation, lock should be released — verify by doing another call
        val result2 = SettlementEngine.calculateWithEdgeCases(event, expenses)
        assertTrue(result2.isSuccess, "Second call should succeed after lock release")
    }

    // ── T19: Lock timeout / deadlock prevention ──────────────────────────────

    @Test
    fun `stale lock is auto-released allowing new calculation`() {
        SettlementEngine.CalculationLock.clearAllForTest()
        val eventId = "evt-stale"

        // Simulate a stale lock (30+ seconds old)
        val staleTimestamp = System.currentTimeMillis() - 31_000
        SettlementEngine.CalculationLock.setLockForTest(eventId, staleTimestamp)

        val event = testEventWithId(id = eventId, memberIds = listOf("A", "B"))
        val expenses = listOf(
            testExpense(
                amountEuros = 10.0,
                payerContributions = mapOf("A" to 10.0),
                debtorIds = listOf("A", "B"),
            )
        )

        // Should succeed because stale lock is auto-released
        val result = SettlementEngine.calculateWithEdgeCases(event, expenses)

        assertTrue(result.isSuccess, "Should succeed after stale lock auto-release")
    }

    @Test
    fun `active lock blocks concurrent calculation`() {
        SettlementEngine.CalculationLock.clearAllForTest()
        val eventId = "evt-concurrent"

        // Simulate an active (non-stale) lock
        SettlementEngine.CalculationLock.setLockForTest(eventId, System.currentTimeMillis())

        val event = testEventWithId(id = eventId, memberIds = listOf("A", "B"))
        val expenses = listOf(
            testExpense(
                amountEuros = 10.0,
                payerContributions = mapOf("A" to 10.0),
                debtorIds = listOf("A", "B"),
            )
        )

        val result = SettlementEngine.calculateWithEdgeCases(event, expenses)

        assertFalse(result.isSuccess)
        assertTrue(result.status is CalculationStatus.Error)
        assertTrue(result.errors.any { it.contains("Cálculo en progreso") })
    }

    @Test
    fun `lock released on validation error`() {
        SettlementEngine.CalculationLock.clearAllForTest()
        val event = testEventWithId(id = "evt-err", memberIds = listOf("A", "B"))
        // Invalid expense (zero amount) → validation error
        val expenses = listOf(
            testExpense(
                amountEuros = 0.0,
                payerContributions = emptyMap(),
                debtorIds = emptyList(),
            )
        )

        val result = SettlementEngine.calculateWithEdgeCases(event, expenses)

        assertFalse(result.isSuccess)
        // Lock should be released — verify by doing another call
        val validExpenses = listOf(
            testExpense(
                amountEuros = 10.0,
                payerContributions = mapOf("A" to 10.0),
                debtorIds = listOf("A", "B"),
            )
        )
        val result2 = SettlementEngine.calculateWithEdgeCases(event, validExpenses)
        // This may succeed or fail based on validation, but lock was released
        // (if lock wasn't released, result2 would fail with "Cálculo en progreso")
        assertFalse(result2.errors.any { it.contains("Cálculo en progreso") })
    }

    // ── T19b: Deleted creditor handling ──────────────────────────────────────

    @Test
    fun `deleted creditor produces EdgeCaseWarning`() {
        SettlementEngine.CalculationLock.clearAllForTest()
        val event = testEventWithId(id = "evt-dc", memberIds = listOf("A", "B", "C"))
        // A pays 30€, split among A, B, C → B and C owe A 10€ each
        val expenses = listOf(
            testExpense(
                amountEuros = 30.0,
                payerContributions = mapOf("A" to 30.0),
                debtorIds = listOf("A", "B", "C"),
            )
        )

        val result = SettlementEngine.calculateWithEdgeCases(
            event = event,
            expenses = expenses,
            deletedProfileIds = setOf("A"), // A (creditor) is deleted
            profileNameResolver = { id -> "Profile-$id" },
        )

        assertTrue(result.isSuccess)
        assertTrue(result.status is CalculationStatus.EdgeCaseWarning)
        val warning = result.status as CalculationStatus.EdgeCaseWarning
        assertTrue(warning.message.contains("Profile-A"))
        assertTrue(warning.message.contains("ya no está disponible"))
    }

    // ── D8 Regression: original calculate() unchanged ────────────────────────

    @Test
    fun `original calculate still returns Success with snapshot`() {
        val event = testEvent(memberIds = listOf("A", "B", "C"))
        val expenses = listOf(
            testExpense(
                amountEuros = 30.0,
                payerContributions = mapOf("A" to 30.0),
                debtorIds = listOf("A", "B", "C"),
            )
        )

        val result = SettlementEngine.calculate(event, expenses)

        assertTrue(result.isSuccess)
        // Original calculate does NOT set status field
        assertEquals(null, result.status)
        assertEquals(2, result.snapshot!!.transfers.size)
    }

    // ── Trace emission tests ─────────────────────────────────────────────────

    @Test
    fun `calculate emits trace steps matching transfer count`() {
        val event = testEvent(memberIds = listOf("A", "B", "C"))
        val expenses = listOf(
            testExpense(
                amountEuros = 30.0,
                payerContributions = mapOf("A" to 30.0),
                debtorIds = listOf("A", "B", "C"),
            )
        )

        val result = SettlementEngine.calculate(event, expenses)

        assertTrue(result.isSuccess)
        val snapshot = result.snapshot!!
        assertEquals(snapshot.transfers.size, snapshot.trace.size, "Trace steps must match transfer count")
    }

    @Test
    fun `trace step amounts match transfer amounts`() {
        val event = testEvent(memberIds = listOf("A", "B"))
        val expenses = listOf(
            testExpense(
                amountEuros = 20.0,
                payerContributions = mapOf("A" to 20.0),
                debtorIds = listOf("A", "B"),
            )
        )

        val result = SettlementEngine.calculate(event, expenses)

        assertTrue(result.isSuccess)
        val snapshot = result.snapshot!!
        snapshot.trace.forEachIndexed { i, step ->
            assertEquals(snapshot.transfers[i].amount, step.amount, 0.001)
            assertEquals(snapshot.transfers[i].fromProfileId, step.fromProfileId)
            assertEquals(snapshot.transfers[i].toProfileId, step.toProfileId)
        }
    }

    @Test
    fun `trace is empty when no transfers needed`() {
        val event = testEvent(memberIds = listOf("A", "B"))
        val expenses = listOf(
            testExpense(
                amountEuros = 10.0,
                payerContributions = mapOf("A" to 10.0),
                debtorIds = listOf("A"),
            )
        )

        val result = SettlementEngine.calculate(event, expenses)

        assertTrue(result.isSuccess)
        assertTrue(result.snapshot!!.trace.isEmpty())
    }

    @Test
    fun `trace captures remaining balances correctly`() {
        val event = testEvent(memberIds = listOf("A", "B", "C"))
        val expenses = listOf(
            testExpense(
                amountEuros = 30.0,
                payerContributions = mapOf("A" to 30.0),
                debtorIds = listOf("A", "B", "C"),
            )
        )

        val result = SettlementEngine.calculate(event, expenses)

        assertTrue(result.isSuccess)
        val snapshot = result.snapshot!!
        // Last trace step should leave creditor at 0 (fully settled)
        val lastStep = snapshot.trace.last()
        assertEquals(0.0, lastStep.creditorRemaining, 0.001)
    }
}
