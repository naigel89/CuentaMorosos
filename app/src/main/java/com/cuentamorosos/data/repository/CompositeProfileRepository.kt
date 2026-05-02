package com.cuentamorosos.data.repository

import com.cuentamorosos.model.ProfileItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class CompositeProfileRepository(
    private val localRepository: LocalProfileRepository,
    private val remoteRepository: FirestoreProfileRepository,
    private val debtRepository: DebtRepository,
    private val expenseRepository: ExpenseRepository,
    private val eventRepository: EventRepository
) : ProfileRepository {

    override fun observeProfiles(): Flow<List<ProfileItem>> {
        return combine(
            localRepository.observeProfiles(),
            remoteRepository.observeProfiles()
        ) { local, remote ->
            // Remote profiles take precedence; local profiles only contribute
            // ghosts whose id is not already present in remote.
            val remoteIds = remote.map { it.id }.toHashSet()
            val localOnly = local.filter { it.isGhost && it.id !in remoteIds }
            (localOnly + remote).sortedBy { it.name.lowercase() }
        }
    }

    override suspend fun saveProfile(profile: ProfileItem) {
        if (profile.isGhost) {
            localRepository.saveProfile(profile)
        } else {
            remoteRepository.saveProfile(profile)
        }
    }

    override suspend fun deleteProfile(profileId: String) {
        localRepository.deleteProfile(profileId)
        remoteRepository.deleteProfile(profileId)
    }

    override suspend fun linkGhostProfile(userEmail: String, userUid: String) {
        val normalizedEmail = userEmail.trim().lowercase()
        if (normalizedEmail.isBlank()) return

        val ghosts = localRepository.getProfiles().filter {
            it.isGhost && it.linkedEmail?.trim()?.lowercase() == normalizedEmail
        }
        
        ghosts.forEach { ghost ->
            // 1. Create the real profile in Firestore
            val realProfile = ghost.copy(
                id = userUid,
                isGhost = false,
                linkedEmail = null
            )
            remoteRepository.saveProfile(realProfile)
            
            // 2. Replace references to the ghost ID with the real UID
            eventRepository.replaceMemberId(ghost.id, userUid)
            debtRepository.replaceProfileId(ghost.id, userUid)
            expenseRepository.replaceProfileId(ghost.id, userUid)
            
            // 3. Remove ghost from local storage
            localRepository.deleteProfile(ghost.id)
        }
    }
}
