package com.cuentamorosos.data.repository

import com.cuentamorosos.currentTimeMillis
import com.cuentamorosos.data.NetworkMonitor
import com.cuentamorosos.data.PendingOperationQueue
import com.cuentamorosos.db.CuentaMorososDatabase
import com.cuentamorosos.model.ProfileItem
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class OfflineFirstProfileRepository(
    private val remoteRepository: ProfileRepository,
    private val database: CuentaMorososDatabase,
    private val networkMonitor: NetworkMonitor,
    private val syncScope: CoroutineScope,
    private val pendingQueue: PendingOperationQueue
) : ProfileRepository {

    private val queries = database.cachedProfileQueries
    private var syncJob: Job? = null

    override fun observeProfiles(): Flow<List<ProfileItem>> {
        // Observe network state and control sync
        networkMonitor.isOnline
            .onEach { isOnline ->
                if (isOnline) {
                    startSync()
                } else {
                    stopSync()
                }
            }
            .launchIn(syncScope)

        return queries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { cachedProfiles ->
                cachedProfiles.map { it.toProfileItem() }
            }
    }

    private fun startSync() {
        syncJob?.cancel()
        syncJob = syncScope.launch(Dispatchers.Default) {
            var backoffMs = 1000L
            val maxBackoffMs = 30000L
            while (isActive) {
                try {
                    remoteRepository.observeProfiles().collect { remoteProfiles ->
                        queries.transaction {
                            remoteProfiles.forEach { profile ->
                                queries.upsert(
                                    id = profile.id,
                                    name = profile.name,
                                    icon = profile.icon,
                                    email = profile.linkedEmail ?: "",
                                    isGhost = if (profile.isGhost) 1 else 0,
                                    totalPendingEuros = profile.totalPendingEuros,
                                    updatedAt = currentTimeMillis()
                                )
                            }
                        }
                    }
                    backoffMs = 1000L
                } catch (e: Exception) {
                    println("[OfflineFirstProfileRepo] Sync error: ${e.message}")
                    delay(backoffMs)
                    backoffMs = minOf(backoffMs * 2, maxBackoffMs)
                }
            }
        }
    }

    private fun stopSync() {
        syncJob?.cancel()
        syncJob = null
    }

    override suspend fun saveProfile(profile: ProfileItem) {
        queries.upsert(
            id = profile.id,
            name = profile.name,
            icon = profile.icon,
            email = profile.linkedEmail ?: "",
            isGhost = if (profile.isGhost) 1 else 0,
            totalPendingEuros = profile.totalPendingEuros,
            updatedAt = currentTimeMillis()
        )
        try {
            remoteRepository.saveProfile(profile)
        } catch (e: Exception) {
            pendingQueue.enqueue(
                id = "profile_${profile.id}_${currentTimeMillis()}",
                entityType = "profile",
                entityId = profile.id,
                operation = "save",
                payload = ""
            )
        }
    }

    override suspend fun deleteProfile(profileId: String) {
        queries.deleteById(profileId)
        try {
            remoteRepository.deleteProfile(profileId)
        } catch (e: Exception) {
            pendingQueue.enqueue(
                id = "profile_${profileId}_${currentTimeMillis()}",
                entityType = "profile",
                entityId = profileId,
                operation = "delete",
                payload = ""
            )
        }
    }

    override suspend fun linkGhostProfile(userEmail: String, userUid: String) {
        remoteRepository.linkGhostProfile(userEmail, userUid)
    }

    private fun com.cuentamorosos.db.CachedProfile.toProfileItem(): ProfileItem = ProfileItem(
        id = id,
        name = name,
        icon = icon,
        totalPendingEuros = totalPendingEuros,
        isGhost = isGhost == 1L,
        linkedEmail = if (email.isBlank()) null else email
    )
}
