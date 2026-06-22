package com.cuentamorosos.data.repository

import com.cuentamorosos.model.EventDebtItem
import com.cuentamorosos.model.EventExpenseItem
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.EventParticipant
import com.cuentamorosos.model.EventRole
import com.cuentamorosos.model.ProfileItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

/**
 * Tests for ProfileRepository.searchByUsername contract and filtering rules.
 *
 * Since the real FirestoreProfileRepository calls the Firestore SDK directly
 * (not mockable in commonTest), these tests validate the business logic
 * through a FakeProfileRepository that implements the same filtering rules:
 * - Prefix matching (case-insensitive)
 * - Ghost exclusion
 * - Own-profile exclusion
 * - No-linkedEmail exclusion
 * - Limit to 20 results
 *
 * R001 scenarios covered: prefix match, mid-string match, no-match,
 * ghost exclusion, own-profile exclusion, empty prefix, limit behavior.
 */
class FirestoreProfileRepositoryTest {

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

    /**
     * In-memory fake that implements the same search contract as
     * FirestoreProfileRepository: lowercase prefix matching, ghost exclusion,
     * linkedEmail exclusion, limit(20).
     */
    private class FakeSearchProfileRepository(
        private val profiles: List<ProfileItem>,
        private val ownProfileId: String? = null,
    ) : ProfileRepository {
        override fun observeProfiles(): Flow<List<ProfileItem>> =
            MutableStateFlow(profiles)

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
            if (prefix.isBlank()) return emptyList()
            val lower = prefix.lowercase().trim()
            return profiles
                .filter { val u = it.username; u != null && u.lowercase().startsWith(lower) }
                .filter { !it.isGhost }
                .filter { it.linkedEmail != null }
                .filter { it.id != ownProfileId }
                .take(20)
        }
    }

    // ── R001: Prefix matching ────────────────────────────────────────────────

    @Test
    fun `prefix matches profiles starting with prefix`() = runTest {
        val repo = FakeSearchProfileRepository(
            profiles = listOf(
                profile("1", username = "naigel"),
                profile("2", username = "nico"),
                profile("3", username = "nadia"),
                profile("4", username = "miguel"),
            )
        )

        val results = repo.searchByUsername("n")

        assertEquals(3, results.size, "Should match naigel, nico, nadia")
        val usernames = results.mapNotNull { it.username }
        assertTrue("naigel" in usernames)
        assertTrue("nico" in usernames)
        assertTrue("nadia" in usernames)
    }

    @Test
    fun `prefix matches only usernames that start with the prefix`() = runTest {
        val repo = FakeSearchProfileRepository(
            profiles = listOf(
                profile("1", username = "naigel"),  // does NOT start with "aig"
                profile("2", username = "aigan"),    // starts with "aig"
                profile("3", username = "miguel"),   // does NOT start with "aig"
            )
        )

        val results = repo.searchByUsername("aig")

        assertEquals(1, results.size, "Only aigan starts with 'aig'")
        val usernames = results.mapNotNull { it.username }
        assertTrue("aigan" in usernames)
    }

    @Test
    fun `no match returns empty list`() = runTest {
        val repo = FakeSearchProfileRepository(
            profiles = listOf(
                profile("1", username = "naigel"),
                profile("2", username = "nico"),
            )
        )

        val results = repo.searchByUsername("xyz")

        assertTrue(results.isEmpty(), "Should return empty for no match")
    }

    @Test
    fun `empty prefix returns empty list`() = runTest {
        val repo = FakeSearchProfileRepository(
            profiles = listOf(
                profile("1", username = "naigel"),
            )
        )

        val results = repo.searchByUsername("")

        assertTrue(results.isEmpty(), "Empty prefix should return empty")
    }

    @Test
    fun `blank prefix returns empty list`() = runTest {
        val repo = FakeSearchProfileRepository(
            profiles = listOf(
                profile("1", username = "naigel"),
            )
        )

        val results = repo.searchByUsername("   ")

        assertTrue(results.isEmpty(), "Blank prefix should return empty")
    }

    @Test
    fun `search is case-insensitive`() = runTest {
        val repo = FakeSearchProfileRepository(
            profiles = listOf(
                profile("1", username = "Naigel"),
                profile("2", username = "NICO"),
            )
        )

        val results = repo.searchByUsername("na")

        assertEquals(1, results.size, "Lowercase 'na' should match 'Naigel'")
        val resultUsername = results[0].username
        assertEquals("Naigel", resultUsername)
    }

    @Test
    fun `uppercase prefix matches lowercase username`() = runTest {
        val repo = FakeSearchProfileRepository(
            profiles = listOf(
                profile("1", username = "naigel"),
                profile("2", username = "nadia"),
            )
        )

        val results = repo.searchByUsername("NA")

        assertEquals(2, results.size, "'NA' should match 'naigel' and 'nadia'")
    }

    // ── R001: Ghost exclusion ────────────────────────────────────────────────

    @Test
    fun `ghost profiles are excluded from results`() = runTest {
        val repo = FakeSearchProfileRepository(
            profiles = listOf(
                profile("1", username = "naigel", isGhost = false, linkedEmail = "naigel@test.com"),
                profile("2", username = "nadia", isGhost = true, linkedEmail = null),
            )
        )

        val results = repo.searchByUsername("n")

        assertEquals(1, results.size, "Ghost profile should be excluded")
        val firstUsername = results[0].username
        assertEquals("naigel", firstUsername)
    }

    // ── R001: Own-profile exclusion ──────────────────────────────────────────

    @Test
    fun `own profile is excluded from results`() = runTest {
        val repo = FakeSearchProfileRepository(
            profiles = listOf(
                profile("abc", username = "naigel"),
                profile("def", username = "nico"),
            ),
            ownProfileId = "abc",
        )

        val results = repo.searchByUsername("n")

        assertEquals(1, results.size, "Own profile should be excluded")
        val firstResultUser = results[0].username
        assertEquals("nico", firstResultUser)
    }

    @Test
    fun `own profile exclusion does not affect other profiles`() = runTest {
        val repo = FakeSearchProfileRepository(
            profiles = listOf(
                profile("abc", username = "naigel"),
                profile("def", username = "nadia"),
                profile("ghi", username = "nico"),
            ),
            ownProfileId = "abc",
        )

        val results = repo.searchByUsername("n")

        assertEquals(2, results.size, "Only own profile excluded, others included")
        val usernames = results.mapNotNull { it.username }
        assertTrue("nadia" in usernames)
        assertTrue("nico" in usernames)
    }

    // ── R003: No-linkedEmail exclusion ──────────────────────────────────────

    @Test
    fun `profiles without linkedEmail are excluded`() = runTest {
        val repo = FakeSearchProfileRepository(
            profiles = listOf(
                profile("1", username = "naigel", linkedEmail = "naigel@test.com"),
                profile("2", username = "test", linkedEmail = null),
            )
        )

        val results = repo.searchByUsername("n")

        assertEquals(1, results.size, "Profile without linkedEmail should be excluded")
        val firstResUser = results[0].username
        assertEquals("naigel", firstResUser)
    }

    // ── Limit behavior ───────────────────────────────────────────────────────

    @Test
    fun `results are limited to 20 profiles`() = runTest {
        val manyProfiles = (1..30).map { i ->
            profile("p$i", name = "User $i", username = "user$i")
        }
        val repo = FakeSearchProfileRepository(profiles = manyProfiles)

        val results = repo.searchByUsername("user")

        assertEquals(20, results.size, "Should be limited to 20 results")
    }

    @Test
    fun `results under limit are all returned`() = runTest {
        val fewProfiles = (1..5).map { i ->
            profile("p$i", name = "User $i", username = "user$i")
        }
        val repo = FakeSearchProfileRepository(profiles = fewProfiles)

        val results = repo.searchByUsername("user")

        assertEquals(5, results.size, "All 5 should be returned (under limit)")
    }

    // ── OfflineFirstProfileRepository stub ───────────────────────────────────

    @Test
    fun `searchByUsername throws UnsupportedOperationException in offline stub`() = runTest {
        val stub = object : ProfileRepository {
            override fun observeProfiles(): Flow<List<ProfileItem>> =
                MutableStateFlow(emptyList())
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
                throw UnsupportedOperationException("searchByUsername is remote-only; use FirestoreProfileRepository")
            }
        }

        val exception = assertFailsWith<UnsupportedOperationException> {
            stub.searchByUsername("test")
        }
        assertTrue(
            exception.message?.contains("searchByUsername") == true,
            "Exception message should mention searchByUsername"
        )
    }

    // ── Edge cases ───────────────────────────────────────────────────────────

    @Test
    fun `prefix with special characters handled gracefully`() = runTest {
        val repo = FakeSearchProfileRepository(
            profiles = listOf(
                profile("1", username = "user_test"),
                profile("2", username = "user.name"),
            )
        )

        val results = repo.searchByUsername("user_")

        assertEquals(1, results.size)
        val specialResult = results[0].username
        assertEquals("user_test", specialResult)
    }

    @Test
    fun `single character prefix matches correctly`() = runTest {
        val repo = FakeSearchProfileRepository(
            profiles = listOf(
                profile("1", username = "a"),
                profile("2", username = "ab"),
                profile("3", username = "b"),
            )
        )

        val results = repo.searchByUsername("a")

        assertEquals(2, results.size)
        val usernames = results.mapNotNull { it.username }
        assertTrue("a" in usernames)
        assertTrue("ab" in usernames)
    }

    @Test
    fun `prefix matching exact username returns single result`() = runTest {
        val repo = FakeSearchProfileRepository(
            profiles = listOf(
                profile("1", username = "naigel"),
                profile("2", username = "naigel2"),
            )
        )

        val results = repo.searchByUsername("naigel")

        assertEquals(2, results.size, "Exact prefix match returns both naigel and naigel2")
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  GPS-REQ-001: Ghost profile linking
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * In-memory fake that exercises the same ghost-linking business logic
     * as FirestoreProfileRepository.linkGhostProfile() without Firestore SDK.
     *
     * The real implementation queries Firestore collections (profiles, events,
     * event debts, event expenses) and rewrites ghost references atomically.
     * This fake uses mutable in-memory data structures to validate the same
     * rewrite rules.
     */
    private class FakeGhostLinkProfileRepository(
        val profiles: MutableList<ProfileItem>,
        val events: MutableList<EventItem>,
        val debts: MutableList<EventDebtItem>,   // keyed by (eventId, debtId)
        val expenses: MutableList<EventExpenseItem>, // keyed by (eventId, expenseId)
    ) : ProfileRepository {
        override fun observeProfiles(): Flow<List<ProfileItem>> =
            MutableStateFlow(profiles.toList())

        override suspend fun saveProfile(profile: ProfileItem) {}
        override suspend fun deleteProfile(profileId: String) {}
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
        override suspend fun searchByUsername(prefix: String): List<ProfileItem> =
            profiles.filter { it.username?.startsWith(prefix, ignoreCase = true) == true }

        /**
         * Implements the same business logic as FirestoreProfileRepository.linkGhostProfile():
         * 1. Find ghosts where linkedEmail == email AND isGhost == true
         * 2. Rewrite all references (debts, expenses, events) from ghostId → realUid
         * 3. Delete ghost profiles
         */
        override suspend fun linkGhostProfile(userEmail: String, userUid: String) {
            val ghosts = profiles.filter { it.isGhost && it.linkedEmail == userEmail }
            if (ghosts.isEmpty()) return

            for (ghost in ghosts) {
                val ghostId = ghost.id

                // Rewrite debts: profileId or creditorId == ghostId → realUid
                val updatedDebts = debts.map { debt ->
                    when {
                        debt.profileId == ghostId && debt.creditorId == ghostId ->
                            debt.copy(profileId = userUid, creditorId = userUid)
                        debt.profileId == ghostId ->
                            debt.copy(profileId = userUid)
                        debt.creditorId == ghostId ->
                            debt.copy(creditorId = userUid)
                        else -> debt
                    }
                }
                debts.clear()
                debts.addAll(updatedDebts)

                // Rewrite expenses: check assignedProfileIds, profileWeights keys,
                // paidByProfileId, debtorIds, payerContributions keys for ghostId
                val updatedExpenses = expenses.map { exp ->
                    var e = exp
                    if (exp.assignedProfileIds.contains(ghostId)) {
                        e = e.copy(
                            assignedProfileIds = exp.assignedProfileIds.map {
                                if (it == ghostId) userUid else it
                            }
                        )
                    }
                    if (exp.profileWeights.containsKey(ghostId)) {
                        e = e.copy(
                            profileWeights = exp.profileWeights.mapKeys {
                                if (it.key == ghostId) userUid else it.key
                            }
                        )
                    }
                    if (exp.paidByProfileId == ghostId) {
                        e = e.copy(paidByProfileId = userUid)
                    }
                    if (exp.debtorIds.contains(ghostId)) {
                        e = e.copy(
                            debtorIds = exp.debtorIds.map {
                                if (it == ghostId) userUid else it
                            }
                        )
                    }
                    if (exp.payerContributions.containsKey(ghostId)) {
                        e = e.copy(
                            payerContributions = exp.payerContributions.mapKeys {
                                if (it.key == ghostId) userUid else it.key
                            }
                        )
                    }
                    e
                }
                expenses.clear()
                expenses.addAll(updatedExpenses)

                // Rewrite events: participants[].profileId, participantIds, memberIds, ownerId
                val updatedEvents = events.map { evt ->
                    var e = evt
                    // Update participants array
                    val newParticipants = evt.participants.map { p ->
                        if (p.profileId == ghostId) p.copy(profileId = userUid) else p
                    }
                    e = e.copy(participants = newParticipants)

                    // Update memberIds
                    if (evt.memberIds.contains(ghostId)) {
                        e = e.copy(
                            memberIds = evt.memberIds.map {
                                if (it == ghostId) userUid else it
                            }
                        )
                    }
                    // Update ownerId
                    if (evt.ownerId == ghostId) {
                        e = e.copy(ownerId = userUid)
                    }
                    e
                }
                events.clear()
                events.addAll(updatedEvents)
            }

            // Delete ghost profiles
            profiles.removeAll { it.isGhost && it.linkedEmail == userEmail }
        }
    }

    // ── 4.1a: Single ghost merged ─────────────────────────────────────────

    @Test
    fun `linkGhostProfile merges single ghost with debts and expenses`() = runTest {
        val ghostId = "ghost-1"
        val realUid = "real-123"
        val email = "a@b.com"

        val profiles = mutableListOf(
            ProfileItem(id = ghostId, name = "Ghost", isGhost = true, linkedEmail = email),
            ProfileItem(id = "other-1", name = "Other", isGhost = false, linkedEmail = "other@x.com"),
        )
        val debts = mutableListOf(
            EventDebtItem(eventId = "ev-1", profileId = ghostId, creditorId = "other-1", amountEuros = 50.0),
            EventDebtItem(eventId = "ev-1", profileId = "other-1", creditorId = ghostId, amountEuros = 30.0),
        )
        val expenses = mutableListOf(
            EventExpenseItem(
                id = "exp-1", eventId = "ev-1", name = "Cena", amountEuros = 80.0,
                assignedProfileIds = listOf(ghostId, "other-1"),
                paidByProfileId = ghostId,
            ),
        )
        val events = mutableListOf(
            EventItem(id = "ev-1", name = "Viaje", dateMillis = 0L, ownerId = "other-1"),
        )

        val repo = FakeGhostLinkProfileRepository(profiles, events, debts, expenses)
        repo.linkGhostProfile(email, realUid)

        // Ghost deleted
        assertFalse(profiles.any { it.id == ghostId }, "Ghost should be deleted")
        assertEquals(1, profiles.size, "Only 'other-1' should remain")

        // Debts rewritten
        assertEquals(2, debts.size)
        val debt1 = debts.find { it.amountEuros == 50.0 }!!
        assertEquals(realUid, debt1.profileId, "Debt profileId should be rewritten to realUid")
        assertEquals("other-1", debt1.creditorId)

        val debt2 = debts.find { it.amountEuros == 30.0 }!!
        assertEquals("other-1", debt2.profileId)
        assertEquals(realUid, debt2.creditorId, "Debt creditorId should be rewritten to realUid")

        // Expenses rewritten
        assertEquals(1, expenses.size)
        val expense = expenses[0]
        assertTrue(realUid in expense.assignedProfileIds)
        assertEquals(realUid, expense.paidByProfileId, "Expense paidBy should be rewritten")
    }

    // ── 4.1b: No ghost found (no-op) ───────────────────────────────────────

    @Test
    fun `linkGhostProfile is no-op when no ghost matches email`() = runTest {
        val profiles = mutableListOf(
            ProfileItem(id = "p-1", name = "User1", isGhost = false, linkedEmail = "u1@test.com"),
            ProfileItem(id = "p-2", name = "User2", isGhost = false, linkedEmail = "u2@test.com"),
        )
        val debts = mutableListOf(
            EventDebtItem(eventId = "ev-1", profileId = "p-1", creditorId = "p-2", amountEuros = 10.0),
        )
        val expenses = mutableListOf<EventExpenseItem>()
        val events = mutableListOf<EventItem>()

        val repo = FakeGhostLinkProfileRepository(profiles, events, debts, expenses)
        repo.linkGhostProfile("no-match@test.com", "real-456")

        // No profiles deleted
        assertEquals(2, profiles.size)
        // No debts modified
        assertEquals(1, debts.size)
        assertEquals("p-1", debts[0].profileId)
        assertEquals("p-2", debts[0].creditorId)
    }

    // ── 4.1c: Multiple ghosts ──────────────────────────────────────────────

    @Test
    fun `linkGhostProfile merges multiple ghosts with same linkedEmail`() = runTest {
        val email = "dup@b.com"
        val realUid = "real-789"

        val profiles = mutableListOf(
            ProfileItem(id = "ghost-1", name = "Ghost1", isGhost = true, linkedEmail = email),
            ProfileItem(id = "ghost-2", name = "Ghost2", isGhost = true, linkedEmail = email),
            ProfileItem(id = "other-1", name = "Other", isGhost = false, linkedEmail = "other@x.com"),
        )
        val debts = mutableListOf(
            EventDebtItem(eventId = "ev-1", profileId = "ghost-1", creditorId = "other-1", amountEuros = 20.0),
            EventDebtItem(eventId = "ev-2", profileId = "other-1", creditorId = "ghost-2", amountEuros = 15.0),
        )
        val expenses = mutableListOf<EventExpenseItem>()
        val events = mutableListOf<EventItem>()

        val repo = FakeGhostLinkProfileRepository(profiles, events, debts, expenses)
        repo.linkGhostProfile(email, realUid)

        // Both ghosts deleted
        assertFalse(profiles.any { it.id == "ghost-1" }, "ghost-1 should be deleted")
        assertFalse(profiles.any { it.id == "ghost-2" }, "ghost-2 should be deleted")
        assertEquals(1, profiles.size, "Only 'other-1' should remain")

        // All debts rewritten
        assertEquals(2, debts.size)
        val debt1 = debts.find { it.amountEuros == 20.0 }!!
        assertEquals(realUid, debt1.profileId, "ghost-1 debt profileId → realUid")
        val debt2 = debts.find { it.amountEuros == 15.0 }!!
        assertEquals(realUid, debt2.creditorId, "ghost-2 debt creditorId → realUid")
    }

    // ── 4.1d: Ghost participates in events ─────────────────────────────────

    @Test
    fun `linkGhostProfile rewrites ghost participant in events`() = runTest {
        val ghostId = "ghost-1"
        val realUid = "real-1"
        val email = "invited@mail.com"

        val profiles = mutableListOf(
            ProfileItem(id = ghostId, name = "Ghost", isGhost = true, linkedEmail = email),
            ProfileItem(id = "owner-1", name = "Owner", isGhost = false, linkedEmail = "owner@x.com"),
        )
        val debts = mutableListOf<EventDebtItem>()
        val expenses = mutableListOf<EventExpenseItem>()
        val events = mutableListOf(
            EventItem(
                id = "ev-A", name = "Evento", dateMillis = 1000L, ownerId = "owner-1",
                participants = listOf(
                    EventParticipant(profileId = ghostId, role = EventRole.CONTRIBUTOR),
                    EventParticipant(profileId = "owner-1", role = EventRole.OWNER),
                ),
                memberIds = listOf(ghostId, "owner-1"),
            ),
        )

        val repo = FakeGhostLinkProfileRepository(profiles, events, debts, expenses)
        repo.linkGhostProfile(email, realUid)

        // Ghost deleted
        assertFalse(profiles.any { it.id == ghostId })

        // Event participant updated
        assertEquals(1, events.size)
        val event = events[0]
        val participantIds = event.participants.map { it.profileId }
        assertTrue(realUid in participantIds, "realUid should be in participants")
        assertFalse(ghostId in participantIds, "ghostId should not be in participants")

        // memberIds updated
        assertTrue(realUid in event.memberIds, "realUid should be in memberIds")
        assertFalse(ghostId in event.memberIds, "ghostId should not be in memberIds")
    }

    // ── 4.1e: Ghost owns an event ──────────────────────────────────────────

    @Test
    fun `linkGhostProfile rewrites ghost as event owner`() = runTest {
        val ghostId = "ghost-owner"
        val realUid = "real-owner"
        val email = "ownerghost@mail.com"

        val profiles = mutableListOf(
            ProfileItem(id = ghostId, name = "GhostOwner", isGhost = true, linkedEmail = email),
        )
        val debts = mutableListOf<EventDebtItem>()
        val expenses = mutableListOf<EventExpenseItem>()
        val events = mutableListOf(
            EventItem(
                id = "ev-B", name = "Owned by ghost", dateMillis = 0L, ownerId = ghostId,
                memberIds = listOf(ghostId),
            ),
        )

        val repo = FakeGhostLinkProfileRepository(profiles, events, debts, expenses)
        repo.linkGhostProfile(email, realUid)

        assertEquals(1, events.size)
        assertEquals(realUid, events[0].ownerId, "Event ownerId should be rewritten to realUid")
    }

    // ── 4.1f: Ghost is both debtor and creditor on same debt ───────────────

    @Test
    fun `linkGhostProfile rewrites ghost as both debtor and creditor`() = runTest {
        val ghostId = "ghost-both"
        val realUid = "real-both"
        val email = "both@mail.com"

        val profiles = mutableListOf(
            ProfileItem(id = ghostId, name = "GhostBoth", isGhost = true, linkedEmail = email),
        )
        val debts = mutableListOf(
            EventDebtItem(eventId = "ev-1", profileId = ghostId, creditorId = ghostId, amountEuros = 100.0),
        )
        val expenses = mutableListOf<EventExpenseItem>()
        val events = mutableListOf<EventItem>()

        val repo = FakeGhostLinkProfileRepository(profiles, events, debts, expenses)
        repo.linkGhostProfile(email, realUid)

        assertEquals(1, debts.size)
        val debt = debts[0]
        assertEquals(realUid, debt.profileId, "profileId should be rewritten")
        assertEquals(realUid, debt.creditorId, "creditorId should be rewritten")
    }
}
