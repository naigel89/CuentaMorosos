package com.cuentamorosos.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IntegritySerializationTest {

    // ── CalculationSnapshot → JSON → CalculationSnapshot roundtrip ───────────

    @Test
    fun `CalculationSnapshot roundtrip preserves all fields`() {
        val transfers = listOf(
            SettlementTransfer(fromProfileId = "A", toProfileId = "B", amount = 10.0),
            SettlementTransfer(fromProfileId = "C", toProfileId = "B", amount = 25.5),
        )
        val original = CalculationSnapshot(
            transfers = transfers,
            totalExpense = 150.0,
            calculatedAtMillis = 1234567890L,
            algorithmVersion = "v1-greedy",
        )

        val json = original.toJson()
        val restored = json.toCalculationSnapshot()

        assertNotNull(restored)
        assertEquals(2, restored.transfers.size)
        assertEquals(150.0, restored.totalExpense)
        assertEquals(1234567890L, restored.calculatedAtMillis)
        assertEquals("v1-greedy", restored.algorithmVersion)
    }

    @Test
    fun `CalculationSnapshot roundtrip preserves transfer details`() {
        val transfers = listOf(
            SettlementTransfer(fromProfileId = "alice", toProfileId = "bob", amount = 42.5),
        )
        val original = CalculationSnapshot(
            transfers = transfers,
            totalExpense = 42.5,
            calculatedAtMillis = 1000L,
        )

        val json = original.toJson()
        val restored = json.toCalculationSnapshot()

        assertNotNull(restored)
        assertEquals("alice", restored.transfers[0].fromProfileId)
        assertEquals("bob", restored.transfers[0].toProfileId)
        assertEquals(42.5, restored.transfers[0].amount)
    }

    // ── List<SettlementTransfer> → JSON → List roundtrip ─────────────────────

    @Test
    fun `transfers list roundtrip preserves order and values`() {
        val original = listOf(
            SettlementTransfer("A", "B", 10.0),
            SettlementTransfer("C", "D", 20.0),
            SettlementTransfer("E", "F", 30.0),
        )

        val json = original.toJson()
        val restored = json.toSettlementTransfers()

        assertEquals(3, restored.size)
        assertEquals("A", restored[0].fromProfileId)
        assertEquals("B", restored[0].toProfileId)
        assertEquals(10.0, restored[0].amount)
        assertEquals("C", restored[1].fromProfileId)
        assertEquals("D", restored[1].toProfileId)
        assertEquals(20.0, restored[1].amount)
        assertEquals("E", restored[2].fromProfileId)
        assertEquals("F", restored[2].toProfileId)
        assertEquals(30.0, restored[2].amount)
    }

    @Test
    fun `single transfer roundtrip`() {
        val original = listOf(SettlementTransfer("X", "Y", 99.99))

        val json = original.toJson()
        val restored = json.toSettlementTransfers()

        assertEquals(1, restored.size)
        assertEquals("X", restored[0].fromProfileId)
        assertEquals("Y", restored[0].toProfileId)
        assertEquals(99.99, restored[0].amount)
    }

    // ── Edge case: empty transfers list ──────────────────────────────────────

    @Test
    fun `empty transfers list roundtrip`() {
        val original = emptyList<SettlementTransfer>()

        val json = original.toJson()
        val restored = json.toSettlementTransfers()

        assertTrue(restored.isEmpty())
    }

    @Test
    fun `snapshot with empty transfers roundtrip`() {
        val original = CalculationSnapshot(
            transfers = emptyList(),
            totalExpense = 0.0,
            calculatedAtMillis = 0L,
        )

        val json = original.toJson()
        val restored = json.toCalculationSnapshot()

        assertNotNull(restored)
        assertTrue(restored.transfers.isEmpty())
        assertEquals(0.0, restored.totalExpense)
    }

    // ── Edge case: snapshot with null-like fields ────────────────────────────

    @Test
    fun `snapshot with zero values roundtrip`() {
        val original = CalculationSnapshot(
            transfers = emptyList(),
            totalExpense = 0.0,
            calculatedAtMillis = 0L,
            algorithmVersion = "v1-greedy",
        )

        val json = original.toJson()
        val restored = json.toCalculationSnapshot()

        assertNotNull(restored)
        assertEquals(0.0, restored.totalExpense)
        assertEquals(0L, restored.calculatedAtMillis)
    }

    @Test
    fun `snapshot with large amounts roundtrip`() {
        val transfers = listOf(
            SettlementTransfer("A", "B", 999999.99),
        )
        val original = CalculationSnapshot(
            transfers = transfers,
            totalExpense = 999999.99,
            calculatedAtMillis = Long.MAX_VALUE,
        )

        val json = original.toJson()
        val restored = json.toCalculationSnapshot()

        assertNotNull(restored)
        assertEquals(999999.99, restored.totalExpense)
        assertEquals(Long.MAX_VALUE, restored.calculatedAtMillis)
    }

    // ── EventExpenseItem JSON snapshot ───────────────────────────────────────

    @Test
    fun `EventExpenseItem toJsonSnapshot produces valid JSON`() {
        val expense = EventExpenseItem(
            id = "exp1",
            eventId = "evt1",
            name = "Cena especial",
            amountEuros = 75.5,
            category = "food",
            paidByProfileId = "p1",
            splitMode = "SIMPLE_AVG",
            debtorIds = listOf("p1", "p2"),
            createdAtMillis = 1000L,
            createdByProfileId = "p1",
        )

        val json = expense.toJsonSnapshot()

        assertTrue(json.contains("\"id\":\"exp1\""))
        assertTrue(json.contains("\"name\":\"Cena especial\""))
        assertTrue(json.contains("\"amountEuros\":75.5"))
        assertTrue(json.contains("\"category\":\"food\""))
        assertTrue(json.contains("\"paidByProfileId\":\"p1\""))
        assertTrue(json.contains("\"splitMode\":\"SIMPLE_AVG\""))
        assertTrue(json.contains("\"debtorIds\":["))
        assertTrue(json.contains("\"p1\""))
        assertTrue(json.contains("\"p2\""))
    }

    @Test
    fun `EventExpenseItem toJsonSnapshot handles special characters in name`() {
        val expense = EventExpenseItem(
            id = "exp1",
            eventId = "evt1",
            name = "Cena con \"comillas\" y \\backslash\\",
            amountEuros = 50.0,
            paidByProfileId = "p1",
        )

        val json = expense.toJsonSnapshot()

        // Should not crash — escaped properly
        assertTrue(json.contains("comillas"))
        assertTrue(json.contains("backslash"))
    }
}
