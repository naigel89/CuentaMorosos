package com.cuentamorosos.data

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cuentamorosos.notifications.NotificationDispatcher
import com.cuentamorosos.notifications.NotificationEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowNotificationManager

/**
 * Tests proving ReminderWorker idempotency through the NotificationDispatcher
 * dedup guard when [CuentaMorososLocalStore] is wired into the dispatch path.
 *
 * The worker dispatches [NotificationEvent.UpcomingEvent] notifications;
 * dedup comes from passing the store to [NotificationDispatcher].
 */
@RunWith(AndroidJUnit4::class)
class ReminderWorkerDedupTest {

    private lateinit var context: Context
    private lateinit var store: CuentaMorososLocalStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        store = CuentaMorososLocalStore(context)
        store.clearAll()
    }

    private fun shadowManager(): ShadowNotificationManager {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return org.robolectric.Shadows.shadowOf(manager)
    }

    // ── Fingerprint format used by worker's UpcomingEvent dispatches ────

    @Test
    fun `worker UpcomingEvent fingerprint format matches dispatcher fingerprintFor`() {
        val event = NotificationEvent.UpcomingEvent(
            eventId = "evt-w1",
            eventName = "Test Event",
            daysUntil = 3,
            dateFormatted = "15/06/2026",
        )

        val fp = NotificationDispatcher.fingerprintFor(event)

        assertEquals(
            "UPCOMING_EVENT:evt-w1:3:15/06/2026",
            fp
        )
    }

    @Test
    fun `worker UpcomingEvent fingerprint is deterministic`() {
        val event1 = NotificationEvent.UpcomingEvent("evt-1", "A", 1, "01/01")
        val event2 = NotificationEvent.UpcomingEvent("evt-1", "A", 1, "01/01")

        assertEquals(
            "Same UpcomingEvent fields must produce identical fingerprint",
            NotificationDispatcher.fingerprintFor(event1),
            NotificationDispatcher.fingerprintFor(event2),
        )
    }

    @Test
    fun `different eventIds produce different UpcomingEvent fingerprints`() {
        val fp1 = NotificationDispatcher.fingerprintFor(
            NotificationEvent.UpcomingEvent("evt-a", "A", 1, "01/01")
        )
        val fp2 = NotificationDispatcher.fingerprintFor(
            NotificationEvent.UpcomingEvent("evt-b", "B", 1, "01/01")
        )

        assertTrue(
            "Different eventIds must produce different fingerprints",
            fp1 != fp2
        )
    }

    // ── Idempotency: worker re-run produces zero new notifications ──────

    @Test
    fun `dispatcher with store skips already-sent UpcomingEvent`() {
        val event = NotificationEvent.UpcomingEvent(
            eventId = "evt-skip-worker",
            eventName = "Skip Test",
            daysUntil = 5,
            dateFormatted = "20/06/2026",
        )

        val fingerprint = NotificationDispatcher.fingerprintFor(event)
        assertFalse("Fingerprint must not be registered before first dispatch", store.hasNotificationBeenSent(fingerprint))

        val dispatcher = NotificationDispatcher(context, localStore = store)

        // First dispatch: posts and records
        dispatcher.dispatch(event)
        assertEquals(
            "First dispatch must post the notification",
            1, shadowManager().size()
        )
        assertTrue("Fingerprint must be recorded after first dispatch", store.hasNotificationBeenSent(fingerprint))

        // Second dispatch: skips (already sent)
        dispatcher.dispatch(event)
        assertEquals(
            "Second dispatch must NOT post (dedup guard)",
            1, shadowManager().size()
        )
    }

    @Test
    fun `dispatcher with store skips all when multiple already sent`() {
        val events = listOf(
            NotificationEvent.UpcomingEvent("evt-1", "Event 1", 3, "01/01/2026"),
            NotificationEvent.UpcomingEvent("evt-2", "Event 2", 7, "05/01/2026"),
            NotificationEvent.UpcomingEvent("evt-3", "Event 3", 10, "08/01/2026"),
        )

        val dispatcher = NotificationDispatcher(context, localStore = store)

        // First run: dispatch all
        events.forEach { dispatcher.dispatch(it) }
        assertEquals(
            "First run must dispatch 3 notifications",
            3, shadowManager().size()
        )

        // Second run: all should be skipped (same events, same fingerprints)
        events.forEach { dispatcher.dispatch(it) }
        assertEquals(
            "Second run must produce zero NEW notifications",
            3, shadowManager().size()
        )
    }

    @Test
    fun `dispatcher with store dispatches only new UpcomingEvent when some already sent`() {
        val event1 = NotificationEvent.UpcomingEvent("evt-old", "Old", 5, "10/01/2026")
        val event2 = NotificationEvent.UpcomingEvent("evt-new", "New", 2, "15/01/2026")

        val dispatcher = NotificationDispatcher(context, localStore = store)

        // Dispatch event1 first (records fingerprint)
        dispatcher.dispatch(event1)
        assertEquals(1, shadowManager().size())

        // Now dispatch event1 again + event2
        dispatcher.dispatch(event1) // should skip
        dispatcher.dispatch(event2) // should post

        assertEquals(
            "Only the new (unsent) notification must be posted",
            2, shadowManager().size()
        )
        assertTrue(
            "New event must be recorded in registry",
            store.hasNotificationBeenSent(NotificationDispatcher.fingerprintFor(event2))
        )
    }

    @Test
    fun `dispatcher without store does NOT record fingerprints - worker would re-dispatch`() {
        val event = NotificationEvent.UpcomingEvent(
            eventId = "evt-no-dedup",
            eventName = "No Dedup Test",
            daysUntil = 4,
            dateFormatted = "01/02/2026",
        )

        val fingerprint = NotificationDispatcher.fingerprintFor(event)

        // Dispatcher WITHOUT store: posts but does NOT record fingerprint
        val dispatcherNoStore = NotificationDispatcher(context)
        dispatcherNoStore.dispatch(event)
        assertEquals(1, shadowManager().size())

        // Fingerprint must NOT be in registry (no store → no recording)
        assertFalse(
            "Without store, fingerprint must NOT be recorded",
            store.hasNotificationBeenSent(fingerprint)
        )
    }

    @Test
    fun `different events without store all post without dedup`() {
        val events = listOf(
            NotificationEvent.UpcomingEvent("evt-nostore-1", "E1", 1, "01/01"),
            NotificationEvent.UpcomingEvent("evt-nostore-2", "E2", 2, "02/01"),
            NotificationEvent.UpcomingEvent("evt-nostore-3", "E3", 3, "03/01"),
        )

        val dispatcherNoStore = NotificationDispatcher(context)

        events.forEach { dispatcherNoStore.dispatch(it) }
        assertEquals(
            "Without store, all different events must post",
            3, shadowManager().size()
        )

        // Dispatch again: same tag+id for same eventId → replacement, not dedup
        // (notification count stays 3 because same tag+id overwrites)
        events.forEach { dispatcherNoStore.dispatch(it) }
        assertEquals(
            "Without store, re-dispatch of same events replaces (same tag+id)",
            3, shadowManager().size()
        )
    }

    @Test
    fun `fingerprints persist across dispatcher instances - simulates worker re-creation`() {
        val event = NotificationEvent.UpcomingEvent(
            eventId = "evt-persist-worker",
            eventName = "Persistence Test",
            daysUntil = 2,
            dateFormatted = "30/06/2026",
        )

        // First "worker run": new dispatcher with store
        val dispatcher1 = NotificationDispatcher(context, localStore = store)
        dispatcher1.dispatch(event)
        assertEquals(1, shadowManager().size())

        // Second "worker run": new dispatcher instance, same store
        val dispatcher2 = NotificationDispatcher(context, localStore = store)
        dispatcher2.dispatch(event)

        assertEquals(
            "Second dispatcher instance must skip already-sent notification (persistence across instances)",
            1, shadowManager().size()
        )
    }
}
