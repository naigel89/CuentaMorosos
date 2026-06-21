package com.cuentamorosos.model

/**
 * Resolves who should be credited for a debt where [currentUserUid] is the debtor.
 *
 * When [EventDebtItem.creditorId] is set, returns it directly as the authoritative source.
 * Otherwise falls back to heuristic resolution.
 *
 * Fallback priority:
 * 1. [EventDebtItem.creditorId] if not null (fast path)
 * 2. First expense's [EventExpenseItem.payerContributions] keys for the same event (excluding current user, sorted)
 * 3. First expense's [EventExpenseItem.paidByProfileId] for the same event (legacy fallback, excluding current user)
 * 4. Event [EventItem.ownerId] (excluding current user)
 * 5. Event ID as fallback
 */
internal fun resolveEventCreditor(
    debt: EventDebtItem,
    expenses: List<EventExpenseItem>,
    eventMap: Map<String, EventItem>,
    currentUserUid: String,
): String {
    if (debt.creditorId != null) return debt.creditorId

    val eventExpenses = expenses.filter { it.eventId == debt.eventId }

    // Primary: resolve from payerContributions keys (multi-payer map)
    val nonUserPayers = eventExpenses
        .flatMap { it.payerContributions.keys }
        .filter { it != currentUserUid && it.isNotBlank() }
        .distinct()
        .sorted()
    if (nonUserPayers.isNotEmpty()) return nonUserPayers.first()

    // Fallback: legacy paidByProfileId when payerContributions is empty
    val legacyPayers = eventExpenses
        .map { it.paidByProfileId }
        .filter { it != currentUserUid && it.isNotBlank() }
        .distinct()
    if (legacyPayers.isNotEmpty()) return legacyPayers.first()

    val event = eventMap[debt.eventId]
    if (event != null && event.ownerId != currentUserUid && event.ownerId.isNotBlank()) {
        return event.ownerId
    }

    return debt.eventId
}
