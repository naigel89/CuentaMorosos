package com.cuentamorosos.model

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Modos de reparto disponibles en la calculadora de CuentaMorosos.
 *
 * Se mantienen únicamente los cuatro modos más habituales para evitar confusión:
 *  - [REAL_CONSUMPTION]: predeterminado, refleja directamente los ítems del evento.
 *  - [SIMPLE_AVG]: la opción más rápida cuando nadie lleva la cuenta de qué consumió.
 *  - [BY_CATEGORY]: útil cuando el evento mezcla gastos compartidos con gastos personales.
 *  - [CUSTOM_PERCENTAGE]: para acuerdos explícitos de porcentaje entre los participantes.
 *
 * Modos eliminados por ser poco frecuentes o difíciles de configurar correctamente:
 *  BY_WEIGHT, BY_INCOME, BASE_PLUS_SURPLUS, BY_ATTENDANCE, MIXED.
 */
enum class SplitMode(
    val id: String,
    val label: String,
    /** Descripción breve del criterio de reparto. */
    val helperText: String,
    /** Ejemplo concreto que ilustra cómo funciona el modo. */
    val exampleText: String,
) {
    /**
     * Modo predeterminado. Cada ítem se divide exclusivamente entre los perfiles
     * asignados a ese ítem. Refleja sin cálculo adicional lo que ya se definió
     * al crear cada gasto del evento.
     */
    REAL_CONSUMPTION(
        id = "real_consumption",
        label = "Consumo real",
        helperText = "Cada ítem se reparte solo entre los perfiles asignados.",
        exampleText = "Alice pidió un café (1,50 €) y Bob una tarta (3,00 €): cada uno paga lo suyo.",
    ),

    /**
     * División equitativa del total entre todos los participantes.
     * Ideal cuando nadie lleva la cuenta de qué consumió cada persona.
     */
    SIMPLE_AVG(
        id = "simple_avg",
        label = "Media simple",
        helperText = "Divide el total a partes iguales entre todos.",
        exampleText = "Una cena de 60 € entre 3 personas → 20 € cada uno.",
    ),

    /**
     * Combina ítems compartidos, ítems solo para algunos perfiles y cargos personales.
     * Cada gasto del evento lleva una categoría que determina cómo se reparte.
     */
    BY_CATEGORY(
        id = "by_category",
        label = "Por categoría",
        helperText = "Cada ítem usa su categoría: compartido, solo seleccionados o cargo individual.",
        exampleText = "La botella de vino (compartida, 12 €) se divide entre todos; el taxi de Ana (personal, 8 €) lo paga solo ella.",
    ),

    /**
     * Cada participante asume el porcentaje del total acordado previamente.
     * La suma de todos los porcentajes debe ser exactamente 100 %.
     */
    CUSTOM_PERCENTAGE(
        id = "custom_percentage",
        label = "% personalizado",
        helperText = "Asigna un porcentaje a cada perfil. La suma debe ser 100 %.",
        exampleText = "Un piso entre 2 personas: uno paga el 60 % y el otro el 40 % del alquiler.",
    );

    companion object {
        fun fromId(id: String): SplitMode = entries.firstOrNull { it.id == id } ?: REAL_CONSUMPTION
    }
}

enum class ExpenseCategory(
    val id: String,
    val label: String,
    val helperText: String,
) {
    SHARED(
        id = "shared",
        label = "Compartido",
        helperText = "Se divide entre todos los perfiles del evento."
    ),
    SELECTED(
        id = "selected",
        label = "Solo seleccionados",
        helperText = "Se reparte entre los perfiles marcados en el ítem."
    ),
    PERSONAL(
        id = "personal",
        label = "Cargo individual",
        helperText = "Se asigna completo al primer perfil seleccionado."
    );

    companion object {
        fun fromId(id: String): ExpenseCategory = entries.firstOrNull { it.id == id } ?: SHARED
    }
}

data class CalculationPreview(
    val amounts: List<Double> = emptyList(),
    val validationMessage: String? = null,
    val summary: String = "",
    val calculatedTotal: Double = 0.0,
)

data class CalculationApplication(
    val total: Double,
    val mode: SplitMode,
    val amounts: List<Double>,
    val summary: String,
)

fun buildCalculationPreview(
    total: Double,
    mode: SplitMode,
    inputs: List<Double>,
    participantIds: List<String> = emptyList(),
    expenses: List<EventExpenseItem> = emptyList(),
): CalculationPreview {
    if (total < 0.0) {
        return CalculationPreview(validationMessage = "El total no puede ser negativo.")
    }

    return when (mode) {
        // ── Predeterminado ──────────────────────────────────────────────────
        // Cada ítem se divide solo entre sus perfiles asignados.
        // Refleja sin cálculo adicional lo que ya se definió al crear los gastos.
        SplitMode.REAL_CONSUMPTION -> buildExpenseDrivenPreview(
            mode = mode,
            participantIds = participantIds,
            expenses = expenses,
        )

        // ── División equitativa ─────────────────────────────────────────────
        // Opción más rápida cuando nadie lleva cuenta de qué consumió cada uno.
        SplitMode.SIMPLE_AVG -> CalculationPreview(
            amounts = splitAmountEvenly(total = total, participants = inputs.size),
            summary = "Media simple entre ${inputs.size} perfiles",
            calculatedTotal = total,
        )

        // ── Por categoría ───────────────────────────────────────────────────
        // Cada ítem lleva su propia regla: compartido, solo seleccionados o personal.
        // Útil cuando el evento mezcla gastos colectivos con gastos individuales.
        SplitMode.BY_CATEGORY -> buildExpenseDrivenPreview(
            mode = mode,
            participantIds = participantIds,
            expenses = expenses,
        )

        // ── Porcentaje personalizado ────────────────────────────────────────
        // Para acuerdos explícitos de porcentaje acordados de antemano.
        // La suma de todos los porcentajes debe ser exactamente 100 %.
        SplitMode.CUSTOM_PERCENTAGE -> {
            val sum = inputs.sum()
            when {
                inputs.isEmpty() -> CalculationPreview(validationMessage = "Añade al menos un perfil al evento.")
                inputs.any { it < 0.0 } -> CalculationPreview(validationMessage = "Los porcentajes no pueden ser negativos.")
                abs(sum - 100.0) > 0.01 -> CalculationPreview(
                    validationMessage = "La suma actual es ${String.format("%.2f", sum)}%. Debe ser 100%."
                )
                else -> CalculationPreview(
                    amounts = distributeProportionally(total = total, factors = inputs),
                    summary = "Porcentajes personalizados: ${inputs.joinToString { String.format("%.0f%%", it) }}",
                    calculatedTotal = total,
                )
            }
        }
    }
}

private fun buildExpenseDrivenPreview(
    mode: SplitMode,
    participantIds: List<String>,
    expenses: List<EventExpenseItem>,
): CalculationPreview {
    if (participantIds.isEmpty()) {
        return CalculationPreview(validationMessage = "Añade al menos un perfil al evento.")
    }
    if (expenses.isEmpty()) {
        return CalculationPreview(validationMessage = "Añade ítems del evento para usar este modo.")
    }
    if (mode == SplitMode.BY_CATEGORY && expenses.any { it.category.isBlank() }) {
        return CalculationPreview(validationMessage = "Hay ítems sin categoría. Debes clasificarlos antes de aplicar este modo.")
    }

    val accumulatedCents = IntArray(participantIds.size)
    val totalExpenseAmount = expenses.sumOf { it.amountEuros }

    expenses.forEach { expense ->
        val recipientIndexes = when (mode) {
            SplitMode.REAL_CONSUMPTION -> indexesForIds(
                participantIds = participantIds,
                assignedIds = expense.assignedProfileIds,
            )
        
            SplitMode.BY_CATEGORY -> when (ExpenseCategory.fromId(expense.category)) {
                ExpenseCategory.SHARED -> participantIds.indices.toList()
                ExpenseCategory.SELECTED -> indexesForIds(
                    participantIds = participantIds,
                    assignedIds = expense.assignedProfileIds,
                )
        
                ExpenseCategory.PERSONAL -> indexesForIds(
                    participantIds = participantIds,
                    assignedIds = expense.assignedProfileIds.take(1),
                )
            }
        
            else -> emptyList()
        }
        
        if (recipientIndexes.isEmpty()) {
            val validationMessage = if (mode == SplitMode.BY_CATEGORY) {
                "Revisa la categoría o los perfiles asignados de `${expense.name}`."
            } else {
                "Asigna al menos un perfil al ítem `${expense.name}` para consumo real."
            }
            return CalculationPreview(validationMessage = validationMessage)
        }
        
        distributeAmountAcrossIndexes(
            accumulatedCents = accumulatedCents,
            amount = expense.amountEuros,
            recipientIndexes = recipientIndexes,
            weights = expense.profileWeights,
            participantIds = participantIds
        )
    }


    return CalculationPreview(
        amounts = accumulatedCents.map { it / 100.0 },
        summary = when (mode) {
            SplitMode.REAL_CONSUMPTION -> "Consumo real sobre ${expenses.size} ítems"
            SplitMode.BY_CATEGORY -> "Por categoría sobre ${expenses.size} ítems"
            else -> ""
        },
        calculatedTotal = totalExpenseAmount,
    )
}

private fun indexesForIds(
    participantIds: List<String>,
    assignedIds: List<String>,
): List<Int> = assignedIds.mapNotNull { id ->
    participantIds.indexOf(id).takeIf { it >= 0 }
}

private fun distributeAmountAcrossIndexes(
    accumulatedCents: IntArray,
    amount: Double,
    recipientIndexes: List<Int>,
    weights: Map<String, Double> = emptyMap(),
    participantIds: List<String> = emptyList(),
) {
    if (recipientIndexes.isEmpty()) return
    
    val amountCents = (amount * 100).roundToInt()
    
    // Si hay pesos definidos para este gasto, los usamos solo en receptores válidos.
    if (weights.isNotEmpty()) {
        val weightedRecipients = recipientIndexes.mapNotNull { index ->
            val participantId = participantIds.getOrNull(index) ?: return@mapNotNull null
            val weight = weights[participantId] ?: return@mapNotNull null
            if (weight > 0.0) {
                index to weight
            } else {
                null
            }
        }
        val totalWeight = weightedRecipients.sumOf { it.second }

        if (totalWeight > 0.0) {
            val provisionalCentsByIndex = weightedRecipients.associate { (index, weight) ->
                index to floor((amountCents * weight) / totalWeight).toInt()
            }
            val assignedCents = provisionalCentsByIndex.values.sum()
            val remainder = amountCents - assignedCents

            weightedRecipients.forEachIndexed { position, (index, _) ->
                val base = provisionalCentsByIndex[index] ?: 0
                val withRemainder = if (position == 0) base + remainder else base
                accumulatedCents[index] += withRemainder
            }
            return
        }
    }
    
    // Reparto equitativo estándar (si no hay pesos o la suma es 0)
    val baseCents = amountCents / recipientIndexes.size
    val remainderCents = amountCents % recipientIndexes.size
    
    recipientIndexes.forEachIndexed { position, index ->
        accumulatedCents[index] += baseCents + if (position == 0) remainderCents else 0
    }
}


private fun distributeProportionally(total: Double, factors: List<Double>): List<Double> {
    val totalCents = (total * 100).roundToInt()
    return distributeProportionallyCents(totalCents = totalCents, factors = factors).map { cents ->
        cents / 100.0
    }
}

private fun distributeProportionallyCents(totalCents: Int, factors: List<Double>): List<Int> {
    if (factors.isEmpty()) return emptyList()

    val totalFactor = factors.sum()
    if (totalFactor <= 0.0) return List(factors.size) { 0 }

    val provisionalCents = factors.map { factor ->
        floor((totalCents * factor) / totalFactor).toInt()
    }
    val assignedCents = provisionalCents.sum()
    val remainder = totalCents - assignedCents

    return provisionalCents.mapIndexed { index, cents ->
        cents + if (index == 0) remainder else 0
    }
}

// ---------------------------------------------------------------------------
// Liquidación mínima de transferencias (debt-simplification algorithm)
// ---------------------------------------------------------------------------

/**
 * Calcula una vista previa del reparto según el [mode] indicado.
 *
 * Modos disponibles:
 *  - [SplitMode.REAL_CONSUMPTION]: cada ítem se divide entre sus perfiles asignados.
 *  - [SplitMode.SIMPLE_AVG]: división equitativa entre todos los participantes.
 *  - [SplitMode.BY_CATEGORY]: cada ítem usa su categoría (compartido / seleccionados / personal).
 *  - [SplitMode.CUSTOM_PERCENTAGE]: cada perfil asume el porcentaje indicado en [inputs].
 *
 * @param total        Importe total del evento en euros.
 * @param mode         Modo de reparto seleccionado.
 * @param inputs       Factores por perfil (porcentajes para [SplitMode.CUSTOM_PERCENTAGE];
 *                     ignorado en los demás modos).
 * @param participantIds IDs de los perfiles participantes, en el mismo orden que [inputs].
 * @param expenses     Ítems de gasto del evento (necesarios para [SplitMode.REAL_CONSUMPTION]
 *                     y [SplitMode.BY_CATEGORY]).
 * @return [CalculationPreview] con los importes calculados o un mensaje de error de validación.
 */
data class SettlementTransfer(
    val fromName: String,
    val toName: String,
    val amount: Double,
)

/**
 * Calcula el conjunto mínimo de transferencias para liquidar los saldos entre perfiles.
 *
 * Algoritmo greedy O(n log n):
 * 1. Calcula el balance neto de cada participante (lo que le deben menos lo que debe).
 * 2. Divide en acreedores (balance > 0) y deudores (balance < 0).
 * 3. En cada iteración, el deudor más negativo transfiere al acreedor más positivo
 *    el mínimo entre sus valores absolutos, reduciendo el número de transferencias.
 *
 * @param profileNames Lista de nombres en el mismo orden que [amounts].
 * @param amounts Importe que cada perfil debe aportar al total. Un balance negativo
 *   significa que esa persona debe recibir dinero (pagó de más o no debe nada).
 *   En el contexto de CuentaMorosos, [amounts] es lo que cada perfil debe pagar.
 * @param totalPaid Importe total ya cubierto por el "organizador" que debe recuperar.
 *   Si es 0.0 se asume reparto entre iguales sin figura de pagador central.
 */
fun buildSettlementTransfers(
    profileNames: List<String>,
    amounts: List<Double>,
): List<SettlementTransfer> {
    if (profileNames.size != amounts.size || profileNames.isEmpty()) return emptyList()

    // Trabajamos en céntimos para evitar errores de coma flotante
    val balanceCents = amounts.map { (it * 100).roundToInt() }.toMutableList()

    val debtors = ArrayDeque<Pair<Int, Int>>()   // (index, negative balance in cents)
    val creditors = ArrayDeque<Pair<Int, Int>>() // (index, positive balance in cents)

    balanceCents.forEachIndexed { index, cents ->
        when {
            cents < 0 -> creditors.addLast(Pair(index, -cents)) // debe recibir
            cents > 0 -> debtors.addLast(Pair(index, cents))    // debe pagar
        }
    }

    // Ordenar de mayor a menor importe para minimizar iteraciones
    val sortedDebtors = ArrayDeque(debtors.sortedByDescending { it.second })
    val sortedCreditors = ArrayDeque(creditors.sortedByDescending { it.second })

    val transfers = mutableListOf<SettlementTransfer>()

    while (sortedDebtors.isNotEmpty() && sortedCreditors.isNotEmpty()) {
        val (debtorIdx, debtorCents) = sortedDebtors.removeFirst()
        val (creditorIdx, creditorCents) = sortedCreditors.removeFirst()

        val transferCents = minOf(debtorCents, creditorCents)
        val transferAmount = transferCents / 100.0

        transfers.add(
            SettlementTransfer(
                fromName = profileNames[debtorIdx],
                toName = profileNames[creditorIdx],
                amount = transferAmount,
            )
        )

        val remainingDebtor = debtorCents - transferCents
        val remainingCreditor = creditorCents - transferCents

        if (remainingDebtor > 0) sortedDebtors.addFirst(Pair(debtorIdx, remainingDebtor))
        if (remainingCreditor > 0) sortedCreditors.addFirst(Pair(creditorIdx, remainingCreditor))
    }

    return transfers
}
