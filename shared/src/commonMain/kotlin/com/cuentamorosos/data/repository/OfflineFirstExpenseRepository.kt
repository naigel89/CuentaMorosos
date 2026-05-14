package com.cuentamorosos.data.repository

import com.cuentamorosos.currentTimeMillis
import com.cuentamorosos.data.NetworkMonitor
import com.cuentamorosos.data.PendingOperationQueue
import com.cuentamorosos.db.CuentaMorososDatabase
import com.cuentamorosos.model.EventExpenseItem
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class OfflineFirstExpenseRepository(
    private val remoteRepository: ExpenseRepository,
    private val database: CuentaMorososDatabase,
    private val networkMonitor: NetworkMonitor,
    private val syncScope: CoroutineScope,
    private val pendingQueue: PendingOperationQueue
) : ExpenseRepository {

    private val queries = database.cachedExpenseQueries
    private var syncJob: Job? = null

    override fun observeExpenses(eventId: String): Flow<List<EventExpenseItem>> {
        // Observe network state and control sync
        networkMonitor.isOnline
            .onEach { isOnline ->
                if (isOnline) {
                    startSync(eventId)
                } else {
                    stopSync()
                }
            }
            .launchIn(syncScope)

        return queries.selectByEvent(eventId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { cachedExpenses ->
                cachedExpenses.map { it.toExpenseItem() }
            }
    }

    private fun startSync(eventId: String) {
        syncJob?.cancel()
        syncJob = syncScope.launch(Dispatchers.Default) {
            var backoffMs = 1000L
            val maxBackoffMs = 30000L
            while (isActive) {
                try {
                    remoteRepository.observeExpenses(eventId).collect { remoteExpenses ->
                        queries.transaction {
                            remoteExpenses.forEach { expense ->
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
                            }
                        }
                    }
                    backoffMs = 1000L
                } catch (e: Exception) {
                    println("[OfflineFirstExpenseRepo] Sync error: ${e.message}")
                    delay(backoffMs)
                    backoffMs = minOf(backoffMs * 2, maxBackoffMs)
                }
            }
        }
    }

    private fun stopSync() {
        syncJob?.cancel()
        syncJob = null
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
        try {
            remoteRepository.saveExpense(expense)
        } catch (e: Exception) {
            pendingQueue.enqueue(
                id = "expense_${expense.id}_${currentTimeMillis()}",
                entityType = "expense",
                entityId = expense.id,
                operation = "save",
                payload = ""
            )
        }
    }

    override suspend fun deleteExpense(eventId: String, expenseId: String) {
        queries.deleteById(expenseId)
        try {
            remoteRepository.deleteExpense(eventId, expenseId)
        } catch (e: Exception) {
            pendingQueue.enqueue(
                id = "expense_${expenseId}_${currentTimeMillis()}",
                entityType = "expense",
                entityId = expenseId,
                operation = "delete",
                payload = ""
            )
        }
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
        profileWeights = emptyMap()
    )
}
