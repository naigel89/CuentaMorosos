package com.cuentamorosos.data

import android.content.Context
import android.content.NotificationManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.messaging.RemoteMessage
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end test: FCM service → NotificationDispatcher → NotificationManager.
 */
@RunWith(AndroidJUnit4::class)
class FcmIntegrationTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun notificationCount(): Int {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return org.robolectric.Shadows.shadowOf(manager).size()
    }

    @Test
    fun `FCM service to dispatcher end-to-end posts notification`() {
        val service = CuentaMorososFirebaseMessagingService()

        val remoteMessage = RemoteMessage.Builder("test")
            .addData("type", "invitation_received")
            .addData("eventId", "evt-e2e")
            .addData("invitationId", "inv-e2e")
            .addData("inviterName", "Test User")
            .addData("eventName", "E2E Event")
            .build()

        service.onMessageReceived(remoteMessage)

        assertEquals(1, notificationCount())
    }

    @Test
    fun `multiple FCM messages produce multiple notifications`() {
        val service = CuentaMorososFirebaseMessagingService()

        val message1 = RemoteMessage.Builder("test1")
            .addData("type", "invitation_received")
            .addData("eventId", "evt-a")
            .addData("invitationId", "inv-a")
            .addData("inviterName", "Alice")
            .addData("eventName", "Event A")
            .build()

        val message2 = RemoteMessage.Builder("test2")
            .addData("type", "calculation_completed")
            .addData("eventId", "evt-b")
            .addData("eventName", "Event B")
            .addData("amountOwed", "15.00")
            .build()

        service.onMessageReceived(message1)
        service.onMessageReceived(message2)

        assertEquals(2, notificationCount())
    }
}
