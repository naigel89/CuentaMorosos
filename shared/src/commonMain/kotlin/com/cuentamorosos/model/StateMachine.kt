package com.cuentamorosos.model

/** Result of attempting a state transition. */
sealed class StateTransitionResult {
    /** Transition is allowed — proceed immediately. */
    data class Allowed(val newState: EventState) : StateTransitionResult()

    /** Transition is blocked — one or more preconditions failed. */
    data class Blocked(val reasons: List<String>) : StateTransitionResult()

    /** Transition is allowed but user should confirm due to side effects. */
    data class AllowedWithWarning(
        val newState: EventState,
        val warning: String,
    ) : StateTransitionResult()
}

/** Context data needed to evaluate transition guards. */
data class TransitionContext(
    val eventName: String = "",
    val eventBaseCurrency: String = "",
    val memberCount: Int = 0,
    val expenseCount: Int = 0,
    val hasInvalidExpenses: Boolean = false,
    val isOwner: Boolean = false,
    val transfersPaidPercentage: Double = 0.0,
    val pendingPayments: Int = 0,
)

/**
 * Evaluates whether a state transition is allowed.
 * Returns [StateTransitionResult] with the decision and any reasons.
 */
fun attemptTransition(
    current: EventState,
    target: EventState,
    context: TransitionContext,
): StateTransitionResult = when (current to target) {
    EventState.DRAFT to EventState.OPEN -> guardDraftToOpen(context)
    EventState.OPEN to EventState.CALCULATED -> guardOpenToCalculated(context)
    EventState.CALCULATED to EventState.OPEN -> guardCalculatedToOpen(context)
    EventState.CALCULATED to EventState.CLOSED -> guardCalculatedToClosed(context)
    EventState.CLOSED to EventState.DRAFT,
    EventState.CLOSED to EventState.OPEN,
    EventState.CLOSED to EventState.CALCULATED,
    -> guardFromClosed()

    else -> StateTransitionResult.Blocked(
        listOf("Transición no válida: ${current.name} → ${target.name}"),
    )
}

private fun guardDraftToOpen(context: TransitionContext): StateTransitionResult {
    val reasons = mutableListOf<String>()
    if (context.eventName.isBlank()) {
        reasons.add("El evento necesita un nombre")
    }
    if (context.eventBaseCurrency.isBlank()) {
        reasons.add("La divisa base es obligatoria")
    }
    if (context.memberCount < 2) {
        reasons.add("Se necesitan al menos 2 participantes para abrir el evento")
    }
    return if (reasons.isEmpty()) {
        StateTransitionResult.Allowed(EventState.OPEN)
    } else {
        StateTransitionResult.Blocked(reasons)
    }
}

private fun guardOpenToCalculated(context: TransitionContext): StateTransitionResult {
    val reasons = mutableListOf<String>()
    if (context.expenseCount == 0) {
        reasons.add("No hay gastos registrados para calcular")
    }
    if (context.hasInvalidExpenses) {
        reasons.add("Hay gastos inválidos. Revisá los importes antes de calcular")
    }
    return if (reasons.isEmpty()) {
        StateTransitionResult.Allowed(EventState.CALCULATED)
    } else {
        StateTransitionResult.Blocked(reasons)
    }
}

private fun guardCalculatedToOpen(context: TransitionContext): StateTransitionResult {
    if (context.transfersPaidPercentage >= 100.0) {
        return StateTransitionResult.Blocked(
            listOf("No se puede recalcular porque todas las transferencias ya están cobradas"),
        )
    }
    val warning = buildString {
        append("Se creará una nueva versión del cálculo. ")
        append("Los pagos marcados como cobrados se revertirán.")
        if (context.pendingPayments > 0) {
            append(" Hay ${context.pendingPayments} transferencia(s) cobrada(s) que se verán afectadas.")
        }
    }
    return StateTransitionResult.AllowedWithWarning(
        EventState.OPEN,
        warning,
    )
}

private fun guardCalculatedToClosed(context: TransitionContext): StateTransitionResult {
    if (!context.isOwner) {
        return StateTransitionResult.Blocked(
            listOf("Solo el propietario puede cerrar el evento"),
        )
    }
    return if (context.pendingPayments > 0) {
        StateTransitionResult.AllowedWithWarning(
            EventState.CLOSED,
            "Hay pagos pendientes de cobrar",
        )
    } else {
        StateTransitionResult.Allowed(EventState.CLOSED)
    }
}

private fun guardFromClosed(): StateTransitionResult =
    StateTransitionResult.Blocked(
        listOf("El evento está cerrado y no puede modificarse"),
    )
