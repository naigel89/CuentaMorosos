package com.cuentamorosos.notifications

sealed class NotificationEvent {
    abstract val eventId: String?

    data class InvitationReceived(
        val invitationId: String,
        override val eventId: String,
        val inviterName: String,
        val eventName: String,
    ) : NotificationEvent()

    data class InvitationAccepted(
        override val eventId: String,
        val inviteeName: String,
        val eventName: String,
    ) : NotificationEvent()

    data class CalculationCompleted(
        override val eventId: String,
        val eventName: String,
        val amountOwed: Double,
    ) : NotificationEvent()

    data class UpcomingEvent(
        override val eventId: String,
        val eventName: String,
        val daysUntil: Int,
        val dateFormatted: String,
    ) : NotificationEvent()
}
