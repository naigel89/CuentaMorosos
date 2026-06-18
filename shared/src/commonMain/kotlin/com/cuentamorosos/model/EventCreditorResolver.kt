package com.cuentamorosos.model

/**
 * Resolves who should be credited for a debt where [currentUserUid] is the debtor.
 *
 * Priority:
 * 1. First expense's [EventExpenseItem.paidByProfileId] for the same event (excluding current user)
 * 2. Event [EventItem.ownerId] (excluding current user)
 * 3. Event ID as fallback
 */
internal fun resolveEventCreditor(
    debt: EventDebtItem,
    expenses: List<EventExpenseItem>,
    eventMap: Map<String, EventItem>,
    currentUserUid: String,
): String {
    val eventExpenses = expenses.filter { it.eventId == debt.eventId }
    val nonUserPayers = eventExpenses
        .map { it.paidByProfileId }
        .filter { it != currentUserUid && it.isNotBlank() }
        .distinct()
    if (nonUserPayers.isNotEmpty()) return nonUserPayers.first()

    val event = eventMap[debt.eventId]
    if (event != null && event.ownerId != currentUserUid && event.ownerId.isNotBlank()) {
        return event.ownerId
    }

    return debt.eventId
}
