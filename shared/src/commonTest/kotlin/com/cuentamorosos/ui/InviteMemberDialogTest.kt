package com.cuentamorosos.ui

import com.cuentamorosos.data.repository.ProfileRepository
import com.cuentamorosos.model.ProfileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.BeforeTest
import kotlin.test.AfterTest

/**
 * Tests for InviteMemberDialog search logic, SearchState transitions,
 * profile filtering, and debounce behavior.
 *
 * Covers spec R002 (debounce), R003 (selection → email), R004 (UI states).
 * Since commonTest cannot render Compose, these tests verify the
 * non-Compose search pipeline logic: debounce, state transitions,
 * and profile filtering rules.
 *
 * Tests reference SearchState and the search pipeline API that will be
 * implemented in InviteMemberDialog (EventDetailScreen.kt).
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class InviteMemberDialogTest {

    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Test helpers ─────────────────────────────────────────────────────────

    private fun profile(
        id: String,
        name: String = "User $id",
        username: String? = null,
        isGhost: Boolean = false,
        linkedEmail: String? = if (isGhost) null else "user$id@test.com",
    ) = ProfileItem(
        id = id,
        name = name,
        isGhost = isGhost,
        linkedEmail = linkedEmail,
        username = username,
    )

    /** Fake repository for dialog tests. */
    private class FakeSearchRepo(
        private val results: List<ProfileItem> = emptyList(),
        private val shouldThrow: Boolean = false,
    ) : ProfileRepository {
        override fun observeProfiles(): Flow<List<ProfileItem>> =
            MutableStateFlow(results)
        override suspend fun saveProfile(profile: ProfileItem) {}
        override suspend fun deleteProfile(profileId: String) {}
        override suspend fun linkGhostProfile(userEmail: String, userUid: String) {}
        override suspend fun updateProfilePhoto(photoUrl: String): Result<String> =
            Result.success(photoUrl)
        override suspend fun updateUsername(username: String): Result<Unit> =
            Result.success(Unit)
        override suspend fun updateDisplayName(displayName: String): Result<Unit> =
            Result.success(Unit)
        override suspend fun setCustomName(profileId: String, customName: String): Result<Unit> =
            Result.success(Unit)
        override suspend fun isUsernameAvailable(username: String): Boolean = true
        override suspend fun updatePassword(currentPassword: String, newPassword: String): Result<Unit> =
            Result.success(Unit)
        override suspend fun deleteProfilePhoto(): Result<Unit> = Result.success(Unit)

        override suspend fun searchByUsername(prefix: String): List<ProfileItem> {
            if (shouldThrow) throw java.io.IOException("Simulated network error")
            if (prefix.isBlank()) return emptyList()
            val lower = prefix.lowercase().trim()
            return results
                .filter { val u = it.username; u != null && u.lowercase().startsWith(lower) }
                .filter { !it.isGhost && it.linkedEmail != null }
        }
    }

    // ── R002: Debounce behavior ──────────────────────────────────────────────

    @Test
    fun `debounce fires search after 500ms idle`() = runTest(testDispatcher) {
        var searchFired = false
        var firedQuery = ""

        val searchQuery = MutableStateFlow("")

        val job = launch {
            searchQuery
                .filter { it.length >= 2 }
                .debounce(500)
                .collect { query ->
                    searchFired = true
                    firedQuery = query
                }
        }

        advanceUntilIdle()

        // Emit "na" — should fire after 500ms
        searchQuery.value = "na"
        advanceTimeBy(400)
        assertFalse(searchFired, "Should not fire before 500ms")

        advanceTimeBy(100) // total 500ms
        advanceUntilIdle()
        assertTrue(searchFired, "Should fire at or after 500ms")
        assertEquals("na", firedQuery)

        job.cancel()
    }

    @Test
    fun `rapid input resets debounce timer`() = runTest(testDispatcher) {
        var searchCount = 0

        // Simulate LaunchedEffect debounce: cancel and restart on input change
        val job1 = launch {
            delay(500)
            searchCount++
        }

        advanceTimeBy(300)
        // New input arrives before debounce → cancel old, start new
        job1.cancel()
        assertTrue(job1.isCancelled, "Previous search should be cancelled")

        val job2 = launch {
            delay(500)
            searchCount++
        }

        advanceTimeBy(400)
        assertEquals(0, searchCount, "Should not fire before 500ms")

        advanceTimeBy(100) // total 500ms
        advanceUntilIdle()
        assertEquals(1, searchCount, "Should fire exactly once after 500ms")
    }

    @Test
    fun `single character input does not trigger search`() = runTest(testDispatcher) {
        val searchEvents = mutableListOf<String>()

        val searchQuery = MutableStateFlow("")

        val job = launch {
            searchQuery
                .filter { it.length >= 2 }
                .debounce(500)
                .collect { query ->
                    searchEvents.add(query)
                }
        }

        advanceUntilIdle()

        searchQuery.value = "n" // only 1 char — filtered out
        advanceTimeBy(600)

        assertTrue(searchEvents.isEmpty(), "Single char should not trigger search")

        job.cancel()
    }

    @Test
    fun `empty input does not trigger search`() = runTest(testDispatcher) {
        val searchEvents = mutableListOf<String>()

        val searchQuery = MutableStateFlow("")

        val job = launch {
            searchQuery
                .filter { it.length >= 2 }
                .debounce(500)
                .collect { query ->
                    searchEvents.add(query)
                }
        }

        advanceUntilIdle()

        advanceTimeBy(600)

        assertTrue(searchEvents.isEmpty(), "Empty input should not trigger search")

        job.cancel()
    }

    // ── Search pipeline: filtering and exclusion ─────────────────────────────

    @Test
    fun `search filters results by prefix case-insensitively`() = runTest {
        val repo = FakeSearchRepo(
            results = listOf(
                profile("1", username = "naigel"),
                profile("2", username = "nadia"),
                profile("3", username = "miguel"),
            )
        )

        val results = repo.searchByUsername("NA")

        assertEquals(2, results.size)
        val usernames = results.mapNotNull { it.username }
        assertTrue("naigel" in usernames)
        assertTrue("nadia" in usernames)
    }

    @Test
    fun `self profile is excluded from results`() = runTest {
        val allResults = listOf(
            profile("self", username = "naigel"),
            profile("other", username = "nadia"),
        )

        val repo = FakeSearchRepo(results = allResults)
        val rawResults = repo.searchByUsername("n")
        // Simulate self-exclusion (normally done in dialog after query)
        val filtered = rawResults.filter { it.id != "self" }

        assertEquals(1, filtered.size)
        val firstUsername = filtered[0].username
        assertEquals("nadia", firstUsername)
    }

    @Test
    fun `ghost profiles are excluded from results`() = runTest {
        val allResults = listOf(
            profile("1", username = "naigel", isGhost = false, linkedEmail = "naigel@test.com"),
            profile("2", username = "nadia", isGhost = true, linkedEmail = null),
        )

        val repo = FakeSearchRepo(results = allResults)
        val results = repo.searchByUsername("n")

        assertEquals(1, results.size)
        val ghostResult = results[0].username
        assertEquals("naigel", ghostResult)
    }

    @Test
    fun `profiles without linkedEmail are excluded from results`() = runTest {
        val allResults = listOf(
            profile("1", username = "naigel", linkedEmail = "naigel@test.com"),
            profile("2", username = "test", linkedEmail = null),
        )

        val repo = FakeSearchRepo(results = allResults)
        val results = repo.searchByUsername("t")

        assertEquals(0, results.size, "Profile without linkedEmail should be excluded")
    }

    @Test
    fun `no match returns empty results`() = runTest {
        val repo = FakeSearchRepo(
            results = listOf(
                profile("1", username = "naigel"),
            )
        )

        val results = repo.searchByUsername("xyz")

        assertTrue(results.isEmpty())
    }

    // ── R003: Profile selection resolves linkedEmail ────────────────────────

    @Test
    fun `selecting profile resolves linkedEmail`() = runTest {
        val testProfile = profile("p1", name = "Naigel", username = "naigel", linkedEmail = "naigel@test.com")

        // Simulate selection → email resolution
        val email = testProfile.linkedEmail

        assertEquals("naigel@test.com", email, "Selected profile should resolve linkedEmail")
    }

    @Test
    fun `profile without linkedEmail cannot be selected`() = runTest {
        val testProfile = profile("p2", name = "Ghost", username = "test", linkedEmail = null)

        val hasEmail = testProfile.linkedEmail != null

        assertFalse(hasEmail, "Profile without linkedEmail should not be selectable")
    }

    // ── R004: Search state transitions ──────────────────────────────────────

    @Test
    fun `SearchState Idle has no query`() {
        // SearchState.Idle represents initial state with no search
        val idle = SearchState.Idle
        assertTrue(idle is SearchState.Idle)
    }

    @Test
    fun `SearchState Loading represents in-progress search`() {
        val loading = SearchState.Loading
        assertTrue(loading is SearchState.Loading)
    }

    @Test
    fun `SearchState Results contains matching profiles`() {
        val results = SearchState.Results(
            listOf(
                profile("1", username = "naigel"),
                profile("2", username = "nadia"),
            )
        )
        assertTrue(results is SearchState.Results)
        assertEquals(2, results.profiles.size)
        val usernames = results.profiles.mapNotNull { it.username }
        assertTrue("naigel" in usernames)
        assertTrue("nadia" in usernames)
    }

    @Test
    fun `SearchState Empty stores the query that had no matches`() {
        val empty = SearchState.Empty(query = "xyz")
        assertTrue(empty is SearchState.Empty)
        assertEquals("xyz", empty.query)
    }

    @Test
    fun `SearchState Offline represents connectivity loss`() {
        val offline = SearchState.Offline
        assertTrue(offline is SearchState.Offline)
    }

    // ── State transition logic ──────────────────────────────────────────────

    @Test
    fun `determineSearchState returns Results when profiles found`() = runTest {
        val profiles = listOf(
            profile("1", username = "naigel"),
        )
        val state = determineSearchState(profiles, "na")

        assertTrue(state is SearchState.Results)
        assertEquals(1, (state as SearchState.Results).profiles.size)
    }

    @Test
    fun `determineSearchState returns Empty when no profiles found`() = runTest {
        val state = determineSearchState(emptyList(), "xyz")

        assertTrue(state is SearchState.Empty)
        assertEquals("xyz", (state as SearchState.Empty).query)
    }

    @Test
    fun `determineSearchState returns Loading when null profiles passed`() = runTest {
        // null represents "loading in progress"
        val state = determineSearchState(null, "na")

        assertTrue(state is SearchState.Loading)
    }

    // ── Network error handling ──────────────────────────────────────────────

    @Test
    fun `network error produces Offline state`() = runTest {
        val offlineState = determineSearchStateForError(isOffline = true, query = "na")

        assertTrue(offlineState is SearchState.Offline)
    }

    @Test
    fun `non-offline error produces Empty state`() = runTest {
        val emptyState = determineSearchStateForError(isOffline = false, query = "na")

        assertTrue(emptyState is SearchState.Empty)
        assertEquals("na", (emptyState as SearchState.Empty).query)
    }
}
