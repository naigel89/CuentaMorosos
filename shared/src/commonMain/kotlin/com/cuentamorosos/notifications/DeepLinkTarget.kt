package com.cuentamorosos.notifications

/** Target state for deep link navigation from notifications. */
data class DeepLinkTarget(
    val pagerPage: Int,
    val eventId: String?,
    val notificationType: String,
)
