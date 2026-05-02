package com.cuentamorosos.data.repository

import android.content.Context
import com.cuentamorosos.data.CuentaMorososLocalStore

object RepositoryProvider {
    private var store: CuentaMorososLocalStore? = null
    
    fun init(context: Context) {
        store = CuentaMorososLocalStore(context)
    }
    
    private fun getStore() = store ?: throw IllegalStateException("RepositoryProvider must be initialized with context before use")
    
    val eventRepository: EventRepository by lazy { FirestoreEventRepository() }
    val debtRepository: DebtRepository by lazy { FirestoreDebtRepository() }
    val expenseRepository: ExpenseRepository by lazy { FirestoreExpenseRepository() }
    val profileRepository: ProfileRepository by lazy { 
        CompositeProfileRepository(
            LocalProfileRepository(getStore()), 
            FirestoreProfileRepository(),
            debtRepository,
            expenseRepository,
            eventRepository
        ) 
    }
    val invitationRepository: InvitationRepository by lazy { FirestoreInvitationRepository() }
}
