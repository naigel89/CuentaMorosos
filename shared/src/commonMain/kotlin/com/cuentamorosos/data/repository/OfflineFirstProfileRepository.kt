package com.cuentamorosos.data.repository

import com.cuentamorosos.currentTimeMillis
import com.cuentamorosos.db.CuentaMorososDatabase
import com.cuentamorosos.model.ProfileItem
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class OfflineFirstProfileRepository(
    private val remoteRepository: ProfileRepository,
    private val database: CuentaMorososDatabase,
    private val scope: CoroutineScope
) : ProfileRepository {

    private val queries = database.cachedProfileQueries

    override fun observeProfiles(): Flow<List<ProfileItem>> {
        scope.launch(Dispatchers.Default) {
            remoteRepository.observeProfiles()
                .collect { remoteProfiles ->
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
        }

        return queries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { cachedProfiles ->
                cachedProfiles.map { it.toProfileItem() }
            }
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
        remoteRepository.saveProfile(profile)
    }

    override suspend fun deleteProfile(profileId: String) {
        queries.deleteById(profileId)
        remoteRepository.deleteProfile(profileId)
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
