package com.cuentamorosos.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.cuentamorosos.db.CuentaMorososDatabase
import com.cuentamorosos.model.AuditAction
import com.cuentamorosos.model.ExpenseAuditEntry
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuditRepositoryTest {

    private lateinit var database: CuentaMorososDatabase
    private lateinit var repository: OfflineFirstAuditRepository

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CuentaMorososDatabase.Schema.create(driver)
        database = CuentaMorososDatabase(driver)
        repository = OfflineFirstAuditRepository(database)
    }

    // ── Record entry for create/edit/delete actions ──────────────────────────

    @Test
    fun `recordEntry stores CREATED action`() = runTest {
        val entry = ExpenseAuditEntry(
            eventId = "evt1",
            expenseId = "exp1",
            action = AuditAction.CREATED,
            profileId = "p1",
            beforeSnapshot = null,
            afterSnapshot = """{"id":"exp1","name":"Dinner"}""",
        )

        repository.recordEntry(entry)

        val entries = repository.getByExpense("exp1")
        assertEquals(1, entries.size)
        assertEquals(AuditAction.CREATED, entries[0].action)
        assertNull(entries[0].beforeSnapshot)
        assertNotNull(entries[0].afterSnapshot)
    }

    @Test
    fun `recordEntry stores UPDATED action with before and after snapshots`() = runTest {
        val entry = ExpenseAuditEntry(
            eventId = "evt1",
            expenseId = "exp1",
            action = AuditAction.UPDATED,
            profileId = "p2",
            beforeSnapshot = """{"name":"Dinner"}""",
            afterSnapshot = """{"name":"Dinner especial"}""",
        )

        repository.recordEntry(entry)

        val entries = repository.getByExpense("exp1")
        assertEquals(1, entries.size)
        assertEquals(AuditAction.UPDATED, entries[0].action)
        assertEquals("""{"name":"Dinner"}""", entries[0].beforeSnapshot)
        assertEquals("""{"name":"Dinner especial"}""", entries[0].afterSnapshot)
    }

    @Test
    fun `recordEntry stores DELETED action with before snapshot only`() = runTest {
        val entry = ExpenseAuditEntry(
            eventId = "evt1",
            expenseId = "exp1",
            action = AuditAction.DELETED,
            profileId = "p3",
            beforeSnapshot = """{"id":"exp1","name":"Dinner"}""",
            afterSnapshot = null,
        )

        repository.recordEntry(entry)

        val entries = repository.getByExpense("exp1")
        assertEquals(1, entries.size)
        assertEquals(AuditAction.DELETED, entries[0].action)
        assertNotNull(entries[0].beforeSnapshot)
        assertNull(entries[0].afterSnapshot)
    }

    // ── Query by event ───────────────────────────────────────────────────────

    @Test
    fun `getByEvent returns all entries for that event`() = runTest {
        repository.recordEntry(
            ExpenseAuditEntry(eventId = "evt1", expenseId = "exp1", action = AuditAction.CREATED, profileId = "p1")
        )
        repository.recordEntry(
            ExpenseAuditEntry(eventId = "evt1", expenseId = "exp2", action = AuditAction.CREATED, profileId = "p1")
        )
        repository.recordEntry(
            ExpenseAuditEntry(eventId = "evt2", expenseId = "exp3", action = AuditAction.CREATED, profileId = "p1")
        )

        val evt1Entries = repository.getByEvent("evt1")

        assertEquals(2, evt1Entries.size)
        assertTrue(evt1Entries.all { it.eventId == "evt1" })
    }

    @Test
    fun `getByEvent returns entries sorted by timestamp ascending`() = runTest {
        repository.recordEntry(
            ExpenseAuditEntry(eventId = "evt1", expenseId = "exp1", action = AuditAction.CREATED, profileId = "p1", timestamp = 3000L)
        )
        repository.recordEntry(
            ExpenseAuditEntry(eventId = "evt1", expenseId = "exp1", action = AuditAction.UPDATED, profileId = "p1", timestamp = 1000L)
        )
        repository.recordEntry(
            ExpenseAuditEntry(eventId = "evt1", expenseId = "exp1", action = AuditAction.UPDATED, profileId = "p1", timestamp = 2000L)
        )

        val entries = repository.getByEvent("evt1")

        assertEquals(3, entries.size)
        assertEquals(1000L, entries[0].timestamp)
        assertEquals(2000L, entries[1].timestamp)
        assertEquals(3000L, entries[2].timestamp)
    }

    // ── Query by expense ─────────────────────────────────────────────────────

    @Test
    fun `getByExpense returns entries for that expense only`() = runTest {
        repository.recordEntry(
            ExpenseAuditEntry(eventId = "evt1", expenseId = "exp1", action = AuditAction.CREATED, profileId = "p1")
        )
        repository.recordEntry(
            ExpenseAuditEntry(eventId = "evt1", expenseId = "exp1", action = AuditAction.UPDATED, profileId = "p2")
        )
        repository.recordEntry(
            ExpenseAuditEntry(eventId = "evt1", expenseId = "exp2", action = AuditAction.CREATED, profileId = "p1")
        )

        val exp1Entries = repository.getByExpense("exp1")

        assertEquals(2, exp1Entries.size)
        assertTrue(exp1Entries.all { it.expenseId == "exp1" })
    }

    @Test
    fun `getByExpense returns entries in chronological order`() = runTest {
        repository.recordEntry(
            ExpenseAuditEntry(eventId = "evt1", expenseId = "exp1", action = AuditAction.UPDATED, profileId = "p1", timestamp = 2000L)
        )
        repository.recordEntry(
            ExpenseAuditEntry(eventId = "evt1", expenseId = "exp1", action = AuditAction.CREATED, profileId = "p1", timestamp = 1000L)
        )

        val entries = repository.getByExpense("exp1")

        assertEquals(2, entries.size)
        assertEquals(AuditAction.CREATED, entries[0].action)
        assertEquals(AuditAction.UPDATED, entries[1].action)
    }

    // ── Append-only ──────────────────────────────────────────────────────────

    @Test
    fun `entries are append-only — multiple records accumulate`() = runTest {
        repository.recordEntry(
            ExpenseAuditEntry(eventId = "evt1", expenseId = "exp1", action = AuditAction.CREATED, profileId = "p1")
        )
        repository.recordEntry(
            ExpenseAuditEntry(eventId = "evt1", expenseId = "exp1", action = AuditAction.UPDATED, profileId = "p2")
        )
        repository.recordEntry(
            ExpenseAuditEntry(eventId = "evt1", expenseId = "exp1", action = AuditAction.UPDATED, profileId = "p3")
        )

        val entries = repository.getByExpense("exp1")

        assertEquals(3, entries.size)
    }

    // ── Delete by event ──────────────────────────────────────────────────────

    @Test
    fun `deleteByEvent removes all audit entries for event`() = runTest {
        repository.recordEntry(
            ExpenseAuditEntry(eventId = "evt1", expenseId = "exp1", action = AuditAction.CREATED, profileId = "p1")
        )
        repository.recordEntry(
            ExpenseAuditEntry(eventId = "evt1", expenseId = "exp2", action = AuditAction.CREATED, profileId = "p1")
        )
        repository.recordEntry(
            ExpenseAuditEntry(eventId = "evt2", expenseId = "exp3", action = AuditAction.CREATED, profileId = "p1")
        )

        repository.deleteByEvent("evt1")

        assertEquals(0, repository.getByEvent("evt1").size)
        assertEquals(1, repository.getByEvent("evt2").size)
    }

    @Test
    fun `getByEvent returns empty for event with no entries`() = runTest {
        assertTrue(repository.getByEvent("nonexistent").isEmpty())
    }
}
