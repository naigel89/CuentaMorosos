package com.cuentamorosos.data.repository

import com.cuentamorosos.model.AdjustmentEntry

/** Repository for adjustment entries that rectify paid transfers. Original debts remain immutable. */
interface AdjustmentRepository {
    fun getAll(): List<AdjustmentEntry>
    fun getByEvent(eventId: String): List<AdjustmentEntry>
    fun getByDebt(eventId: String, transferKey: String): List<AdjustmentEntry>
    fun computeNetEffectCents(originalCents: Int, eventId: String, transferKey: String): Int
    suspend fun createEntry(entry: AdjustmentEntry)
    suspend fun deleteByEvent(eventId: String)
}
