package com.cuentamorosos.data.repository

import com.cuentamorosos.db.CuentaMorososDatabase
import com.cuentamorosos.model.AuditAction
import com.cuentamorosos.model.ExpenseAuditEntry

/**
 * SQLDelight-backed implementation of [AuditRepository].
 * Audit entries are append-only; individual entries cannot be modified or deleted.
 * Only cascade delete (by event) is supported.
 */
class OfflineFirstAuditRepository(
    private val database: CuentaMorososDatabase,
) : AuditRepository {

    private val queries = database.cachedAuditEntryQueries

    override fun getAll(): List<ExpenseAuditEntry> =
        queries.selectAll().executeAsList().map { it.toEntry() }

    override fun getByEvent(eventId: String): List<ExpenseAuditEntry> =
        queries.selectByEvent(eventId).executeAsList().map { it.toEntry() }

    override fun getByExpense(expenseId: String): List<ExpenseAuditEntry> =
        queries.selectByExpense(expenseId).executeAsList().map { it.toEntry() }

    override suspend fun recordEntry(entry: ExpenseAuditEntry) {
        queries.insertEntry(
            id = entry.id,
            eventId = entry.eventId,
            expenseId = entry.expenseId,
            action = entry.action.name,
            profileId = entry.profileId,
            timestamp = entry.timestamp,
            beforeSnapshot = entry.beforeSnapshot,
            afterSnapshot = entry.afterSnapshot,
        )
    }

    override suspend fun deleteByEvent(eventId: String) {
        queries.deleteByEvent(eventId)
    }

    private fun com.cuentamorosos.db.CachedAuditEntry.toEntry(): ExpenseAuditEntry =
        ExpenseAuditEntry(
            id = id,
            eventId = eventId,
            expenseId = expenseId,
            action = AuditAction.valueOf(action),
            profileId = profileId,
            timestamp = timestamp,
            beforeSnapshot = beforeSnapshot,
            afterSnapshot = afterSnapshot,
        )
}
