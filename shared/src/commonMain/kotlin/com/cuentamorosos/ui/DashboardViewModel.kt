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
import com.cuentamorosos.model.resolveEventCreditor
import com.cuentamorosos.notifications.NotificationEvent
import kotlin.math.abs
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
            if (event.state == EventState.CALCULATED) {

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
        val totalOwedToYou = calculateTotalOwedToYou(debts, currentUserUid)

        val totalYouOwe = debts
            .filter { !it.paid && it.profileId == currentUserUid && it.amountEuros > 0.0 }
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
            .filter { !it.paid && it.profileId != currentUserUid && it.amountEuros > 0.0 }
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
        val youOweDebts = debts.filter { !it.paid && it.profileId == currentUserUid && it.amountEuros > 0.0 }

        // Build a map: expense paidByProfileId → total amount owed
        val creditorAmounts = mutableMapOf<String, Double>()
        for (debt in youOweDebts) {
            val creditorId = resolveEventCreditor(debt, expenses, eventMap, currentUserUid)
            creditorAmounts[creditorId] = (creditorAmounts[creditorId] ?: 0.0) + debt.amountEuros
        }

        val youOwe = creditorAmounts.map { (creditorId, amount) ->
            val profile = profileMap[creditorId]
            val name = profile?.name
                ?: eventMap[creditorId]?.name
                ?: "Desconocido"
            // Attach event info for the detail view
            val relatedDebts = youOweDebts.filter { debt ->
                resolveEventCreditor(debt, expenses, eventMap, currentUserUid) == creditorId
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

}

/**
 * Builds a unified debt breakdown, netting profiles that appear in both lists.
 *
 * - Profiles in only one list: keep as-is (single-direction).
 * - Profiles in both lists: compute net = owedToYou.amount - youOwe.amount.
 *   Zero net → excluded. Direction determined by net sign.
 *   Merged events: owedToYou events stay positive, youOwe events become negative.
 * - Final list sorted by amount descending.
 */
internal fun buildUnifiedBreakdown(
    owedToYou: List<DebtBreakdownItem>,
    youOwe: List<DebtBreakdownItem>,
): List<UnifiedDebtItem> {
    val owedMap = owedToYou.associateBy { it.profileId }
    val oweMap = youOwe.associateBy { it.profileId }
    val allProfileIds = (owedMap.keys + oweMap.keys).toSet()
    val unified = mutableListOf<UnifiedDebtItem>()

    for (profileId in allProfileIds) {
        val owedItem = owedMap[profileId]
        val oweItem = oweMap[profileId]

        when {
            // Only owedToYou: unchanged (events stay positive)
            owedItem != null && oweItem == null -> {
                if (owedItem.amount <= 0.0) continue
                unified.add(
                    UnifiedDebtItem(
                        profileId = owedItem.profileId,
                        profileName = owedItem.profileName,
                        amount = owedItem.amount,
                        direction = DebtDirection.OWED_TO_YOU,
                        events = owedItem.events,
                    )
                )
            }

            // Only youOwe: events become negative to indicate owed direction
            owedItem == null && oweItem != null -> {
                if (oweItem.amount <= 0.0) continue
                unified.add(
                    UnifiedDebtItem(
                        profileId = oweItem.profileId,
                        profileName = oweItem.profileName,
                        amount = oweItem.amount,
                        direction = DebtDirection.YOU_OWE,
                        events = oweItem.events.map { it.copy(amount = -abs(it.amount)) },
                    )
                )
            }

            // Both: net the amounts
            owedItem != null && oweItem != null -> {
                val netAmount = owedItem.amount - oweItem.amount
                if (abs(netAmount) <= 0.001) continue // treat near-zero as zero

                val direction = if (netAmount > 0) DebtDirection.OWED_TO_YOU else DebtDirection.YOU_OWE
                val mergedEvents = owedItem.events.map { it.copy(amount = abs(it.amount)) } +
                    oweItem.events.map { it.copy(amount = -abs(it.amount)) }

                unified.add(
                    UnifiedDebtItem(
                        profileId = owedItem.profileId,
                        profileName = owedItem.profileName,
                        amount = abs(netAmount),
                        direction = direction,
                        events = mergedEvents,
                    )
                )
            }
        }
    }

    return unified.sortedByDescending { it.amount }
}

/**
 * Calculates the total amount owed TO the current user by other profiles.
 *
 * Excludes:
 * - Paid debts (settled)
 * - Debts where [currentUserUid] is the debtor (those belong to `totalYouOwe`)
 *
 * Only unpaid debts from profiles OTHER than the current user are counted.
 */
internal fun calculateTotalOwedToYou(
    debts: List<EventDebtItem>,
    currentUserUid: String,
): Double {
    return debts
        .filter { !it.paid && it.profileId != currentUserUid && it.amountEuros > 0.0 }
        .sumOf { it.amountEuros }
}
