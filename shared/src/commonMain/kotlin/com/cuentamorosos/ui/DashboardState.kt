package com.cuentamorosos.ui

enum class AlertType {
    NO_PARTICIPANTS,
    NO_EXPENSES,
    PENDING_CALCULATIONS,
}

enum class EventStatus {
    ACTIVE,
    SETTLING,
    CLOSED,
}

data class SmartAlert(
    val type: AlertType,
    val message: String,
    val icon: String,
    val eventId: String,
)

data class ActivityItem(
    val eventName: String,
    val eventId: String,
    val timestamp: Long,
    val amount: Double,
    val status: EventStatus,
)

data class EventHistoryItem(
    val eventId: String,
    val eventName: String,
    val amount: Double, // positive = they owe you, negative = you owe
    val participantCount: Int,
    val status: EventStatus,
)

data class DashboardState(
    val totalOwedToYou: Double = 0.0,
    val totalYouOwe: Double = 0.0,
    val smartAlerts: List<SmartAlert> = emptyList(),
    val recentActivity: List<ActivityItem> = emptyList(),
    val eventHistory: List<EventHistoryItem> = emptyList(),
)
