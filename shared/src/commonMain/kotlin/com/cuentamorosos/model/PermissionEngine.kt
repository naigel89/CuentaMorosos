package com.cuentamorosos.model

/**
 * Sealed hierarchy of all permission-guarded actions in the system.
 */
sealed class EventAction {
    object CreateExpense : EventAction()
    data class EditExpense(val expenseOwnerId: String) : EventAction()
    data class DeleteExpense(val expenseOwnerId: String) : EventAction()
    object ManageParticipants : EventAction()
    object AssignRoles : EventAction()
    object Calculate : EventAction()
    object Close : EventAction()
    object DeleteEvent : EventAction()
    object Reopen : EventAction()
    object LeaveEvent : EventAction()
}

/**
 * Pure permission engine — no side effects, no state.
 * All functions are deterministic given their inputs.
 */
object PermissionEngine {

    /**
     * Returns the role of a profile within an event.
     * Owner is determined by ownerId match; otherwise looks up participants.
     * Defaults to READER if not found.
     */
    fun getRole(profileId: String, event: EventItem): EventRole {
        if (profileId.isBlank()) {
            println("[PermissionEngine] getRole: profileId is BLANK → READER")
            return EventRole.READER
        }
        if (event.ownerId == profileId) {
            println("[PermissionEngine] getRole: ownerId==profileId ($profileId) → OWNER")
            return EventRole.OWNER
        }
        println("[PermissionEngine] getRole: ownerId='${event.ownerId}' != profileId='$profileId', checking participants...")
        val participant = event.participants.find { it.profileId == profileId }
        val role = participant?.role ?: EventRole.READER
        println("[PermissionEngine] getRole: participant found=${participant != null}, participants=${event.participants.map { "${it.profileId}:${it.role}" }}, role=$role")
        return role
    }

    /**
     * Checks whether a given role can perform an action.
     * Pure function — does not look up the role itself.
     */
    fun hasPermission(role: EventRole, action: EventAction): Boolean = when (action) {
        EventAction.CreateExpense -> role != EventRole.READER
        is EventAction.EditExpense -> role == EventRole.OWNER ||
            (role == EventRole.CONTRIBUTOR && action.expenseOwnerId.isNotBlank())
        is EventAction.DeleteExpense -> role == EventRole.OWNER
        EventAction.ManageParticipants -> role == EventRole.OWNER || role == EventRole.CONTRIBUTOR
        EventAction.AssignRoles,
        EventAction.Calculate,
        EventAction.Close,
        EventAction.DeleteEvent,
        EventAction.Reopen -> role == EventRole.OWNER
        EventAction.LeaveEvent -> true
    }

    /**
     * Full permission check: resolves role then checks action.
     * For EditExpense, verifies the expense owner matches the profile when role is CONTRIBUTOR.
     */
    fun canDo(profileId: String, event: EventItem, action: EventAction): Boolean {
        val role = getRole(profileId, event)
        return when (action) {
            EventAction.CreateExpense -> role != EventRole.READER
            is EventAction.EditExpense -> role == EventRole.OWNER ||
                (role == EventRole.CONTRIBUTOR && action.expenseOwnerId == profileId)
            is EventAction.DeleteExpense -> role == EventRole.OWNER
            EventAction.ManageParticipants -> role == EventRole.OWNER || role == EventRole.CONTRIBUTOR
            EventAction.AssignRoles,
            EventAction.Calculate,
            EventAction.Close,
            EventAction.DeleteEvent,
            EventAction.Reopen -> role == EventRole.OWNER
            EventAction.LeaveEvent -> true
        }
    }

    /**
     * Handles owner leaving an event.
     * Promotes the oldest CONTRIBUTOR to OWNER.
     * If no contributors exist, removes the owner (event needs someone to claim ownership).
     * Blocks if the owner is the only participant.
     */
    fun onOwnerLeave(
        participants: List<EventParticipant>,
        ownerId: String,
    ): Result<List<EventParticipant>> {
        val others = participants.filter { it.profileId != ownerId }

        if (others.isEmpty()) {
            return Result.failure(
                IllegalStateException("No se puede abandonar el evento sin dejar un propietario")
            )
        }

        val oldestContributor = others
            .filter { it.role == EventRole.CONTRIBUTOR }
            .minByOrNull { it.joinedAtMillis }

        return if (oldestContributor != null) {
            Result.success(
                participants.map {
                    if (it.profileId == oldestContributor.profileId) {
                        it.copy(role = EventRole.OWNER)
                    } else if (it.profileId == ownerId) {
                        it.copy(role = EventRole.CONTRIBUTOR)
                    } else {
                        it
                    }
                }
            )
        } else {
            // No contributors — owner leaves, remaining participants stay as-is (READERs)
            Result.success(others)
        }
    }

    /**
     * Checks whether a participant can be removed.
     * Blocks removal if the profile is a payer or debtor in any expense.
     */
    fun canRemoveParticipant(
        profileId: String,
        expenses: List<EventExpenseItem>,
    ): Result<Unit> {
        val isPayer = expenses.any { it.payerContributions.containsKey(profileId) }
        val isDebtor = expenses.any { it.debtorIds.contains(profileId) }

        return when {
            isPayer || isDebtor -> Result.failure(
                IllegalStateException("No se puede eliminar a este perfil porque participa en gastos del evento")
            )
            else -> Result.success(Unit)
        }
    }

    /**
     * Combined guard: checks role permission AND state-based integrity.
     * Returns true only when the profile has OWNER role AND the event is in OPEN state.
     */
    fun canDeleteExpenseWithGuard(
        profileId: String,
        event: EventItem,
        expenseId: String,
        allExpenses: List<EventExpenseItem>,
    ): Result<Unit> {
        // Role-based check first
        if (!canDo(profileId, event, EventAction.DeleteExpense(expenseId))) {
            return Result.failure(
                IllegalStateException("No tenés permiso para eliminar este gasto")
            )
        }
        // State-based integrity check
        return if (IntegrityGuard.canDeleteExpense(event.state, expenseId, allExpenses)) {
            Result.success(Unit)
        } else {
            Result.failure(
                when (event.state) {
                    EventState.CALCULATED -> IllegalStateException(
                        "No se puede eliminar un gasto en un evento calculado. Volvé a estado Abierto primero."
                    )
                    EventState.CLOSED -> IllegalStateException(
                        "El evento está cerrado y no puede modificarse."
                    )
                    else -> IllegalStateException("No se puede eliminar este gasto en el estado actual.")
                }
            )
        }
    }

    /**
     * Transfers ownership from one participant to another.
     * The old owner becomes a CONTRIBUTOR.
     */
    fun transferOwnership(
        fromProfileId: String,
        toProfileId: String,
        participants: List<EventParticipant>,
    ): Result<List<EventParticipant>> {
        if (fromProfileId == toProfileId) {
            return Result.failure(IllegalStateException("No se puede transferir la propiedad al mismo perfil"))
        }

        val targetExists = participants.any { it.profileId == toProfileId }
        if (!targetExists) {
            return Result.failure(IllegalStateException("El perfil destino no es participante"))
        }

        return Result.success(
            participants.map {
                when (it.profileId) {
                    fromProfileId -> it.copy(role = EventRole.CONTRIBUTOR)
                    toProfileId -> it.copy(role = EventRole.OWNER)
                    else -> it
                }
            }
        )
    }
}

/**
 * Extension: get the role of a profile in an event.
 */
fun EventItem.getParticipantRole(profileId: String): EventRole =
    PermissionEngine.getRole(profileId, this)

/**
 * Extension: check if a profile can perform an action on an event.
 */
fun EventItem.canDo(profileId: String, action: EventAction): Boolean =
    PermissionEngine.canDo(profileId, this, action)

// ── Participant serialization for SQLDelight cache ────────────────────────────

/**
 * Serializes a list of EventParticipant to a compact string format.
 * Format: "profileId|role|joinedAtMillis;profileId|role|joinedAtMillis;..."
 */
fun List<EventParticipant>.serializeParticipants(): String =
    if (isEmpty()) "" else joinToString(";") { "${it.profileId}|${it.role.name}|${it.joinedAtMillis}" }

/**
 * Deserializes a string back to a list of EventParticipant.
 * Returns empty list on parse failure for safety.
 */
fun deserializeParticipants(raw: String): List<EventParticipant> =
    if (raw.isBlank()) emptyList() else runCatching {
        raw.split(";").mapNotNull { segment ->
            val parts = segment.split("|")
            if (parts.size < 3) return@mapNotNull null
            val profileId = parts[0].takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val role = runCatching { EventRole.valueOf(parts[1]) }.getOrNull() ?: return@mapNotNull null
            val joinedAt = parts[2].toLongOrNull() ?: 0L
            EventParticipant(profileId = profileId, role = role, joinedAtMillis = joinedAt)
        }
    }.getOrDefault(emptyList())
