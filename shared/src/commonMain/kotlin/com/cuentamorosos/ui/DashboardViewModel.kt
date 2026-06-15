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
            ) { events, debts, expenses, profiles ->
                computeState(events, debts, expenses, profiles)
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
        expenses: List<EventExpenseItem>,
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

        // ── Compute totals ──
        val totalOwedToYou = debts
            .filter { !it.paid }
            .sumOf { it.amountEuros }

        val totalYouOwe = debts
            .filter { !it.paid && it.profileId == currentUserUid }
            .sumOf { it.amountEuros }

        // ── Compute breakdowns ──
        val profileMap = profiles.associateBy { it.id }
        val eventMap = events.associateBy { it.id }

        val (owedToYouBreakdown, youOweBreakdown) = computeProfileBreakdown(
            debts = debts,
            expenses = expenses,
            eventMap = eventMap,
            profileMap = profileMap,
            currentUserUid = currentUserUid,
        )

        // ── Build unified list (one person per row, sorted by amount desc) ──
        val unifiedBreakdown = buildUnifiedBreakdown(
            owedToYou = owedToYouBreakdown,
            youOwe = youOweBreakdown,
        )

        return DashboardState(
            totalOwedToYou = totalOwedToYou,
            totalYouOwe = totalYouOwe,
            owedToYouBreakdown = owedToYouBreakdown,
            youOweBreakdown = youOweBreakdown,
            unifiedBreakdown = unifiedBreakdown,
        )
    }

    /**
     * Splits debts into two groups:
     * 1. **owedToYou** — debts where OTHERS owe the current user (profileId != currentUserUid).
     *    Grouped by debtor profile (the person who owes you).
     * 2. **youOwe** — debts where the current user OWES others (profileId == currentUserUid).
     *    Resolves the creditor from expenses (paidByProfileId), then groups by that person.
     */
    private fun computeProfileBreakdown(
        debts: List<EventDebtItem>,
        expenses: List<EventExpenseItem>,
        eventMap: Map<String, EventItem>,
        profileMap: Map<String, ProfileItem>,
        currentUserUid: String,
    ): Pair<List<DebtBreakdownItem>, List<DebtBreakdownItem>> {
        // ── "Te deben" — others owe you ──
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

        // ── "Debes" — you owe others ──
        val youOweDebts = debts.filter { !it.paid && it.profileId == currentUserUid }

        // Build a map: expense paidByProfileId → total amount owed
        val creditorAmounts = mutableMapOf<String, Double>()
        for (debt in youOweDebts) {
            val creditorId = resolveCreditor(debt, expenses, eventMap, currentUserUid)
            creditorAmounts[creditorId] = (creditorAmounts[creditorId] ?: 0.0) + debt.amountEuros
        }

        val youOwe = creditorAmounts.map { (creditorId, amount) ->
            val profile = profileMap[creditorId]
            val name = profile?.name
                ?: eventMap[creditorId]?.name
                ?: "Desconocido"
            // Attach event info for the detail view
            val relatedDebts = youOweDebts.filter { debt ->
                resolveCreditor(debt, expenses, eventMap, currentUserUid) == creditorId
            }
            DebtBreakdownItem(
                profileId = creditorId,
                profileName = name,
                amount = amount,
                events = relatedDebts.map { debt ->
                    val event = eventMap[debt.eventId]
                    EventDebt(
                        eventId = debt.eventId,
                        eventName = event?.name ?: "Evento",
                        amount = debt.amountEuros,
                    )
                },
            )
        }.sortedByDescending { it.amount }

        return Pair(owedToYou, youOwe)
    }

    /**
     * Determines the creditor (who is owed money) for a given debt where the current user
     * is the debtor.
     *
     * Resolution priority:
     * 1. First expense's [EventExpenseItem.paidByProfileId] for the same event
     * 2. Event [EventItem.ownerId]
     * 3. Event ID as fallback
     */
    private fun resolveCreditor(
        debt: EventDebtItem,
        expenses: List<EventExpenseItem>,
        eventMap: Map<String, EventItem>,
        currentUserUid: String,
    ): String {
        // 1. Try expenses — find who paid for expenses in this event (excluding the current user)
        val eventExpenses = expenses.filter { it.eventId == debt.eventId }
        val nonUserPayers = eventExpenses
            .map { it.paidByProfileId }
            .filter { it != currentUserUid && it.isNotBlank() }
            .distinct()
        if (nonUserPayers.isNotEmpty()) {
            // If multiple people paid, they're grouped per-creditor by the caller,
            // but here we just return the first for initial grouping.
            // The caller groups and aggregates per unique creditor.
            return nonUserPayers.first()
        }

        // 2. Fallback to event owner
        val event = eventMap[debt.eventId]
        if (event != null && event.ownerId != currentUserUid && event.ownerId.isNotBlank()) {
            return event.ownerId
        }

        // 3. Ultimate fallback — use event ID (will show event name)
        return debt.eventId
    }

    /**
     * Merges [owedToYou] and [youOwe] into a single sorted list of [UnifiedDebtItem].
     *
     * Each item carries a [DebtDirection] so the UI knows how to colour it.
     * Zero amounts are excluded.
     */
    private fun buildUnifiedBreakdown(
        owedToYou: List<DebtBreakdownItem>,
        youOwe: List<DebtBreakdownItem>,
    ): List<UnifiedDebtItem> {
        val unified = mutableListOf<UnifiedDebtItem>()

        for (item in owedToYou) {
            if (item.amount <= 0.0) continue
            unified.add(
                UnifiedDebtItem(
                    profileId = item.profileId,
                    profileName = item.profileName,
                    amount = item.amount,
                    direction = DebtDirection.OWED_TO_YOU,
                )
            )
        }

        for (item in youOwe) {
            if (item.amount <= 0.0) continue
            unified.add(
                UnifiedDebtItem(
                    profileId = item.profileId,
                    profileName = item.profileName,
                    amount = item.amount,
                    direction = DebtDirection.YOU_OWE,
                )
            )
        }

        return unified.sortedByDescending { it.amount }
    }
}
