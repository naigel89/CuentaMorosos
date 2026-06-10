package com.cuentamorosos

import app.cash.sqldelight.db.SqlDriver
import com.cuentamorosos.data.NetworkMonitor
import com.cuentamorosos.data.PendingOperationQueue
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
