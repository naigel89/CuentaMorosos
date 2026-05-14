package com.cuentamorosos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuentamorosos.data.repository.DebtRepository
import com.cuentamorosos.data.repository.EventRepository
import com.cuentamorosos.data.repository.ExpenseRepository
import com.cuentamorosos.data.repository.ProfileRepository
import com.cuentamorosos.model.EventDebtItem
import com.cuentamorosos.model.EventExpenseItem
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.ProfileItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val eventRepository: EventRepository,
    private val debtRepository: DebtRepository,
    private val expenseRepository: ExpenseRepository,
    private val profileRepository: ProfileRepository,
    @Suppress("UNUSED_PARAMETER") private val currentUserUid: String,
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                eventRepository.observeEvents(),
                debtRepository.observeAllDebts(),
                expenseRepository.observeAllExpenses(),
                profileRepository.observeProfiles(),
            ) { events, debts, expenses, profiles ->
                computeState(events, debts, expenses, profiles)
            }.collect { newState ->
                _state.value = newState
            }
        }
    }

    private fun computeState(
        events: List<EventItem>,
        debts: List<EventDebtItem>,
        expenses: List<EventExpenseItem>,
        @Suppress("UNUSED_PARAMETER") profiles: List<ProfileItem>,
    ): DashboardState {
        // Simplification: show total pending across all events.
        // Proper creditor/debtor tracking requires matching currentUser to a ProfileItem,
        // which is not reliably available yet.
        val totalOwedToYou = debts
            .filter { !it.paid }
            .sumOf { it.amountEuros }

        val totalYouOwe = 0.0

        val smartAlerts = computeSmartAlerts(events, expenses)
        val recentActivity = buildRecentActivity(events, expenses)

        return DashboardState(
            totalOwedToYou = totalOwedToYou,
            totalYouOwe = totalYouOwe,
            smartAlerts = smartAlerts,
            recentActivity = recentActivity,
        )
    }

    private fun computeSmartAlerts(
        events: List<EventItem>,
        expenses: List<EventExpenseItem>,
    ): List<SmartAlert> {
        val alerts = mutableListOf<SmartAlert>()

        val eventsWithExpenses = expenses.map { it.eventId }.toSet()
        val eventsWithCalculation = events
            .filter { it.lastCalculationMode != null }
            .map { it.id }
            .toSet()

        events.forEach { event ->
            // Events without participants
            if (event.memberIds.isEmpty()) {
                alerts.add(
                    SmartAlert(
                        type = AlertType.NO_PARTICIPANTS,
                        message = "${event.name} sin participantes",
                        icon = "\uD83D\uDC65",
                        eventId = event.id,
                    ),
                )
            }

            // Events without expenses
            if (event.id !in eventsWithExpenses) {
                alerts.add(
                    SmartAlert(
                        type = AlertType.NO_EXPENSES,
                        message = "${event.name} sin gastos",
                        icon = "\uD83E\uDDFE",
                        eventId = event.id,
                    ),
                )
            }

            // Events with expenses but no calculation
            if (event.id in eventsWithExpenses && event.id !in eventsWithCalculation) {
                alerts.add(
                    SmartAlert(
                        type = AlertType.PENDING_CALCULATIONS,
                        message = "${event.name} pendiente de calcular",
                        icon = "\uD83E\uDDEE",
                        eventId = event.id,
                    ),
                )
            }
        }

        return alerts
    }

    private fun buildRecentActivity(
        events: List<EventItem>,
        expenses: List<EventExpenseItem>,
    ): List<ActivityItem> {
        return events
            .sortedByDescending { it.dateMillis }
            .take(20)
            .map { event ->
                val eventExpenses = expenses.filter { it.eventId == event.id }
                val totalAmount = eventExpenses.sumOf { it.amountEuros }
                val status = if (event.lastCalculationMode != null) {
                    EventStatus.SETTLING
                } else {
                    EventStatus.ACTIVE
                }

                ActivityItem(
                    eventName = event.name,
                    eventId = event.id,
                    timestamp = event.dateMillis,
                    amount = totalAmount,
                    status = status,
                )
            }
    }
}
