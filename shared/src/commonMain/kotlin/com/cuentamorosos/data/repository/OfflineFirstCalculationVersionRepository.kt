package com.cuentamorosos.data.repository

import com.cuentamorosos.db.CuentaMorososDatabase
import com.cuentamorosos.model.CalculationVersion

/**
 * SQLDelight-backed implementation of [CalculationVersionRepository].
 * Versions are append-only with monotonic numbering per event.
 * When saving a version with version=0, the next available number is auto-assigned.
 */
class OfflineFirstCalculationVersionRepository(
    private val database: CuentaMorososDatabase,
) : CalculationVersionRepository {

    private val queries = database.cachedCalculationVersionQueries

    override fun getByEvent(eventId: String): List<CalculationVersion> =
        queries.selectByEvent(eventId).executeAsList().map { it.toVersion() }

    override fun getVersion(eventId: String, version: Int): CalculationVersion? =
        queries.selectByVersion(eventId, version.toLong()).executeAsOneOrNull()?.toVersion()

    override fun getMaxVersion(eventId: String): Int {
        val result = queries.selectMaxVersion(eventId).executeAsOne()
        return result.maxVersion?.toInt() ?: 0
    }

    override suspend fun saveVersion(version: CalculationVersion): CalculationVersion {
        val assignedVersion = if (version.version == 0) {
            getMaxVersion(version.eventId) + 1
        } else {
            version.version
        }

        val toSave = version.copy(version = assignedVersion)
        queries.insertVersion(
            id = toSave.id,
            eventId = toSave.eventId,
            version = assignedVersion.toLong(),
            snapshotJson = toSave.snapshotJson,
            createdAt = toSave.createdAt,
            isActive = if (toSave.isActive) 1L else 0L,
        )
        return toSave
    }

    override suspend fun deactivateAll(eventId: String) {
        queries.updateIsActiveByEvent(eventId)
    }

    override suspend fun deleteByEvent(eventId: String) {
        queries.deleteByEvent(eventId)
    }

    private fun com.cuentamorosos.db.CachedCalculationVersion.toVersion(): CalculationVersion =
        CalculationVersion(
            id = id,
            eventId = eventId,
            version = version.toInt(),
            snapshotJson = snapshotJson,
            createdAt = createdAt,
            isActive = isActive != 0L,
        )
}
