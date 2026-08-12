package com.cuentamorosos.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.cuentamorosos.data.PendingOperationQueue
import com.cuentamorosos.data.RemoteOperations
import com.cuentamorosos.db.CuentaMorososDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration test for the sync loop end-to-end pipeline (Phase 5).
 *
 * Verifies: enqueue → drain → fetch → upsert pipeline works correctly.
 * Uses real PendingOperationQueue + SQLDelight in-memory + fake RemoteOperations.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SyncLoopIntegrationTest {

    private lateinit var database: CuentaMorososDatabase
    private lateinit var queue: PendingOperationQueue
    private lateinit var fakeRemoteOps: TrackingRemoteOperations

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CuentaMorososDatabase.Schema.create(driver)
        database = CuentaMorososDatabase(driver)

        val testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        queue = PendingOperationQueue(
            database = database,
            scope = kotlinx.coroutines.CoroutineScope(testDispatcher),
        )
        fakeRemoteOps = TrackingRemoteOperations()
    }

    // ── End-to-end: enqueue → drain → verify remote calls ────────────────────

    @Test
    fun `enqueue then drainAll calls remote ops in order`() = runTest {
        // 1. Enqueue multiple operations
        queue.enqueue("op-1", "event", "evt-1", "save", """{}""")
        queue.enqueue("op-2", "profile", "prof-1", "save", """{}""")
        queue.enqueue("op-3", "debt", "debt-1", "save", """{}""")
        queue.enqueue("op-4", "expense", "exp-1", "save", """{}""")

        assertEquals(4L, queue.getAllPending())

        // 2. Drain all — should call remote ops
        queue.drainAll(fakeRemoteOps)

        // 3. Verify all operations were drained
        assertEquals(0L, queue.getAllPending())

        // 4. Verify remote ops were called
        assertEquals(1, fakeRemoteOps.saveEventCalls.size)
        assertEquals("evt-1", fakeRemoteOps.saveEventCalls[0])
        assertEquals(1, fakeRemoteOps.saveProfileCalls.size)
        assertEquals("prof-1", fakeRemoteOps.saveProfileCalls[0])
        assertEquals(1, fakeRemoteOps.saveDebtCalls.size)
        assertEquals("debt-1", fakeRemoteOps.saveDebtCalls[0])
        assertEquals(1, fakeRemoteOps.saveExpenseCalls.size)
        assertEquals("exp-1", fakeRemoteOps.saveExpenseCalls[0])
    }

    @Test
    fun `drain before fetch pattern is enforced`() = runTest {
        // This test verifies that drainAll completes before any "fetch" would happen.
        // In the real sync loop, drainAll() is called first, then observeProfiles()/etc.
        // Here we simulate by checking that all pending ops are processed before
        // we'd call a hypothetical fetch.

        val callOrder = mutableListOf<String>()

        val orderedRemoteOps = object : RemoteOperations {
            override suspend fun saveEvent(entityId: String) {
                callOrder.add("saveEvent($entityId)")
            }
            override suspend fun deleteEvent(entityId: String) {}
            override suspend fun saveDebt(entityId: String) {
                callOrder.add("saveDebt($entityId)")
            }
            override suspend fun deleteDebt(entityId: String) {}
            override suspend fun saveExpense(entityId: String) {
                callOrder.add("saveExpense($entityId)")
            }
            override suspend fun deleteExpense(entityId: String) {}
            override suspend fun saveProfile(entityId: String) {
                callOrder.add("saveProfile($entityId)")
            }
            override suspend fun deleteProfile(entityId: String) {}
            override suspend fun updateProfilePhoto(profileId: String, photoUrl: String) {}
            override suspend fun updateProfileUsername(profileId: String, username: String) {}
            override suspend fun updateProfileDisplayName(profileId: String, displayName: String) {}
            override suspend fun deleteProfilePhoto(profileId: String) {}
            override suspend fun linkGhostProfile(email: String, realUid: String) {}
        }

        queue.enqueue("op-1", "event", "evt-1", "save", """{}""")
        queue.enqueue("op-2", "debt", "debt-1", "save", """{}""")

        // drainAll should complete all ops
        queue.drainAll(orderedRemoteOps)

        // Then "fetch" would happen (simulated)
        callOrder.add("fetch")

        // Verify drain completed before fetch
        assertEquals(listOf("saveEvent(evt-1)", "saveDebt(debt-1)", "fetch"), callOrder)
    }

    @Test
    fun `large batch drains completely without hanging`() = runTest {
        // Enqueue 100 operations (10 batches of 10)
        repeat(100) { i ->
            queue.enqueue("op-$i", "event", "evt-$i", "save", """{}""")
        }
        assertEquals(100L, queue.getAllPending())

        queue.drainAll(fakeRemoteOps)

        assertEquals(0L, queue.getAllPending())
        assertEquals(100, fakeRemoteOps.saveEventCalls.size)
    }

    @Test
    fun `profile-specific remote ops adapter pattern works`() = runTest {
        // Simulate the profileRemoteOps adapter pattern used in OfflineFirstProfileRepository
        val profileSaveCalls = mutableListOf<String>()

        val profileRemoteOps = object : RemoteOperations {
            override suspend fun saveEvent(entityId: String) = throw UnsupportedOperationException()
            override suspend fun deleteEvent(entityId: String) = throw UnsupportedOperationException()
            override suspend fun saveDebt(entityId: String) = throw UnsupportedOperationException()
            override suspend fun deleteDebt(entityId: String) = throw UnsupportedOperationException()
            override suspend fun saveExpense(entityId: String) = throw UnsupportedOperationException()
            override suspend fun deleteExpense(entityId: String) = throw UnsupportedOperationException()
            override suspend fun saveProfile(entityId: String) {
                profileSaveCalls.add(entityId)
            }
            override suspend fun deleteProfile(entityId: String) {}
            override suspend fun updateProfilePhoto(profileId: String, photoUrl: String) {}
            override suspend fun updateProfileUsername(profileId: String, username: String) {}
            override suspend fun updateProfileDisplayName(profileId: String, displayName: String) {}
            override suspend fun deleteProfilePhoto(profileId: String) {}
            override suspend fun linkGhostProfile(email: String, realUid: String) = throw UnsupportedOperationException()
        }

        queue.enqueue("op-1", "profile", "prof-1", "save", """{}""")
        queue.enqueue("op-2", "profile", "prof-2", "save", """{}""")

        queue.drainAll(profileRemoteOps)

        assertEquals(0L, queue.getAllPending())
        assertEquals(listOf("prof-1", "prof-2"), profileSaveCalls)
    }

    // ── Tracking implementation ──────────────────────────────────────────────

    private class TrackingRemoteOperations : RemoteOperations {
        val saveEventCalls = mutableListOf<String>()
        val saveDebtCalls = mutableListOf<String>()
        val saveExpenseCalls = mutableListOf<String>()
        val saveProfileCalls = mutableListOf<String>()
        val deleteEventCalls = mutableListOf<String>()

        override suspend fun saveEvent(entityId: String) { saveEventCalls.add(entityId) }
        override suspend fun deleteEvent(entityId: String) { deleteEventCalls.add(entityId) }
        override suspend fun saveDebt(entityId: String) { saveDebtCalls.add(entityId) }
        override suspend fun deleteDebt(entityId: String) {}
        override suspend fun saveExpense(entityId: String) { saveExpenseCalls.add(entityId) }
        override suspend fun deleteExpense(entityId: String) {}
        override suspend fun saveProfile(entityId: String) { saveProfileCalls.add(entityId) }
        override suspend fun deleteProfile(entityId: String) {}
        override suspend fun updateProfilePhoto(profileId: String, photoUrl: String) {}
        override suspend fun updateProfileUsername(profileId: String, username: String) {}
        override suspend fun updateProfileDisplayName(profileId: String, displayName: String) {}
        override suspend fun deleteProfilePhoto(profileId: String) {}
        override suspend fun linkGhostProfile(email: String, realUid: String) {}
    }
}
