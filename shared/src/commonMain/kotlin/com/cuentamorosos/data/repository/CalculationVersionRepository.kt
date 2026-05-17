package com.cuentamorosos.data.repository

import com.cuentamorosos.model.CalculationVersion

/** Repository for versioned calculation snapshots. Versions are append-only and never overwritten. */
interface CalculationVersionRepository {
    fun getByEvent(eventId: String): List<CalculationVersion>
    fun getVersion(eventId: String, version: Int): CalculationVersion?
    fun getMaxVersion(eventId: String): Int
    suspend fun saveVersion(version: CalculationVersion): CalculationVersion
    suspend fun deactivateAll(eventId: String)
    suspend fun deleteByEvent(eventId: String)
}
