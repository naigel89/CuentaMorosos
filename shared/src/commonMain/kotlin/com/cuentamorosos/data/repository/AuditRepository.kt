package com.cuentamorosos.data.repository

import com.cuentamorosos.model.AuditAction
import com.cuentamorosos.model.ExpenseAuditEntry

/** Repository for expense audit trail entries. Append-only; entries are only removed via cascade delete. */
interface AuditRepository {
    fun getAll(): List<ExpenseAuditEntry>
    fun getByEvent(eventId: String): List<ExpenseAuditEntry>
    fun getByExpense(expenseId: String): List<ExpenseAuditEntry>
    suspend fun recordEntry(entry: ExpenseAuditEntry)
    suspend fun deleteByEvent(eventId: String)
}
