package com.cuentamorosos.data.repository

import com.cuentamorosos.currentTimeMillis
import com.cuentamorosos.data.NetworkMonitor
import com.cuentamorosos.data.PendingOperationQueue
import com.cuentamorosos.data.RemoteOperations
import com.cuentamorosos.db.CuentaMorososDatabase
import com.cuentamorosos.model.EventDebtItem
import com.cuentamorosos.model.SettlementTransfer
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val applyMutex = Mutex()
    private val applyingEvents = mutableSetOf<String>()
    private val pendingEventDeletes = mutableSetOf<String>()

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
                    // 0. Drain pending event deletes FIRST (Fix 8)
                    if (pendingEventDeletes.isNotEmpty()) {
                        val toRetry = pendingEventDeletes.toSet()
                        for (eventId in toRetry) {
                            try {
                                remoteRepository.deleteAllDebtsForEvent(eventId)
                                pendingEventDeletes.remove(eventId)
                            } catch (e: Exception) {
                                println("[OfflineFirstDebtRepo] pending delete retry FAILED for $eventId")
                            }
                        }
                    }

                    // 1. Drain pending operations FIRST
                    pendingQueue.drainAll(debtRemoteOps)

                    // 2. One-shot initial fetch — populates cache BEFORE snapshot listener
                    val initialDebts = withTimeoutOrNull(15_000) {
                        remoteRepository.fetchAllDebts()
                    }
                    if (initialDebts != null) {
                        val filtered = initialDebts.filter { it.eventId !in applyingEvents }
                        if (filtered.size != initialDebts.size) {
                            println("[OfflineFirstDebtRepo] initial fetch filtered ${initialDebts.size - filtered.size} debts from applying events")
                        }
                        upsertDebts(filtered)
                        println("[OfflineFirstDebtRepo] Initial fetch: ${filtered.size} debts (from ${initialDebts.size} total)")
                    } else {
                        println("[OfflineFirstDebtRepo] Initial fetch timed out after 15s")
                    }

                    // 3. Then subscribe to realtime changes
                    remoteRepository.observeAllDebts()
                        .onEach { remoteDebts ->
                            val filtered = remoteDebts.filter { it.eventId !in applyingEvents }
                            if (filtered.size != remoteDebts.size) {
                                println("[OfflineFirstDebtRepo] filtered ${remoteDebts.size - filtered.size} debts from applying events")
                            }
                            println("[OfflineFirstDebtRepo] received ${remoteDebts.size} debts from Firestore, upserting ${filtered.size}")
                            upsertDebts(filtered)
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
                    creditorId = debt.creditorId,
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
            creditorId = debt.creditorId,
            amountEuros = debt.amountEuros,
            paid = if (debt.paid) 1 else 0,
            notes = debt.notes,
            calculationMode = debt.calculationMode,
            updatedAt = currentTimeMillis()
        )
        try {
            remoteRepository.saveDebt(debt)
        } catch (e: Exception) {
            println("[OfflineFirstDebtRepo] saveDebt remote FAILED for ${debt.id}: ${e.message}")
            e.printStackTrace()
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
            println("[OfflineFirstDebtRepo] deleteDebt remote FAILED for $debtId: ${e.message}")
            e.printStackTrace()
            pendingQueue.enqueue(
                id = "debt_${debtId}_${currentTimeMillis()}",
                entityType = "debt",
                entityId = debtId,
                operation = "delete",
                payload = ""
            )
        }
    }

    override suspend fun deleteAllDebtsForEvent(eventId: String) {
        queries.deleteByEvent(eventId)
        try {
            remoteRepository.deleteAllDebtsForEvent(eventId)
        } catch (e: Exception) {
            println("[OfflineFirstDebtRepo] deleteAllDebtsForEvent remote FAILED for $eventId: ${e.message}")
            e.printStackTrace()
            pendingEventDeletes.add(eventId)
        }
    }

    override suspend fun applyCalculation(
        eventId: String,
        modeId: String,
        transfers: List<SettlementTransfer>,
        paidTransferIndices: List<Int>,
    ) {
        println("🔍 [applyCalculation] eventId=$eventId transfers=${transfers.size}")
        transfers.forEachIndexed { i, t ->
            println("🔍   transfer[$i]: from=${t.fromProfileId} to=${t.toProfileId} amount=${t.amount}")
        }
        println("🔍 [applyCalculation] paidTransferIndices=$paidTransferIndices")

        var totalBefore = 0.0
        var totalAfter = 0.0
        val before = queries.selectByEvent(eventId).executeAsList()
        println("🔍 [applyCalculation] debts BEFORE delete: ${before.size}")
        before.forEach { d ->
            println("🔍   debt: ${d.profileId} -> ${d.creditorId} amount=${d.amountEuros} paid=${d.paid}")
            totalBefore += d.amountEuros
        }
        println("🔍 [applyCalculation] totalBefore=$totalBefore")

        withTimeoutOrNull(30_000L) {
            applyMutex.withLock {
                applyingEvents.add(eventId)
                try {
                    deleteAllDebtsForEvent(eventId)

                    transfers.forEachIndexed { index, transfer ->
                        val debt = EventDebtItem(
                            eventId = eventId,
                            profileId = transfer.fromProfileId,
                            creditorId = transfer.toProfileId,
                            amountEuros = transfer.amount,
                            calculationMode = modeId,
                            paid = index in paidTransferIndices,
                        )
                        println("🔍   saving debt: profile=${debt.profileId} creditor=${debt.creditorId} amount=${debt.amountEuros} paid=${debt.paid}")
                        saveDebt(debt)
                    }

                    val after = queries.selectByEvent(eventId).executeAsList()
                    println("🔍 [applyCalculation] debts AFTER apply: ${after.size}")
                    after.forEach { d ->
                        println("🔍   debt: ${d.profileId} -> ${d.creditorId} amount=${d.amountEuros} paid=${d.paid}")
                        totalAfter += d.amountEuros
                    }
                    println("🔍 [applyCalculation] totalAfter=$totalAfter")
                } finally {
                    applyingEvents.remove(eventId)
                }
            }
        }
    }

    override suspend fun deleteDebtsForProfile(profileId: String) {
        queries.deleteByProfile(profileId)
        try {
            remoteRepository.deleteDebtsForProfile(profileId)
        } catch (e: Exception) {
            println("[OfflineFirstDebtRepo] deleteDebtsForProfile remote FAILED for $profileId: ${e.message}")
            e.printStackTrace()
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
        creditorId = creditorId,
        amountEuros = amountEuros,
        notes = notes,
        paid = paid == 1L,
        calculationMode = calculationMode
    )
}
