package com.cuentamorosos

import app.cash.sqldelight.db.SqlDriver
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

/**
 * Provides all repositories for the app.
 *
 * Creates Firestore (remote) repositories and wraps them with OfflineFirst
 * repositories that use SQLDelight for local caching.
 */
class RepositoryProvider(
    sqlDriver: SqlDriver,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val database = CuentaMorososDatabase(sqlDriver)

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
        scope = scope
    )

    val debtRepository: DebtRepository = OfflineFirstDebtRepository(
        remoteRepository = remoteDebtRepository,
        database = database,
        scope = scope
    )

    val expenseRepository: ExpenseRepository = OfflineFirstExpenseRepository(
        remoteRepository = remoteExpenseRepository,
        database = database,
        scope = scope
    )

    val profileRepository: ProfileRepository = OfflineFirstProfileRepository(
        remoteRepository = remoteProfileRepository,
        database = database,
        scope = scope
    )

    // Invitations are online-only (no offline cache needed)
    val invitationRepository: InvitationRepository = remoteInvitationRepository
}
