package com.cuentamorosos.ui

import com.cuentamorosos.model.EventDebtItem
import com.cuentamorosos.model.ProfileItem

/**
 * Pure function for computing per-profile debt breakdown lists.
 * Groups unpaid debts by profile and resolves profile names.
 */
internal fun computeProfileBreakdown(
    debts: List<EventDebtItem>,
    profiles: List<ProfileItem>,
    currentUserUid: String,
): BreakdownLists {
    val profileMap = profiles.associateBy { it.id }
    val unpaidDebts = debts.filter { !it.paid }

    // Debts where others owe the current user
    val owedToYouBreakdown = unpaidDebts
        .filter { it.profileId != currentUserUid }
        .groupBy { it.profileId }
        .map { (profileId, profileDebts) ->
            val profile = profileMap[profileId]
            DebtBreakdownItem(
                profileId = profileId,
                profileName = profile?.name ?: "Desconocido",
                amount = profileDebts.sumOf { it.amountEuros },
            )
        }
        .sortedByDescending { it.amount }

    // Debts where the current user owes
    val youOweBreakdown = unpaidDebts
        .filter { it.profileId == currentUserUid }
        .groupBy { it.profileId }
        .map { (profileId, profileDebts) ->
            val profile = profileMap[profileId]
            DebtBreakdownItem(
                profileId = profileId,
                profileName = profile?.name ?: "Desconocido",
                amount = profileDebts.sumOf { it.amountEuros },
            )
        }
        .sortedByDescending { it.amount }

    return BreakdownLists(owedToYouBreakdown, youOweBreakdown)
}

internal data class BreakdownLists(
    val owedToYouBreakdown: List<DebtBreakdownItem>,
    val youOweBreakdown: List<DebtBreakdownItem>,
)
