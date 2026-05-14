package com.cuentamorosos.data.repository

import com.cuentamorosos.currentTimeMillis
import com.cuentamorosos.db.CuentaMorososDatabase
import com.cuentamorosos.model.EventDebtItem
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class OfflineFirstDebtRepository(
    private val remoteRepository: DebtRepository,
    private val database: CuentaMorososDatabase,
    private val scope: CoroutineScope
) : DebtRepository {

    private val queries = database.cachedDebtQueries

    override fun observeDebts(eventId: String): Flow<List<EventDebtItem>> {
        // Sync remote debts for this event in the background
        scope.launch(Dispatchers.Default) {
            remoteRepository.observeDebts(eventId)
                .collect { remoteDebts ->
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
        }

        // Single Source of Truth: Local cache
        return queries.selectByEvent(eventId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { cachedDebts ->
                cachedDebts.map { it.toDebtItem() }
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
        remoteRepository.saveDebt(debt)
    }

    override suspend fun deleteDebt(eventId: String, debtId: String) {
        queries.deleteById(debtId)
        remoteRepository.deleteDebt(eventId, debtId)
    }

    override suspend fun deleteDebtsForProfile(profileId: String) {
        queries.deleteByProfile(profileId)
        remoteRepository.deleteDebtsForProfile(profileId)
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
