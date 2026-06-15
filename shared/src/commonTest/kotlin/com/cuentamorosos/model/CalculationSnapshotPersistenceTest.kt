package com.cuentamorosos.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for CalculationSnapshot JSON serialization roundtrip,
 * including the participantBalances field (R007, R009).
 *
 * T-13: Roundtrip tests ensure serialize → deserialize preserves
 *       transfers, totalExpense, calculatedAtMillis, algorithmVersion,
 *       AND participantBalances.
 * T-14: Null safety tests ensure malformed/null/empty JSON returns null.
 */
class CalculationSnapshotPersistenceTest {

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun makeSnapshot(
        transfers: List<SettlementTransfer> = listOf(
            SettlementTransfer("ana-1", "carlos-1", 25.0),
            SettlementTransfer("ana-1", "luis-1", 10.0),
        ),
        totalExpense: Double = 150.0,
        calculatedAtMillis: Long = 1718400000000L,
        algorithmVersion: String = "v1-greedy",
        participantBalances: Map<String, Double> = mapOf(
            "ana-1" to -35.0,
            "carlos-1" to 25.0,
            "luis-1" to 10.0,
        ),
    ) = CalculationSnapshot(
        transfers = transfers,
        totalExpense = totalExpense,
        calculatedAtMillis = calculatedAtMillis,
        algorithmVersion = algorithmVersion,
        participantBalances = participantBalances,
    )

    // ── T-13: Roundtrip tests ───────────────────────────────────────────────

    @Test
    fun `roundtrip preserves transfers`() {
        val original = makeSnapshot()
        val json = original.toJson()
        val restored = json.toCalculationSnapshot()

        assertNotNull(restored, "Deserialized snapshot must not be null")
        assertEquals(original.transfers.size, restored.transfers.size)
        original.transfers.forEachIndexed { i, transfer ->
            assertEquals(transfer.fromProfileId, restored.transfers[i].fromProfileId)
            assertEquals(transfer.toProfileId, restored.transfers[i].toProfileId)
            assertEquals(transfer.amount, restored.transfers[i].amount, 0.001)
        }
    }

    @Test
    fun `roundtrip preserves totalExpense`() {
        val original = makeSnapshot(totalExpense = 299.99)
        val json = original.toJson()
        val restored = json.toCalculationSnapshot()

        assertNotNull(restored)
        assertEquals(original.totalExpense, restored.totalExpense, 0.001)
    }

    @Test
    fun `roundtrip preserves calculatedAtMillis`() {
        val original = makeSnapshot(calculatedAtMillis = 1718400123456L)
        val json = original.toJson()
        val restored = json.toCalculationSnapshot()

        assertNotNull(restored)
        assertEquals(original.calculatedAtMillis, restored.calculatedAtMillis)
    }

    @Test
    fun `roundtrip preserves algorithmVersion`() {
        val original = makeSnapshot(algorithmVersion = "v2-priority-queue")
        val json = original.toJson()
        val restored = json.toCalculationSnapshot()

        assertNotNull(restored)
        assertEquals(original.algorithmVersion, restored.algorithmVersion)
    }

    @Test
    fun `roundtrip preserves participantBalances`() {
        val original = makeSnapshot(
            participantBalances = mapOf(
                "pepe-1" to -23.0,
                "luis-1" to 13.0,
                "ana-1" to 10.0,
            ),
        )
        val json = original.toJson()
        val restored = json.toCalculationSnapshot()

        assertNotNull(restored, "Deserialized snapshot must not be null")
        assertEquals(
            original.participantBalances.size,
            restored.participantBalances.size,
            "participantBalances must have same number of entries",
        )
        original.participantBalances.forEach { (profileId, balance) ->
            val actualBalance = restored.participantBalances[profileId]
            assertNotNull(actualBalance, "Profile $profileId must be present in restored balances")
            assertEquals(
                balance,
                actualBalance,
                0.001,
                "Balance for $profileId must match after roundtrip",
            )
        }
    }

    @Test
    fun `roundtrip with empty transfers and participantBalances`() {
        val original = makeSnapshot(
            transfers = emptyList(),
            totalExpense = 0.0,
            participantBalances = emptyMap(),
        )
        val json = original.toJson()
        val restored = json.toCalculationSnapshot()

        assertNotNull(restored, "Deserialized snapshot must not be null for zero-balance case")
        assertTrue(restored.transfers.isEmpty(), "Transfers must be empty")
        assertTrue(restored.participantBalances.isEmpty(), "participantBalances must be empty")
        assertEquals(0.0, restored.totalExpense, 0.001)
    }

    @Test
    fun `roundtrip with single participant self-netting`() {
        val original = makeSnapshot(
            transfers = emptyList(),
            totalExpense = 10.0,
            participantBalances = mapOf("solo-1" to 0.0),
        )
        val json = original.toJson()
        val restored = json.toCalculationSnapshot()

        assertNotNull(restored)
        assertEquals(1, restored.participantBalances.size)
        val balance = restored.participantBalances["solo-1"]
        assertNotNull(balance)
        assertEquals(0.0, balance, 0.001)
    }

    // ── T-14: Null safety tests ──────────────────────────────────────────────

    @Test
    fun `null JSON returns null`() {
        assertNull("".toCalculationSnapshot())
    }

    @Test
    fun `empty JSON returns null`() {
        assertNull("{}".toCalculationSnapshot())
    }

    @Test
    fun `malformed JSON returns null`() {
        assertNull("not valid json at all".toCalculationSnapshot())
    }

    @Test
    fun `JSON missing transfers returns null`() {
        val json = """{"totalExpense":100.0,"calculatedAtMillis":1,"algorithmVersion":"v1"}"""
        assertNull(json.toCalculationSnapshot())
    }

    @Test
    fun `JSON missing totalExpense returns null`() {
        val json = """{"transfers":[],"calculatedAtMillis":1,"algorithmVersion":"v1"}"""
        assertNull(json.toCalculationSnapshot())
    }

    @Test
    fun `old snapshot without participantBalances still deserializes`() {
        val oldJson = """{"transfers":[{"from":"A","to":"B","amount":10.0}],"totalExpense":10.0,"calculatedAtMillis":1,"algorithmVersion":"v1-greedy"}"""
        val restored = oldJson.toCalculationSnapshot()

        assertNotNull(restored, "Old format must still deserialize")
        assertEquals(1, restored.transfers.size)
        assertEquals(10.0, restored.totalExpense, 0.001)
        assertTrue(restored.participantBalances.isEmpty(), "Old format must yield empty participantBalances")
    }

    @Test
    fun `JSON with only partial fields returns null`() {
        val json = """{"transfers":[]}"""
        assertNull(json.toCalculationSnapshot())
    }

    @Test
    fun `transfers array with valid entries is parsed`() {
        val json = """{"transfers":[{"from":"A","to":"B","amount":15.5},{"from":"C","to":"D","amount":7.25}],"totalExpense":22.75,"calculatedAtMillis":1,"algorithmVersion":"v1"}"""
        val restored = json.toCalculationSnapshot()

        assertNotNull(restored)
        assertEquals(2, restored.transfers.size)
    }
}
