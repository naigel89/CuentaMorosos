package com.cuentamorosos.data

import android.content.Context
import android.content.NotificationManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.messaging.RemoteMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowNotificationManager

@RunWith(AndroidJUnit4::class)
class FcmParsingTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun shadowManager(): ShadowNotificationManager {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return org.robolectric.Shadows.shadowOf(manager)
    }

    // ── Valid parsing ─────────────────────────────────────────────────────────

    @Test
    fun `onMessageReceived parses invitation_received correctly`() {
        val service = CuentaMorososFirebaseMessagingService()
        val remoteMessage = RemoteMessage.Builder("test")
            .addData("type", "invitation_received")
            .addData("eventId", "evt-1")
            .addData("invitationId", "inv-1")
            .addData("inviterName", "Ana")
            .addData("eventName", "Asado")
            .build()

        service.onMessageReceived(remoteMessage)

        val notification = shadowManager().getNotification("INVITATION_RECEIVED", "evt-1".hashCode())
        assertNotNull(notification)
    }

    @Test
    fun `onMessageReceived parses invitation_accepted correctly`() {
        val service = CuentaMorososFirebaseMessagingService()
        val remoteMessage = RemoteMessage.Builder("test")
            .addData("type", "invitation_accepted")
            .addData("eventId", "evt-2")
            .addData("inviteeName", "Bob")
            .addData("eventName", "Cena")
            .build()

        service.onMessageReceived(remoteMessage)

        val notification = shadowManager().getNotification("INVITATION_ACCEPTED", "evt-2".hashCode())
        assertNotNull(notification)
    }

    @Test
    fun `onMessageReceived parses calculation_completed correctly`() {
        val service = CuentaMorososFirebaseMessagingService()
        val remoteMessage = RemoteMessage.Builder("test")
            .addData("type", "calculation_completed")
            .addData("eventId", "evt-3")
            .addData("eventName", "Fiesta")
            .addData("amountOwed", "25.50")
            .build()

        service.onMessageReceived(remoteMessage)

        val notification = shadowManager().getNotification("CALCULATION_COMPLETED", "evt-3".hashCode())
        assertNotNull(notification)
    }

    @Test
    fun `onMessageReceived parses upcoming_event correctly`() {
        val service = CuentaMorososFirebaseMessagingService()
        val remoteMessage = RemoteMessage.Builder("test")
            .addData("type", "upcoming_event")
            .addData("eventId", "evt-4")
            .addData("eventName", "Viaje")
            .addData("daysUntil", "3")
            .addData("dateFormatted", "15/06/2026")
            .build()

        service.onMessageReceived(remoteMessage)

        val notification = shadowManager().getNotification("UPCOMING_EVENT", "evt-4".hashCode())
        assertNotNull(notification)
    }

    // ── Missing fields ────────────────────────────────────────────────────────

    @Test
    fun `onMessageReceived ignores message with empty data`() {
        val service = CuentaMorososFirebaseMessagingService()
        val remoteMessage = RemoteMessage.Builder("test").build()

        service.onMessageReceived(remoteMessage)

        assertEquals(0, shadowManager().size())
    }

    @Test
    fun `onMessageReceived ignores message without type field`() {
        val service = CuentaMorososFirebaseMessagingService()
        val remoteMessage = RemoteMessage.Builder("test")
            .addData("eventId", "evt-1")
            .build()

        service.onMessageReceived(remoteMessage)

        assertEquals(0, shadowManager().size())
    }

    @Test
    fun `onMessageReceived ignores unknown type`() {
        val service = CuentaMorososFirebaseMessagingService()
        val remoteMessage = RemoteMessage.Builder("test")
            .addData("type", "unknown_type")
            .build()

        service.onMessageReceived(remoteMessage)

        assertEquals(0, shadowManager().size())
    }

    @Test
    fun `onMessageReceived ignores invitation_received with missing eventId`() {
        val service = CuentaMorososFirebaseMessagingService()
        val remoteMessage = RemoteMessage.Builder("test")
            .addData("type", "invitation_received")
            .addData("invitationId", "inv-1")
            .addData("inviterName", "Ana")
            .addData("eventName", "Asado")
            .build()

        service.onMessageReceived(remoteMessage)

        assertEquals(0, shadowManager().size())
    }

    @Test
    fun `onMessageReceived ignores calculation_completed with invalid amountOwed`() {
        val service = CuentaMorososFirebaseMessagingService()
        val remoteMessage = RemoteMessage.Builder("test")
            .addData("type", "calculation_completed")
            .addData("eventId", "evt-1")
            .addData("eventName", "Cena")
            .addData("amountOwed", "not-a-number")
            .build()

        service.onMessageReceived(remoteMessage)

        assertEquals(0, shadowManager().size())
    }
}
