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
 * Tests password clearing behavior across navigation paths.
 *
 * Verifies that password fields are emptied after:
 * - Back to menu navigation (fromAccountViewModel.onBackToMenu())
 * - Successful password change
 *
 * Note: Compose-level password clearing (SplashAuthScreen, RegisterScreen)
 * is verified via manual testing / integration tests since it requires
 * Compose UI test infrastructure.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PasswordClearingTest {

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

    private fun createRepo(updatePasswordResult: Result<Unit> = Result.success(Unit)) = object : ProfileRepository {
        override fun observeProfiles(): Flow<List<ProfileItem>> = MutableStateFlow(emptyList())
        override suspend fun saveProfile(profile: ProfileItem) {}
        override suspend fun deleteProfile(profileId: String) {}
        override suspend fun linkGhostProfile(userEmail: String, userUid: String) {}
        override suspend fun updateProfilePhoto(photoUrl: String): Result<String> = Result.success(photoUrl)
        override suspend fun updateUsername(username: String): Result<Unit> = Result.success(Unit)
        override suspend fun updateDisplayName(displayName: String): Result<Unit> = Result.success(Unit)
        override suspend fun setCustomName(profileId: String, customName: String): Result<Unit> = Result.success(Unit)
        override suspend fun isUsernameAvailable(username: String): Boolean = true
        override suspend fun updatePassword(currentPassword: String, newPassword: String): Result<Unit> = updatePasswordResult
        override suspend fun deleteProfilePhoto(): Result<Unit> = Result.success(Unit)
        override suspend fun searchByUsername(prefix: String): List<ProfileItem> = throw UnsupportedOperationException()
    }

    @Test
    fun `onBackToMenu clears both password fields`() = runTest(testDispatcher) {
        val vm = AccountViewModel(createRepo(), "testUid")
        advanceUntilIdle()

        // Set passwords
        vm.setCurrentPassword("mySecret123")
        vm.setNewPassword("newSecret456")

        // Navigate back to menu
        vm.onBackToMenu()

        // Verify passwords are cleared
        assertEquals("", vm.currentPassword.value, "currentPassword should be empty after back-to-menu")
        assertEquals("", vm.newPassword.value, "newPassword should be empty after back-to-menu")
        assertEquals(PasswordState.Idle, vm.passwordState.value, "passwordState should be Idle")
    }

    @Test
    fun `successful password change clears password fields`() = runTest(testDispatcher) {
        val repo = createRepo(updatePasswordResult = Result.success(Unit))
        val vm = AccountViewModel(repo, "testUid")
        advanceUntilIdle()

        vm.setCurrentPassword("oldPass123")
        vm.setNewPassword("newPass456")
        vm.changePassword()

        advanceUntilIdle()

        // After successful change, passwords should be cleared
        assertEquals("", vm.currentPassword.value, "currentPassword should be cleared after successful change")
        assertEquals("", vm.newPassword.value, "newPassword should be cleared after successful change")
        assertEquals(PasswordState.Success::class, vm.passwordState.value::class,
            "Should be in Success state after successful change")
    }

    @Test
    fun `failed password change preserves password fields`() = runTest(testDispatcher) {
        val repo = createRepo(updatePasswordResult = Result.failure(Exception("auth error")))
        val vm = AccountViewModel(repo, "testUid")
        advanceUntilIdle()

        vm.setCurrentPassword("oldPass123")
        vm.setNewPassword("newPass456")
        vm.changePassword()

        advanceUntilIdle()

        // After failed change, passwords should NOT be cleared (user can retry)
        assertEquals("oldPass123", vm.currentPassword.value, "currentPassword should be preserved after failure")
        assertEquals("newPass456", vm.newPassword.value, "newPassword should be preserved after failure")
        assertTrue(vm.passwordState.value is PasswordState.Error, "Should be in Error state")
    }

    @Test
    fun `password fields initially empty`() = runTest(testDispatcher) {
        val vm = AccountViewModel(createRepo(), "testUid")
        advanceUntilIdle()

        assertEquals("", vm.currentPassword.value, "currentPassword should start empty")
        assertEquals("", vm.newPassword.value, "newPassword should start empty")
        assertEquals(PasswordState.Idle, vm.passwordState.value, "passwordState should start Idle")
    }
}
