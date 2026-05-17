package com.cuentamorosos.model

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Modos de reparto disponibles en la calculadora de CuentaMorosos.
 */
enum class SplitMode(
    val id: String,
    val label: String,
    val helperText: String,
    val exampleText: String,
) {
    REAL_CONSUMPTION(
        id = "real_consumption",
        label = "Consumo real",
        helperText = "Cada ítem se reparte solo entre los perfiles asignados.",
        exampleText = "Alice pidió un café (1,50 €) y Bob una tarta (3,00 €): cada uno paga lo suyo.",
    ),
    SIMPLE_AVG(
        id = "simple_avg",
        label = "Media simple",
        helperText = "Divide el total a partes iguales entre todos.",
        exampleText = "Una cena de 60 € entre 3 personas → 20 € cada uno.",
    ),
    BY_CATEGORY(
        id = "by_category",
        label = "Por categoría",
        helperText = "Cada ítem usa su categoría: compartido, solo seleccionados o cargo individual.",
        exampleText = "La botella de vino (compartida, 12 €) se divide entre todos; el taxi de Ana (personal, 8 €) lo paga solo ella.",
    ),
    CUSTOM_PERCENTAGE(
        id = "custom_percentage",
        label = "% personalizado",
        helperText = "Asigna un porcentaje a cada perfil. La suma debe ser 100 %.",
        exampleText = "Un piso entre 2 personas: uno paga el 60 % y el otro el 40 % del alquiler.",
    ),
    EXACT(
        id = "exact",
        label = "Importe exacto",
        helperText = "Asigna un importe exacto a cada perfil. La suma debe coincidir con el total.",
        exampleText = "Ana paga 30 €, Bob 45,50 € y Charlie 24,50 €: cada uno sabe cuánto debe.",
    ),
    PARTS(
        id = "parts",
        label = "Por partes",
        helperText = "Asigna partes enteras (1-100) a cada perfil. El total se reparte proporcionalmente.",
        exampleText = "Un asado entre 3: uno pone 3 partes, otro 2 y otro 1 → se divide en 6 partes.",
    );

    companion object {
        fun fromId(id: String): SplitMode = entries.firstOrNull { it.id == id } ?: REAL_CONSUMPTION
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
    profileWeights: Map<String, Double> = emptyMap(),
    payerContributions: Map<String, Double> = emptyMap(),
): CalculationPreview {
    if (total < 0.0) {
        return CalculationPreview(validationMessage = "El total no puede ser negativo.")
    }

    return when (mode) {
        SplitMode.REAL_CONSUMPTION -> buildExpenseDrivenPreview(
            mode = mode,
            participantIds = participantIds,
            expenses = expenses,
        )
        SplitMode.SIMPLE_AVG -> {
            val ids = if (participantIds.isNotEmpty()) participantIds else inputs.mapIndexed { i, _ -> "p$i" }
            val amounts = SplitCalculator.calculateEqual(total = total, debtorIds = ids)
            CalculationPreview(
                amounts = amounts.values.toList(),
                summary = "Media simple entre ${ids.size} perfiles",
                calculatedTotal = total,
            )
        }
        SplitMode.BY_CATEGORY -> buildExpenseDrivenPreview(
            mode = mode,
            participantIds = participantIds,
            expenses = expenses,
        )
        SplitMode.CUSTOM_PERCENTAGE -> {
            val sum = inputs.sum()
            when {
                inputs.isEmpty() -> CalculationPreview(validationMessage = "Añade al menos un perfil al evento.")
                inputs.any { it < 0.0 } -> CalculationPreview(validationMessage = "Los porcentajes no pueden ser negativos.")
                abs(sum - 100.0) > 0.01 -> CalculationPreview(
                    validationMessage = "La suma actual es ${formatTwoDecimals(sum)}%. Debe ser 100%."
                )
                else -> {
                    val ids = if (participantIds.isNotEmpty()) participantIds else inputs.mapIndexed { i, _ -> "p$i" }
                    val percentages = ids.zip(inputs).toMap()
                    val amounts = SplitCalculator.calculatePercentage(total = total, percentages = percentages)
                    CalculationPreview(
                        amounts = amounts.values.toList(),
                        summary = "Porcentajes personalizados: ${inputs.joinToString { "${formatZeroDecimals(it)}%" }}",
                        calculatedTotal = total,
                    )
                }
            }
        }
        SplitMode.EXACT -> {
            val amounts = if (payerContributions.isNotEmpty()) {
                payerContributions
            } else {
                val ids = if (participantIds.isNotEmpty()) participantIds else inputs.mapIndexed { i, _ -> "p$i" }
                ids.zip(inputs).toMap()
            }
            if (amounts.isEmpty()) {
                CalculationPreview(validationMessage = "Añade al menos un perfil al evento.")
            } else {
                runCatching {
                    val result = SplitCalculator.calculateExact(total = total, amounts = amounts)
                    CalculationPreview(
                        amounts = result.values.toList(),
                        summary = "Importes exactos: ${result.size} perfiles",
                        calculatedTotal = total,
                    )
                }.getOrElse {
                    CalculationPreview(validationMessage = it.message ?: "Error en importes exactos")
                }
            }
        }
        SplitMode.PARTS -> {
            val parts = if (profileWeights.isNotEmpty()) {
                profileWeights.mapValues { (_, v) -> v.toInt() }
            } else {
                val ids = if (participantIds.isNotEmpty()) participantIds else inputs.mapIndexed { i, _ -> "p$i" }
                ids.zip(inputs.map { it.toInt() }).toMap()
            }
            if (parts.isEmpty()) {
                CalculationPreview(validationMessage = "Añade al menos un perfil al evento.")
            } else {
                runCatching {
                    val result = SplitCalculator.calculateParts(total = total, parts = parts)
                    CalculationPreview(
                        amounts = result.values.toList(),
                        summary = "Por partes: ${parts.values.sum()} partes entre ${result.size} perfiles",
                        calculatedTotal = total,
                    )
                }.getOrElse {
                    CalculationPreview(validationMessage = it.message ?: "Error en reparto por partes")
                }
            }
        }
    }
}

private fun formatTwoDecimals(value: Double): String {
    val intPart = value.toLong()
    val decPart = ((value - intPart) * 100).roundToInt().let { if (it < 0) -it else it }
    return "$intPart.${if (decPart < 10) "0$decPart" else "$decPart"}"
}

private fun formatZeroDecimals(value: Double): String = value.roundToInt().toString()

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
            SplitMode.BY_CATEGORY -> {
                val category = ExpenseCategory.fromId(expense.category)
                // SHARED divides among all; all other categories split among assigned profiles
                if (category == ExpenseCategory.SHARED) {
                    participantIds.indices.toList()
                } else {
                    indexesForIds(
                        participantIds = participantIds,
                        assignedIds = expense.assignedProfileIds,
                    )
                }
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

    if (weights.isNotEmpty()) {
        val weightedRecipients = recipientIndexes.mapNotNull { index ->
            val participantId = participantIds.getOrNull(index) ?: return@mapNotNull null
            val weight = weights[participantId] ?: return@mapNotNull null
            if (weight > 0.0) index to weight else null
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
// Liquidación mínima de transferencias
// ---------------------------------------------------------------------------

data class SettlementTransfer(
    val fromProfileId: String,
    val toProfileId: String,
    val amount: Double,
)

fun buildSettlementTransfers(
    profileIds: List<String>,
    amounts: List<Double>,
): List<SettlementTransfer> {
    if (profileIds.size != amounts.size || profileIds.isEmpty()) return emptyList()

    val balanceCents = amounts.map { (it * 100).roundToInt() }.toMutableList()

    val debtors = ArrayDeque<Pair<Int, Int>>()
    val creditors = ArrayDeque<Pair<Int, Int>>()

    balanceCents.forEachIndexed { index, cents ->
        when {
            cents < 0 -> creditors.addLast(Pair(index, -cents))
            cents > 0 -> debtors.addLast(Pair(index, cents))
        }
    }

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
                fromProfileId = profileIds[debtorIdx],
                toProfileId = profileIds[creditorIdx],
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
