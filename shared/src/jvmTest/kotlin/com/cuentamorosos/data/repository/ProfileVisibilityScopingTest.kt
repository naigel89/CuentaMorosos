package com.cuentamorosos.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.cuentamorosos.data.NetworkMonitor
import com.cuentamorosos.data.PendingOperationQueue
import com.cuentamorosos.db.CuentaMorososDatabase
import com.cuentamorosos.model.ProfileItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Proves the remote profile subscription is *scoped*.
 *
 * This is the regression guard for the change that removed the app's hard user
 * ceiling. `FirestoreProfileRepository.observeProfiles()` used to be
 * `collection.snapshots` — the entire `profiles` collection, once per session, for
 * every user — so global reads grew with the square of the user count.
 *
 * These tests run against real dispatchers rather than virtual time, because
 * `startSyncLoop()` hardcodes `Dispatchers.Default`; a `TestScheduler` has no
 * control over it, so `advanceUntilIdle()` would return before the loop had done
 * anything and every assertion would trivially pass on an empty recording.
 */
class ProfileVisibilityScopingTest {

    private lateinit var database: CuentaMorososDatabase
    private lateinit var scope: CoroutineScope
    private lateinit var queue: PendingOperationQueue

    /** Records every id set the sync loop asks for. */
    private class RecordingRemote(
        private val profilesById: Map<String, ProfileItem> = emptyMap(),
    ) : ProfileRepository {
        private val lock = Any()
        private val recorded = mutableListOf<Set<String>>()

        val requestedIdSets: List<Set<String>>
            get() = synchronized(lock) { recorded.toList() }

        override fun observeVisibleProfiles(coParticipantIds: Set<String>): Flow<List<ProfileItem>> {
            synchronized(lock) { recorded.add(coParticipantIds) }
            return MutableStateFlow(coParticipantIds.mapNotNull { profilesById[it] })
        }

        // Deliberately blows up: nothing in the sync path may fall back to the
        // unscoped read any more.
        override fun observeProfiles(): Flow<List<ProfileItem>> =
            throw AssertionError("sync must not read the whole profiles collection")

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
        override suspend fun searchByUsername(prefix: String): List<ProfileItem> = emptyList()
    }

    private val alwaysOnline = object : NetworkMonitor {
        override val isOnline: Flow<Boolean> = MutableStateFlow(true)
    }

    private fun profile(id: String) = ProfileItem(id = id, name = "Name $id", ownerId = id)

    /** Spins until [condition] holds, so the assertion never races the sync loop. */
    private fun awaitUntil(timeoutMs: Long = 5_000, what: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(10)
        }
        throw AssertionError("timed out after ${timeoutMs}ms waiting for: $what")
    }

    /** Gives the loop a chance to do something it should NOT do. */
    private fun settle() = Thread.sleep(300)

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CuentaMorososDatabase.Schema.create(driver)
        database = CuentaMorososDatabase(driver)
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        queue = PendingOperationQueue(database = database, scope = scope)
    }

    @AfterTest
    fun teardown() {
        scope.cancel()
    }

    private fun repositoryFor(
        remote: ProfileRepository,
        visibleIds: Flow<Set<String>>,
    ) = OfflineFirstProfileRepository(
        remoteRepository = remote,
        database = database,
        networkMonitor = alwaysOnline,
        syncScope = scope,
        pendingQueue = queue,
        visibleProfileIds = visibleIds,
    )

    @Test
    fun `sync asks the remote only for the visible ids`() {
        val visible = setOf("user-me", "user-friend")
        val remote = RecordingRemote(mapOf("user-friend" to profile("user-friend")))

        repositoryFor(remote, MutableStateFlow(visible)).startSync()

        awaitUntil(what = "the first subscription") { remote.requestedIdSets.isNotEmpty() }
        assertEquals(visible, remote.requestedIdSets.first())
    }

    @Test
    fun `an empty visible set still subscribes, so own profile and ghosts arrive`() {
        // Own ghosts are not id-addressable; the remote resolves them by ownerId.
        // Skipping the subscription entirely would leave a signed-in user with no
        // profile at all.
        val remote = RecordingRemote()

        repositoryFor(remote, MutableStateFlow(emptySet())).startSync()

        awaitUntil(what = "a subscription with an empty id set") { remote.requestedIdSets.isNotEmpty() }
        assertEquals(emptySet(), remote.requestedIdSets.first())
    }

    @Test
    fun `a change in the visible set re-scopes the subscription`() {
        val ids = MutableStateFlow(setOf("user-me"))
        val remote = RecordingRemote(
            mapOf("user-me" to profile("user-me"), "user-new" to profile("user-new")),
        )

        repositoryFor(remote, ids).startSync()
        awaitUntil(what = "the first subscription") { remote.requestedIdSets.isNotEmpty() }

        ids.value = setOf("user-me", "user-new")

        awaitUntil(what = "the re-scoped subscription") { remote.requestedIdSets.size >= 2 }
        assertEquals(
            listOf(setOf("user-me"), setOf("user-me", "user-new")),
            remote.requestedIdSets.take(2),
        )
    }

    @Test
    fun `an unchanged visible set does not resubscribe`() {
        // observeEvents re-emits on every local write; without distinctUntilChanged
        // each one would tear down and rebuild every Firestore listener.
        val ids = MutableStateFlow(setOf("user-me"))
        val remote = RecordingRemote(mapOf("user-me" to profile("user-me")))

        repositoryFor(remote, ids).startSync()
        awaitUntil(what = "the first subscription") { remote.requestedIdSets.isNotEmpty() }

        ids.value = setOf("user-me")
        settle()

        assertEquals(1, remote.requestedIdSets.size)
    }

    @Test
    fun `only the profiles the remote returned reach the local cache`() {
        val remote = RecordingRemote(
            mapOf("user-me" to profile("user-me"), "user-friend" to profile("user-friend")),
        )

        repositoryFor(remote, MutableStateFlow(setOf("user-me", "user-friend"))).startSync()

        awaitUntil(what = "both profiles cached") {
            database.cachedProfileQueries.selectAll().executeAsList().size == 2
        }
        val cached = database.cachedProfileQueries.selectAll().executeAsList().map { it.id }.sorted()
        assertEquals(listOf("user-friend", "user-me"), cached)
        assertTrue(remote.requestedIdSets.isNotEmpty())
    }
}
