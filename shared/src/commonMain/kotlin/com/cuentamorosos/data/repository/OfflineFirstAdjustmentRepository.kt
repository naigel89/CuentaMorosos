package com.cuentamorosos.data.repository

import com.cuentamorosos.db.CuentaMorososDatabase
import com.cuentamorosos.model.AdjustmentEntry

/**
 * SQLDelight-backed implementation of [AdjustmentRepository].
 * Adjustments rectify paid transfers without mutating the original debt.
 * Net effect = original amount + sum of all adjustment deltas for the target debt.
 */
class OfflineFirstAdjustmentRepository(
    private val database: CuentaMorososDatabase,
) : AdjustmentRepository {

    private val queries = database.cachedAdjustmentEntryQueries

    override fun getAll(): List<AdjustmentEntry> =
        queries.selectAll().executeAsList().map { it.toEntry() }

    override fun getByEvent(eventId: String): List<AdjustmentEntry> =
        queries.selectByEvent(eventId).executeAsList().map { it.toEntry() }

    override fun getByDebt(eventId: String, transferKey: String): List<AdjustmentEntry> =
        queries.selectByDebt(eventId, transferKey).executeAsList().map { it.toEntry() }

    override fun computeNetEffectCents(originalCents: Int, eventId: String, transferKey: String): Int {
        val totalDelta = queries.sumByDebt(eventId, transferKey).executeAsOne()
        return originalCents + totalDelta.toInt()
    }

    override suspend fun createEntry(entry: AdjustmentEntry) {
        queries.insertEntry(
            id = entry.id,
            eventId = entry.eventId,
            transferKey = entry.transferKey,
            deltaCents = entry.deltaCents.toLong(),
            reason = entry.reason,
            profileId = entry.profileId,
            timestamp = entry.timestamp,
        )
    }

    override suspend fun deleteByEvent(eventId: String) {
        queries.deleteByEvent(eventId)
    }

    private fun com.cuentamorosos.db.CachedAdjustmentEntry.toEntry(): AdjustmentEntry =
        AdjustmentEntry(
            id = id,
            eventId = eventId,
            transferKey = transferKey,
            deltaCents = deltaCents.toInt(),
            reason = reason,
            profileId = profileId,
            timestamp = timestamp,
        )
}
