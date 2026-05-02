package com.cuentamorosos.data.repository

import com.cuentamorosos.data.CuentaMorososLocalStore
import com.cuentamorosos.model.ProfileItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocalProfileRepository(private val store: CuentaMorososLocalStore) : ProfileRepository {
    private val _profiles = MutableStateFlow(store.loadProfiles())
    
    override fun observeProfiles(): Flow<List<ProfileItem>> = _profiles.asStateFlow()

    fun getProfiles(): List<ProfileItem> = _profiles.value

    override suspend fun saveProfile(profile: ProfileItem) {
        val current = store.loadProfiles().toMutableList()
        val index = current.indexOfFirst { it.id == profile.id }
        if (index != -1) {
            current[index] = profile
        } else {
            current.add(profile)
        }
        store.saveProfiles(current)
        _profiles.value = store.loadProfiles()
    }

    override suspend fun deleteProfile(profileId: String) {
        val current = store.loadProfiles().filter { it.id != profileId }
        store.saveProfiles(current)
        _profiles.value = store.loadProfiles()
    }

    override suspend fun linkGhostProfile(userEmail: String, userUid: String) {
        // Handled by CompositeProfileRepository
    }
}
