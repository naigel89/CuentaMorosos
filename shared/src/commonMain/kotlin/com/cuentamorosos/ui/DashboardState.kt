package com.cuentamorosos.ui

/**
 * Direction of a debt relative to the current user.
 */
enum class DebtDirection { OWED_TO_YOU, YOU_OWE }

/**
 * A single item in the unified debt breakdown.
 *
 * Each item represents one person with whom the current user has an active debt.
 * - [OWED_TO_YOU]: this person owes you money (te deben)
 * - [YOU_OWE]: you owe this person money (debes)
 */
data class UnifiedDebtItem(
    val profileId: String,
    val profileName: String,
    val amount: Double,
    val direction: DebtDirection,
)

data class EventDebt(
    val eventId: String,
    val eventName: String,
    val amount: Double,
)

data class DebtBreakdownItem(
    val profileId: String,
    val profileName: String,
    val amount: Double,
    val events: List<EventDebt> = emptyList(),
)

data class DashboardState(
    val isLoading: Boolean = true,
    val totalOwedToYou: Double = 0.0,
    val totalYouOwe: Double = 0.0,
    val owedToYouBreakdown: List<DebtBreakdownItem> = emptyList(),
    val youOweBreakdown: List<DebtBreakdownItem> = emptyList(),
    val unifiedBreakdown: List<UnifiedDebtItem> = emptyList(),
)

/**
 * Summary data for the financial overview cards on the dashboard.
 *
 * Computed from [DashboardState] via [toFinancialSummary].
 */
data class FinancialSummary(
    val debes: Double,
    val teDeben: Double,
    val netBalance: Double,
    val debesCount: Int,
    val teDebenCount: Int,
)

/**
 * Extracts the financial summary fields from this [DashboardState].
 *
 * - [FinancialSummary.teDeben] = [totalOwedToYou]
 * - [FinancialSummary.debes] = [totalYouOwe]
 * - [FinancialSummary.netBalance] = totalOwedToYou - totalYouOwe (positive = you're owed)
 * - [FinancialSummary.debesCount] = [youOweBreakdown].size
 * - [FinancialSummary.teDebenCount] = [owedToYouBreakdown].size
 */
fun DashboardState.toFinancialSummary(): FinancialSummary = FinancialSummary(
    debes = totalYouOwe,
    teDeben = totalOwedToYou,
    netBalance = totalOwedToYou - totalYouOwe,
    debesCount = youOweBreakdown.size,
    teDebenCount = owedToYouBreakdown.size,
)
