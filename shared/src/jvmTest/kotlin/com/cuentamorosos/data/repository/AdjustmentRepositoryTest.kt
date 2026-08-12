package com.cuentamorosos.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.cuentamorosos.db.CuentaMorososDatabase
import com.cuentamorosos.model.AdjustmentEntry
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdjustmentRepositoryTest {

    private lateinit var database: CuentaMorososDatabase
    private lateinit var repository: OfflineFirstAdjustmentRepository

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CuentaMorososDatabase.Schema.create(driver)
        database = CuentaMorososDatabase(driver)
        repository = OfflineFirstAdjustmentRepository(database)
    }

    // ── Net effect with multiple deltas ──────────────────────────────────────

    @Test
    fun `computeNetEffectCents with multiple adjustments`() = runTest {
        // Original: 1000 cents (10.00 EUR)
        // Delta -200 + Delta +100 = -100 net
        // Expected: 1000 + (-100) = 900 cents
        repository.createEntry(
            AdjustmentEntry(
                eventId = "evt1",
                transferKey = "debt-1",
                deltaCents = -200,
                reason = "Correction",
                profileId = "p1",
            )
        )
        repository.createEntry(
            AdjustmentEntry(
                eventId = "evt1",
                transferKey = "debt-1",
                deltaCents = 100,
                reason = "Surcharge",
                profileId = "p1",
            )
        )

        val result = repository.computeNetEffectCents(1000, "evt1", "debt-1")

        assertEquals(900, result)
    }

    @Test
    fun `computeNetEffectCents with no adjustments returns original`() = runTest {
        val result = repository.computeNetEffectCents(5000, "evt1", "debt-1")

        assertEquals(5000, result)
    }

    @Test
    fun `computeNetEffectCents with negative net effect`() = runTest {
        repository.createEntry(
            AdjustmentEntry(
                eventId = "evt1",
                transferKey = "debt-1",
                deltaCents = -500,
                reason = "Overcharge correction",
                profileId = "p1",
            )
        )

        val result = repository.computeNetEffectCents(5000, "evt1", "debt-1")

        assertEquals(4500, result)
    }

    // ── Create + query ───────────────────────────────────────────────────────

    @Test
    fun `createAdjustment entry is retrievable`() = runTest {
        val entry = AdjustmentEntry(
            eventId = "evt1",
            transferKey = "debt-1",
            deltaCents = -300,
            reason = "Error de cálculo",
            profileId = "p1",
        )

        repository.createEntry(entry)

        val adjustments = repository.getByDebt("evt1", "debt-1")
        assertEquals(1, adjustments.size)
        assertEquals(-300, adjustments[0].deltaCents)
        assertEquals("Error de cálculo", adjustments[0].reason)
        assertEquals("p1", adjustments[0].profileId)
    }

    // ── Sum by debt ──────────────────────────────────────────────────────────

    @Test
    fun `getByDebt returns all adjustments for a specific debt`() = runTest {
        repository.createEntry(
            AdjustmentEntry(eventId = "evt1", transferKey = "debt-1", deltaCents = -100, reason = "A", profileId = "p1")
        )
        repository.createEntry(
            AdjustmentEntry(eventId = "evt1", transferKey = "debt-1", deltaCents = 200, reason = "B", profileId = "p1")
        )
        repository.createEntry(
            AdjustmentEntry(eventId = "evt1", transferKey = "debt-2", deltaCents = 50, reason = "C", profileId = "p1")
        )

        val debt1Adjustments = repository.getByDebt("evt1", "debt-1")

        assertEquals(2, debt1Adjustments.size)
        assertTrue(debt1Adjustments.all { it.transferKey == "debt-1" })
    }

    @Test
    fun `getByDebt returns empty for debt with no adjustments`() = runTest {
        val result = repository.getByDebt("evt1", "nonexistent")

        assertTrue(result.isEmpty())
    }

    // ── Get by event ─────────────────────────────────────────────────────────

    @Test
    fun `getByEvent returns all adjustments for an event`() = runTest {
        repository.createEntry(
            AdjustmentEntry(eventId = "evt1", transferKey = "d1", deltaCents = -100, reason = "A", profileId = "p1")
        )
        repository.createEntry(
            AdjustmentEntry(eventId = "evt1", transferKey = "d2", deltaCents = 200, reason = "B", profileId = "p1")
        )
        repository.createEntry(
            AdjustmentEntry(eventId = "evt2", transferKey = "d3", deltaCents = 50, reason = "C", profileId = "p1")
        )

        val evt1Adjustments = repository.getByEvent("evt1")

        assertEquals(2, evt1Adjustments.size)
        assertTrue(evt1Adjustments.all { it.eventId == "evt1" })
    }

    // ── Delete by event ──────────────────────────────────────────────────────

    @Test
    fun `deleteByEvent removes adjustments for event only`() = runTest {
        repository.createEntry(
            AdjustmentEntry(eventId = "evt1", transferKey = "d1", deltaCents = -100, reason = "A", profileId = "p1")
        )
        repository.createEntry(
            AdjustmentEntry(eventId = "evt2", transferKey = "d2", deltaCents = 200, reason = "B", profileId = "p1")
        )

        repository.deleteByEvent("evt1")

        assertEquals(0, repository.getByEvent("evt1").size)
        assertEquals(1, repository.getByEvent("evt2").size)
    }
}
