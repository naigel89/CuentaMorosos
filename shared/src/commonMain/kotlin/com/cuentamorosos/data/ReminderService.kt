package com.cuentamorosos.data

import com.cuentamorosos.currentTimeMillis
import com.cuentamorosos.model.EventDebtItem
import com.cuentamorosos.model.EventExpenseItem
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.formatEuros
import com.cuentamorosos.model.resolveEventCreditor

data class ReminderMessage(
    val title: String,
    val body: String,
    val type: ReminderType = ReminderType.PENDING_DEBT,
    val eventId: String? = null,
    val profileName: String? = null,
    val amountEuros: Double? = null,
    val isOwedToYou: Boolean = false,
)

enum class ReminderType { PENDING_DEBT }

object ReminderService {
    private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

    fun buildReminderMessages(
        events: List<EventItem>,
        debts: List<EventDebtItem>,
        expenses: List<EventExpenseItem>,
        profiles: List<ProfileItem>,
        currentUserUid: String,
        reminderDays: Int,
        remindersEnabled: Boolean,
        nowMillis: Long = currentTimeMillis(),
    ): List<ReminderMessage> {
        if (!remindersEnabled) return emptyList()

        val thresholdMillis = reminderDays.coerceAtLeast(1) * MILLIS_PER_DAY
        val profileMap = profiles.associateBy { it.id }
        val eventMap = events.associateBy { it.id }

        return buildList {
            events.forEach { event ->
                val eventDebts = debts.filter { it.eventId == event.id }
                val pendingDebts = eventDebts.filter { !it.paid && it.amountEuros > 0.0 }
                val eventAge = nowMillis - event.dateMillis

                if (eventAge < thresholdMillis) return@forEach

                pendingDebts.forEach { debt ->
                    if (debt.profileId != currentUserUid) {
                        // Debt owed TO current user ("te debe")
                        val profile = profileMap[debt.profileId]
                        val profileName = profile?.displayName ?: profile?.name ?: debt.profileId

                        add(
                            ReminderMessage(
                                title = "Pendientes en ${event.name}",
                                body = "$profileName te debe ${formatEuros(debt.amountEuros)}",
                                type = ReminderType.PENDING_DEBT,
                                eventId = event.id,
                                profileName = profileName,
                                amountEuros = debt.amountEuros,
                                isOwedToYou = true,
                            )
                        )
                    } else {
                        // Debt owed BY current user ("debes a")
                        val creditorId = resolveEventCreditor(debt, expenses, eventMap, currentUserUid)
                        val creditorProfile = profileMap[creditorId]
                        val profileName = creditorProfile?.displayName ?: creditorProfile?.name ?: creditorId

                        add(
                            ReminderMessage(
                                title = "Pendientes en ${event.name}",
                                body = "Debes ${formatEuros(debt.amountEuros)} a $profileName",
                                type = ReminderType.PENDING_DEBT,
                                eventId = event.id,
                                profileName = profileName,
                                amountEuros = debt.amountEuros,
                                isOwedToYou = false,
                            )
                        )
                    }
                }
            }
        }.distinctBy { "${it.title}-${it.body}" }
    }
}
