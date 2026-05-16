package com.cuentamorosos.model

/**
 * Summary of what would be cascade-deleted when an event is removed.
 * Profiles are never deleted — they belong to the account, not the event.
 */
data class CascadeSummary(
    val eventId: String,
    val itemCount: Int,
    val calculationCount: Int,
    val profilePreservationNote: String,
)

/**
 * Pure integrity guard functions. Called by ViewModels before mutations
 * to enforce state-based invariants. Separate from PermissionEngine
 * (which handles role-based access).
 */
object IntegrityGuard {

    /**
     * Returns false if the event is not in OPEN state.
     * Expenses can only be deleted while the event is still being configured.
     */
    fun canDeleteExpense(
        eventState: EventState,
        expenseId: String,
        allExpenses: List<EventExpenseItem>,
    ): Boolean = eventState == EventState.OPEN

    /**
     * Cascade delete is always allowed — the event owner can delete the entire event
     * at any state. All expenses, calculations, and audit entries cascade with it.
     * Profiles are preserved (they belong to the account).
     */
    fun canDeleteEvent(cascadeSummary: CascadeSummary): Boolean = true

    /**
     * Returns false if the debt has already been marked as paid.
     * Paid debts are immutable — corrections must use AdjustmentEntry.
     */
    fun canModifyPaidDebt(debt: EventDebtItem): Boolean = !debt.paid

    /**
     * Returns true only if the debt is already paid.
     * Adjustments are only meaningful for settled (paid) transfers.
     */
    fun canCreateAdjustment(debt: EventDebtItem): Boolean = debt.paid
}

/**
 * Builds a cascade summary showing what would be deleted if the event is removed.
 */
fun buildCascadeSummary(
    eventId: String,
    expenses: List<EventExpenseItem>,
    calculations: List<CalculationSnapshot>,
): CascadeSummary = CascadeSummary(
    eventId = eventId,
    itemCount = expenses.size,
    calculationCount = calculations.size,
    profilePreservationNote = "Profiles are preserved — they belong to the account, not the event",
)
