package com.cuentamorosos.data.repository

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.cuentamorosos.db.CuentaMorososDatabase
import com.cuentamorosos.model.EventParticipant
import com.cuentamorosos.model.EventRole
import com.cuentamorosos.model.EventState
import com.cuentamorosos.model.serializeParticipants
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class EventPersistenceTest {

    private lateinit var queries: com.cuentamorosos.db.CachedEventQueries

    @BeforeTest
    fun setup() {
        val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CuentaMorososDatabase.Schema.create(driver)
        val database = CuentaMorososDatabase(driver)
        queries = database.cachedEventQueries
    }

    @Test
    fun `cached events survive without deleteAll`() {
        queries.upsert(
            id = "evt1",
            name = "Test Event",
            dateMillis = 1000L,
            ownerId = "owner1",
            memberIds = "owner1",
            participants = listOf(
                EventParticipant("owner1", EventRole.OWNER, 1000L)
            ).serializeParticipants(),
            base_currency = "EUR",
            lastCalculationMode = null,
            lastCalculationTotal = null,
            lastCalculationTimestamp = null,
            lastCalculationSummary = null,
            updatedAt = 1000L,
            state = "OPEN",
            startDateMillis = 1000L,
            endDateMillis = 1000L,
        )

        val cached = queries.selectAll().executeAsList()
        assertEquals(1, cached.size)
        assertEquals("evt1", cached[0].id)
    }

    @Test
    fun `deleteAll clears all cached events`() {
        queries.upsert(
            id = "evt2",
            name = "ToDelete",
            dateMillis = 2000L,
            ownerId = "owner1",
            memberIds = "owner1",
            participants = listOf(
                EventParticipant("owner1", EventRole.OWNER, 2000L)
            ).serializeParticipants(),
            base_currency = "EUR",
            lastCalculationMode = null,
            lastCalculationTotal = null,
            lastCalculationTimestamp = null,
            lastCalculationSummary = null,
            updatedAt = 2000L,
            state = "DRAFT",
            startDateMillis = 2000L,
            endDateMillis = 2000L,
        )

        queries.deleteAll()

        val afterDelete = queries.selectAll().executeAsList()
        assertEquals(0, afterDelete.size)
    }

    // ── DRAFT → OPEN fallback on load ────────────────────────────────────────

    @Test
    fun `loading persisted DRAFT string falls back to OPEN`() {
        // Simulates the persistence layer fallback:
        // runCatching { EventState.valueOf("DRAFT") }.getOrDefault(EventState.OPEN)
        val storedState = "DRAFT"
        val resolvedState = runCatching { EventState.valueOf(storedState) }.getOrDefault(EventState.OPEN)
        assertEquals(EventState.OPEN, resolvedState)
    }

    @Test
    fun `loading valid OPEN state string resolves to OPEN`() {
        val storedState = "OPEN"
        val resolvedState = runCatching { EventState.valueOf(storedState) }.getOrDefault(EventState.OPEN)
        assertEquals(EventState.OPEN, resolvedState)
    }

    @Test
    fun `loading valid CALCULATED state string resolves to CALCULATED`() {
        val storedState = "CALCULATED"
        val resolvedState = runCatching { EventState.valueOf(storedState) }.getOrDefault(EventState.OPEN)
        assertEquals(EventState.CALCULATED, resolvedState)
    }
}
