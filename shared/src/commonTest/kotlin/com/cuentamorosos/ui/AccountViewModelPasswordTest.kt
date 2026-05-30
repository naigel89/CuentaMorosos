package com.cuentamorosos.ui

import com.cuentamorosos.data.repository.ProfileRepository
import com.cuentamorosos.model.ProfileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlin.test.*

/**
 * Unit tests for AccountViewModel password change error handling.
 *
 * Verifies that when updatePassword fails with a reauthentication error,
 * the ViewModel transitions to the correct Error UI state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelPasswordTest {

    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `password change with reauth error shows error state`() = runTest(testDispatcher) {
        val repo = object : ProfileRepository {
            override fun observeProfiles(): Flow<List<ProfileItem>> = MutableStateFlow(emptyList())
            override suspend fun saveProfile(profile: ProfileItem) {}
            override suspend fun deleteProfile(profileId: String) {}
            override suspend fun linkGhostProfile(userEmail: String, userUid: String) {}
            override suspend fun updateProfilePhoto(photoUrl: String): Result<String> = Result.success(photoUrl)
            override suspend fun updateUsername(username: String): Result<Unit> = Result.success(Unit)
            override suspend fun updateDisplayName(displayName: String): Result<Unit> = Result.success(Unit)
            override suspend fun setCustomName(profileId: String, customName: String): Result<Unit> = Result.success(Unit)
            override suspend fun isUsernameAvailable(username: String): Boolean = true
            override suspend fun updatePassword(currentPassword: String, newPassword: String): Result<Unit> {
                return Result.failure(Exception("requiresRecentLogin"))
            }
        }

        val vm = AccountViewModel(repo, "testUid")

        // Let init coroutines settle
        advanceUntilIdle()

        // Initial state should be Idle
        assertEquals(PasswordState.Idle, vm.passwordState.value)

        vm.setCurrentPassword("oldPass")
        vm.setNewPassword("newPass")
        vm.changePassword()

        // Advance past the viewModelScope.launch to process the coroutine
        advanceUntilIdle()

        val state = vm.passwordState.value
        assertTrue(state is PasswordState.Error, "Expected Error state after reauth failure, got $state")
        assertEquals("requiresRecentLogin", (state as PasswordState.Error).message)
    }

    @Test
    fun `password change with empty fields shows validation error`() = runTest(testDispatcher) {
        val repo = object : ProfileRepository {
            override fun observeProfiles(): Flow<List<ProfileItem>> = MutableStateFlow(emptyList())
            override suspend fun saveProfile(profile: ProfileItem) {}
            override suspend fun deleteProfile(profileId: String) {}
            override suspend fun linkGhostProfile(userEmail: String, userUid: String) {}
            override suspend fun updateProfilePhoto(photoUrl: String): Result<String> = Result.success(photoUrl)
            override suspend fun updateUsername(username: String): Result<Unit> = Result.success(Unit)
            override suspend fun updateDisplayName(displayName: String): Result<Unit> = Result.success(Unit)
            override suspend fun setCustomName(profileId: String, customName: String): Result<Unit> = Result.success(Unit)
            override suspend fun isUsernameAvailable(username: String): Boolean = true
            override suspend fun updatePassword(currentPassword: String, newPassword: String): Result<Unit> = Result.success(Unit)
        }

        val vm = AccountViewModel(repo, "testUid")
        advanceUntilIdle()

        vm.changePassword() // both fields blank

        advanceUntilIdle()

        val state = vm.passwordState.value
        assertTrue(state is PasswordState.Error, "Expected validation Error, got $state")
    }

    @Test
    fun `password change with short new password shows validation error`() = runTest(testDispatcher) {
        val repo = object : ProfileRepository {
            override fun observeProfiles(): Flow<List<ProfileItem>> = MutableStateFlow(emptyList())
            override suspend fun saveProfile(profile: ProfileItem) {}
            override suspend fun deleteProfile(profileId: String) {}
            override suspend fun linkGhostProfile(userEmail: String, userUid: String) {}
            override suspend fun updateProfilePhoto(photoUrl: String): Result<String> = Result.success(photoUrl)
            override suspend fun updateUsername(username: String): Result<Unit> = Result.success(Unit)
            override suspend fun updateDisplayName(displayName: String): Result<Unit> = Result.success(Unit)
            override suspend fun setCustomName(profileId: String, customName: String): Result<Unit> = Result.success(Unit)
            override suspend fun isUsernameAvailable(username: String): Boolean = true
            override suspend fun updatePassword(currentPassword: String, newPassword: String): Result<Unit> = Result.success(Unit)
        }

        val vm = AccountViewModel(repo, "testUid")
        advanceUntilIdle()

        vm.setCurrentPassword("oldPass")
        vm.setNewPassword("12345") // only 5 chars, less than minimum 6
        vm.changePassword()

        advanceUntilIdle()

        val state = vm.passwordState.value
        assertTrue(state is PasswordState.Error, "Expected validation Error for short password, got $state")
    }
}
