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
    private val currentUserUid: String,
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
        profiles: List<ProfileItem>,
    ): DashboardState {
        val totalOwedToYou = debts
            .filter { !it.paid }
            .sumOf { it.amountEuros }

        val totalYouOwe = debts
            .filter { !it.paid && it.profileId == currentUserUid }
            .sumOf { it.amountEuros }

        val smartAlerts = computeSmartAlerts(events, expenses)
        val allEvents = buildAllEvents(events, debts, expenses)
        val breakdown = computeProfileBreakdown(debts, profiles, currentUserUid)

        return DashboardState(
            totalOwedToYou = totalOwedToYou,
            totalYouOwe = totalYouOwe,
            smartAlerts = smartAlerts,
            allEvents = allEvents,
            owedToYouBreakdown = breakdown.owedToYouBreakdown,
            youOweBreakdown = breakdown.youOweBreakdown,
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
            if (event.effectiveMemberIds.isEmpty()) {
                alerts.add(
                    SmartAlert(
                        type = AlertType.NO_PARTICIPANTS,
                        message = "${event.name} sin participantes",
                        icon = "\uD83D\uDC65",
                        eventId = event.id,
                    ),
                )
            }

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

    private fun buildAllEvents(
        events: List<EventItem>,
        debts: List<EventDebtItem>,
        expenses: List<EventExpenseItem>,
    ): List<DashboardEventRow> = events.map { event ->
        val eventExpenses = expenses.filter { it.eventId == event.id }
        val eventDebts = debts.filter { it.eventId == event.id }
        val totalExpenses = eventExpenses.sumOf { it.amountEuros }
        val totalDebts = eventDebts.sumOf { it.amountEuros }
        val netAmount = if (totalExpenses > 0) totalExpenses else totalDebts

        DashboardEventRow(
            eventId = event.id,
            eventName = event.name,
            amount = netAmount,
            participantCount = event.effectiveMemberIds.size,
            state = event.state,
            dateMillis = event.dateMillis,
        )
    }.sortedByDescending { it.dateMillis }
}
