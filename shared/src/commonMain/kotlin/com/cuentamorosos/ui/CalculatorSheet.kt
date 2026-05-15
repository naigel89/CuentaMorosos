package com.cuentamorosos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetValue
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
import com.cuentamorosos.model.CalculationApplication
import com.cuentamorosos.model.CalculationPreview
import com.cuentamorosos.model.EventExpenseItem
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.SplitMode
import com.cuentamorosos.model.buildCalculationPreview
import com.cuentamorosos.model.buildSettlementTransfers
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorSheet(
    profiles: List<ProfileItem>,
    eventExpenses: List<EventExpenseItem>,
    onDismiss: () -> Unit,
    onApply: (CalculationApplication) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val expenseTotal = eventExpenses.sumOf { it.amountEuros }

    var totalText by remember(eventExpenses) {
        mutableStateOf(if (eventExpenses.isEmpty()) "" else expenseTotal.toString())
    }
    var selectedModeId by remember { mutableStateOf(SplitMode.REAL_CONSUMPTION.id) }
    var percentageInputs by remember(profiles.size) { mutableStateOf(defaultPercentageInputs(profiles.size)) }
    var showComparison by remember { mutableStateOf(false) }

    val totalValue = parseEuroAmount(totalText)
    val selectedMode = SplitMode.fromId(selectedModeId)

    fun previewFor(mode: SplitMode): CalculationPreview {
        val rawInputs = when (mode) {
            SplitMode.REAL_CONSUMPTION -> List(profiles.size) { 1.0 }
            SplitMode.SIMPLE_AVG -> List(profiles.size) { 1.0 }
            SplitMode.BY_CATEGORY -> List(profiles.size) { 1.0 }
            SplitMode.CUSTOM_PERCENTAGE -> percentageInputs.map { parseDecimalValue(it) ?: Double.NaN }
        }

        if (rawInputs.any { it.isNaN() }) {
            return CalculationPreview(validationMessage = "Revisa los valores introducidos para este reparto.")
        }

        return buildCalculationPreview(
            total = totalValue ?: expenseTotal,
            mode = mode,
            inputs = rawInputs,
            participantIds = profiles.map { it.id },
            expenses = eventExpenses,
        )
    }

    val selectedPreview = if (totalValue != null || eventExpenses.isNotEmpty()) previewFor(selectedMode) else null
    val canApply = selectedPreview?.validationMessage == null && selectedPreview != null

    val colors = NeoFintechColors.light()
    val shapes = NeoFintechShapes
    val typography = NeoFintechTypography()
    val monoFont = JetBrainsMonoFontFamily()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
    ) {
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
                style = typography.bodyMedium.copy(color = colors.onSurfaceVariant),
            )

            // Total input field
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
                    focusedBorderColor = colors.primaryContainer,
                    unfocusedBorderColor = colors.outlineVariant,
                ),
            )

            // Mode selector chip
            ModeSelectorChip(
                selectedMode = selectedMode,
                onModeSelected = { mode -> selectedModeId = mode.id },
            )

            // Mode helper text + example
            Text(
                text = selectedMode.helperText,
                style = typography.bodySmall.copy(color = colors.onSurfaceVariant),
            )
            Text(
                text = "Ej.: ${selectedMode.exampleText}",
                style = typography.bodySmall.copy(color = colors.onSurfaceVariant),
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
                            style = typography.bodySmall.copy(color = colors.onSurfaceVariant),
                        )
                        Text(
                            text = formatEuros(expense.amountEuros),
                            style = typography.labelSmall.copy(
                                fontFamily = monoFont,
                                color = colors.primaryContainer,
                            ),
                        )
                    }
                }
            }

            // Validation message
            selectedPreview?.validationMessage?.let { message ->
                Text(
                    text = message,
                    style = typography.bodySmall.copy(color = colors.error),
                )
            }

            // Preview breakdown (when valid)
            if (selectedPreview != null && selectedPreview.validationMessage == null) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = colors.outlineVariant,
                )

                PreviewBreakdown(
                    profiles = profiles,
                    amounts = selectedPreview.amounts,
                    summary = selectedPreview.summary,
                )

                // Collapsible comparison card
                ComparisonCard(
                    total = totalValue ?: expenseTotal,
                    profiles = profiles,
                    eventExpenses = eventExpenses,
                    percentageInputs = percentageInputs,
                    expanded = showComparison,
                    onToggleExpanded = { showComparison = !showComparison },
                )

                // Settlement card
                val transfers = remember(profiles, selectedPreview.amounts) {
                    buildSettlementTransfers(
                        profileNames = profiles.map { it.name },
                        amounts = selectedPreview.amounts,
                    )
                }
                if (transfers.isNotEmpty()) {
                    SettlementCard(transfers = transfers)
                }
            }

            // Bottom bar buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        if (selectedPreview != null && selectedPreview.validationMessage == null) {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    onApply(
                                        CalculationApplication(
                                            total = selectedPreview.calculatedTotal,
                                            mode = selectedMode,
                                            amounts = selectedPreview.amounts,
                                            summary = selectedPreview.summary,
                                        )
                                    )
                                }
                            }
                        }
                    },
                    enabled = canApply,
                    modifier = Modifier.weight(1f),
                    shape = shapes.md,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.buttonContainer,
                        contentColor = colors.onButton,
                        disabledContainerColor = colors.surfaceContainerHigh,
                        disabledContentColor = colors.onSurfaceVariant,
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
                        contentColor = colors.onSurface,
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant),
                ) {
                    Text("Cancelar")
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
    val colors = NeoFintechColors.light()
    val shapes = NeoFintechShapes
    val typography = NeoFintechTypography()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${profile.icon} ${profile.name}",
            modifier = Modifier.weight(1f),
            style = typography.bodyMedium,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            label = { Text(label) },
            singleLine = true,
            shape = shapes.md,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primaryContainer,
                unfocusedBorderColor = colors.outlineVariant,
            ),
        )
    }
}
