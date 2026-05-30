package com.cuentamorosos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuentamorosos.data.repository.DebtRepository
import com.cuentamorosos.data.repository.ProfileRepository
import com.cuentamorosos.model.ProfileItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfilesViewModel(
    private val profileRepository: ProfileRepository,
    private val debtRepository: DebtRepository,
) : ViewModel() {

    private val _profiles = MutableStateFlow<List<ProfileItem>>(emptyList())
    val profiles: StateFlow<List<ProfileItem>> = _profiles.asStateFlow()

    init {
        observeProfiles()
    }

    private fun observeProfiles() {
        viewModelScope.launch {
            profileRepository.observeProfiles()
                .collect { updatedProfiles ->
                    println("[ProfilesViewModel] Emitted ${updatedProfiles.size} profiles: ${updatedProfiles.map { "${it.id}:${it.name}" }}")
                    _profiles.value = updatedProfiles
                }
        }
    }

    fun saveProfile(profile: ProfileItem) {
        viewModelScope.launch {
            profileRepository.saveProfile(profile)
        }
    }

    fun deleteProfile(profile: ProfileItem) {
        viewModelScope.launch {
            runCatching { debtRepository.deleteDebtsForProfile(profile.id) }
            runCatching { profileRepository.deleteProfile(profile.id) }
        }
    }
}
