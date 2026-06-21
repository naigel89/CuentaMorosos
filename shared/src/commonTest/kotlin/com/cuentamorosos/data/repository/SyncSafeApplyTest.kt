package com.cuentamorosos.data.repository

import com.cuentamorosos.model.EventDebtItem
import com.cuentamorosos.model.SettlementTransfer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for the sync-safe calculation apply guard (Fix 2).
 *
 * Verifies that the [DebtRepository.applyCalculation] transaction is protected
 * from snapshot-listener interference by a Mutex-guarded [applyingEvents] flag
 * with a 30-second timeout.
 *
 * The [FakeGuardedDebtRepository] mirrors the same Mutex + applyingEvents +
 * withTimeout pattern that [OfflineFirstDebtRepository] uses, allowing us
 * to test the guard behavior at the interface level.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SyncSafeApplyTest {

    // ── Helper factories ──────────────────────────────────────────────────

    private fun makeTransfer(from: String, to: String, amount: Double) =
        SettlementTransfer(fromProfileId = from, toProfileId = to, amount = amount)

    private fun makeOldDebt(
        eventId: String = "ev-1",
        profileId: String = "p-1",
    ) = EventDebtItem(
        eventId = eventId,
        profileId = profileId,
        amountEuros = 100.0,
    )

    // ── Test 1: Normal apply replaces debts correctly ─────────────────────

    @Test
    fun `applyCalculation replaces debts and leaves no old artifacts`() = runTest {
        val repo = FakeGuardedDebtRepository()
        val eventId = "ev-1"
        val modeId = "settlement-v1"

        // Pre-populate with 3 old debts
        val oldDebts = listOf(
            makeOldDebt(eventId, "p-a"),
            makeOldDebt(eventId, "p-b"),
            makeOldDebt(eventId, "p-c"),
        )
        oldDebts.forEach { repo.saveDebt(it) }
        assertEquals(3, repo.fetchDebtsForEvent(eventId).size)

        // Apply new calculation: 2 transfers
        val transfers = listOf(
            makeTransfer("p-a", "p-b", 10.0),
            makeTransfer("p-c", "p-a", 15.0),
        )

        // Call through the DebtRepository interface so the test verifies
        // the contract — this WON'T COMPILE until applyCalculation is
        // added to DebtRepository (T2.2).
        val debtRepo: DebtRepository = repo
        debtRepo.applyCalculation(eventId, modeId, transfers)

        // After apply, ONLY the 2 new debts should exist
        val result = repo.fetchDebtsForEvent(eventId)
        assertEquals(2, result.size)

        val amounts = result.map { it.amountEuros }.sorted()
        assertEquals(listOf(10.0, 15.0), amounts)

        // Verify the debts have correct profileId/creditorId from transfers
        val profiles = result.map { it.profileId }.toSet()
        assertTrue("p-a" in profiles)
        assertTrue("p-c" in profiles)

        // Verify creditorId is propagated
        val debtForPa = result.find { it.profileId == "p-a" } ?: error("Expected debt for p-a")
        assertEquals("p-b", debtForPa.creditorId)

        val debtForPc = result.find { it.profileId == "p-c" } ?: error("Expected debt for p-c")
        assertEquals("p-a", debtForPc.creditorId)
    }

    // ── Test 2: Concurrent applies are serialized by Mutex ────────────────

    @Test
    fun `concurrent applies are serialized by mutex`() = runTest {
        val repo = FakeGuardedDebtRepository()
        val eventId = "ev-1"
        val modeId = "settlement-v1"

        val debtRepo: DebtRepository = repo

        // First apply: uses a controlled lock to delay completion
        repo.holdApplyUntilReleased = true

        // Launch first apply in background
        val transfers1 = listOf(makeTransfer("p-a", "p-b", 50.0))
        val job1 = launch {
            debtRepo.applyCalculation(eventId, modeId, transfers1)
        }

        // Wait for the first apply to acquire the lock
        delay(100)

        // Verify the first apply IS in progress (has the lock)
        assertTrue(repo.isApplyInProgress)

        // Launch second apply — should suspend waiting for the mutex
        val transfers2 = listOf(makeTransfer("p-c", "p-b", 30.0))
        val job2 = launch {
            debtRepo.applyCalculation(eventId, modeId, transfers2)
        }

        // Verify second apply is NOT executing concurrently
        delay(100)
        assertTrue(repo.isApplyInProgress)

        // Release the first apply
        repo.releaseApply()

        // Both jobs should complete
        job1.join()
        job2.join()

        // After BOTH complete, only the LAST apply's debts remain
        val result = repo.fetchDebtsForEvent(eventId)
        assertEquals(1, result.size)
        assertEquals(30.0, result[0].amountEuros)
        assertEquals("p-c", result[0].profileId)
    }

    // ── Test 3: Snapshot filter skips debts from applying events ─────────

    @Test
    fun `snapshot filter skips debts from events currently being applied`() = runTest {
        val repo = FakeGuardedDebtRepository()
        val eventId = "ev-1"
        val modeId = "settlement-v1"
        val otherEventId = "ev-2"

        // Pre-populate with debts for both events
        repo.saveDebt(makeOldDebt(eventId, "p-a"))
        repo.saveDebt(makeOldDebt(otherEventId, "p-b"))

        // Simulate remote snapshot: debts for both events arrive
        val remoteDebts = listOf(
            makeOldDebt(eventId, "p-a"),       // should be FILTERED during apply
            makeOldDebt(otherEventId, "p-b"),  // should PASS through
        )

        // Start apply in background, holding the lock
        repo.holdApplyUntilReleased = true
        val debtRepo: DebtRepository = repo
        val job = launch {
            debtRepo.applyCalculation(eventId, modeId, emptyList())
        }
        delay(100) // Let it acquire the lock
        assertTrue(repo.isApplyInProgress)

        // Apply snapshot filter (mirrors the real listener's filter)
        val filtered = repo.snapshotFilter(remoteDebts)
        assertEquals(1, filtered.size, "Only non-applying event debts should pass filter")
        assertEquals(otherEventId, filtered[0].eventId)

        // Release and verify
        repo.releaseApply()
        job.join()

        // After apply completes, filter should pass ALL debts
        val filteredAfter = repo.snapshotFilter(remoteDebts)
        assertEquals(2, filteredAfter.size, "After apply, all debts should pass filter")
    }

    // ── Test 4: Timeout releases the applying flag ────────────────────────

    @Test
    fun `timeout releases applying flag after 30 seconds`() = runTest {
        val repo = FakeGuardedDebtRepository()
        val eventId = "ev-1"
        val modeId = "settlement-v1"

        // Make the guard operation take extremely long (simulate timeout)
        repo.applyDelayMs = 40_000L // > 30s timeout
        repo.timeoutOverrideMs = 500L // Override timeout to 500ms for test speed

        val transfers = listOf(makeTransfer("p-a", "p-b", 10.0))
        val debtRepo: DebtRepository = repo

        // applyCalculation should complete (possibly with timeout)
        debtRepo.applyCalculation(eventId, modeId, transfers)

        // After timeout, the flag should be released
        assertTrue(!repo.isApplyInProgress, "Flag should be released after timeout")
    }

    // ── Test 5: Normal sync resumes after apply completes ────────────────

    @Test
    fun `normal sync resumes after apply completes`() = runTest {
        val repo = FakeGuardedDebtRepository()
        val eventId = "ev-1"
        val modeId = "settlement-v1"

        val debtRepo: DebtRepository = repo

        // 1. Apply calculation
        debtRepo.applyCalculation(eventId, modeId, listOf(makeTransfer("p-a", "p-b", 10.0)))

        // 2. Verify apply is done, flag is cleared
        assertTrue(!repo.isApplyInProgress)

        // 3. Simulate snapshot listener receiving new remote data
        val remoteDebts = listOf(
            makeOldDebt(eventId, "p-a"),
            makeOldDebt("ev-2", "p-b"),
        )
        val filtered = repo.snapshotFilter(remoteDebts)

        // 4. All debts should pass filter since no apply is in progress
        assertEquals(2, filtered.size)
    }

    // ── Fake implementation ───────────────────────────────────────────────

    /**
     * A test double that implements [DebtRepository] and adds the sync-guarded
     * [applyCalculation] method with the same Mutex + applyingEvents +
     * withTimeout pattern used by [OfflineFirstDebtRepository].
     *
     * Features for testing:
     * - [holdApplyUntilReleased] / [releaseApply]: control lock acquisition for
     *   concurrency tests
     * - [applyDelayMs]: artificial delay inside the guarded block
     * - [timeoutOverrideMs]: override the 30s timeout (0 = use default)
     * - [snapshotFilter]: mirrors the listener filter logic
     * - [isApplyInProgress]: whether the guard flag is currently held
     */
    private class FakeGuardedDebtRepository : DebtRepository {

        private val applyMutex = Mutex()
        val applyingEvents = mutableSetOf<String>()
        private val debts = mutableListOf<EventDebtItem>()

        // Test control knobs
        var holdApplyUntilReleased = false
        var applyDelayMs = 0L
        var timeoutOverrideMs = 0L

        private val holdLatch = Mutex(locked = holdApplyUntilReleased)

        val isApplyInProgress: Boolean get() = applyingEvents.isNotEmpty()

        fun releaseApply() {
            holdApplyUntilReleased = false
            if (holdLatch.isLocked) holdLatch.unlock()
        }

        /**
         * Mirrors the snapshot-listener filter: debts whose eventId is in
         * [applyingEvents] are skipped to prevent sync-loop interference.
         */
        fun snapshotFilter(remoteDebts: List<EventDebtItem>): List<EventDebtItem> {
            return remoteDebts.filter { it.eventId !in applyingEvents }
        }

        // Overrides DebtRepository.applyCalculation with Mutex-guarded logic
        // that mirrors OfflineFirstDebtRepository's implementation.
        override suspend fun applyCalculation(
            eventId: String,
            modeId: String,
            transfers: List<SettlementTransfer>,
        ) {
            val effectiveTimeout = if (timeoutOverrideMs > 0) timeoutOverrideMs else 30_000L
            applyMutex.withLock {
                applyingEvents.add(eventId)
                try {
                    withTimeoutOrNull(effectiveTimeout) {
                        // Delete old debts for this event
                        debts.removeAll { it.eventId == eventId }

                        // Wait for test control if needed
                        if (holdApplyUntilReleased) {
                            while (holdApplyUntilReleased) {
                                delay(50)
                            }
                        }

                        if (applyDelayMs > 0) {
                            delay(applyDelayMs)
                        }

                        // Create new debts from transfers
                        transfers.forEach { transfer ->
                            debts.add(
                                EventDebtItem(
                                    eventId = eventId,
                                    profileId = transfer.fromProfileId,
                                    creditorId = transfer.toProfileId,
                                    amountEuros = transfer.amount,
                                    calculationMode = modeId,
                                )
                            )
                        }
                    }
                } finally {
                    applyingEvents.remove(eventId)
                }
            }
        }

        // ── DebtRepository interface methods ──────────────────────────

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
            debts.removeAll { it.eventId == eventId }
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
    }
}
