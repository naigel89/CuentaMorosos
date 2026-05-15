package com.cuentamorosos.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

data class EventItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val dateMillis: Long,
    val lastCalculationMode: String? = null,
    val lastCalculationTotal: Double? = null,
    val lastCalculationTimestamp: Long? = null,
    val lastCalculationSummary: String? = null,
)

data class ProfileItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val icon: String,
    val totalPendingEuros: Double = 0.0,
)

data class UserPreferences(
    val themeMode: String = "system",
    @Deprecated("No longer used — design system is fixed")
    val accentColorId: String = "rose",
    val reminderDays: Int = 7,
    val remindersEnabled: Boolean = true,
)

data class EventDebtItem(
    val id: String = UUID.randomUUID().toString(),
    val eventId: String,
    val profileId: String,
    val amountEuros: Double = 0.0,
    val notes: String = "",
    val paid: Boolean = false,
    val calculationMode: String? = null,
)

data class EventExpenseItem(
    val id: String = UUID.randomUUID().toString(),
    val eventId: String,
    val name: String,
    val amountEuros: Double,
    val category: String = "shared",
    val assignedProfileIds: List<String> = emptyList(),
    val profileWeights: Map<String, Double> = emptyMap(),
    val paidByProfileId: String = "",
)

private val eventDateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

fun EventItem.formattedDate(): String = synchronized(eventDateFormatter) {
    eventDateFormatter.format(Date(dateMillis))
}

fun parseEventDate(value: String): Long? = synchronized(eventDateFormatter) {
    runCatching {
        eventDateFormatter.isLenient = false
        eventDateFormatter.parse(value)?.time
    }.getOrNull()
}

fun currentDateText(): String = synchronized(eventDateFormatter) {
    eventDateFormatter.format(Date())
}

fun formatEuros(value: Double): String = String.format(Locale.getDefault(), "%.2f €", value)

fun parseEuroAmount(value: String): Double? = value
    .trim()
    .replace("€", "")
    .replace(',', '.')
    .toDoubleOrNull()

fun splitAmountEvenly(total: Double, participants: Int): List<Double> {
    if (participants <= 0) return emptyList()

    val totalCents = (total * 100).roundToInt()
    val baseCents = totalCents / participants
    val remainderCents = totalCents % participants

    return List(participants) { index ->
        val assignedCents = baseCents + if (index == 0) remainderCents else 0
        assignedCents / 100.0
    }
}
