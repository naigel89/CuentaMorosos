package com.cuentamorosos.ui

import com.cuentamorosos.model.EventState

enum class AlertType {
    NO_PARTICIPANTS,
    NO_EXPENSES,
    PENDING_CALCULATIONS,
}

data class SmartAlert(
    val type: AlertType,
    val message: String,
    val icon: String,
    val eventId: String,
)

data class DashboardEventRow(
    val eventId: String,
    val eventName: String,
    val amount: Double,
    val participantCount: Int,
    val state: EventState,
    val dateMillis: Long,
)

data class DebtBreakdownItem(
    val profileId: String,
    val profileName: String,
    val amount: Double,
)

data class DashboardState(
    val isLoading: Boolean = true,
    val totalOwedToYou: Double = 0.0,
    val totalYouOwe: Double = 0.0,
    val smartAlerts: List<SmartAlert> = emptyList(),
    val allEvents: List<DashboardEventRow> = emptyList(),
    val owedToYouBreakdown: List<DebtBreakdownItem> = emptyList(),
    val youOweBreakdown: List<DebtBreakdownItem> = emptyList(),
)
