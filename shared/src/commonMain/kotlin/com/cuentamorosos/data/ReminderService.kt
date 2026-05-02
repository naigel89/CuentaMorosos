package com.cuentamorosos.data

import com.cuentamorosos.currentTimeMillis
import com.cuentamorosos.model.EventDebtItem
import com.cuentamorosos.model.EventExpenseItem
import com.cuentamorosos.model.EventItem

data class ReminderMessage(
    val title: String,
    val body: String,
)

object ReminderService {
    private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

    fun buildReminderMessages(
        events: List<EventItem>,
        debts: List<EventDebtItem>,
        expenses: List<EventExpenseItem>,
        reminderDays: Int,
        remindersEnabled: Boolean,
        nowMillis: Long = currentTimeMillis(),
    ): List<ReminderMessage> {
        if (!remindersEnabled) return emptyList()

        val thresholdMillis = reminderDays.coerceAtLeast(1) * MILLIS_PER_DAY

        return buildList {
            events.forEach { event ->
                val eventDebts = debts.filter { it.eventId == event.id }
                val eventExpenses = expenses.filter { it.eventId == event.id }
                val pendingDebts = eventDebts.filter { !it.paid && it.amountEuros > 0.0 }
                val eventAge = nowMillis - event.dateMillis

                if (pendingDebts.isNotEmpty() && eventAge >= thresholdMillis) {
                    add(
                        ReminderMessage(
                            title = "Pendientes en ${event.name}",
                            body = "Han pasado al menos ${reminderDays.coerceAtLeast(1)} días y siguen abiertos ${pendingDebts.size} pagos."
                        )
                    )
                }

                val isIncomplete = eventDebts.isEmpty() || eventDebts.any { it.amountEuros <= 0.0 } || eventExpenses.isEmpty()
                if (isIncomplete) {
                    add(
                        ReminderMessage(
                            title = "Evento incompleto: ${event.name}",
                            body = "Revisa perfiles, importes o ítems del evento para poder cerrarlo correctamente."
                        )
                    )
                }
            }
        }.distinctBy { "${it.title}-${it.body}" }
    }
}
