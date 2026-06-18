package com.cuentamorosos.data

import app.cash.sqldelight.db.SqlDriver
import com.cuentamorosos.currentTimeMillis
import com.cuentamorosos.db.CuentaMorososDatabase
import com.cuentamorosos.db.PendingOperation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PendingOperationQueue(
    database: CuentaMorososDatabase,
    private val scope: CoroutineScope,
    private val maxRetries: Int = 5,
    private val baseDelayMs: Long = 1000,
    private val maxDelayMs: Long = 30000,
) {
    private val queries = database.pendingOperationQueries
    private val mutex = Mutex()

    suspend fun enqueue(
        id: String,
        entityType: String,
        entityId: String,
        operation: String,
        payload: String,
    ) = mutex.withLock {
        queries.insertOperation(
            id = id,
            entityType = entityType,
            entityId = entityId,
            operation = operation,
            payload = payload,
            createdAt = currentTimeMillis(),
            retryCount = 0L,
        )
    }

    suspend fun dequeue(limit: Int): List<PendingOperation> = mutex.withLock {
        queries.selectPending(maxRetries.toLong(), limit.toLong()).executeAsList()
    }

    suspend fun markComplete(id: String) = mutex.withLock {
        queries.deleteOperation(id)
    }

    suspend fun markFailed(id: String) = mutex.withLock {
        queries.updateRetryCount(id)
    }

    suspend fun getAllPending(): Long = mutex.withLock {
        queries.countPending(maxRetries.toLong()).executeAsOne()
    }

    suspend fun drain(
        remoteOps: RemoteOperations,
    ) {
        val pending = dequeue(10)
        for (op in pending) {
            try {
                when (op.operation) {
                    "save" -> {
                        when (op.entityType) {
                            "event" -> remoteOps.saveEvent(op.entityId)
                            "debt" -> remoteOps.saveDebt(op.entityId)
                            "expense" -> remoteOps.saveExpense(op.entityId)
                            "profile" -> remoteOps.saveProfile(op.entityId)
                        }
                    }
                    "delete" -> {
                        when (op.entityType) {
                            "event" -> remoteOps.deleteEvent(op.entityId)
                            "debt" -> remoteOps.deleteDebt(op.entityId)
                            "expense" -> remoteOps.deleteExpense(op.entityId)
                            "profile" -> remoteOps.deleteProfile(op.entityId)
                        }
                    }
                    "updatePhoto" -> remoteOps.updateProfilePhoto(op.entityId, op.payload)
                    "updateUsername" -> remoteOps.updateProfileUsername(op.entityId, op.payload)
                    "updateDisplayName" -> remoteOps.updateProfileDisplayName(op.entityId, op.payload)
                    "deletePhoto" -> remoteOps.deleteProfilePhoto(op.entityId)
                }
                markComplete(op.id)
            } catch (e: Exception) {
                println("[PendingOperationQueue] Drain failed for ${op.id}: ${e.message}")
                markFailed(op.id)
            }
        }
    }

    /**
     * Drains all pending operations in a loop until the queue is empty.
     * Processes operations in batches of 10 until getAllPending() returns 0.
     */
    suspend fun drainAll(
        remoteOps: RemoteOperations,
    ) {
        while (getAllPending() > 0) {
            drain(remoteOps)
        }
    }
}

/**
 * Interface for remote operations used by PendingOperationQueue.drain()
 */
interface RemoteOperations {
    suspend fun saveEvent(entityId: String)
    suspend fun deleteEvent(entityId: String)
    suspend fun saveDebt(entityId: String)
    suspend fun deleteDebt(entityId: String)
    suspend fun saveExpense(entityId: String)
    suspend fun deleteExpense(entityId: String)
    suspend fun saveProfile(entityId: String)
    suspend fun deleteProfile(entityId: String)
    suspend fun updateProfilePhoto(profileId: String, photoUrl: String)
    suspend fun updateProfileUsername(profileId: String, username: String)
    suspend fun updateProfileDisplayName(profileId: String, displayName: String)
    suspend fun deleteProfilePhoto(profileId: String)
}
