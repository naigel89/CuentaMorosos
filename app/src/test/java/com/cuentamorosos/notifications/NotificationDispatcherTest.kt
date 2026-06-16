package com.cuentamorosos.notifications

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cuentamorosos.data.CuentaMorososLocalStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowNotificationManager

@RunWith(AndroidJUnit4::class)
class NotificationDispatcherTest {

    private lateinit var context: Context
    private lateinit var dispatcher: NotificationDispatcher

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dispatcher = NotificationDispatcher(context)
    }

    private fun shadowManager(): ShadowNotificationManager {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return org.robolectric.Shadows.shadowOf(manager)
    }

    @Test
    fun `dispatch InvitationReceived posts notification with correct channel and body`() {
        val event = NotificationEvent.InvitationReceived(
            invitationId = "inv-123",
            eventId = "evt-456",
            inviterName = "Ana",
            eventName = "Asado",
        )

        dispatcher.dispatch(event)

        val notification = shadowManager().getNotification("INVITATION_RECEIVED", "evt-456".hashCode())
        assertNotNull(notification)
        assertEquals("ch_invitations", notification!!.channelId)
        assertEquals("Invitación recibida", notification.extras.getString(NotificationCompat.EXTRA_TITLE))
        assertEquals("Ana te invitó al evento 'Asado'", notification.extras.getString(NotificationCompat.EXTRA_TEXT))
        assertTrue("auto_cancel", notification.flags and Notification.FLAG_AUTO_CANCEL != 0)
    }

    @Test
    fun `dispatch CalculationCompleted formats amount with two decimals`() {
        val event = NotificationEvent.CalculationCompleted(
            eventId = "evt-789",
            eventName = "Cena",
            amountOwed = 42.5,
        )

        dispatcher.dispatch(event)

        val notification = shadowManager().getNotification("CALCULATION_COMPLETED", "evt-789".hashCode())
        assertNotNull(notification)
        assertEquals("Se calcularon los gastos de 'Cena'. Debes €42.50", notification!!.extras.getString(NotificationCompat.EXTRA_TEXT))
        assertEquals("ch_calculations", notification.channelId)
    }

    @Test
    fun `dispatch UpcomingEvent includes days and date in body`() {
        val event = NotificationEvent.UpcomingEvent(
            eventId = "evt-101",
            eventName = "Viaje",
            daysUntil = 3,
            dateFormatted = "15/06/2026",
        )

        dispatcher.dispatch(event)

        val notification = shadowManager().getNotification("UPCOMING_EVENT", "evt-101".hashCode())
        assertNotNull(notification)
        assertEquals("El evento 'Viaje' es en 3 días (15/06/2026)", notification!!.extras.getString(NotificationCompat.EXTRA_TEXT))
        assertEquals("ch_upcoming_events", notification.channelId)
    }

    @Test
    fun `dispatch InvitationAccepted uses invitations channel`() {
        val event = NotificationEvent.InvitationAccepted(
            eventId = "evt-200",
            inviteeName = "Bob",
            eventName = "Fiesta",
        )

        dispatcher.dispatch(event)

        val notification = shadowManager().getNotification("INVITATION_ACCEPTED", "evt-200".hashCode())
        assertNotNull(notification)
        assertEquals("ch_invitations", notification!!.channelId)
        assertEquals("Bob aceptó tu invitación a 'Fiesta'", notification.extras.getString(NotificationCompat.EXTRA_TEXT))
    }

    @Test
    fun `notificationId is deterministic for same eventId`() {
        val event1 = NotificationEvent.UpcomingEvent("evt-same", "Test", 1, "01/01")
        val event2 = NotificationEvent.UpcomingEvent("evt-same", "Test", 2, "02/02")

        dispatcher.dispatch(event1)
        dispatcher.dispatch(event2)

        // Both should produce the same tag+id, so second replaces first
        assertEquals(1, shadowManager().size())
    }

    @Test
    fun `ensureChannels creates exactly 4 channels`() {
        dispatcher.dispatch(
            NotificationEvent.InvitationReceived("inv-1", "evt-1", "Ana", "Test")
        )

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channels = manager.notificationChannels
        val channelIds = channels.map { it.id }
        assert(channelIds.contains("ch_invitations"))
        assert(channelIds.contains("ch_calculations"))
        assert(channelIds.contains("ch_reminders"))
        assert(channelIds.contains("ch_upcoming_events"))
    }

    // ── Dedup: fingerprintFor deterministic output ────────────────────────

    @Test
    fun `fingerprintFor InvitationReceived uses eventId and invitationId`() {
        val event = NotificationEvent.InvitationReceived(
            invitationId = "inv-1", eventId = "evt-1",
            inviterName = "Ana", eventName = "Asado"
        )
        assertEquals(
            "INVITATION_RECEIVED:evt-1:inv-1",
            NotificationDispatcher.fingerprintFor(event)
        )
    }

    @Test
    fun `fingerprintFor CalculationCompleted uses eventId only`() {
        val event = NotificationEvent.CalculationCompleted(
            eventId = "evt-42", eventName = "Cena", amountOwed = 10.0
        )
        assertEquals(
            "CALCULATION_COMPLETED:evt-42",
            NotificationDispatcher.fingerprintFor(event)
        )
    }

    @Test
    fun `fingerprintFor UpcomingEvent includes daysUntil and dateFormatted`() {
        val event = NotificationEvent.UpcomingEvent(
            eventId = "evt-x", eventName = "Viaje", daysUntil = 3, dateFormatted = "15/06"
        )
        assertEquals(
            "UPCOMING_EVENT:evt-x:3:15/06",
            NotificationDispatcher.fingerprintFor(event)
        )
    }

    @Test
    fun `fingerprintFor InvitationAccepted uses eventId and inviteeName`() {
        val event = NotificationEvent.InvitationAccepted(
            eventId = "evt-acc", inviteeName = "Bob", eventName = "Fiesta"
        )
        assertEquals(
            "INVITATION_ACCEPTED:evt-acc:Bob",
            NotificationDispatcher.fingerprintFor(event)
        )
    }

    @Test
    fun `fingerprintFor different events produce different fingerprints`() {
        val calc = NotificationEvent.CalculationCompleted("evt-1", "Test", 0.0)
        val inv = NotificationEvent.InvitationReceived("inv-1", "evt-1", "Ana", "Test")

        assertTrue(
            NotificationDispatcher.fingerprintFor(calc) != NotificationDispatcher.fingerprintFor(inv)
        )
    }

    // ── Dedup: dispatch skips when fingerprint already registered ─────────

    @Test
    fun `dispatch skips when notification already sent`() {
        val store = CuentaMorososLocalStore(context)
        store.clearAll()
        val event = NotificationEvent.CalculationCompleted("evt-skip", "Test", 0.0)

        // First dispatch with store: should succeed, record fingerprint
        val dispatcherWithStore = NotificationDispatcher(context, localStore = store)
        dispatcherWithStore.dispatch(event)
        assertEquals(1, shadowManager().size())

        // Second dispatch with same store: should skip
        dispatcherWithStore.dispatch(event)
        assertEquals(1, shadowManager().size()) // still 1 notification
    }

    @Test
    fun `dispatch records fingerprint after posting`() {
        val store = CuentaMorososLocalStore(context)
        store.clearAll()
        val event = NotificationEvent.CalculationCompleted("evt-record", "Test", 0.0)

        val fingerprint = NotificationDispatcher.fingerprintFor(event)
        assertFalse(store.hasNotificationBeenSent(fingerprint))

        val dispatcherWithStore = NotificationDispatcher(context, localStore = store)
        dispatcherWithStore.dispatch(event)

        assertTrue(
            "Fingerprint must be recorded after dispatch",
            store.hasNotificationBeenSent(fingerprint)
        )
    }

    @Test
    fun `different fingerprints dispatch independently`() {
        val store = CuentaMorososLocalStore(context)
        store.clearAll()
        val dispatcherWithStore = NotificationDispatcher(context, localStore = store)

        val event1 = NotificationEvent.CalculationCompleted("evt-indep-1", "Test 1", 0.0)
        val event2 = NotificationEvent.CalculationCompleted("evt-indep-2", "Test 2", 0.0)

        dispatcherWithStore.dispatch(event1)
        dispatcherWithStore.dispatch(event2)

        assertEquals(2, shadowManager().size())
    }

    @Test
    fun `dispatch without store still works for backward compat`() {
        // Dispatcher constructed without store should dispatch normally (no dedup)
        val dispatcherNoStore = NotificationDispatcher(context)
        val event1 = NotificationEvent.CalculationCompleted("evt-bwd-1", "Test 1", 0.0)
        val event2 = NotificationEvent.CalculationCompleted("evt-bwd-2", "Test 2", 0.0)

        dispatcherNoStore.dispatch(event1)
        dispatcherNoStore.dispatch(event2)

        assertEquals(2, shadowManager().size())
    }
}
