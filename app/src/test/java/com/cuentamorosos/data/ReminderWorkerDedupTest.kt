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
 * The worker dispatches [NotificationEvent.PaymentReminder] notifications;
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

    // ── Fingerprint format used by worker's PaymentReminder dispatches ──

    @Test
    fun `worker PaymentReminder fingerprint format matches dispatcher fingerprintFor`() {
        val event = NotificationEvent.PaymentReminder(
            eventId = "evt-w1",
            profileName = "Luis",
            amountEuros = 15.50,
            isOwedToYou = true,
        )

        val fp = NotificationDispatcher.fingerprintFor(event)

        assertEquals(
            "PAYMENT_REMINDER:evt-w1:Luis",
            fp
        )
    }

    @Test
    fun `worker PaymentReminder fingerprint is deterministic`() {
        val event1 = NotificationEvent.PaymentReminder("evt-1", "Luis", 10.0, true)
        val event2 = NotificationEvent.PaymentReminder("evt-1", "Luis", 10.0, true)

        assertEquals(
            "Same PaymentReminder fields must produce identical fingerprint",
            NotificationDispatcher.fingerprintFor(event1),
            NotificationDispatcher.fingerprintFor(event2),
        )
    }

    @Test
    fun `different eventIds produce different PaymentReminder fingerprints`() {
        val fp1 = NotificationDispatcher.fingerprintFor(
            NotificationEvent.PaymentReminder("evt-a", "Luis", 10.0, true)
        )
        val fp2 = NotificationDispatcher.fingerprintFor(
            NotificationEvent.PaymentReminder("evt-b", "Luis", 10.0, true)
        )

        assertTrue(
            "Different eventIds must produce different fingerprints",
            fp1 != fp2
        )
    }

    // ── Idempotency: worker re-run produces zero new notifications ──────

    @Test
    fun `dispatcher with store skips already-sent PaymentReminder`() {
        val event = NotificationEvent.PaymentReminder(
            eventId = "evt-skip-worker",
            profileName = "Luis",
            amountEuros = 15.50,
            isOwedToYou = true,
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
            NotificationEvent.PaymentReminder("evt-1", "Luis", 10.0, true),
            NotificationEvent.PaymentReminder("evt-2", "Ana", 20.0, false),
            NotificationEvent.PaymentReminder("evt-3", "Bob", 30.0, true),
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
    fun `dispatcher with store dispatches only new PaymentReminder when some already sent`() {
        val event1 = NotificationEvent.PaymentReminder("evt-old", "Luis", 10.0, true)
        val event2 = NotificationEvent.PaymentReminder("evt-new", "Ana", 20.0, false)

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
        val event = NotificationEvent.PaymentReminder(
            eventId = "evt-no-dedup",
            profileName = "Luis",
            amountEuros = 15.50,
            isOwedToYou = true,
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
            NotificationEvent.PaymentReminder("evt-nostore-1", "Luis", 10.0, true),
            NotificationEvent.PaymentReminder("evt-nostore-2", "Ana", 20.0, false),
            NotificationEvent.PaymentReminder("evt-nostore-3", "Bob", 30.0, true),
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
        val event = NotificationEvent.PaymentReminder(
            eventId = "evt-persist-worker",
            profileName = "Luis",
            amountEuros = 15.50,
            isOwedToYou = true,
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
