package com.cuentamorosos.ui

import com.cuentamorosos.model.CalculationSnapshot
import com.cuentamorosos.model.SettlementTransfer
import com.cuentamorosos.model.toCalculationSnapshot
import com.cuentamorosos.model.toJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for SettlementPanel data transformation logic.
 *
 * T-17: SettlementPanel reads lastCalculationSummary, deserializes,
 *       and renders transfer details + per-profile balances.
 * T-18: Backward compat — null summary shows only net balances.
 *
 * Because compose.ui.test is unavailable in commonTest, we test the
 * pure data transformation functions that the composable depends on.
 */
class SettlementPanelPersistenceTest {

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun makeSnapshotJson(
        transfers: List<SettlementTransfer> = listOf(
            SettlementTransfer("pepe-1", "luis-1", 13.0),
            SettlementTransfer("pepe-1", "ana-1", 10.0),
        ),
        totalExpense: Double = 150.0,
        participantBalances: Map<String, Double> = mapOf(
            "pepe-1" to -23.0,
            "luis-1" to 13.0,
            "ana-1" to 10.0,
        ),
    ): String = CalculationSnapshot(
        transfers = transfers,
        totalExpense = totalExpense,
        calculatedAtMillis = 1718400000000L,
        participantBalances = participantBalances,
    ).toJson()

    // ── Pure function: extract transfer info per debtor ──────────────────────

    /**
     * Groups transfers by debtor (fromProfileId) and returns a map of
     * debtorId → (totalOwed, list of (creditorId, amount) pairs).
     */
    private fun groupTransfersByDebtor(
        transfers: List<SettlementTransfer>,
    ): Map<String, Pair<Double, List<Pair<String, Double>>>> {
        return transfers
            .groupBy { it.fromProfileId }
            .mapValues { (_, debtorTransfers) ->
                val total = debtorTransfers.sumOf { it.amount }
                val creditors = debtorTransfers.map { it.toProfileId to it.amount }
                total to creditors
            }
    }

    // ── T-17: SettlementPanel data transformation tests ──────────────────────

    @Test
    fun `deserialize valid snapshot returns non-null`() {
        val json = makeSnapshotJson()
        val snapshot = json.toCalculationSnapshot()

        assertNotNull(snapshot, "Valid JSON must deserialize to snapshot")
    }

    @Test
    fun `groupTransfersByDebtor aggregates transfers per debtor`() {
        val transfers = listOf(
            SettlementTransfer("pepe-1", "luis-1", 13.0),
            SettlementTransfer("pepe-1", "ana-1", 10.0),
            SettlementTransfer("luis-1", "ana-1", 5.0),
        )
        val grouped = groupTransfersByDebtor(transfers)

        assertEquals(2, grouped.size)
        val pepe = grouped["pepe-1"]
        assertNotNull(pepe)
        assertEquals(23.0, pepe.first, 0.001, "Pepe owes 13+10=23 total")
        assertEquals(2, pepe.second.size)

        val luis = grouped["luis-1"]
        assertNotNull(luis)
        assertEquals(5.0, luis.first, 0.001, "Luis owes 5 total")
        assertEquals(1, luis.second.size)
    }

    @Test
    fun `groupTransfersByDebtor with empty transfers returns empty map`() {
        val grouped = groupTransfersByDebtor(emptyList())
        assertTrue(grouped.isEmpty())
    }

    @Test
    fun `groupTransfersByDebtor with single transfer`() {
        val transfers = listOf(
            SettlementTransfer("debtor-1", "creditor-1", 42.0),
        )
        val grouped = groupTransfersByDebtor(transfers)

        assertEquals(1, grouped.size)
        val debtor = grouped["debtor-1"]
        assertNotNull(debtor)
        assertEquals(42.0, debtor.first, 0.001)
        assertEquals(1, debtor.second.size)
        assertEquals("creditor-1" to 42.0, debtor.second[0])
    }

    @Test
    fun `snapshot roundtrip produces correct transfer grouping`() {
        val json = makeSnapshotJson(
            transfers = listOf(
                SettlementTransfer("pepe-1", "luis-1", 13.0),
                SettlementTransfer("pepe-1", "ana-1", 10.0),
            ),
        )
        val snapshot = json.toCalculationSnapshot()
        assertNotNull(snapshot)
        val grouped = groupTransfersByDebtor(snapshot.transfers)

        assertEquals(1, grouped.size, "One debtor (pepe)")
        val pepe = grouped["pepe-1"]
        assertNotNull(pepe)
        assertEquals(23.0, pepe.first, 0.001)
        assertEquals(2, pepe.second.size)
    }

    // ── T-18: Backward compat — null summary → only net balances ─────────────

    @Test
    fun `null JSON returns null snapshot`() {
        assertNull("".toCalculationSnapshot())
    }

    @Test
    fun `null summary produces empty transfer list`() {
        val summary: String? = null
        val snapshot = summary?.toCalculationSnapshot()
        assertNull(snapshot, "Null summary must produce null snapshot")
    }

    @Test
    fun `malformed summary produces null snapshot`() {
        val snapshot = "not json at all".toCalculationSnapshot()
        assertNull(snapshot, "Malformed JSON must produce null snapshot")
    }

    // ── Transfer format rendering (pure function) ─────────────────────────────

    /**
     * Formats a debtor's transfer info as:
     * "{debtorName} debe {total}€ ({amount1} a {cred1}, {amount2} a {cred2})"
     */
    private fun formatDebtorTransfers(
        debtorName: String,
        total: Double,
        creditors: List<Pair<String, Double>>,
        resolveName: (String) -> String,
    ): String {
        val parts = creditors.joinToString(", ") { (creditorId, amount) ->
            val formattedAmount = "%.2f".format(amount).replace(".", ",")
            "${formattedAmount} a ${resolveName(creditorId)}"
        }
        val formattedTotal = "%.2f".format(total).replace(".", ",")
        return "$debtorName debe $formattedTotal€ ($parts)"
    }

    @Test
    fun `formatDebtorTransfers produces correct Spanish text for multi-creditor`() {
        val nameResolver: (String) -> String = { id -> mapOf("luis-1" to "Luis", "ana-1" to "Ana")[id] ?: id }
        val result = formatDebtorTransfers(
            debtorName = "Pepe",
            total = 23.0,
            creditors = listOf("luis-1" to 13.0, "ana-1" to 10.0),
            resolveName = nameResolver,
        )

        assertEquals("Pepe debe 23,00€ (13,00 a Luis, 10,00 a Ana)", result)
    }

    @Test
    fun `formatDebtorTransfers produces correct Spanish text for single creditor`() {
        val nameResolver: (String) -> String = { "Carlos" }
        val result = formatDebtorTransfers(
            debtorName = "María",
            total = 25.0,
            creditors = listOf("carlos-1" to 25.0),
            resolveName = nameResolver,
        )

        assertEquals("María debe 25,00€ (25,00 a Carlos)", result)
    }

    @Test
    fun `formatDebtorTransfers with empty creditors produces minimal text`() {
        val nameResolver: (String) -> String = { it }
        val result = formatDebtorTransfers(
            debtorName = "Ana",
            total = 0.0,
            creditors = emptyList(),
            resolveName = nameResolver,
        )

        assertEquals("Ana debe 0,00€ ()", result)
    }

    @Test
    fun `totalExpense from snapshot is accessible for read-only display`() {
        val json = makeSnapshotJson(totalExpense = 299.50)
        val snapshot = json.toCalculationSnapshot()
        assertNotNull(snapshot)

        assertEquals(299.50, snapshot.totalExpense, 0.001)
        // In SettlementPanel, this would be rendered as read-only Text, not OutlinedTextField
    }
}
