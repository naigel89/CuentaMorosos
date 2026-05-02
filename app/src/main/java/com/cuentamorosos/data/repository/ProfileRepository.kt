package com.cuentamorosos.data.repository

import com.cuentamorosos.model.ProfileItem
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfiles(): Flow<List<ProfileItem>>
    suspend fun saveProfile(profile: ProfileItem)
    suspend fun deleteProfile(profileId: String)
    suspend fun linkGhostProfile(userEmail: String, userUid: String)
}
