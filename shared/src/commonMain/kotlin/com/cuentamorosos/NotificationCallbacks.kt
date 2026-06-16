package com.cuentamorosos

import com.cuentamorosos.notifications.NotificationEvent

data class NotificationCallbacks(
    val onInvitationReceived: ((NotificationEvent.InvitationReceived) -> Unit)? = null,
    val onInvitationAccepted: ((NotificationEvent.InvitationAccepted) -> Unit)? = null,
    val onCalculationCompleted: ((NotificationEvent.CalculationCompleted) -> Unit)? = null,
)
