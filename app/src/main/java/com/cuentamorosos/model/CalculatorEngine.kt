package com.cuentamorosos.model

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

enum class SplitMode(
    val id: String,
    val label: String,
    val helperText: String,
) {
    SIMPLE_AVG(
        id = "simple_avg",
        label = "Media simple",
        helperText = "Divide el total a partes iguales."
    ),
    REAL_CONSUMPTION(
        id = "real_consumption",
        label = "Consumo real",
        helperText = "Cada ítem se reparte solo entre los perfiles asignados."
    ),
    CUSTOM_PERCENTAGE(
        id = "custom_percentage",
        label = "% personalizado",
        helperText = "Exige que la suma sea exactamente 100%."
    ),
    BY_CATEGORY(
        id = "by_category",
        label = "Por categoría",
        helperText = "Cada categoría aplica una regla distinta al gasto."
    ),
    BY_WEIGHT(
        id = "by_weight",
        label = "Por peso",
        helperText = "Reparte proporcionalmente a un factor numérico."
    ),
    BY_INCOME(
        id = "by_income",
        label = "Capacidad de pago",
        helperText = "Reparte proporcionalmente al valor económico introducido."
    ),
    BASE_PLUS_SURPLUS(
        id = "base_plus_surplus",
        label = "Base + excedente",
        helperText = "Aplica una cuota base común y reparte el resto por factores."
    ),
    BY_ATTENDANCE(
        id = "by_attendance",
        label = "Por asistencia",
        helperText = "Reparte según días o sesiones asistidas."
    ),
    MIXED(
        id = "mixed",
        label = "Mixto",
        helperText = "Combina categorías por ítem con reparto adicional del remanente."
    );

    companion object {
        fun fromId(id: String): SplitMode = entries.firstOrNull { it.id == id } ?: SIMPLE_AVG
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
    baseAmount: Double? = null,
): CalculationPreview {
    if (total < 0.0) {
        return CalculationPreview(validationMessage = "El total no puede ser negativo.")
    }

    return when (mode) {
        SplitMode.SIMPLE_AVG -> CalculationPreview(
            amounts = splitAmountEvenly(total = total, participants = inputs.size),
            summary = "Media simple entre ${inputs.size} perfiles",
            calculatedTotal = total,
        )

        SplitMode.REAL_CONSUMPTION -> buildExpenseDrivenPreview(
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
                    validationMessage = "La suma actual es ${String.format("%.2f", sum)}%. Debe ser 100%."
                )
                else -> CalculationPreview(
                    amounts = distributeProportionally(total = total, factors = inputs),
                    summary = "Porcentajes personalizados: ${inputs.joinToString { String.format("%.0f%%", it) }}",
                    calculatedTotal = total,
                )
            }
        }

        SplitMode.BY_CATEGORY -> buildExpenseDrivenPreview(
            mode = mode,
            participantIds = participantIds,
            expenses = expenses,
        )

        SplitMode.BY_WEIGHT -> when {
            inputs.isEmpty() -> CalculationPreview(validationMessage = "Añade al menos un perfil al evento.")
            inputs.any { it <= 0.0 } -> CalculationPreview(validationMessage = "Todos los factores deben ser mayores que 0.")
            else -> CalculationPreview(
                amounts = distributeProportionally(total = total, factors = inputs),
                summary = "Reparto por peso/factor: ${inputs.joinToString()}",
                calculatedTotal = total,
            )
        }

        SplitMode.BY_INCOME -> when {
            inputs.isEmpty() -> CalculationPreview(validationMessage = "Añade al menos un perfil al evento.")
            inputs.any { it <= 0.0 } -> CalculationPreview(validationMessage = "Los valores de capacidad de pago deben ser mayores que 0.")
            else -> CalculationPreview(
                amounts = distributeProportionally(total = total, factors = inputs),
                summary = "Capacidad de pago: ${inputs.joinToString()}",
                calculatedTotal = total,
            )
        }

        SplitMode.BASE_PLUS_SURPLUS -> when {
            inputs.isEmpty() -> CalculationPreview(validationMessage = "Añade al menos un perfil al evento.")
            inputs.any { it <= 0.0 } -> CalculationPreview(validationMessage = "Los factores del excedente deben ser mayores que 0.")
            baseAmount == null || baseAmount < 0.0 -> CalculationPreview(validationMessage = "La cuota base debe ser un número válido y no negativo.")
            else -> buildBasePlusSurplusPreview(
                total = total,
                factors = inputs,
                baseAmount = baseAmount,
            )
        }

        SplitMode.BY_ATTENDANCE -> when {
            inputs.isEmpty() -> CalculationPreview(validationMessage = "Añade al menos un perfil al evento.")
            inputs.any { it <= 0.0 } -> CalculationPreview(validationMessage = "Las asistencias deben ser mayores que 0.")
            else -> CalculationPreview(
                amounts = distributeProportionally(total = total, factors = inputs),
                summary = "Reparto por asistencia: ${inputs.joinToString()} sesiones",
                calculatedTotal = total,
            )
        }

        SplitMode.MIXED -> buildMixedPreview(
            total = total,
            participantIds = participantIds,
            expenses = expenses,
            remainderFactors = inputs,
        )
    }
}

private fun buildBasePlusSurplusPreview(
    total: Double,
    factors: List<Double>,
    baseAmount: Double,
): CalculationPreview {
    val participantCount = factors.size
    val totalCents = (total * 100).roundToInt()
    val baseCentsPerParticipant = (baseAmount * 100).roundToInt()
    val baseTotalCents = baseCentsPerParticipant * participantCount

    if (baseTotalCents > totalCents) {
        return CalculationPreview(
            validationMessage = "La cuota base supera el total del evento para ${participantCount} perfiles."
        )
    }

    val surplusCents = totalCents - baseTotalCents
    val extraCents = distributeProportionallyCents(totalCents = surplusCents, factors = factors)
    val amounts = List(participantCount) { index ->
        (baseCentsPerParticipant + extraCents.getOrElse(index) { 0 }) / 100.0
    }

    return CalculationPreview(
        amounts = amounts,
        summary = "Base ${formatEuros(baseAmount)} + excedente repartido por factores",
        calculatedTotal = total,
    )
}

private fun buildMixedPreview(
    total: Double,
    participantIds: List<String>,
    expenses: List<EventExpenseItem>,
    remainderFactors: List<Double>,
): CalculationPreview {
    val expensePreview = buildExpenseDrivenPreview(
        mode = SplitMode.BY_CATEGORY,
        participantIds = participantIds,
        expenses = expenses,
    )
    if (expensePreview.validationMessage != null) return expensePreview

    val remainder = total - expensePreview.calculatedTotal
    if (remainder < -0.01) {
        return CalculationPreview(
            validationMessage = "El total introducido no puede ser menor que la suma de los ítems del evento."
        )
    }

    val normalizedRemainder = if (remainder < 0.0) 0.0 else remainder
    val safeFactors = if (remainderFactors.isEmpty() || remainderFactors.any { it <= 0.0 }) {
        List(participantIds.size) { 1.0 }
    } else {
        remainderFactors
    }
    val remainderDistribution = distributeProportionally(total = normalizedRemainder, factors = safeFactors)
    val combinedAmounts = expensePreview.amounts.mapIndexed { index, amount ->
        val combinedCents = ((amount + remainderDistribution.getOrElse(index) { 0.0 }) * 100).roundToInt()
        combinedCents / 100.0
    }

    return CalculationPreview(
        amounts = combinedAmounts,
        summary = "Mixto: categorías por ítem + remanente repartido",
        calculatedTotal = total,
    )
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
) {
    if (recipientIndexes.isEmpty()) return

    val amountCents = (amount * 100).roundToInt()
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
