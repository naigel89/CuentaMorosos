package com.cuentamorosos.data

import android.util.Log
import com.cuentamorosos.notifications.NotificationDispatcher
import com.cuentamorosos.notifications.NotificationEvent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class CuentaMorososFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "CM_FCM"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed")
        serviceScope.launch {
            FirebaseUserSyncManager.saveTokenForCurrentUser(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data

        if (data.isEmpty()) {
            Log.w(TAG, "FCM message with empty data payload, ignoring")
            return
        }

        val type = data["type"] ?: run {
            Log.w(TAG, "FCM message without 'type' field, ignoring")
            return
        }

        val event = when (type) {
            "invitation_received" -> parseInvitationReceived(data)
            "invitation_accepted" -> parseInvitationAccepted(data)
            "calculation_completed" -> parseCalculationCompleted(data)
            "upcoming_event" -> parseUpcomingEvent(data)
            else -> {
                Log.w(TAG, "Unknown FCM type: $type")
                return
            }
        }

        if (event != null) {
            NotificationDispatcher(applicationContext).dispatch(event)
        } else {
            Log.w(TAG, "Failed to parse FCM payload for type: $type")
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    // ── Parsing methods ─────────────────────────────────────────────────────

    private fun parseInvitationReceived(data: Map<String, String>): NotificationEvent? {
        val eventId = data["eventId"] ?: return null
        val invitationId = data["invitationId"] ?: return null
        val inviterName = data["inviterName"] ?: return null
        val eventName = data["eventName"] ?: return null
        return NotificationEvent.InvitationReceived(
            invitationId = invitationId,
            eventId = eventId,
            inviterName = inviterName,
            eventName = eventName,
        )
    }

    private fun parseInvitationAccepted(data: Map<String, String>): NotificationEvent? {
        val eventId = data["eventId"] ?: return null
        val inviteeName = data["inviteeName"] ?: return null
        val eventName = data["eventName"] ?: return null
        return NotificationEvent.InvitationAccepted(
            eventId = eventId,
            inviteeName = inviteeName,
            eventName = eventName,
        )
    }

    private fun parseCalculationCompleted(data: Map<String, String>): NotificationEvent? {
        val eventId = data["eventId"] ?: return null
        val eventName = data["eventName"] ?: return null
        val amountOwed = data["amountOwed"]?.toDoubleOrNull() ?: return null
        return NotificationEvent.CalculationCompleted(
            eventId = eventId,
            eventName = eventName,
            amountOwed = amountOwed,
        )
    }

    private fun parseUpcomingEvent(data: Map<String, String>): NotificationEvent? {
        val eventId = data["eventId"] ?: return null
        val eventName = data["eventName"] ?: return null
        val daysUntil = data["daysUntil"]?.toIntOrNull() ?: return null
        val dateFormatted = data["dateFormatted"] ?: return null
        return NotificationEvent.UpcomingEvent(
            eventId = eventId,
            eventName = eventName,
            daysUntil = daysUntil,
            dateFormatted = dateFormatted,
        )
    }
}
