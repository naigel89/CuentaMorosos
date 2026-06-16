package com.cuentamorosos.data.repository

import com.cuentamorosos.model.EventDebtItem
import kotlinx.coroutines.flow.Flow

interface DebtRepository {
    fun observeDebts(eventId: String): Flow<List<EventDebtItem>>
    fun observeAllDebts(): Flow<List<EventDebtItem>>
    suspend fun saveDebt(debt: EventDebtItem)
    suspend fun deleteDebt(eventId: String, debtId: String)
    /** Elimina todas las deudas asociadas a un perfil en todos los eventos. */
    suspend fun deleteDebtsForProfile(profileId: String)
    suspend fun replaceProfileId(oldId: String, newId: String)
    suspend fun fetchDebtsForEvent(eventId: String): List<EventDebtItem>
}
