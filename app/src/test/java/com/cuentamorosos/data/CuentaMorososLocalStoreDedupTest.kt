package com.cuentamorosos.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.EventState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
class CuentaMorososLocalStoreDedupTest {

    private lateinit var context: Context
    private lateinit var store: CuentaMorososLocalStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        store = CuentaMorososLocalStore(context)
        // Clear any pre-existing fingerprints from SharedPreferences
        store.clearAll()
    }

    // ── Basic registry behavior ──────────────────────────────────────────

    @Test
    fun `hasNotificationBeenSent returns false for unknown fingerprint`() {
        assertFalse(store.hasNotificationBeenSent("CALCULATION_COMPLETED:evt-unknown"))
    }

    @Test
    fun `recordNotificationSent and hasNotificationBeenSent roundtrip`() {
        val fingerprint = "CALCULATION_COMPLETED:evt-1"

        assertFalse(store.hasNotificationBeenSent(fingerprint))
        store.recordNotificationSent(fingerprint)
        assertTrue(store.hasNotificationBeenSent(fingerprint))
    }

    @Test
    fun `dedup - same fingerprint registered twice returns true`() {
        val fingerprint = "INVITATION_RECEIVED:evt-1:inv-1"

        store.recordNotificationSent(fingerprint)
        store.recordNotificationSent(fingerprint) // second call, same fingerprint

        assertTrue(store.hasNotificationBeenSent(fingerprint))
    }

    // ── Null / blank safety ──────────────────────────────────────────────

    @Test
    fun `hasNotificationBeenSent returns false for empty fingerprint`() {
        assertFalse(store.hasNotificationBeenSent(""))
    }

    @Test
    fun `hasNotificationBeenSent returns false for blank fingerprint`() {
        assertFalse(store.hasNotificationBeenSent("   "))
    }

    @Test
    fun `recordNotificationSent with empty fingerprint no-ops`() {
        // Should not throw
        store.recordNotificationSent("")
        // Verify no side effects — registry should still be empty
        assertFalse(store.hasNotificationBeenSent("CALCULATION_COMPLETED:evt-test"))
    }

    @Test
    fun `recordNotificationSent with blank fingerprint no-ops`() {
        store.recordNotificationSent("   ")
        assertFalse(store.hasNotificationBeenSent("CALCULATION_COMPLETED:evt-test"))
    }

    // ── Persistence across store instances (simulates app restart) ────────

    @Test
    fun `fingerprint survives store recreation`() {
        val fingerprint = "CALCULATION_COMPLETED:evt-persist"

        store.recordNotificationSent(fingerprint)

        // Simulate app restart: create a new store instance with same Context
        val newStore = CuentaMorososLocalStore(context)
        assertTrue(
            "Fingerprint must survive app restart",
            newStore.hasNotificationBeenSent(fingerprint)
        )
    }

    @Test
    fun `fingerprint does not persist across clearAll`() {
        val fingerprint = "CALCULATION_COMPLETED:evt-clear"

        store.recordNotificationSent(fingerprint)
        assertTrue(store.hasNotificationBeenSent(fingerprint))

        store.clearAll()

        val newStore = CuentaMorososLocalStore(context)
        assertFalse(
            "After clearAll, fingerprint should be gone",
            newStore.hasNotificationBeenSent(fingerprint)
        )
    }

    // ── Cleanup: old pruned, recent kept ─────────────────────────────────

    @Test
    fun `cleanupOldEntries prunes entries older than threshold`() {
        // The cleanup method checks epoch-millis prefix of each entry.
        val now = System.currentTimeMillis()
        val oldEpoch = now - (31L * 24 * 60 * 60 * 1000) // 31 days ago

        val recentFingerprint = "CALCULATION_COMPLETED:evt-recent"
        store.recordNotificationSent(recentFingerprint)

        // Directly insert an old entry into SharedPreferences StringSet
        val prefs = context.getSharedPreferences("cuenta_morosos_store", Context.MODE_PRIVATE)
        val oldEntry = "$oldEpoch|CALCULATION_COMPLETED:evt-old"
        val currentSet = prefs.getStringSet("sent_fingerprints", emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.add(oldEntry)
        prefs.edit().putStringSet("sent_fingerprints", currentSet).commit()

        // Verify that the recent entry exists before cleanup
        assertTrue(store.hasNotificationBeenSent(recentFingerprint))

        // Run cleanup with default 30-day threshold
        store.cleanupOldEntries()

        // Recent entry should survive
        assertTrue(
            "Recent entry must survive cleanup",
            store.hasNotificationBeenSent(recentFingerprint)
        )
        // Old entry should be pruned
        assertFalse(
            "Old entry must be pruned by cleanup",
            store.hasNotificationBeenSent("CALCULATION_COMPLETED:evt-old")
        )
    }

    @Test
    fun `cleanupOldEntries with empty registry no-ops safely`() {
        // clean state already (clearAll in setUp)
        store.cleanupOldEntries() // Should not throw
        // No assertion needed — the test is that this does not crash
    }

    // ── Thread safety ────────────────────────────────────────────────────

    @Test
    fun `concurrent writes do not lose data`() {
        val threadCount = 5
        val latch = CountDownLatch(threadCount)
        val fingerprints = (1..threadCount).map { "INVITATION_RECEIVED:evt-1:inv-$it" }

        // Launch multiple threads writing different fingerprints concurrently
        val threads = fingerprints.map { fp ->
            Thread {
                store.recordNotificationSent(fp)
                latch.countDown()
            }
        }
        threads.forEach { it.start() }
        latch.await()

        // All fingerprints must be persisted
        fingerprints.forEach { fp ->
            assertTrue(
                "Fingerprint $fp must be persisted after concurrent write",
                store.hasNotificationBeenSent(fp)
            )
        }
        assertEquals(
            threadCount,
            fingerprints.count { store.hasNotificationBeenSent(it) }
        )
    }

    // ── Migration seed ───────────────────────────────────────────────────

    @Test
    fun `seedDedupMigration seeds only CALCULATED events`() {
        val events = listOf(
            EventItem(
                id = "evt-1", name = "Event 1", dateMillis = System.currentTimeMillis(),
                ownerId = "owner-1", state = EventState.CALCULATED,
            ),
            EventItem(
                id = "evt-2", name = "Event 2", dateMillis = System.currentTimeMillis(),
                ownerId = "owner-1", state = EventState.CALCULATED,
            ),
            EventItem(
                id = "evt-3", name = "Event 3", dateMillis = System.currentTimeMillis(),
                ownerId = "owner-1", state = EventState.OPEN, // NOT calculated
            ),
            EventItem(
                id = "evt-4", name = "Event 4", dateMillis = System.currentTimeMillis(),
                ownerId = "owner-1", state = EventState.DRAFT, // NOT calculated
            ),
        )

        store.seedDedupMigration(events)

        // CALCULATED events should be seeded
        assertTrue(store.hasNotificationBeenSent("CALCULATION_COMPLETED:evt-1"))
        assertTrue(store.hasNotificationBeenSent("CALCULATION_COMPLETED:evt-2"))
        // Non-CALCULATED events must NOT be seeded
        assertFalse(store.hasNotificationBeenSent("CALCULATION_COMPLETED:evt-3"))
        assertFalse(store.hasNotificationBeenSent("CALCULATION_COMPLETED:evt-4"))
    }

    @Test
    fun `seedDedupMigration with empty list no-ops`() {
        store.seedDedupMigration(emptyList())
        // No fingerprints should be created
        assertFalse(store.hasNotificationBeenSent("CALCULATION_COMPLETED:any-event"))
    }
}
