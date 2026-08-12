package com.cuentamorosos.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.cuentamorosos.db.CuentaMorososDatabase
import com.cuentamorosos.model.CalculationSnapshot
import com.cuentamorosos.model.CalculationVersion
import com.cuentamorosos.model.SettlementTransfer
import com.cuentamorosos.model.toJson
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CalculationVersionRepositoryTest {

    private lateinit var database: CuentaMorososDatabase
    private lateinit var repository: OfflineFirstCalculationVersionRepository

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CuentaMorososDatabase.Schema.create(driver)
        database = CuentaMorososDatabase(driver)
        repository = OfflineFirstCalculationVersionRepository(database)
    }

    private fun testSnapshot(transfers: List<SettlementTransfer> = emptyList(), total: Double = 0.0) =
        CalculationSnapshot(
            transfers = transfers,
            totalExpense = total,
            calculatedAtMillis = System.currentTimeMillis(),
        )

    // ── Monotonic version numbering ──────────────────────────────────────────

    @Test
    fun `saveVersion auto-increments from 1 when no previous versions`() = runTest {
        val snapshot = testSnapshot(total = 100.0)
        val version = CalculationVersion(eventId = "evt1", version = 0, snapshotJson = snapshot.toJson())

        val saved = repository.saveVersion(version)

        assertEquals(1, saved.version)
    }

    @Test
    fun `saveVersion creates versions 1, 2, 3 sequentially`() = runTest {
        val snapshot = testSnapshot(total = 100.0)

        val v1 = repository.saveVersion(CalculationVersion(eventId = "evt1", version = 0, snapshotJson = snapshot.toJson()))
        val v2 = repository.saveVersion(CalculationVersion(eventId = "evt1", version = 0, snapshotJson = snapshot.toJson()))
        val v3 = repository.saveVersion(CalculationVersion(eventId = "evt1", version = 0, snapshotJson = snapshot.toJson()))

        assertEquals(1, v1.version)
        assertEquals(2, v2.version)
        assertEquals(3, v3.version)
    }

    // ── No overwrite ─────────────────────────────────────────────────────────

    @Test
    fun `saving same version number creates new entry with auto-increment`() = runTest {
        val snapshot1 = testSnapshot(total = 100.0)
        val snapshot2 = testSnapshot(total = 200.0)

        val v1 = repository.saveVersion(CalculationVersion(eventId = "evt1", version = 0, snapshotJson = snapshot1.toJson()))
        val v2 = repository.saveVersion(CalculationVersion(eventId = "evt1", version = 0, snapshotJson = snapshot2.toJson()))

        // v1 should still be version 1, v2 should be version 2
        assertEquals(1, v1.version)
        assertEquals(2, v2.version)
        assertEquals(2, repository.getByEvent("evt1").size)
    }

    @Test
    fun `previous version persists after recalculation`() = runTest {
        val snapshot1 = testSnapshot(total = 100.0)
        val snapshot2 = testSnapshot(total = 200.0)

        repository.saveVersion(CalculationVersion(eventId = "evt1", version = 0, snapshotJson = snapshot1.toJson()))
        repository.saveVersion(CalculationVersion(eventId = "evt1", version = 0, snapshotJson = snapshot2.toJson()))

        val retrieved = repository.getVersion("evt1", 1)
        assertNotNull(retrieved)
        assertTrue(retrieved.snapshotJson.contains("100.0"))
    }

    // ── Active version management ────────────────────────────────────────────

    @Test
    fun `only one version is active per event after deactivateAll`() = runTest {
        val snapshot = testSnapshot(total = 100.0)

        repository.saveVersion(CalculationVersion(eventId = "evt1", version = 0, snapshotJson = snapshot.toJson(), isActive = true))
        repository.saveVersion(CalculationVersion(eventId = "evt1", version = 0, snapshotJson = snapshot.toJson(), isActive = true))

        repository.deactivateAll("evt1")

        val versions = repository.getByEvent("evt1")
        assertTrue(versions.none { it.isActive })
    }

    // ── Query by version ─────────────────────────────────────────────────────

    @Test
    fun `getVersion returns correct snapshot`() = runTest {
        val snapshot = testSnapshot(total = 150.0)

        repository.saveVersion(CalculationVersion(eventId = "evt1", version = 0, snapshotJson = snapshot.toJson()))
        repository.saveVersion(CalculationVersion(eventId = "evt1", version = 0, snapshotJson = testSnapshot(total = 200.0).toJson()))

        val v1 = repository.getVersion("evt1", 1)
        assertNotNull(v1)
        assertTrue(v1.snapshotJson.contains("150.0"))
    }

    @Test
    fun `getVersion returns null for non-existent version`() = runTest {
        assertNull(repository.getVersion("evt1", 99))
    }

    @Test
    fun `getByEvent returns all versions sorted by version number`() = runTest {
        val snapshot = testSnapshot(total = 100.0)

        repository.saveVersion(CalculationVersion(eventId = "evt1", version = 0, snapshotJson = snapshot.toJson()))
        repository.saveVersion(CalculationVersion(eventId = "evt1", version = 0, snapshotJson = snapshot.toJson()))
        repository.saveVersion(CalculationVersion(eventId = "evt1", version = 0, snapshotJson = snapshot.toJson()))

        val versions = repository.getByEvent("evt1")

        assertEquals(3, versions.size)
        assertEquals(1, versions[0].version)
        assertEquals(2, versions[1].version)
        assertEquals(3, versions[2].version)
    }

    @Test
    fun `getByEvent returns empty for unknown event`() = runTest {
        assertTrue(repository.getByEvent("unknown").isEmpty())
    }

    // ── Explicit version number ──────────────────────────────────────────────

    @Test
    fun `saveVersion preserves explicit version number when non-zero`() = runTest {
        val snapshot = testSnapshot(total = 100.0)
        val version = CalculationVersion(eventId = "evt1", version = 5, snapshotJson = snapshot.toJson())

        val saved = repository.saveVersion(version)

        assertEquals(5, saved.version)
    }

    // ── Delete by event ──────────────────────────────────────────────────────

    @Test
    fun `deleteByEvent removes all versions for event`() = runTest {
        val snapshot = testSnapshot(total = 100.0)

        repository.saveVersion(CalculationVersion(eventId = "evt1", version = 0, snapshotJson = snapshot.toJson()))
        repository.saveVersion(CalculationVersion(eventId = "evt1", version = 0, snapshotJson = snapshot.toJson()))
        repository.saveVersion(CalculationVersion(eventId = "evt2", version = 0, snapshotJson = snapshot.toJson()))

        repository.deleteByEvent("evt1")

        assertEquals(0, repository.getByEvent("evt1").size)
        assertEquals(1, repository.getByEvent("evt2").size)
    }
}
