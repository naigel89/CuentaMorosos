package com.cuentamorosos

import app.cash.sqldelight.db.SqlDriver
import com.cuentamorosos.data.NetworkMonitor
import com.cuentamorosos.data.PendingOperationQueue
import com.cuentamorosos.data.RemoteOperations
import com.cuentamorosos.data.repository.DebtRepository
import com.cuentamorosos.data.repository.EventRepository
import com.cuentamorosos.data.repository.ExpenseRepository
import com.cuentamorosos.data.repository.FirestoreDebtRepository
import com.cuentamorosos.data.repository.FirestoreEventRepository
import com.cuentamorosos.data.repository.FirestoreExpenseRepository
import com.cuentamorosos.data.repository.FirestoreInvitationRepository
import com.cuentamorosos.data.repository.FirestoreProfileRepository
import com.cuentamorosos.data.repository.InvitationRepository
import com.cuentamorosos.data.repository.OfflineFirstDebtRepository
import com.cuentamorosos.data.repository.OfflineFirstEventRepository
import com.cuentamorosos.data.repository.OfflineFirstExpenseRepository
import com.cuentamorosos.data.repository.OfflineFirstProfileRepository
import com.cuentamorosos.data.repository.ProfileRepository
import com.cuentamorosos.db.CuentaMorososDatabase
import com.cuentamorosos.model.EventDebtItem
import com.cuentamorosos.model.EventExpenseItem
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.EventState
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.deserializeParticipants
import com.cuentamorosos.model.migrateMemberIdsToParticipants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Provides all repositories for the app.
 *
 * Creates Firestore (remote) repositories and wraps them with OfflineFirst
 * repositories that use SQLDelight for local caching.
 */
class RepositoryProvider(
    sqlDriver: SqlDriver,
    private val networkMonitor: NetworkMonitor,
) {
    private val database = CuentaMorososDatabase(sqlDriver)
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val pendingQueue = PendingOperationQueue(database, syncScope)

    // Remote (Firestore) repositories — use Firebase singletons via dev.gitlive
    val remoteEventRepository: EventRepository = FirestoreEventRepository()
    val remoteDebtRepository: DebtRepository = FirestoreDebtRepository()
    val remoteExpenseRepository: ExpenseRepository = FirestoreExpenseRepository()
    val remoteProfileRepository: ProfileRepository = FirestoreProfileRepository()
    val remoteInvitationRepository: InvitationRepository = FirestoreInvitationRepository()

    // OfflineFirst repositories — wrap remote with local SQLDelight cache
    val eventRepository: EventRepository = OfflineFirstEventRepository(
        remoteRepository = remoteEventRepository,
        database = database,
        networkMonitor = networkMonitor,
        syncScope = syncScope,
        pendingQueue = pendingQueue,
    )

    val debtRepository: DebtRepository = OfflineFirstDebtRepository(
        remoteRepository = remoteDebtRepository,
        database = database,
        networkMonitor = networkMonitor,
        syncScope = syncScope,
        pendingQueue = pendingQueue,
    )

    val expenseRepository: ExpenseRepository = OfflineFirstExpenseRepository(
        remoteRepository = remoteExpenseRepository,
        database = database,
        networkMonitor = networkMonitor,
        syncScope = syncScope,
        pendingQueue = pendingQueue,
    )

    val profileRepository: ProfileRepository = OfflineFirstProfileRepository(
        remoteRepository = remoteProfileRepository,
        database = database,
        networkMonitor = networkMonitor,
        syncScope = syncScope,
        pendingQueue = pendingQueue,
    )

    // Invitations are online-only (no offline cache needed)
    val invitationRepository: InvitationRepository = remoteInvitationRepository

    /**
     * Start sync for all offline-first repositories with 500ms staggered delays.
     * Call this from MainActivity after first render.
     */
    fun startSyncStaggered(scope: CoroutineScope) {
        scope.launch(Dispatchers.Default) {
            try {
                println("[RepositoryProvider] Starting event sync...")
                (eventRepository as OfflineFirstEventRepository).startSync()
                println("[RepositoryProvider] Event sync started, waiting 500ms...")
                delay(500)
                println("[RepositoryProvider] Starting debt sync...")
                (debtRepository as OfflineFirstDebtRepository).startSync()
                println("[RepositoryProvider] Debt sync started, waiting 500ms...")
                delay(500)
                println("[RepositoryProvider] Starting expense sync...")
                (expenseRepository as OfflineFirstExpenseRepository).startSync()
                println("[RepositoryProvider] Expense sync started, waiting 500ms...")
                delay(500)
                println("[RepositoryProvider] Starting profile sync...")
                (profileRepository as OfflineFirstProfileRepository).startSync()
                println("[RepositoryProvider] All offline-first syncs started")
            } catch (e: Exception) {
                println("[RepositoryProvider] Sync start error: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Attempts to drain all pending operations to Firestore before clearing local data.
     * This is a last-chance sync on sign-out. If any operation fails, the data will be lost.
     */
    suspend fun drainAllBeforeLogout() {
        println("[RepositoryProvider] Draining pending operations before logout...")
        pendingQueue.drainAll(object : RemoteOperations {
            override suspend fun saveEvent(entityId: String) {
                val local = database.cachedEventQueries.selectById(entityId)
                    .executeAsOneOrNull()?.let { ev ->
                        migrateMemberIdsToParticipants(EventItem(
                            id = ev.id,
                            name = ev.name,
                            dateMillis = ev.dateMillis,
                            ownerId = ev.ownerId,
                            memberIds = if (ev.memberIds.isBlank()) emptyList() else ev.memberIds.split(","),
                            participants = deserializeParticipants(ev.participants),
                            baseCurrency = ev.base_currency,
                            lastCalculationMode = ev.lastCalculationMode,
                            lastCalculationTotal = ev.lastCalculationTotal,
                            lastCalculationTimestamp = ev.lastCalculationTimestamp,
                            lastCalculationSummary = ev.lastCalculationSummary,
                            startDateMillis = if (ev.startDateMillis == 0L) ev.dateMillis else ev.startDateMillis,
                            endDateMillis = if (ev.endDateMillis == 0L) ev.dateMillis else ev.endDateMillis,
                            state = runCatching { EventState.valueOf(ev.state) }.getOrDefault(EventState.OPEN)
                        ))
                    }
                if (local != null) remoteEventRepository.saveEvent(local)
            }
            override suspend fun deleteEvent(entityId: String) = remoteEventRepository.deleteEvent(entityId)
            override suspend fun saveDebt(entityId: String) {
                val local = database.cachedDebtQueries.selectById(entityId)
                    .executeAsOneOrNull()?.let { d ->
                        EventDebtItem(
                            id = d.id, eventId = d.eventId,
                            profileId = d.profileId, amountEuros = d.amountEuros,
                            notes = d.notes, paid = d.paid == 1L,
                            calculationMode = d.calculationMode
                        )
                    }
                if (local != null) remoteDebtRepository.saveDebt(local)
            }
            override suspend fun deleteDebt(entityId: String) = throw UnsupportedOperationException()
            override suspend fun saveExpense(entityId: String) {
                val local = database.cachedExpenseQueries.selectById(entityId)
                    .executeAsOneOrNull()?.let { ex ->
                        EventExpenseItem(
                            id = ex.id, eventId = ex.eventId, name = ex.description,
                            amountEuros = ex.amountEuros, category = ex.category,
                            paidByProfileId = ex.paidByProfileId,
                            assignedProfileIds = parseJsonStringArray(ex.assigned_profile_ids),
                            profileWeights = parseJsonDoubleMap(ex.profile_weights),
                            splitMode = ex.split_mode ?: "SIMPLE_AVG",
                            payerContributions = parseJsonDoubleMap(ex.payer_contributions),
                            debtorIds = parseJsonStringArray(ex.debtor_ids),
                            createdAtMillis = ex.dateMillis,
                        )
                    }
                if (local != null) remoteExpenseRepository.saveExpense(local)
            }
            override suspend fun deleteExpense(entityId: String) = throw UnsupportedOperationException()
            override suspend fun saveProfile(entityId: String) {
                val local = database.cachedProfileQueries.selectById(entityId)
                    .executeAsOneOrNull()?.let { p ->
                        ProfileItem(
                            id = p.id, name = p.name,
                            totalPendingEuros = p.totalPendingEuros,
                            isGhost = p.isGhost == 1L, linkedEmail = p.email,
                            ownerId = p.ownerId, photoUrl = p.photo_url,
                            username = p.username, displayName = p.display_name,
                        )
                    }
                if (local != null) remoteProfileRepository.saveProfile(local)
            }
            override suspend fun deleteProfile(entityId: String) = remoteProfileRepository.deleteProfile(entityId)
            override suspend fun updateProfilePhoto(profileId: String, photoUrl: String) { remoteProfileRepository.updateProfilePhoto(photoUrl) }
            override suspend fun updateProfileUsername(profileId: String, username: String) { remoteProfileRepository.updateUsername(username) }
            override suspend fun updateProfileDisplayName(profileId: String, displayName: String) { remoteProfileRepository.updateDisplayName(displayName) }
            override suspend fun deleteProfilePhoto(profileId: String) { remoteProfileRepository.deleteProfilePhoto() }
        })
        println("[RepositoryProvider] Pending operations drain complete")
    }

    /**
     * Clears all local cached data (SQLDelight tables).
     * Called on sign-out and before sync to prevent data leakage between users.
     */
    fun clearLocalData() {
        database.cachedEventQueries.deleteAll()
        database.cachedProfileQueries.deleteAll()
        database.cachedDebtQueries.deleteAll()
        database.cachedExpenseQueries.deleteAll()
        database.pendingOperationQueries.deleteAll()
    }
}

private fun parseJsonStringArray(raw: String?): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    return raw.removeSurrounding("[", "]").split(",")
        .map { it.trim().removeSurrounding("\"") }
        .filter { it.isNotEmpty() }
}

private fun parseJsonDoubleMap(raw: String?): Map<String, Double> {
    if (raw.isNullOrBlank()) return emptyMap()
    val inner = raw.removeSurrounding("{", "}").trim()
    if (inner.isEmpty()) return emptyMap()
    return inner.split(",").associate {
        val parts = it.split(":", limit = 2)
        val key = parts[0].trim().removeSurrounding("\"")
        val value = parts.getOrNull(1)?.trim()?.toDoubleOrNull() ?: 0.0
        key to value
    }
}
