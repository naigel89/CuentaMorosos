package com.cuentamorosos.data.repository

import com.cuentamorosos.model.EventExpenseItem
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun observeExpenses(eventId: String): Flow<List<EventExpenseItem>>
    fun observeAllExpenses(): Flow<List<EventExpenseItem>>
    suspend fun saveExpense(expense: EventExpenseItem)
    suspend fun deleteExpense(eventId: String, expenseId: String)
    suspend fun replaceProfileId(oldId: String, newId: String)
}
