package com.cuentamorosos.data.repository

import com.cuentamorosos.currentTimeMillis
import com.cuentamorosos.db.CuentaMorososDatabase
import com.cuentamorosos.model.EventExpenseItem
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class OfflineFirstExpenseRepository(
    private val remoteRepository: ExpenseRepository,
    private val database: CuentaMorososDatabase,
    private val scope: CoroutineScope
) : ExpenseRepository {

    private val queries = database.cachedExpenseQueries

    override fun observeExpenses(eventId: String): Flow<List<EventExpenseItem>> {
        scope.launch(Dispatchers.Default) {
            remoteRepository.observeExpenses(eventId)
                .collect { remoteExpenses ->
                    queries.transaction {
                        remoteExpenses.forEach { expense ->
                            queries.upsert(
                                id = expense.id,
                                eventId = expense.eventId,
                                description = expense.name,
                                amountEuros = expense.amountEuros,
                                category = expense.category,
                                paidByProfileId = expense.assignedProfileIds.firstOrNull() ?: "",
                                dateMillis = 0L, // Not in model, using placeholder
                                updatedAt = currentTimeMillis()
                            )
                        }
                    }
                }
        }

        return queries.selectByEvent(eventId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { cachedExpenses ->
                cachedExpenses.map { it.toExpenseItem() }
            }
    }

    override suspend fun saveExpense(expense: EventExpenseItem) {
        queries.upsert(
            id = expense.id,
            eventId = expense.eventId,
            description = expense.name,
            amountEuros = expense.amountEuros,
            category = expense.category,
            paidByProfileId = expense.assignedProfileIds.firstOrNull() ?: "",
            dateMillis = 0L,
            updatedAt = currentTimeMillis()
        )
        remoteRepository.saveExpense(expense)
    }

    override suspend fun deleteExpense(eventId: String, expenseId: String) {
        queries.deleteById(expenseId)
        remoteRepository.deleteExpense(eventId, expenseId)
    }

    override suspend fun replaceProfileId(oldId: String, newId: String) {
        remoteRepository.replaceProfileId(oldId, newId)
    }

    private fun com.cuentamorosos.db.CachedExpense.toExpenseItem(): EventExpenseItem = EventExpenseItem(
        id = id,
        eventId = eventId,
        name = description,
        amountEuros = amountEuros,
        category = category,
        assignedProfileIds = listOf(paidByProfileId).filter { it.isNotBlank() },
        profileWeights = emptyMap() // Weights not cached for now to avoid complexity
    )
}
