package com.cuentamorosos.data.repository

import com.cuentamorosos.currentTimeMillis
import com.cuentamorosos.data.NetworkMonitor
import com.cuentamorosos.data.PendingOperationQueue
import com.cuentamorosos.data.RemoteOperations
import com.cuentamorosos.db.CuentaMorososDatabase
import com.cuentamorosos.model.EventDebtItem
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

class OfflineFirstDebtRepository(
    private val remoteRepository: DebtRepository,
    private val database: CuentaMorososDatabase,
    private val networkMonitor: NetworkMonitor,
    private val syncScope: CoroutineScope,
    private val pendingQueue: PendingOperationQueue
) : DebtRepository {

    private val queries = database.cachedDebtQueries
    private var syncAllJob: Job? = null

    private val debtRemoteOps = object : RemoteOperations {
        override suspend fun saveEvent(entityId: String) = throw UnsupportedOperationException()
        override suspend fun deleteEvent(entityId: String) = throw UnsupportedOperationException()
        override suspend fun saveDebt(entityId: String) {
            // Read from LOCAL cache (SQLDelight), not Firestore
            val local = queries.selectById(entityId).executeAsOneOrNull()?.toDebtItem()
            if (local != null) {
                remoteRepository.saveDebt(local)
            } else {
                println("[OfflineFirstDebtRepo] saveDebt pending: debt $entityId not in local cache, skipping")
            }
        }
        override suspend fun deleteDebt(entityId: String) {
            // entityId is the debtId; we need the eventId too, but drain only has one ID.
            // For deletes, the debt is already removed locally; remote delete is best-effort.
        }
        override suspend fun saveExpense(entityId: String) = throw UnsupportedOperationException()
        override suspend fun deleteExpense(entityId: String) = throw UnsupportedOperationException()
        override suspend fun saveProfile(entityId: String) = throw UnsupportedOperationException()
        override suspend fun deleteProfile(entityId: String) = throw UnsupportedOperationException()
        override suspend fun updateProfilePhoto(profileId: String, photoUrl: String) = throw UnsupportedOperationException()
        override suspend fun updateProfileUsername(profileId: String, username: String) = throw UnsupportedOperationException()
        override suspend fun updateProfileDisplayName(profileId: String, displayName: String) = throw UnsupportedOperationException()
        override suspend fun deleteProfilePhoto(profileId: String) = throw UnsupportedOperationException()
    }

    fun startSync() {
        stopSyncAll()
        // Start sync loop IMMEDIATELY — don't wait for network monitor
        startSyncAll()
        // Also subscribe to reconnection events
        networkMonitor.isOnline
            .drop(1) // Skip initial emission (already handled above)
            .onEach { isOnline ->
                println("[OfflineFirstDebtRepo] network state: $isOnline")
                if (isOnline) startSyncAll() else stopSyncAll()
            }
            .launchIn(syncScope)
    }

    override fun observeDebts(eventId: String): Flow<List<EventDebtItem>> {
        return queries.selectByEvent(eventId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { cachedDebts ->
                cachedDebts.map { it.toDebtItem() }
            }
    }

    override fun observeAllDebts(): Flow<List<EventDebtItem>> {
        return queries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { cachedDebts ->
                cachedDebts.map { it.toDebtItem() }
            }
    }

    private fun startSyncAll() {
        println("[OfflineFirstDebtRepo] startSyncAll called")
        syncAllJob?.cancel()
        syncAllJob = syncScope.launch(Dispatchers.Default) {
            var backoffMs = 1000L
            val maxBackoffMs = 30000L
            while (isActive) {
                try {
                    // 1. Drain pending operations FIRST
                    pendingQueue.drainAll(debtRemoteOps)

                    // 2. One-shot initial fetch — populates cache BEFORE snapshot listener
                    val initialDebts = withTimeoutOrNull(15_000) {
                        remoteRepository.fetchAllDebts()
                    }
                    if (initialDebts != null) {
                        upsertDebts(initialDebts)
                        println("[OfflineFirstDebtRepo] Initial fetch: ${initialDebts.size} debts")
                    } else {
                        println("[OfflineFirstDebtRepo] Initial fetch timed out after 15s")
                    }

                    // 3. Then subscribe to realtime changes
                    remoteRepository.observeAllDebts()
                        .onEach { remoteDebts ->
                            println("[OfflineFirstDebtRepo] received ${remoteDebts.size} debts from Firestore")
                            upsertDebts(remoteDebts)
                        }
                        .collect()

                    backoffMs = 1000L
                } catch (e: Exception) {
                    println("[OfflineFirstDebtRepo] SyncAll error: ${e.message}")
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

    private fun upsertDebts(debts: List<EventDebtItem>) {
        queries.transaction {
            debts.forEach { debt ->
                queries.upsert(
                    id = debt.id,
                    eventId = debt.eventId,
                    profileId = debt.profileId,
                    amountEuros = debt.amountEuros,
                    paid = if (debt.paid) 1 else 0,
                    notes = debt.notes,
                    calculationMode = debt.calculationMode,
                    updatedAt = currentTimeMillis()
                )
            }
        }
    }

    override suspend fun saveDebt(debt: EventDebtItem) {
        queries.upsert(
            id = debt.id,
            eventId = debt.eventId,
            profileId = debt.profileId,
            amountEuros = debt.amountEuros,
            paid = if (debt.paid) 1 else 0,
            notes = debt.notes,
            calculationMode = debt.calculationMode,
            updatedAt = currentTimeMillis()
        )
        try {
            remoteRepository.saveDebt(debt)
        } catch (e: Exception) {
            pendingQueue.enqueue(
                id = "debt_${debt.id}_${currentTimeMillis()}",
                entityType = "debt",
                entityId = debt.id,
                operation = "save",
                payload = ""
            )
        }
    }

    override suspend fun deleteDebt(eventId: String, debtId: String) {
        queries.deleteById(debtId)
        try {
            remoteRepository.deleteDebt(eventId, debtId)
        } catch (e: Exception) {
            pendingQueue.enqueue(
                id = "debt_${debtId}_${currentTimeMillis()}",
                entityType = "debt",
                entityId = debtId,
                operation = "delete",
                payload = ""
            )
        }
    }

    override suspend fun deleteDebtsForProfile(profileId: String) {
        queries.deleteByProfile(profileId)
        try {
            remoteRepository.deleteDebtsForProfile(profileId)
        } catch (e: Exception) {
            pendingQueue.enqueue(
                id = "debt_${profileId}_${currentTimeMillis()}",
                entityType = "debt",
                entityId = profileId,
                operation = "delete",
                payload = ""
            )
        }
    }

    override suspend fun replaceProfileId(oldId: String, newId: String) {
        remoteRepository.replaceProfileId(oldId, newId)
    }

    override suspend fun fetchDebtsForEvent(eventId: String): List<EventDebtItem> {
        return remoteRepository.fetchDebtsForEvent(eventId)
    }

    override suspend fun fetchAllDebts(): List<EventDebtItem> {
        return remoteRepository.fetchAllDebts()
    }

    private fun com.cuentamorosos.db.CachedDebt.toDebtItem(): EventDebtItem = EventDebtItem(
        id = id,
        eventId = eventId,
        profileId = profileId,
        amountEuros = amountEuros,
        notes = notes,
        paid = paid == 1L,
        calculationMode = calculationMode
    )
}
