package com.cuentamorosos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuentamorosos.data.repository.ProfileRepository
import com.cuentamorosos.model.ProfileItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * ViewModel for the Account Settings screen and its sub-screens.
 *
 * Manages sub-screen navigation (main menu, name+photo, username, security),
 * form state for display name / username / password editing, and username
 * availability validation with debounced remote checks.
 */
class AccountViewModel(
    private val profileRepository: ProfileRepository,
    private val currentProfileId: String,
) : ViewModel() {

    // ── Sub-screen navigation ─────────────────────────────────────────────────
    // 0 = main menu, 1 = name+photo, 2 = username, 3 = security

    private val _subScreenIndex = MutableStateFlow(0)
    val subScreenIndex: StateFlow<Int> = _subScreenIndex.asStateFlow()

    // ── Photo URI (stored as string representation of the URI) ────────────────

    private val _selectedPhotoUri = MutableStateFlow<String?>(null)
    val selectedPhotoUri: StateFlow<String?> = _selectedPhotoUri.asStateFlow()

    // ── Display name editing ──────────────────────────────────────────────────

    private val _displayNameText = MutableStateFlow("")
    val displayNameText: StateFlow<String> = _displayNameText.asStateFlow()

    private var hasInitialLoadDone = false

    // ── Username editing with async validation ─────────────────────────────────

    private val _usernameText = MutableStateFlow("")
    val usernameText: StateFlow<String> = _usernameText.asStateFlow()

    private val _usernameAvailability = MutableStateFlow<UsernameAvailability>(UsernameAvailability.Idle)
    val usernameAvailability: StateFlow<UsernameAvailability> = _usernameAvailability.asStateFlow()

    // ── Password editing ──────────────────────────────────────────────────────

    private val _currentPassword = MutableStateFlow("")
    val currentPassword: StateFlow<String> = _currentPassword.asStateFlow()

    private val _newPassword = MutableStateFlow("")
    val newPassword: StateFlow<String> = _newPassword.asStateFlow()

    private val _passwordState = MutableStateFlow<PasswordState>(PasswordState.Idle)
    val passwordState: StateFlow<PasswordState> = _passwordState.asStateFlow()

    // ── UI state ──────────────────────────────────────────────────────────────

    private val _state = MutableStateFlow<AccountUiState>(AccountUiState.Idle)
    val state: StateFlow<AccountUiState> = _state.asStateFlow()

    private val _currentProfile = MutableStateFlow<ProfileItem?>(null)
    val currentProfile: StateFlow<ProfileItem?> = _currentProfile.asStateFlow()

    init {
        loadProfile()
        setupUsernameValidation()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            profileRepository.observeProfiles().collect { profiles ->
                val own = profiles.firstOrNull { it.id == currentProfileId }
                _currentProfile.value = own
                _displayNameText.value = own?.displayName ?: own?.name ?: ""
                _usernameText.value = own?.username ?: ""
            }
        }
    }

    private fun setupUsernameValidation() {
        viewModelScope.launch {
            _usernameText
                .debounce(500)
                .filter { it.length >= 3 }
                .distinctUntilChanged()
                .map { username ->
                    _usernameAvailability.value = UsernameAvailability.Checking
                    val available = profileRepository.isUsernameAvailable(username)
                    if (available) UsernameAvailability.Available else UsernameAvailability.Taken
                }
                .collect { _usernameAvailability.value = it }
        }
    }

    fun navigateTo(screen: Int) {
        println("[AccountViewModel] navigateTo($screen)")
        _subScreenIndex.value = screen
    }

    fun onBackToMenu() {
        println("[AccountViewModel] onBackToMenu")
        _subScreenIndex.value = 0
    }

    fun setUsername(text: String) {
        _usernameText.value = text
    }

    fun setDisplayName(text: String) {
        _displayNameText.value = text
    }

    fun setSelectedPhotoUri(uri: String?) {
        _selectedPhotoUri.value = uri
    }

    fun setCurrentPassword(pwd: String) {
        _currentPassword.value = pwd
    }

    fun setNewPassword(pwd: String) {
        _newPassword.value = pwd
    }

    fun saveDisplayName() {
        viewModelScope.launch {
            println("[AccountViewModel] saveDisplayName called, text='${_displayNameText.value}'")
            _state.value = AccountUiState.Loading
            val name = _displayNameText.value.trim()
            if (name.isBlank()) {
                println("[AccountViewModel] saveDisplayName: name is blank, aborting")
                _state.value = AccountUiState.Error("El nombre no puede estar vacío")
                return@launch
            }
            println("[AccountViewModel] saveDisplayName: calling updateDisplayName('$name')")
            profileRepository.updateDisplayName(name)
                .onSuccess {
                    println("[AccountViewModel] saveDisplayName: success")
                    _state.value = AccountUiState.Success("Nombre actualizado")
                }
                .onFailure {
                    println("[AccountViewModel] saveDisplayName: failure ${it.message}")
                    _state.value = AccountUiState.Error(it.message ?: "Error al guardar")
                }
        }
    }

    fun saveUsername() {
        viewModelScope.launch {
            println("[AccountViewModel] saveUsername called, text='${_usernameText.value}'")
            _state.value = AccountUiState.Loading
            val username = _usernameText.value.trim()
            if (username.length < 3) {
                println("[AccountViewModel] saveUsername: too short, aborting")
                _state.value = AccountUiState.Error("El nombre de usuario debe tener al menos 3 caracteres")
                return@launch
            }
            println("[AccountViewModel] saveUsername: calling updateUsername('$username')")
            profileRepository.updateUsername(username)
                .onSuccess {
                    println("[AccountViewModel] saveUsername: success")
                    _state.value = AccountUiState.Success("Nombre de usuario actualizado")
                }
                .onFailure {
                    println("[AccountViewModel] saveUsername: failure ${it.message}")
                    _state.value = AccountUiState.Error(it.message ?: "Error al guardar")
                }
        }
    }

    fun updatePhoto(downloadUrl: String) {
        viewModelScope.launch {
            println("[AccountViewModel] updatePhoto called, url='$downloadUrl'")
            _state.value = AccountUiState.Loading
            profileRepository.updateProfilePhoto(downloadUrl)
                .onSuccess {
                    println("[AccountViewModel] updatePhoto: success")
                    _state.value = AccountUiState.Success("Foto actualizada")
                }
                .onFailure {
                    println("[AccountViewModel] updatePhoto: failure ${it.message}")
                    _state.value = AccountUiState.Error(it.message ?: "Error al subir la foto")
                }
        }
    }

    /**
     * Deletes the profile photo: sets photoUrl to null in Firestore and local cache.
     * The actual Storage file deletion should happen on the Android side before calling this.
     */
    fun deletePhoto() {
        viewModelScope.launch {
            println("[AccountViewModel] deletePhoto called")
            _state.value = AccountUiState.Loading
            profileRepository.deleteProfilePhoto()
                .onSuccess {
                    println("[AccountViewModel] deletePhoto: success")
                    _state.value = AccountUiState.Success("Foto eliminada")
                }
                .onFailure {
                    println("[AccountViewModel] deletePhoto: failure ${it.message}")
                    _state.value = AccountUiState.Error(it.message ?: "Error al eliminar la foto")
                }
        }
    }

    fun changePassword() {
        viewModelScope.launch {
            _passwordState.value = PasswordState.Loading
            val current = _currentPassword.value
            val new = _newPassword.value
            if (current.isBlank() || new.isBlank()) {
                _passwordState.value = PasswordState.Error("Ambos campos son obligatorios")
                return@launch
            }
            if (new.length < 6) {
                _passwordState.value = PasswordState.Error("La nueva contraseña debe tener al menos 6 caracteres")
                return@launch
            }
            profileRepository.updatePassword(current, new)
                .onSuccess {
                    _passwordState.value = PasswordState.Success("Contraseña actualizada")
                    _currentPassword.value = ""
                    _newPassword.value = ""
                }
                .onFailure { _passwordState.value = PasswordState.Error(it.message ?: "Error al cambiar contraseña") }
        }
    }

    fun clearState() {
        _state.value = AccountUiState.Idle
        _passwordState.value = PasswordState.Idle
    }
}

// ── UI state sealed classes ────────────────────────────────────────────────────

sealed class UsernameAvailability {
    data object Idle : UsernameAvailability()
    data object Checking : UsernameAvailability()
    data object Available : UsernameAvailability()
    data object Taken : UsernameAvailability()
}

sealed class PasswordState {
    data object Idle : PasswordState()
    data object Loading : PasswordState()
    data class Success(val message: String) : PasswordState()
    data class Error(val message: String) : PasswordState()
}

sealed class AccountUiState {
    data object Idle : AccountUiState()
    data object Loading : AccountUiState()
    data class Success(val message: String) : AccountUiState()
    data class Error(val message: String) : AccountUiState()
}
