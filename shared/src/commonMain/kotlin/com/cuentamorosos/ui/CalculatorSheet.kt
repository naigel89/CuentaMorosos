package com.cuentamorosos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuentamorosos.model.CalculationResult
import com.cuentamorosos.model.CalculationSnapshot
import com.cuentamorosos.model.EventExpenseItem
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.SettlementEngine
import com.cuentamorosos.model.SplitMode
import com.cuentamorosos.model.formatEuros
import com.cuentamorosos.model.parseEuroAmount
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

// ── CalculatorSheet ───────────────────────────────────────────────────────────

@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorSheet(
    event: EventItem,
    profiles: List<ProfileItem>,
    eventExpenses: List<EventExpenseItem>,
    onDismiss: () -> Unit,
    onApply: (modeId: String, CalculationResult) -> Unit,
    _deletedProfileIds: Set<String> = emptySet(),
    _priorSnapshot: CalculationSnapshot? = null,
    currentUserUid: String? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val expenseTotal = eventExpenses.sumOf { it.amountEuros }

    var totalText by remember(eventExpenses) {
        mutableStateOf(if (eventExpenses.isEmpty()) "" else expenseTotal.toString())
    }
    var selectedModeId by remember { mutableStateOf(SplitMode.REAL_CONSUMPTION.id) }
    var percentageInputs by remember(profiles.size) { mutableStateOf(defaultPercentageInputs(profiles.size)) }
    var exactAmountInputs by remember(profiles.size) { mutableStateOf(List(profiles.size) { "" }) }
    var partsInputs by remember(profiles.size) { mutableStateOf(List(profiles.size) { "1" }) }

    // Calculation state
    var isCalculating by remember { mutableStateOf(false) }
    var calculationResult by remember { mutableStateOf<CalculationResult?>(null) }
    var paidTransferIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }

    val totalValue = parseEuroAmount(totalText)
    val selectedMode = SplitMode.fromId(selectedModeId)

    val themeColors = MaterialTheme.colorScheme
    val shapes = NeoFintechShapes
    val typography = NeoFintechTypography()
    val monoFont = JetBrainsMonoFontFamily()

    fun runCalculation() {
        if (isCalculating) return
        isCalculating = true
        paidTransferIndices = emptySet()

        // Apply mode-specific weights to expenses for EXACT and PARTS modes
        val adjustedExpenses = when (selectedMode) {
            SplitMode.EXACT -> {
                val profileWeights = profiles.mapIndexedNotNull { index, profile ->
                    val amount = parseDecimalValue(exactAmountInputs[index])
                    if (amount != null) profile.id to amount else null
                }.toMap()
                eventExpenses.map { it.copy(splitMode = "EXACT", profileWeights = profileWeights) }
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

        calculationResult = SettlementEngine.calculateWithEdgeCases(
            event = event,
            expenses = adjustedExpenses,
            profileNameResolver = { id -> profiles.find { it.id == id }?.name ?: id },
        )

        isCalculating = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = themeColors.surfaceContainerLow,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
            // Title
            Text(
                text = "Calculadora automática",
                style = typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
            )

            Text(
                text = "Simula el reparto en tiempo real y confirma solo cuando el resultado te encaje.",
                style = typography.bodyMedium.copy(color = themeColors.onSurfaceVariant),
            )

            // Total input field (informational)
            OutlinedTextField(
                value = totalText,
                onValueChange = { totalText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Total del evento (€)") },
                supportingText = {
                    if (eventExpenses.isNotEmpty()) {
                        Text("Sugerido según ítems: ${formatEuros(expenseTotal)}")
                    }
                },
                singleLine = true,
                shape = shapes.md,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = themeColors.primary,
                    unfocusedBorderColor = themeColors.outline,
                ),
            )

            // Mode selector chip (preserved)
            ModeSelectorChip(
                selectedMode = selectedMode,
                onModeSelected = { mode -> selectedModeId = mode.id },
            )

            // Mode helper text + example
            Text(
                text = selectedMode.helperText,
                style = typography.bodySmall.copy(color = themeColors.onSurfaceVariant),
            )
            Text(
                text = "Ej.: ${selectedMode.exampleText}",
                style = typography.bodySmall.copy(color = themeColors.onSurfaceVariant),
            )

            // Parameter input for CUSTOM_PERCENTAGE
            if (selectedMode == SplitMode.CUSTOM_PERCENTAGE) {
                Text(
                    text = "Porcentaje por perfil",
                    style = typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
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
            }

            // Parameter input for EXACT mode
            if (selectedMode == SplitMode.EXACT) {
                Text(
                    text = "Importe exacto por perfil",
                    style = typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
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
                // EXACT sum validation feedback
                val exactSum = exactAmountInputs
                    .mapNotNull { parseDecimalValue(it) }
                    .sum()
                val exactSumValid = totalValue?.let { kotlin.math.abs(exactSum - it) <= 0.01 } ?: false
                if (exactAmountInputs.any { it.isNotBlank() }) {
                    Text(
                        text = if (exactSumValid) {
                            "Suma: ${formatEuros(exactSum)} ✓"
                        } else {
                            "Suma: ${formatEuros(exactSum)} — debe coincidir con ${formatEuros(totalValue ?: 0.0)} (±0,01 €)"
                        },
                        style = typography.bodySmall.copy(
                            color = if (exactSumValid) themeColors.primary else themeColors.error,
                        ),
                    )
                }
            }

            // Parameter input for PARTS mode
            if (selectedMode == SplitMode.PARTS) {
                Text(
                    text = "Partes por perfil",
                    style = typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                profiles.forEachIndexed { index, profile ->
                    ParameterInputRow(
                        profile = profile,
                        label = "Partes",
                        value = partsInputs[index],
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
                // PARTS sum validation feedback
                val partsSum = partsInputs
                    .mapNotNull { it.toIntOrNull() }
                    .sum()
                val partsSumValid = partsSum > 0
                Text(
                    text = "Total de partes: $partsSum",
                    style = typography.bodySmall.copy(
                        color = if (partsSumValid) themeColors.primary else themeColors.error,
                    ),
                )
            }

            // Available expenses list
            if (eventExpenses.isNotEmpty()) {
                Text(
                    text = "Ítems disponibles",
                    style = typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                eventExpenses.forEach { expense ->
                    val category = com.cuentamorosos.model.ExpenseCategory.fromId(expense.category)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "• ${expense.name} · ${category.label}",
                            style = typography.bodySmall.copy(color = themeColors.onSurfaceVariant),
                        )
                        Text(
                            text = formatEuros(expense.amountEuros),
                            style = typography.labelSmall.copy(
                                fontFamily = monoFont,
                                color = themeColors.primary,
                            ),
                        )
                    }
                }
            }

            // Calculate button
            val modeInputsValid = when (selectedMode) {
                SplitMode.EXACT -> {
                    val exactSum = exactAmountInputs
                        .mapNotNull { parseDecimalValue(it) }
                        .sum()
                    totalValue?.let { kotlin.math.abs(exactSum - it) <= 0.01 } == true
                }
                SplitMode.PARTS -> {
                    partsInputs
                        .mapNotNull { it.toIntOrNull() }
                        .sum() > 0
                }
                else -> true
            }
            Button(
                onClick = { runCalculation() },
                enabled = !isCalculating && eventExpenses.isNotEmpty() && modeInputsValid,
                modifier = Modifier.fillMaxWidth(),
                shape = shapes.md,
                colors = ButtonDefaults.buttonColors(
                    containerColor = themeColors.primary,
                    contentColor = themeColors.onPrimary,
                    disabledContainerColor = themeColors.surfaceContainerHigh,
                    disabledContentColor = themeColors.onSurfaceVariant,
                ),
            ) {
                if (isCalculating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = themeColors.onPrimary,
                    )
                    Text(
                        text = "Calculando...",
                        modifier = Modifier.padding(start = 8.dp),
                    )
                } else {
                    Text("Calcular")
                }
            }

            // Calculation results
            calculationResult?.let { result ->
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = themeColors.outlineVariant,
                )

                // Transfer list panel (sole result display)
                result.snapshot?.let { snapshot ->
                    val profileNameResolver: (String) -> String = { id ->
                        profiles.find { it.id == id }?.name ?: id
                    }
                    TransferListPanel(
                        snapshot = snapshot,
                        status = result.status,
                        profileNameResolver = profileNameResolver,
                        profiles = profiles,
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
            }

            // Null-snapshot error display: shows when calculation failed to produce a snapshot
            calculationResult?.takeIf { it.snapshot == null }?.let { result ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = themeColors.errorContainer,
                    ),
                    shape = shapes.md,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "❌ Error en el cálculo",
                            style = typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = themeColors.onErrorContainer,
                            ),
                        )
                        if (result.errors.isNotEmpty()) {
                            Text(
                                text = result.errors.joinToString("\n"),
                                style = typography.bodySmall.copy(color = themeColors.onErrorContainer),
                            )
                        }
                        result.status?.let { status ->
                            Text(
                                text = status.message,
                                style = typography.bodySmall.copy(
                                    color = themeColors.onErrorContainer,
                                    fontWeight = FontWeight.Medium,
                                ),
                            )
                        }
                    }
                }
            }

            // Bottom bar buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val canApply = calculationResult?.isSuccess == true && !isCalculating

                Button(
                    onClick = {
                        calculationResult?.let { result ->
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    onApply(selectedModeId, result)
                                }
                            }
                        }
                    },
                    enabled = canApply,
                    modifier = Modifier.weight(1f),
                    shape = shapes.md,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeColors.primary,
                        contentColor = themeColors.onPrimary,
                        disabledContainerColor = themeColors.surfaceContainerHigh,
                        disabledContentColor = themeColors.onSurfaceVariant,
                    ),
                ) {
                    Text("Aplicar cálculo")
                }
                OutlinedButton(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                onDismiss()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = shapes.md,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = themeColors.primary,
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, themeColors.outline),
                ) {
                    Text("Cancelar")
                }
            }
        }

        }
    }
}

// ── ParameterInputRow (extracted from EventDetailScreen) ──────────────────────

@Composable
fun ParameterInputRow(
    profile: ProfileItem,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    val themeColors = MaterialTheme.colorScheme
    val shapes = NeoFintechShapes
    val typography = NeoFintechTypography()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            ProfileAvatar(
                name = profile.name,
                emoji = profile.icon,
                photoUrl = profile.photoUrl,
                size = 24.dp,
            )
            Text(
                text = profile.name,
                style = typography.bodyMedium,
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            label = { Text(label) },
            singleLine = true,
            shape = shapes.md,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = themeColors.primary,
                unfocusedBorderColor = themeColors.outline,
            ),
        )
    }
}
