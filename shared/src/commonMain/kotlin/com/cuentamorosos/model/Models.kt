package com.cuentamorosos.model

import androidx.compose.ui.graphics.Color
import com.cuentamorosos.currentTimeMillis
import com.cuentamorosos.formatDateMillis
import com.cuentamorosos.generateUuid
import com.cuentamorosos.parseDateString
import com.cuentamorosos.currentDateText
import kotlin.math.roundToInt

enum class ExpenseCategory(
    val id: String,
    val label: String,
    val iconEmoji: String,
    val iconBgColor: Color,
) {
    SHARED("shared", "Compartido", "\uD83D\uDC65", Color(0xFFB388FF)),
    FLIGHT("flight", "Vuelo", "\u2708\uFE0F", Color(0xFFDFDCE1)),
    ACCOMMODATION("accommodation", "Alojamiento", "\uD83C\uDFE8", Color(0xFFC8C5CB)),
    FOOD("food", "Comida", "\uD83C\uDF7D\uFE0F", Color(0xFFFFB4AB)),
    TRANSPORT("transport", "Transporte", "\uD83D\uDE8C", Color(0xFF39FF14)),
    OTHER("other", "Otro", "\uD83D\uDCE6", Color(0xFFBACCB0));

    companion object {
        fun fromId(id: String): ExpenseCategory = entries.firstOrNull { it.id == id } ?: OTHER
    }
}

data class EventItem(
    val id: String = generateUuid(),
    val name: String,
    val dateMillis: Long,
    val ownerId: String,
    val memberIds: List<String> = emptyList(),
    val lastCalculationMode: String? = null,
    val lastCalculationTotal: Double? = null,
    val lastCalculationTimestamp: Long? = null,
    val lastCalculationSummary: String? = null,
)

data class ProfileItem(
    val id: String = generateUuid(),
    val name: String,
    val icon: String,
    val totalPendingEuros: Double = 0.0,
    val isGhost: Boolean = false,
    val linkedEmail: String? = null,
)

data class UserPreferences(
    val themeMode: String = "system",
    @Deprecated("No longer used — design system is fixed")
    val accentColorId: String = "rose",
    val reminderDays: Int = 7,
    val remindersEnabled: Boolean = true,
)

data class EventDebtItem(
    val id: String = generateUuid(),
    val eventId: String,
    val profileId: String,
    val amountEuros: Double = 0.0,
    val notes: String = "",
    val paid: Boolean = false,
    val calculationMode: String? = null,
)

data class EventExpenseItem(
    val id: String = generateUuid(),
    val eventId: String,
    val name: String,
    val amountEuros: Double,
    val category: String = "shared",
    val assignedProfileIds: List<String> = emptyList(),
    val profileWeights: Map<String, Double> = emptyMap(),
)

data class EventInvitation(
    val id: String = generateUuid(),
    val eventId: String,
    val eventName: String,
    val invitedByUid: String,
    val invitedByEmail: String,
    val invitedEmail: String,
    val status: String = InvitationStatus.PENDING,
    val createdAtMillis: Long = currentTimeMillis(),
    val expiresAtMillis: Long = currentTimeMillis() + 7L * 24 * 60 * 60 * 1000,
)

object InvitationStatus {
    const val PENDING = "pending"
    const val ACCEPTED = "accepted"
    const val REJECTED = "rejected"
}

fun EventItem.formattedDate(): String = formatDateMillis(dateMillis)

fun parseEventDate(value: String): Long? = parseDateString(value)

fun currentDateFormatted(): String = currentDateText()

fun formatEuros(value: Double): String = buildString {
    val intPart = value.toLong()
    val decPart = ((value - intPart) * 100).roundToInt().let { if (it < 0) -it else it }
    append(intPart)
    append('.')
    if (decPart < 10) append('0')
    append(decPart)
    append(" €")
}

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
