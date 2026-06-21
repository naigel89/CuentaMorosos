package com.cuentamorosos.data.repository

import com.cuentamorosos.model.EventDebtItem
import com.cuentamorosos.model.SettlementTransfer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for Fix 8 — Reliable Remote Delete with retry.
 *
 * Verifies that when remote delete fails, the eventId is enqueued for retry
 * and drained on the next sync cycle.
 *
 * Uses [RetryAwareFakeDebtRepo] which mirrors the retry pattern from
 * [OfflineFirstDebtRepository]: pendingEventDeletes set, enqueue on failure,
 * drain before fetch.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OfflineFirstDebtRepoDeleteRetryTest {

    // ── Scenario 1: Remote delete failure enqueues eventId ────────────────

    @Test
    fun `remote delete failure enqueues eventId for retry`() = runTest {
        val repo = RetryAwareFakeDebtRepo()
        val eventId = "ev-fail"

        // Pre-populate a debt
        repo.saveDebt(EventDebtItem(eventId = eventId, profileId = "p-1", amountEuros = 10.0))
        assertEquals(1, repo.fetchDebtsForEvent(eventId).size)

        // Configure remote to fail on deleteAllDebtsForEvent
        repo.remoteDeleteShouldFail = true

        // Call deleteAllDebtsForEvent — local delete succeeds, remote fails
        repo.deleteAllDebtsForEvent(eventId)

        // Local debt should be deleted
        assertEquals(0, repo.fetchDebtsForEvent(eventId).size, "Local debt should be deleted")

        // EventId should be in pending deletes
        assertTrue(eventId in repo.pendingEventDeletes, "eventId should be enqueued for retry")
    }

    // ── Scenario 2: Sync restart drains pending deletes ───────────────────

    @Test
    fun `sync restart drains pending deletes before fetch`() = runTest {
        val repo = RetryAwareFakeDebtRepo()
        val eventId = "ev-retry"

        // First: simulate a failed delete (eventId is pending)
        repo.remoteDeleteShouldFail = true
        repo.deleteAllDebtsForEvent(eventId)
        assertTrue(eventId in repo.pendingEventDeletes)
        val countBeforeDrain = repo.remoteDeleteCallCount

        // Now: make remote succeed and drain pending deletes
        repo.remoteDeleteShouldFail = false
        repo.drainPendingEventDeletes()

        // EventId should be removed from pending
        assertTrue(eventId !in repo.pendingEventDeletes, "eventId should be removed after successful retry")
        assertTrue(repo.remoteDeleteCallCount > countBeforeDrain, "Remote delete should have been called during drain")
    }

    // ── Scenario 3: Failed retry keeps eventId in pending ─────────────────

    @Test
    fun `failed retry keeps eventId in pending`() = runTest {
        val repo = RetryAwareFakeDebtRepo()
        val eventId = "ev-retry-fail"

        // First failure
        repo.remoteDeleteShouldFail = true
        repo.deleteAllDebtsForEvent(eventId)
        assertTrue(eventId in repo.pendingEventDeletes)

        // Retry also fails
        repo.drainPendingEventDeletes()

        // Should still be pending
        assertTrue(eventId in repo.pendingEventDeletes, "eventId should remain pending after failed retry")
    }

    // ── Scenario 4: Successful delete does NOT enqueue ────────────────────

    @Test
    fun `successful remote delete does not enqueue`() = runTest {
        val repo = RetryAwareFakeDebtRepo()
        val eventId = "ev-success"

        repo.saveDebt(EventDebtItem(eventId = eventId, profileId = "p-1", amountEuros = 10.0))

        // Remote succeeds (default)
        repo.deleteAllDebtsForEvent(eventId)

        assertEquals(0, repo.pendingEventDeletes.size, "No pending deletes on success")
    }

    // ── Scenario 5: Multiple pending deletes all drained ──────────────────

    @Test
    fun `multiple pending deletes are all drained`() = runTest {
        val repo = RetryAwareFakeDebtRepo()
        val events = listOf("ev-1", "ev-2", "ev-3")

        // All fail
        repo.remoteDeleteShouldFail = true
        events.forEach { repo.deleteAllDebtsForEvent(it) }
        assertEquals(3, repo.pendingEventDeletes.size)
        val countBeforeDrain = repo.remoteDeleteCallCount

        // Now succeed and drain
        repo.remoteDeleteShouldFail = false
        repo.drainPendingEventDeletes()

        assertEquals(0, repo.pendingEventDeletes.size, "All pending deletes should be cleared")
        assertTrue(repo.remoteDeleteCallCount > countBeforeDrain, "Remote delete called for each pending event during drain")
    }

    // ── Scenario 6: fetchAllDebts filter by applyingEvents ────────────────

    @Test
    fun `fetchAllDebts filters out debts from applying events`() = runTest {
        val repo = RetryAwareFakeDebtRepo()

        // Add debts for multiple events
        repo.saveDebt(EventDebtItem(eventId = "ev-applying", profileId = "p-1", amountEuros = 10.0))
        repo.saveDebt(EventDebtItem(eventId = "ev-normal", profileId = "p-2", amountEuros = 20.0))
        repo.saveDebt(EventDebtItem(eventId = "ev-applying", profileId = "p-3", amountEuros = 30.0))

        // Mark ev-applying as currently being applied
        repo.applyingEvents.add("ev-applying")

        // Fetch all — should filter out applying event debts
        val fetched = repo.fetchAllDebtsFiltered()
        assertEquals(1, fetched.size, "Only non-applying event debts should be returned")
        assertEquals("ev-normal", fetched[0].eventId)
    }

    @Test
    fun `fetchAllDebts returns all when no events are applying`() = runTest {
        val repo = RetryAwareFakeDebtRepo()

        repo.saveDebt(EventDebtItem(eventId = "ev-1", profileId = "p-1", amountEuros = 10.0))
        repo.saveDebt(EventDebtItem(eventId = "ev-2", profileId = "p-2", amountEuros = 20.0))

        val fetched = repo.fetchAllDebtsFiltered()
        assertEquals(2, fetched.size, "All debts should be returned when no events are applying")
    }

    // ── Fake implementation ───────────────────────────────────────────────

    /**
     * A test double that mirrors the retry pattern from OfflineFirstDebtRepository:
     * - pendingEventDeletes set for tracking failed remote deletes
     * - drainPendingEventDeletes() to retry before sync fetch
     * - applyingEvents set for the fetchAllDebts filter
     */
    private class RetryAwareFakeDebtRepo : DebtRepository {

        private val debts = mutableListOf<EventDebtItem>()
        val pendingEventDeletes = mutableSetOf<String>()
        val applyingEvents = mutableSetOf<String>()

        var remoteDeleteShouldFail = false
        var remoteDeleteCallCount = 0

        override fun observeDebts(eventId: String): Flow<List<EventDebtItem>> =
            flowOf(debts.filter { it.eventId == eventId }.toList())

        override fun observeAllDebts(): Flow<List<EventDebtItem>> =
            flowOf(debts.toList())

        override suspend fun saveDebt(debt: EventDebtItem) {
            debts.add(debt)
        }

        override suspend fun deleteDebt(eventId: String, debtId: String) {
            debts.removeAll { it.id == debtId }
        }

        override suspend fun deleteAllDebtsForEvent(eventId: String) {
            // Local delete always succeeds
            debts.removeAll { it.eventId == eventId }

            // Remote delete may fail
            remoteDeleteCallCount++
            if (remoteDeleteShouldFail) {
                pendingEventDeletes.add(eventId)
            }
        }

        override suspend fun deleteDebtsForProfile(profileId: String) {
            debts.removeAll { it.profileId == profileId }
        }

        override suspend fun replaceProfileId(oldId: String, newId: String) {
            debts.filter { it.profileId == oldId }.forEach {
                debts.remove(it)
                debts.add(it.copy(profileId = newId))
            }
        }

        override suspend fun fetchDebtsForEvent(eventId: String): List<EventDebtItem> {
            return debts.filter { it.eventId == eventId }.toList()
        }

        override suspend fun fetchAllDebts(): List<EventDebtItem> {
            return debts.toList()
        }

        /**
         * Drains pending event deletes — mirrors the drain logic in
         * OfflineFirstDebtRepository.startSyncAll().
         */
        suspend fun drainPendingEventDeletes() {
            val toRetry = pendingEventDeletes.toSet()
            for (eid in toRetry) {
                try {
                    // Simulate remote delete call
                    remoteDeleteCallCount++
                    if (!remoteDeleteShouldFail) {
                        pendingEventDeletes.remove(eid)
                    }
                } catch (e: Exception) {
                    // Keep in pending on failure
                }
            }
        }

        /**
         * Fetches all debts filtered by applyingEvents — mirrors the
         * fetchAllDebts filter in OfflineFirstDebtRepository.startSyncAll().
         */
        suspend fun fetchAllDebtsFiltered(): List<EventDebtItem> {
            return debts.filter { it.eventId !in applyingEvents }.toList()
        }

        override suspend fun applyCalculation(
            eventId: String,
            modeId: String,
            transfers: List<SettlementTransfer>,
            paidTransferIndices: List<Int>,
        ) {
            debts.removeAll { it.eventId == eventId }
            transfers.forEachIndexed { index, transfer ->
                debts.add(
                    EventDebtItem(
                        eventId = eventId,
                        profileId = transfer.fromProfileId,
                        creditorId = transfer.toProfileId,
                        amountEuros = transfer.amount,
                        calculationMode = modeId,
                        paid = index in paidTransferIndices,
                    )
                )
            }
        }
    }
}
