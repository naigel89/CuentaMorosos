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
 * Unit tests for AccountViewModel username debounce and validation logic.
 *
 * Verifies that:
 * - Usernames shorter than 3 characters do NOT trigger availability checks
 *   regardless of debounce timing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {

    /** Shared scheduler so Dispatchers.Main and test dispatcher advance together. */
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
    fun `username availability not checked for short inputs`() = runTest(testDispatcher) {
        var availabilityChecked = false

        val repo = object : ProfileRepository {
            override fun observeProfiles(): Flow<List<ProfileItem>> = MutableStateFlow(emptyList())
            override suspend fun saveProfile(profile: ProfileItem) {}
            override suspend fun deleteProfile(profileId: String) {}
            override suspend fun linkGhostProfile(userEmail: String, userUid: String) {}
            override suspend fun updateProfilePhoto(photoUrl: String): Result<String> = Result.success(photoUrl)
            override suspend fun updateUsername(username: String): Result<Unit> = Result.success(Unit)
            override suspend fun updateDisplayName(displayName: String): Result<Unit> = Result.success(Unit)
            override suspend fun setCustomName(profileId: String, customName: String): Result<Unit> = Result.success(Unit)
            override suspend fun isUsernameAvailable(username: String): Boolean {
                availabilityChecked = true
                return true
            }
            override suspend fun updatePassword(currentPassword: String, newPassword: String): Result<Unit> = Result.success(Unit)
            override suspend fun deleteProfilePhoto(): Result<Unit> = Result.success(Unit)
        }

        val vm = AccountViewModel(repo, "testUid")

        // Let init coroutines (loadProfile + setupUsernameValidation) settle
        advanceUntilIdle()

        vm.setUsername("ab") // less than 3 chars — filter blocks before collect

        // Advance past the 500ms debounce — the debounce WILL fire but the
        // filter { it.length >= 3 } must block the short input.
        advanceTimeBy(600L)

        assertFalse(availabilityChecked, "Should not check availability for <3 chars")
    }

    @Test
    fun `username availability checked for 3-plus char inputs`() = runTest(testDispatcher) {
        var checkedUsername = ""

        val repo = object : ProfileRepository {
            override fun observeProfiles(): Flow<List<ProfileItem>> = MutableStateFlow(emptyList())
            override suspend fun saveProfile(profile: ProfileItem) {}
            override suspend fun deleteProfile(profileId: String) {}
            override suspend fun linkGhostProfile(userEmail: String, userUid: String) {}
            override suspend fun updateProfilePhoto(photoUrl: String): Result<String> = Result.success(photoUrl)
            override suspend fun updateUsername(username: String): Result<Unit> = Result.success(Unit)
            override suspend fun updateDisplayName(displayName: String): Result<Unit> = Result.success(Unit)
            override suspend fun setCustomName(profileId: String, customName: String): Result<Unit> = Result.success(Unit)
            override suspend fun isUsernameAvailable(username: String): Boolean {
                checkedUsername = username
                return true
            }
            override suspend fun updatePassword(currentPassword: String, newPassword: String): Result<Unit> = Result.success(Unit)
            override suspend fun deleteProfilePhoto(): Result<Unit> = Result.success(Unit)
        }

        val vm = AccountViewModel(repo, "testUid")

        // Let init coroutines settle
        advanceUntilIdle()

        vm.setUsername("abc") // >= 3 chars — should pass through debounce + filter

        // Advance past the 500ms debounce
        advanceTimeBy(600L)

        assertEquals("abc", checkedUsername, "Should check availability for >=3 chars")
    }

    @Test
    fun `username availability shows idle initially`() = runTest {
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
            override suspend fun deleteProfilePhoto(): Result<Unit> = Result.success(Unit)
        }

        val vm = AccountViewModel(repo, "testUid")

        assertEquals(UsernameAvailability.Idle, vm.usernameAvailability.value)
    }
}
