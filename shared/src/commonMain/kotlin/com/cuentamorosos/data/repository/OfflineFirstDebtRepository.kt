package com.cuentamorosos.data.repository

import com.cuentamorosos.currentTimeMillis
import com.cuentamorosos.data.NetworkMonitor
import com.cuentamorosos.data.PendingOperationQueue
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

class OfflineFirstDebtRepository(
    private val remoteRepository: DebtRepository,
    private val database: CuentaMorososDatabase,
    private val networkMonitor: NetworkMonitor,
    private val syncScope: CoroutineScope,
    private val pendingQueue: PendingOperationQueue
) : DebtRepository {

    private val queries = database.cachedDebtQueries
    private var syncJob: Job? = null

    override fun observeDebts(eventId: String): Flow<List<EventDebtItem>> {
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

        // Single Source of Truth: Local cache
        return queries.selectByEvent(eventId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { cachedDebts ->
                cachedDebts.map { it.toDebtItem() }
            }
    }

    override fun observeAllDebts(): Flow<List<EventDebtItem>> {
        // Observe network state and control sync
        networkMonitor.isOnline
            .onEach { isOnline ->
                if (isOnline) {
                    startSyncAll()
                } else {
                    stopSync()
                }
            }
            .launchIn(syncScope)

        return queries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { cachedDebts ->
                cachedDebts.map { it.toDebtItem() }
            }
    }

    private fun startSync(eventId: String) {
        syncJob?.cancel()
        syncJob = syncScope.launch(Dispatchers.Default) {
            var backoffMs = 1000L
            val maxBackoffMs = 30000L
            while (isActive) {
                try {
                    remoteRepository.observeDebts(eventId).collect { remoteDebts ->
                        queries.transaction {
                            remoteDebts.forEach { debt ->
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
                    backoffMs = 1000L
                } catch (e: Exception) {
                    println("[OfflineFirstDebtRepo] Sync error: ${e.message}")
                    delay(backoffMs)
                    backoffMs = minOf(backoffMs * 2, maxBackoffMs)
                }
            }
        }
    }

    private fun startSyncAll() {
        syncJob?.cancel()
        syncJob = syncScope.launch(Dispatchers.Default) {
            var backoffMs = 1000L
            val maxBackoffMs = 30000L
            while (isActive) {
                try {
                    remoteRepository.observeAllDebts().collect { remoteDebts ->
                        queries.transaction {
                            remoteDebts.forEach { debt ->
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
                    backoffMs = 1000L
                } catch (e: Exception) {
                    println("[OfflineFirstDebtRepo] SyncAll error: ${e.message}")
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
