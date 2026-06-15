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
import com.cuentamorosos.model.EventState
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.notifications.NotificationEvent
import kotlinx.coroutines.delay
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
    private val onCalculationCompleted: ((NotificationEvent.CalculationCompleted) -> Unit)? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    // Track which events we already notified as CALCULATED to avoid duplicates
    private val notifiedCalculatedEventIds = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            combine(
                eventRepository.observeEvents(),
                debtRepository.observeAllDebts(),
                expenseRepository.observeAllExpenses(),
                profileRepository.observeProfiles(),
            ) { events, debts, _, profiles ->
                computeState(events, debts, profiles)
            }.collect { newState ->
                _state.value = newState.copy(isLoading = false)
            }
        }

        viewModelScope.launch {
            delay(8_000)
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    private fun computeState(
        events: List<EventItem>,
        debts: List<EventDebtItem>,
        profiles: List<ProfileItem>,
    ): DashboardState {
        // ── TRIGGER: Detect CALCULATED transitions ──
        events.forEach { event ->
            if (event.state == EventState.CALCULATED &&
                event.id !in notifiedCalculatedEventIds
            ) {
                notifiedCalculatedEventIds.add(event.id)

                // Calculate how much the current user owes for this event
                val amountOwed = debts
                    .filter { it.eventId == event.id && it.profileId == currentUserUid && !it.paid }
                    .sumOf { it.amountEuros }

                if (amountOwed > 0) {
                    onCalculationCompleted?.invoke(
                        NotificationEvent.CalculationCompleted(
                            eventId = event.id,
                            eventName = event.name,
                            amountOwed = amountOwed,
                        )
                    )
                }
            }
        }

        // ── Existing logic ──
        val totalOwedToYou = debts
            .filter { !it.paid }
            .sumOf { it.amountEuros }

        val totalYouOwe = debts
            .filter { !it.paid && it.profileId == currentUserUid }
            .sumOf { it.amountEuros }

        val breakdown = computeProfileBreakdown(events, debts, profiles, currentUserUid)

        return DashboardState(
            totalOwedToYou = totalOwedToYou,
            totalYouOwe = totalYouOwe,
            owedToYouBreakdown = breakdown.owedToYouBreakdown,
            youOweBreakdown = breakdown.youOweBreakdown,
        )
    }

    private data class ProfileBreakdown(
        val owedToYouBreakdown: List<DebtBreakdownItem>,
        val youOweBreakdown: List<DebtBreakdownItem>,
    )

    private fun computeProfileBreakdown(
        events: List<EventItem>,
        debts: List<EventDebtItem>,
        profiles: List<ProfileItem>,
        currentUserUid: String,
    ): ProfileBreakdown {
        val profileMap = profiles.associateBy { it.id }
        val eventMap = events.associateBy { it.id }

        // Deudas donde otros te deben (profileId != currentUserUid)
        val owedToYou = debts
            .filter { !it.paid && it.profileId != currentUserUid }
            .groupBy { it.profileId }
            .map { (profileId, profileDebts) ->
                val profile = profileMap[profileId]
                DebtBreakdownItem(
                    profileId = profileId,
                    profileName = profile?.name ?: "Desconocido",
                    amount = profileDebts.sumOf { it.amountEuros },
                    events = profileDebts.map { debt ->
                        val event = eventMap[debt.eventId]
                        EventDebt(
                            eventId = debt.eventId,
                            eventName = event?.name ?: "Evento",
                            amount = debt.amountEuros,
                        )
                    },
                )
            }
            .sortedByDescending { it.amount }

        // Deudas donde tú debes (profileId == currentUserUid)
        val youOwe = debts
            .filter { !it.paid && it.profileId == currentUserUid }
            .groupBy { it.eventId }
            .map { (eventId, eventDebts) ->
                val event = eventMap[eventId]
                DebtBreakdownItem(
                    profileId = eventId,
                    profileName = event?.name ?: "Evento",
                    amount = eventDebts.sumOf { it.amountEuros },
                    events = eventDebts.map { debt ->
                        EventDebt(
                            eventId = debt.eventId,
                            eventName = event?.name ?: "Evento",
                            amount = debt.amountEuros,
                        )
                    },
                )
            }
            .sortedByDescending { it.amount }

        return ProfileBreakdown(
            owedToYouBreakdown = owedToYou,
            youOweBreakdown = youOwe,
        )
    }
}
