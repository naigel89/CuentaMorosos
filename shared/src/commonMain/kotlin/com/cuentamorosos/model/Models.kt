package com.cuentamorosos.model

import androidx.compose.ui.graphics.Color
import com.cuentamorosos.currentTimeMillis
import com.cuentamorosos.formatDateMillis
import com.cuentamorosos.generateUuid
import com.cuentamorosos.parseDateString
import com.cuentamorosos.currentDateText
import kotlin.math.roundToInt

/**
 * The only supported currency for this release.
 * All amounts are stored and displayed in EUR.
 */
const val SUPPORTED_CURRENCY = "EUR"

/**
 * Event lifecycle states.
 * Transitions (informational): DRAFT → OPEN → CALCULATED → CLOSED
 */
enum class EventState {
    /** Event being configured, not yet active. */
    DRAFT,
    /** Event is active, expenses can be added. */
    OPEN,
    /** Debts have been calculated. */
    CALCULATED,
    /** Event is finalized, no further changes. */
    CLOSED,
}

/**
 * Role-based access levels for event participants.
 */
enum class EventRole {
    /** Full control over event, expenses, participants, and lifecycle. */
    OWNER,
    /** Can create and edit own expenses, view all. */
    CONTRIBUTOR,
    /** View-only access, no write operations. */
    READER,
}

/**
 * Represents a participant in an event with their access role.
 */
data class EventParticipant(
    val profileId: String,
    val role: EventRole = EventRole.CONTRIBUTOR,
    val joinedAtMillis: Long = 0L,
)

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
    val participants: List<EventParticipant> = emptyList(),
    val startDateMillis: Long = dateMillis,
    val endDateMillis: Long = dateMillis,
    val baseCurrency: String = SUPPORTED_CURRENCY,
    val creatorId: String = "",
    val state: EventState = EventState.DRAFT,
    val lastCalculationMode: String? = null,
    val lastCalculationTotal: Double? = null,
    val lastCalculationTimestamp: Long? = null,
    val lastCalculationSummary: String? = null,
) {
    /**
     * Computed member IDs for backward compatibility.
     * Returns participant profileIds when participants list is populated,
     * otherwise falls back to the legacy memberIds field.
     */
    val effectiveMemberIds: List<String>
        get() = if (participants.isNotEmpty()) participants.map { it.profileId } else memberIds
}

data class ProfileItem(
    val id: String = generateUuid(),
    val name: String,
    val icon: String,
    val totalPendingEuros: Double = 0.0,
    val isGhost: Boolean = false,
    val linkedEmail: String? = null,
    val ownerId: String = "",
    val photoUrl: String? = null,
    val username: String? = null,
    val displayName: String? = null,
    val customNames: Map<String, String> = emptyMap(),
)

/**
 * Resolves the visible display name for a given viewer.
 * Priority: viewer-specific customName > profile displayName > original name.
 */
fun ProfileItem.displayNameFor(viewerId: String): String =
    customNames[viewerId] ?: displayName ?: name

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

/** Snapshot of a calculation run — immutable once created. */
data class CalculationSnapshot(
    val transfers: List<SettlementTransfer>,
    val totalExpense: Double,
    val calculatedAtMillis: Long,
    val algorithmVersion: String = "v1-greedy",
    val participantBalances: Map<String, Double> = emptyMap(),
)

/** Result of running the settlement calculation. */
data class CalculationResult(
    val snapshot: CalculationSnapshot? = null,
    val errors: List<String> = emptyList(),
    val status: CalculationStatus? = null,
) {
    val isSuccess: Boolean get() = snapshot != null && errors.isEmpty()
}

/** Status of a calculation run, including edge case detection. */
sealed class CalculationStatus {
    abstract val message: String
    data class Success(override val message: String = "Cálculo completado") : CalculationStatus()
    data class ZeroBalance(override val message: String = "Todo está saldado") : CalculationStatus()
    data class EdgeCaseWarning(override val message: String) : CalculationStatus()
    data class Error(override val message: String) : CalculationStatus()
}

data class EventExpenseItem(
    val id: String = generateUuid(),
    val eventId: String,
    val name: String,
    val amountEuros: Double,
    val category: String = "shared",
    val assignedProfileIds: List<String> = emptyList(),
    val profileWeights: Map<String, Double> = emptyMap(),
    val paidByProfileId: String = "",
    val splitMode: String = "SIMPLE_AVG",
    val payerContributions: Map<String, Double> = emptyMap(),
    val debtorIds: List<String> = emptyList(),
    val exchangeRate: Double? = null,
    val itemCurrency: String? = null,
    val createdAtMillis: Long = 0,
    val createdByProfileId: String = "",
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

// ── State helpers ─────────────────────────────────────────────────────────────

fun EventItem.isDraft(): Boolean = state == EventState.DRAFT
fun EventItem.isOpen(): Boolean = state == EventState.OPEN
fun EventItem.isCalculated(): Boolean = state == EventState.CALCULATED
fun EventItem.isClosed(): Boolean = state == EventState.CLOSED

fun EventItem.stateLabel(): String = when (state) {
    EventState.DRAFT -> "Borrador"
    EventState.OPEN -> "Abierto"
    EventState.CALCULATED -> "Calculado"
    EventState.CLOSED -> "Cerrado"
}

fun EventItem.canTransitionTo(target: EventState, context: TransitionContext): StateTransitionResult =
    attemptTransition(state, target, context)

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

// ── Audit & Integrity Models ────────────────────────────────────────────────

/** Actions tracked in the expense audit trail. */
enum class AuditAction {
    CREATED,
    UPDATED,
    DELETED,
}

/** Immutable audit record for expense mutations. Append-only; only cascade-deleted with event. */
data class ExpenseAuditEntry(
    val id: String = generateUuid(),
    val eventId: String,
    val expenseId: String,
    val action: AuditAction,
    val profileId: String,
    val timestamp: Long = currentTimeMillis(),
    val beforeSnapshot: String? = null,
    val afterSnapshot: String? = null,
)

/** Versioned snapshot of a calculation run. Previous versions are never overwritten. */
data class CalculationVersion(
    val id: String = generateUuid(),
    val eventId: String,
    val version: Int,
    val snapshotJson: String,
    val createdAt: Long = currentTimeMillis(),
    val isActive: Boolean = false,
)

/** Rectification entry for correcting paid transfers. Original debt remains immutable. */
data class AdjustmentEntry(
    val id: String = generateUuid(),
    val eventId: String,
    val transferKey: String,
    val deltaCents: Int,
    val reason: String,
    val profileId: String,
    val timestamp: Long = currentTimeMillis(),
)

// ── JSON Snapshot Helpers ───────────────────────────────────────────────────

private fun String.escapeJson(): String = this
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
    .replace("\n", "\\n")
    .replace("\r", "\\r")
    .replace("\t", "\\t")

/** Serializes this expense to a JSON string for audit before/after snapshots. */
fun EventExpenseItem.toJsonSnapshot(): String = buildString {
    append("{")
    append("\"id\":\"${id.escapeJson()}\",")
    append("\"eventId\":\"${eventId.escapeJson()}\",")
    append("\"name\":\"${name.escapeJson()}\",")
    append("\"amountEuros\":$amountEuros,")
    append("\"category\":\"${category.escapeJson()}\",")
    append("\"paidByProfileId\":\"${paidByProfileId.escapeJson()}\",")
    append("\"splitMode\":\"${splitMode.escapeJson()}\",")
    append("\"debtorIds\":[${debtorIds.joinToString(",") { "\"${it.escapeJson()}\"" }}],")
    append("\"assignedProfileIds\":[${assignedProfileIds.joinToString(",") { "\"${it.escapeJson()}\"" }}],")
    append("\"profileWeights\":{${profileWeights.entries.joinToString(",") { (k, v) -> "\"${k.escapeJson()}\":$v" }}},")
    append("\"createdAtMillis\":$createdAtMillis,")
    append("\"createdByProfileId\":\"${createdByProfileId.escapeJson()}\"")
    append("}")
}

/** Serializes this transfer to a compact JSON object. */
fun SettlementTransfer.toJson(): String = buildString {
    append("{\"from\":\"${fromProfileId.escapeJson()}\",\"to\":\"${toProfileId.escapeJson()}\",\"amount\":$amount}")
}

/** Serializes a list of transfers to a JSON array string. */
fun List<SettlementTransfer>.toJson(): String = joinToString(",", "[", "]") { it.toJson() }

/** Serializes this calculation snapshot to a JSON string for version storage. */
fun CalculationSnapshot.toJson(): String = buildString {
    append("{\"transfers\":${transfers.toJson()},")
    append("\"totalExpense\":$totalExpense,")
    append("\"calculatedAtMillis\":$calculatedAtMillis,")
    append("\"algorithmVersion\":\"${algorithmVersion.escapeJson()}\"}")
}

/** Deserializes a JSON array of transfers back to a list. */
fun String.toSettlementTransfers(): List<SettlementTransfer> {
    val transfers = mutableListOf<SettlementTransfer>()
    val regex = """\{"from":"([^"]*)","to":"([^"]*)","amount":([\d.eE+\-]+)\}""".toRegex()
    for (match in regex.findAll(this)) {
        transfers.add(
            SettlementTransfer(
                fromProfileId = match.groupValues[1],
                toProfileId = match.groupValues[2],
                amount = match.groupValues[3].toDouble(),
            )
        )
    }
    return transfers
}

/** Deserializes a JSON calculation snapshot string back to a CalculationSnapshot. */
fun String.toCalculationSnapshot(): CalculationSnapshot? {
    val transfersRegex = """"transfers"\s*:\s*(\[[^\]]*\])""".toRegex()
    val transfersMatch = transfersRegex.find(this) ?: return null
    val transfers = transfersMatch.groupValues[1].toSettlementTransfers()

    val totalRegex = """"totalExpense"\s*:\s*([\d.eE+\-]+)""".toRegex()
    val totalMatch = totalRegex.find(this) ?: return null
    val totalExpense = totalMatch.groupValues[1].toDouble()

    val timeRegex = """"calculatedAtMillis"\s*:\s*(\d+)""".toRegex()
    val timeMatch = timeRegex.find(this) ?: return null
    val calculatedAtMillis = timeMatch.groupValues[1].toLong()

    val algoRegex = """"algorithmVersion"\s*:\s*"([^"]*)"""".toRegex()
    val algoMatch = algoRegex.find(this)
    val algorithmVersion = algoMatch?.groupValues?.get(1) ?: "v1-greedy"

    return CalculationSnapshot(
        transfers = transfers,
        totalExpense = totalExpense,
        calculatedAtMillis = calculatedAtMillis,
        algorithmVersion = algorithmVersion,
    )
}
