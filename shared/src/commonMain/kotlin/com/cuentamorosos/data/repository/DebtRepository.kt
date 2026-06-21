package com.cuentamorosos.data.repository

import com.cuentamorosos.model.EventDebtItem
import com.cuentamorosos.model.SettlementTransfer
import kotlinx.coroutines.flow.Flow

interface DebtRepository {
    fun observeDebts(eventId: String): Flow<List<EventDebtItem>>
    fun observeAllDebts(): Flow<List<EventDebtItem>>
    suspend fun saveDebt(debt: EventDebtItem)
    suspend fun deleteDebt(eventId: String, debtId: String)
    /** Elimina todas las deudas de un evento de forma atómica. */
    suspend fun deleteAllDebtsForEvent(eventId: String)
    /** Elimina todas las deudas asociadas a un perfil en todos los eventos. */
    suspend fun deleteDebtsForProfile(profileId: String)
    suspend fun replaceProfileId(oldId: String, newId: String)
    suspend fun fetchDebtsForEvent(eventId: String): List<EventDebtItem>
    /** One-shot fetch of ALL debts for the current user across all events. */
    suspend fun fetchAllDebts(): List<EventDebtItem>
    /**
     * Atomically replaces all debts for [eventId] with the given [transfers],
     * tagged with [modeId]. Implementations MUST guard against sync-loop
     * interference during the delete+create window.
     */
    suspend fun applyCalculation(
        eventId: String,
        modeId: String,
        transfers: List<SettlementTransfer>,
    )
}
