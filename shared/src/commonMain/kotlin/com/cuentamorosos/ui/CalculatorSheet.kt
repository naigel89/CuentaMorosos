package com.cuentamorosos.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cuentamorosos.model.CalculationResult
import com.cuentamorosos.model.CalculationSnapshot
import com.cuentamorosos.model.CalculationStatus
import com.cuentamorosos.model.EventExpenseItem
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.ExpenseCategory
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.SettlementEngine
import com.cuentamorosos.model.SplitMode
import com.cuentamorosos.model.formatEuros
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

// ── Utility functions (moved from EventDetailScreen) ──────────────────────────

private fun parseDecimalValue(value: String): Double? = value
    .trim()
    .replace(',', '.')
    .toDoubleOrNull()

private fun defaultPercentageInputs(count: Int): List<String> {
    if (count <= 0) return emptyList()
    val base = 100 / count
    val remainder = 100 % count

    return List(count) { index ->
        (base + if (index == 0) remainder else 0).toString()
    }
}

private fun List<String>.updateAt(index: Int, value: String): List<String> = mapIndexed { currentIndex, currentValue ->
    if (currentIndex == index) value else currentValue
}

/**
 * Reparte los importes exactos (definidos A NIVEL DE EVENTO) entre un gasto
 * concreto, proporcionalmente y en céntimos con método del resto mayor, de modo
 * que los pesos de cada gasto suman EXACTAMENTE el importe de ese gasto.
 *
 * El motor valida cada gasto por separado (`SplitCalculator.calculateExact`
 * exige que los pesos sumen el importe del gasto ±1 céntimo): pasarle los
 * importes del evento tal cual reventaba con un `require` en cuanto el evento
 * tenía más de un gasto — justo al cuadrar el total, que es lo que pide la UI.
 *
 * Los perfiles que quedan con 0 céntimos se excluyen del mapa: el motor exige
 * un mínimo de 0,01 por deudor listado.
 */
internal fun scaleExactWeightsForExpense(
    exactAmounts: Map<String, Double>,
    expenseAmountEuros: Double,
    eventTotalEuros: Double,
): Map<String, Double> {
    if (exactAmounts.isEmpty()) return emptyMap()
    val expenseCents = (expenseAmountEuros * 100).roundToInt()
    val totalCents = (eventTotalEuros * 100).roundToInt()
    if (expenseCents <= 0 || totalCents <= 0) return emptyMap()

    // Orden estable por id: el reparto del resto es determinista y reproducible.
    data class Alloc(val id: String, var cents: Long, val fraction: Long)

    val allocations = exactAmounts.entries.sortedBy { it.key }.map { (id, euros) ->
        val exactCents = (euros * 100).roundToInt().toLong()
        val raw = exactCents * expenseCents
        Alloc(id = id, cents = raw / totalCents, fraction = raw % totalCents)
    }

    var remainder = expenseCents - allocations.sumOf { it.cents }
    if (remainder > 0) {
        // Faltan céntimos: al mayor resto primero (desempate por id).
        val order = allocations.sortedWith(compareByDescending<Alloc> { it.fraction }.thenBy { it.id })
        var i = 0
        while (remainder > 0) {
            order[i % order.size].cents += 1
            remainder--
            i++
        }
    } else {
        // Sobran (la suma tecleada puede pasarse 1 céntimo): se quitan de quien más tiene.
        while (remainder < 0) {
            val richest = allocations.filter { it.cents > 0 }.maxByOrNull { it.cents } ?: break
            richest.cents -= 1
            remainder++
        }
    }

    return allocations.filter { it.cents > 0 }.associate { it.id to it.cents / 100.0 }
}

// ── CalculatorSheet ───────────────────────────────────────────────────────────

/**
 * Simulación en vivo del reparto: no hay botón "Calcular" — el resultado se
 * recalcula con cada cambio de modo o de parámetros ([SettlementEngine] es puro
 * y síncrono, así que puede correr en composición sin coste apreciable).
 * Los botones Aplicar/Cancelar viven fuera del scroll, siempre visibles.
 */
@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorSheet(
    event: EventItem,
    profiles: List<ProfileItem>,
    eventExpenses: List<EventExpenseItem>,
    onDismiss: () -> Unit,
    onApply: (modeId: String, CalculationResult, paidTransferIndices: List<Int>) -> Unit,
    _deletedProfileIds: Set<String> = emptySet(),
    _priorSnapshot: CalculationSnapshot? = null,
    currentUserUid: String? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val expenseTotal = eventExpenses.sumOf { it.amountEuros }

    var selectedModeId by remember { mutableStateOf(SplitMode.REAL_CONSUMPTION.id) }
    var percentageInputs by remember(profiles.size) { mutableStateOf(defaultPercentageInputs(profiles.size)) }
    var exactAmountInputs by remember(profiles.size) { mutableStateOf(List(profiles.size) { "" }) }
    var partsInputs by remember(profiles.size) { mutableStateOf(List(profiles.size) { "1" }) }
    var paidTransferIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }

    val selectedMode = SplitMode.fromId(selectedModeId)

    val colors = LocalNeoFintechColors.current
    val themeColors = MaterialTheme.colorScheme
    val shapes = NeoFintechShapes
    val typography = NeoFintechTypography()
    val monoFont = JetBrainsMonoFontFamily()

    // ── Validación por modo ───────────────────────────────────────────────────
    val exactSum = exactAmountInputs.mapNotNull { parseDecimalValue(it) }.sum()
    val percentageSum = percentageInputs.mapNotNull { parseDecimalValue(it) }.sum()
    val partsSum = partsInputs.mapNotNull { it.toIntOrNull() }.sum()

    val modeInputsValid = when (selectedMode) {
        SplitMode.EXACT -> abs(exactSum - expenseTotal) <= 0.01
        SplitMode.PARTS -> partsSum > 0
        else -> true
    }

    // ── Cálculo en vivo ───────────────────────────────────────────────────────
    val calculationResult: CalculationResult? = remember(
        eventExpenses, profiles, selectedModeId,
        percentageInputs, exactAmountInputs, partsInputs,
    ) {
        if (eventExpenses.isEmpty()) return@remember null

        val valid = when (selectedMode) {
            SplitMode.EXACT -> abs(
                exactAmountInputs.mapNotNull { parseDecimalValue(it) }.sum() - expenseTotal
            ) <= 0.01
            SplitMode.PARTS -> partsInputs.mapNotNull { it.toIntOrNull() }.sum() > 0
            else -> true
        }
        if (!valid) return@remember null

        // Apply mode-specific weights to expenses for EXACT, PARTS and % modes
        val adjustedExpenses = when (selectedMode) {
            SplitMode.EXACT -> {
                val exactByProfile = profiles.mapIndexedNotNull { index, profile ->
                    val amount = parseDecimalValue(exactAmountInputs[index])
                    if (amount != null) profile.id to amount else null
                }.toMap()
                eventExpenses.map { expense ->
                    expense.copy(
                        splitMode = "EXACT",
                        profileWeights = scaleExactWeightsForExpense(
                            exactAmounts = exactByProfile,
                            expenseAmountEuros = expense.amountEuros,
                            eventTotalEuros = expenseTotal,
                        ),
                    )
                }
            }
            SplitMode.PARTS -> {
                val profileWeights = profiles.mapIndexedNotNull { index, profile ->
                    val parts = partsInputs[index].toIntOrNull()
                    if (parts != null && parts > 0) profile.id to parts.toDouble() else null
                }.toMap()
                eventExpenses.map { it.copy(splitMode = "PARTS", profileWeights = profileWeights) }
            }
            SplitMode.CUSTOM_PERCENTAGE -> {
                val profileWeights = profiles.mapIndexedNotNull { index, profile ->
                    val pct = parseDecimalValue(percentageInputs[index])
                    if (pct != null) profile.id to pct else null
                }.toMap()
                eventExpenses.map { it.copy(splitMode = "CUSTOM_PERCENTAGE", profileWeights = profileWeights) }
            }
            else -> eventExpenses
        }

        // El motor valida con require() y su try no captura: cualquier entrada
        // que se le escape a la validación de la UI debe acabar en el banner de
        // error, nunca tumbando la app en plena composición.
        runCatching {
            SettlementEngine.calculateWithEdgeCases(
                event = event,
                expenses = adjustedExpenses,
                profileNameResolver = { id -> profiles.find { it.id == id }?.name ?: id },
            )
        }.getOrElse { e ->
            val message = e.message ?: "No se pudo calcular el reparto"
            CalculationResult(
                errors = listOf(message),
                status = CalculationStatus.Error(message),
            )
        }
    }

    // Un resultado nuevo invalida los pagos marcados sobre el anterior.
    LaunchedEffect(calculationResult) {
        paidTransferIndices = emptySet()
    }

    fun hideAnd(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) action()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = themeColors.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ── Contenido desplazable ────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .animateContentSize(NeoFintechMotion.resize),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Calculadora",
                        style = typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = themeColors.onSurface,
                    )
                    Text(
                        text = "El reparto se recalcula al instante con cada cambio.",
                        style = typography.bodyMedium.copy(color = themeColors.onSurfaceVariant),
                    )
                }

                // Total del evento: dato, no input — sale de los ítems.
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = shapes.lg,
                    color = themeColors.surface,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        SectionLabel(text = "Total del evento")
                        Row(verticalAlignment = Alignment.Bottom) {
                            AnimatedAmount(
                                value = expenseTotal,
                                style = typography.displayLarge.copy(
                                    fontFamily = monoFont,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = themeColors.onSurface,
                                suffix = "",
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "€",
                                style = typography.titleLarge,
                                color = themeColors.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                        Text(
                            text = "${eventExpenses.size} ítems · ${profiles.size} participantes",
                            style = typography.bodySmall.copy(
                                color = themeColors.onSurfaceVariant.copy(alpha = 0.75f),
                            ),
                        )
                    }
                }

                ModeSelectorChip(
                    selectedMode = selectedMode,
                    onModeSelected = { mode -> selectedModeId = mode.id },
                )

                Text(
                    text = "${selectedMode.helperText} Ej.: ${selectedMode.exampleText}",
                    style = typography.bodySmall.copy(color = themeColors.onSurfaceVariant),
                )

                // ── Parámetros por modo ──────────────────────────────────────
                Crossfade(
                    targetState = selectedMode,
                    animationSpec = tween(NeoFintechMotion.MEDIUM_MS, easing = NeoFintechMotion.standard),
                    label = "modeParams",
                ) { mode ->
                    when (mode) {
                        SplitMode.CUSTOM_PERCENTAGE -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionLabel(text = "Porcentaje por perfil")
                            profiles.forEachIndexed { index, profile ->
                                ParameterInputRow(
                                    profile = profile,
                                    label = "%",
                                    value = percentageInputs[index],
                                    onValueChange = { value ->
                                        percentageInputs = percentageInputs.updateAt(index, value)
                                    },
                                )
                            }
                            SumValidationBar(
                                sum = percentageSum,
                                target = 100.0,
                                formatValue = { v -> "${formatAmount(v, suffix = "", decimals = 0)} %" },
                            )
                        }

                        SplitMode.EXACT -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionLabel(text = "Importe exacto por perfil")
                            profiles.forEachIndexed { index, profile ->
                                ParameterInputRow(
                                    profile = profile,
                                    label = "€",
                                    value = exactAmountInputs[index],
                                    onValueChange = { value ->
                                        exactAmountInputs = exactAmountInputs.updateAt(index, value)
                                    },
                                )
                            }
                            SumValidationBar(
                                sum = exactSum,
                                target = expenseTotal,
                                formatValue = { v -> formatEuros(v) },
                            )
                        }

                        SplitMode.PARTS -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionLabel(text = "Partes por perfil")
                            profiles.forEachIndexed { index, profile ->
                                ParameterInputRow(
                                    profile = profile,
                                    label = "Partes",
                                    value = partsInputs[index],
                                    keyboardType = KeyboardType.Number,
                                    onValueChange = { value ->
                                        val filtered = value.filter { it.isDigit() }
                                        val intValue = filtered.toIntOrNull()
                                        val clamped = when {
                                            intValue == null -> ""
                                            intValue < 1 -> "1"
                                            intValue > 100 -> "100"
                                            else -> filtered
                                        }
                                        partsInputs = partsInputs.updateAt(index, clamped)
                                    },
                                )
                            }
                            Text(
                                text = "Total de partes: $partsSum",
                                style = typography.labelSmall.copy(
                                    fontFamily = monoFont,
                                    color = if (partsSum > 0) colors.primaryContainer else colors.error,
                                ),
                            )
                        }

                        else -> Spacer(Modifier.height(0.dp))
                    }
                }

                // ── Ítems del evento ─────────────────────────────────────────
                if (eventExpenses.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = shapes.lg,
                        color = themeColors.surfaceContainerLowest,
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(9.dp),
                        ) {
                            SectionLabel(text = "Ítems del evento")
                            eventExpenses.forEach { expense ->
                                val category = ExpenseCategory.fromId(expense.category)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "${expense.name} · ${category.label}",
                                        style = typography.bodySmall.copy(color = themeColors.onSurface),
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        text = formatEuros(expense.amountEuros),
                                        style = typography.labelSmall.copy(
                                            fontFamily = monoFont,
                                            color = themeColors.onSurfaceVariant,
                                        ),
                                    )
                                }
                            }
                            HorizontalDivider(color = themeColors.outlineVariant.copy(alpha = 0.4f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "${eventExpenses.size} ítems",
                                    style = typography.bodySmall.copy(color = themeColors.onSurfaceVariant),
                                )
                                Text(
                                    text = formatEuros(expenseTotal),
                                    style = typography.labelMedium.copy(
                                        fontFamily = monoFont,
                                        fontWeight = FontWeight.Bold,
                                        color = themeColors.onSurface,
                                    ),
                                )
                            }
                        }
                    }
                }

                // ── Resultado en vivo ────────────────────────────────────────
                calculationResult?.snapshot?.let { snapshot ->
                    val profileNameResolver: (String) -> String = { id ->
                        profiles.find { it.id == id }?.name ?: id
                    }
                    TransferListPanel(
                        snapshot = snapshot,
                        status = calculationResult.status,
                        profileNameResolver = profileNameResolver,
                        profiles = profiles,
                        showTotalHero = false,
                        paidTransferIndices = paidTransferIndices,
                        onTogglePaid = { index ->
                            paidTransferIndices = if (index in paidTransferIndices) {
                                paidTransferIndices - index
                            } else {
                                paidTransferIndices + index
                            }
                        },
                        currentProfileId = currentUserUid,
                    )
                }

                // Error sin snapshot: banner de estado, no tarjeta con emoji.
                calculationResult?.takeIf { it.snapshot == null }?.let { result ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = shapes.lg,
                        color = colors.error.copy(alpha = 0.08f),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 5.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(colors.error),
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "No se pudo calcular el reparto",
                                    style = typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.error,
                                    ),
                                )
                                if (result.errors.isNotEmpty()) {
                                    Text(
                                        text = result.errors.joinToString("\n"),
                                        style = typography.bodySmall.copy(color = colors.error),
                                    )
                                }
                                result.status?.let { status ->
                                    Text(
                                        text = status.message,
                                        style = typography.bodySmall.copy(
                                            color = colors.error,
                                            fontWeight = FontWeight.Medium,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))
            }

            // ── Footer fijo: Aplicar / Cancelar ──────────────────────────────
            HorizontalDivider(color = themeColors.outlineVariant.copy(alpha = 0.5f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val canApply = calculationResult?.isSuccess == true

                Button(
                    onClick = {
                        calculationResult?.let { result ->
                            hideAnd { onApply(selectedModeId, result, paidTransferIndices.toList()) }
                        }
                    },
                    enabled = canApply,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    shape = shapes.md,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeColors.primary,
                        contentColor = colors.onPrimaryContainer,
                        disabledContainerColor = themeColors.surfaceContainerHigh,
                        disabledContentColor = themeColors.onSurfaceVariant,
                    ),
                ) {
                    Text("Aplicar cálculo", fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = { hideAnd(onDismiss) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    shape = shapes.md,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = themeColors.onSurfaceVariant,
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, themeColors.outlineVariant),
                ) {
                    Text("Cancelar")
                }
            }
        }
    }
}

// ── Barra de validación de suma ───────────────────────────────────────────────

/**
 * La validación como barra que se llena hacia el objetivo, no como frase:
 * verde al cuadrar, ámbar mientras falta, rojo si se pasa. El relleno y el
 * color animan con las curvas del sistema.
 */
@Composable
private fun SumValidationBar(
    sum: Double,
    target: Double,
    formatValue: (Double) -> String,
) {
    val colors = LocalNeoFintechColors.current
    val typography = NeoFintechTypography()
    val monoFont = JetBrainsMonoFontFamily()

    val matches = abs(sum - target) <= 0.01
    val over = sum > target + 0.01
    val accentTarget = when {
        matches -> colors.primaryContainer
        over -> colors.error
        else -> colors.warning
    }
    val accent by animateColorAsState(
        targetValue = accentTarget,
        animationSpec = NeoFintechMotion.color,
        label = "sumValidationAccent",
    )
    val fraction = if (target > 0) (sum / target).toFloat().coerceIn(0f, 1f) else 0f

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${formatValue(sum)} de ${formatValue(target)}",
                style = typography.labelSmall.copy(fontFamily = monoFont),
                color = accent,
            )
            if (matches) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "Cuadra",
                        style = typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = accent,
                    )
                }
            } else {
                Text(
                    text = if (over) "Sobra ${formatValue(sum - target)}" else "Faltan ${formatValue(target - sum)}",
                    style = typography.bodySmall,
                    color = accent,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(NeoFintechShapes.full)
                .background(LocalNeoFintechColors.current.surfaceContainerHigh),
        ) {
            AnimatedProportionBar(
                proportion = fraction,
                modifier = Modifier.height(4.dp),
                color = accent,
                shape = NeoFintechShapes.full,
            )
        }
    }
}

// ── ParameterInputRow (extracted from EventDetailScreen) ──────────────────────

/**
 * Fila compacta: el campo ya no ocupa media pantalla — ancho fijo, importe en
 * mono alineado a la derecha y la unidad como sufijo dentro del campo.
 */
@Composable
fun ParameterInputRow(
    profile: ProfileItem,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Decimal,
) {
    val themeColors = MaterialTheme.colorScheme
    val shapes = NeoFintechShapes
    val typography = NeoFintechTypography()
    val monoFont = JetBrainsMonoFontFamily()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileAvatar(
            name = profile.name,
            photoUrl = profile.photoUrl,
            size = 28.dp,
        )
        Text(
            text = profile.name,
            style = typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = themeColors.onSurface,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.width(118.dp),
            singleLine = true,
            shape = shapes.md,
            textStyle = LocalTextStyle.current.copy(
                fontFamily = monoFont,
                textAlign = TextAlign.End,
            ),
            suffix = {
                Text(
                    text = label,
                    style = typography.bodySmall,
                    color = themeColors.onSurfaceVariant,
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = themeColors.primary,
                unfocusedBorderColor = themeColors.outlineVariant,
                cursorColor = themeColors.primary,
            ),
        )
    }
}
