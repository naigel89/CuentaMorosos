package com.cuentamorosos.data

import com.cuentamorosos.currentTimeMillis
import com.cuentamorosos.formatDateMillis
import com.cuentamorosos.model.EventDebtItem
import com.cuentamorosos.model.EventExpenseItem
import com.cuentamorosos.model.EventItem

data class ReminderMessage(
    val title: String,
    val body: String,
    val type: ReminderType = ReminderType.PENDING_DEBT,
    val eventId: String? = null,
    val daysUntil: Int? = null,
    val dateFormatted: String? = null,
)

enum class ReminderType { PENDING_DEBT, INCOMPLETE_EVENT, UPCOMING_EVENT }

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

    fun buildUpcomingEventMessages(
        events: List<EventItem>,
        reminderDays: Int,
        nowMillis: Long = currentTimeMillis(),
    ): List<ReminderMessage> {
        val windowEnd = nowMillis + reminderDays.coerceAtLeast(1) * MILLIS_PER_DAY

        return events
            .filter { event ->
                // Event starts in the future but within the reminder window
                event.startDateMillis in nowMillis..windowEnd
            }
            .map { event ->
                val daysUntil = ((event.startDateMillis - nowMillis) / MILLIS_PER_DAY)
                    .toInt()
                    .coerceAtLeast(0)
                val dateFormatted = formatDateMillis(event.startDateMillis)

                ReminderMessage(
                    title = "Próximo evento: ${event.name}",
                    body = "El evento '${event.name}' es en $daysUntil días ($dateFormatted)",
                    type = ReminderType.UPCOMING_EVENT,
                    eventId = event.id,
                    daysUntil = daysUntil,
                    dateFormatted = dateFormatted,
                )
            }
            .distinctBy { "${it.title}-${it.body}" }
    }
}
