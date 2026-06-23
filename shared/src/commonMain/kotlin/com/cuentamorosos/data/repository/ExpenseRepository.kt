package com.cuentamorosos.data.repository

import com.cuentamorosos.model.EventExpenseItem
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun observeExpenses(eventId: String): Flow<List<EventExpenseItem>>
    fun observeAllExpenses(): Flow<List<EventExpenseItem>>
    suspend fun saveExpense(expense: EventExpenseItem)
    suspend fun deleteExpense(eventId: String, expenseId: String)
    suspend fun replaceProfileId(oldId: String, newId: String)
    suspend fun fetchExpensesForEvent(eventId: String): List<EventExpenseItem>
    /** One-shot fetch of ALL expenses for the current user across all events. */
    suspend fun fetchAllExpenses(): List<EventExpenseItem>
    /** Elimina todos los gastos de un evento de forma atómica. */
    suspend fun deleteAllExpensesForEvent(eventId: String)
}
