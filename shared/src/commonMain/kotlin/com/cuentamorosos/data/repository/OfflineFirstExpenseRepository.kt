package com.cuentamorosos.data.repository

import com.cuentamorosos.currentTimeMillis
import com.cuentamorosos.data.NetworkMonitor
import com.cuentamorosos.data.PendingOperationQueue
import com.cuentamorosos.data.RemoteOperations
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
import kotlinx.coroutines.withTimeoutOrNull

class OfflineFirstExpenseRepository(
    private val remoteRepository: ExpenseRepository,
    private val database: CuentaMorososDatabase,
    private val networkMonitor: NetworkMonitor,
    private val syncScope: CoroutineScope,
    private val pendingQueue: PendingOperationQueue
) : ExpenseRepository {

    private val queries = database.cachedExpenseQueries
    private var syncAllJob: Job? = null

    private val expenseRemoteOps = object : RemoteOperations {
        override suspend fun saveEvent(entityId: String) = throw UnsupportedOperationException()
        override suspend fun deleteEvent(entityId: String) = throw UnsupportedOperationException()
        override suspend fun saveDebt(entityId: String) = throw UnsupportedOperationException()
        override suspend fun deleteDebt(entityId: String) = throw UnsupportedOperationException()
        override suspend fun saveExpense(entityId: String) {
            // Read from LOCAL cache (SQLDelight), not Firestore
            val local = queries.selectById(entityId).executeAsOneOrNull()?.toExpenseItem()
            if (local != null) {
                remoteRepository.saveExpense(local)
            } else {
                println("[OfflineFirstExpenseRepo] saveExpense pending: expense $entityId not in local cache, skipping")
            }
        }
        override suspend fun deleteExpense(entityId: String) {
            // entityId is the expenseId; remote delete is best-effort
        }
        override suspend fun saveProfile(entityId: String) = throw UnsupportedOperationException()
        override suspend fun deleteProfile(entityId: String) = throw UnsupportedOperationException()
        override suspend fun updateProfilePhoto(profileId: String, photoUrl: String) = throw UnsupportedOperationException()
        override suspend fun updateProfileUsername(profileId: String, username: String) = throw UnsupportedOperationException()
        override suspend fun updateProfileDisplayName(profileId: String, displayName: String) = throw UnsupportedOperationException()
        override suspend fun deleteProfilePhoto(profileId: String) = throw UnsupportedOperationException()
        override suspend fun linkGhostProfile(email: String, realUid: String) = throw UnsupportedOperationException()
    }

    fun startSync() {
        stopSyncAll()
        // Start sync loop IMMEDIATELY — don't wait for network monitor
        startSyncAll()
        // Also subscribe to reconnection events
        networkMonitor.isOnline
            .drop(1) // Skip initial emission (already handled above)
            .onEach { isOnline ->
                if (isOnline) startSyncAll() else stopSyncAll()
            }
            .launchIn(syncScope)
    }

    override fun observeExpenses(eventId: String): Flow<List<EventExpenseItem>> {
        return queries.selectByEvent(eventId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { cachedExpenses ->
                cachedExpenses.map { it.toExpenseItem() }
            }
    }

    override fun observeAllExpenses(): Flow<List<EventExpenseItem>> {
        return queries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { cachedExpenses ->
                cachedExpenses.map { it.toExpenseItem() }
            }
    }

    private fun startSyncAll() {
        syncAllJob?.cancel()
        syncAllJob = syncScope.launch(Dispatchers.Default) {
            var backoffMs = 1000L
            val maxBackoffMs = 30000L
            while (isActive) {
                try {
                    // 1. Drain pending operations FIRST
                    pendingQueue.drainAll(expenseRemoteOps)

                    // 2. One-shot initial fetch — populates cache BEFORE snapshot listener
                    val initialExpenses = withTimeoutOrNull(15_000) {
                        remoteRepository.fetchAllExpenses()
                    }
                    if (initialExpenses != null) {
                        upsertExpenses(initialExpenses)
                        println("[OfflineFirstExpenseRepo] Initial fetch: ${initialExpenses.size} expenses")
                    } else {
                        println("[OfflineFirstExpenseRepo] Initial fetch timed out after 15s")
                    }

                    // 3. Then subscribe to realtime changes
                    remoteRepository.observeAllExpenses()
                        .onEach { remoteExpenses ->
                            upsertExpenses(remoteExpenses)
                        }
                        .collect()

                    backoffMs = 1000L
                } catch (e: Exception) {
                    println("[OfflineFirstExpenseRepo] SyncAll error: ${e.message}")
                    delay(backoffMs)
                    backoffMs = minOf(backoffMs * 2, maxBackoffMs)
                }
            }
        }
    }

    private fun stopSyncAll() {
        syncAllJob?.cancel()
        syncAllJob = null
    }

    private fun upsertExpenses(expenses: List<EventExpenseItem>) {
        queries.transaction {
            expenses.forEach { expense ->
                queries.upsert(
                    id = expense.id,
                    eventId = expense.eventId,
                    description = expense.name,
                    amountEuros = expense.amountEuros,
                    category = expense.category,
                    paidByProfileId = expense.paidByProfileId,
                    dateMillis = expense.createdAtMillis,
                    updatedAt = currentTimeMillis(),
                    debtor_ids = expense.debtorIds.toJsonArray(),
                    payer_contributions = expense.payerContributions.toJsonObject(),
                    assigned_profile_ids = expense.assignedProfileIds.toJsonArray(),
                    profile_weights = expense.profileWeights.toJsonObject(),
                    split_mode = expense.splitMode
                )
            }
        }
    }

    override suspend fun saveExpense(expense: EventExpenseItem) {
        queries.upsert(
            id = expense.id,
            eventId = expense.eventId,
            description = expense.name,
            amountEuros = expense.amountEuros,
            category = expense.category,
            paidByProfileId = expense.paidByProfileId,
            dateMillis = expense.createdAtMillis,
            updatedAt = currentTimeMillis(),
            debtor_ids = expense.debtorIds.toJsonArray(),
            payer_contributions = expense.payerContributions.toJsonObject(),
            assigned_profile_ids = expense.assignedProfileIds.toJsonArray(),
            profile_weights = expense.profileWeights.toJsonObject(),
            split_mode = expense.splitMode
        )
        try {
            remoteRepository.saveExpense(expense)
        } catch (e: Exception) {
            println("[OfflineFirstExpenseRepo] saveExpense remote FAILED for ${expense.id}: ${e.message}")
            e.printStackTrace()
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
            println("[OfflineFirstExpenseRepo] deleteExpense remote FAILED for $expenseId: ${e.message}")
            e.printStackTrace()
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

    override suspend fun fetchExpensesForEvent(eventId: String): List<EventExpenseItem> {
        return remoteRepository.fetchExpensesForEvent(eventId)
    }

    override suspend fun fetchAllExpenses(): List<EventExpenseItem> {
        return remoteRepository.fetchAllExpenses()
    }

    private fun List<String>.toJsonArray(): String =
        joinToString(prefix = "[", postfix = "]", separator = ",") { "\"$it\"" }

    private fun Map<String, Double>.toJsonObject(): String =
        entries.joinToString(prefix = "{", postfix = "}", separator = ",") { (k, v) -> "\"$k\":$v" }

    private fun String?.toStringArray(): List<String> =
        if (isNullOrBlank()) emptyList()
        else removeSurrounding("[", "]").split(",").map { it.trim().removeSurrounding("\"") }.filter { it.isNotEmpty() }

    private fun String?.toMapDouble(): Map<String, Double> =
        if (isNullOrBlank()) emptyMap()
        else {
            val inner = removeSurrounding("{", "}").trim()
            if (inner.isEmpty()) emptyMap()
            else inner.split(",").associate {
                val parts = it.split(":", limit = 2)
                val key = parts[0].trim().removeSurrounding("\"")
                val value = parts.getOrNull(1)?.trim()?.toDoubleOrNull() ?: 0.0
                key to value
            }
        }

    private fun com.cuentamorosos.db.CachedExpense.toExpenseItem(): EventExpenseItem = EventExpenseItem(
        id = id,
        eventId = eventId,
        name = description,
        amountEuros = amountEuros,
        category = category,
        assignedProfileIds = assigned_profile_ids.toStringArray().let {
            if (it.isEmpty() && paidByProfileId.isNotBlank()) listOf(paidByProfileId) else it
        },
        profileWeights = profile_weights.toMapDouble(),
        paidByProfileId = paidByProfileId,
        splitMode = split_mode ?: "SIMPLE_AVG",
        payerContributions = payer_contributions.toMapDouble(),
        debtorIds = debtor_ids.toStringArray(),
        exchangeRate = null,
        itemCurrency = null,
        createdAtMillis = dateMillis,
        createdByProfileId = ""
    )
}
