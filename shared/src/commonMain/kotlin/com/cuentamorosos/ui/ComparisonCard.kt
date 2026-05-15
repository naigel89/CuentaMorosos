package com.cuentamorosos.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuentamorosos.model.CalculationPreview
import com.cuentamorosos.model.EventExpenseItem
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.SplitMode
import com.cuentamorosos.model.buildCalculationPreview
import com.cuentamorosos.model.formatEuros
import com.cuentamorosos.model.parseEuroAmount

/**
 * Collapsible card showing all 4 split modes with their per-profile amounts.
 * Compact format: "Mode: amount1 · amount2 · amount3"
 * Uses smaller typography (bodySmall).
 */
@Composable
fun ComparisonCard(
    total: Double,
    profiles: List<ProfileItem>,
    eventExpenses: List<EventExpenseItem>,
    percentageInputs: List<String>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
) {
    val colors = NeoFintechColors.light()
    val shapes = NeoFintechShapes
    val typography = NeoFintechTypography()

    fun previewFor(mode: SplitMode): CalculationPreview {
        val rawInputs = when (mode) {
            SplitMode.REAL_CONSUMPTION -> List(profiles.size) { 1.0 }
            SplitMode.SIMPLE_AVG -> List(profiles.size) { 1.0 }
            SplitMode.BY_CATEGORY -> List(profiles.size) { 1.0 }
            SplitMode.CUSTOM_PERCENTAGE -> percentageInputs.map { parseDecimalValue(it) ?: Double.NaN }
        }

        return if (rawInputs.any { it.isNaN() }) {
            CalculationPreview(validationMessage = "Requiere completar parámetros válidos.")
        } else {
            buildCalculationPreview(
                total = total,
                mode = mode,
                inputs = rawInputs,
                participantIds = profiles.map { it.id },
                expenses = eventExpenses,
            )
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpanded() },
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceContainer,
        ),
        shape = shapes.lg,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Comparativa rápida",
                    style = typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    text = if (expanded) "▲" else "▼",
                    style = typography.bodySmall,
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SplitMode.entries.forEach { mode ->
                        val preview = previewFor(mode)
                        val text = preview.validationMessage ?: preview.amounts.joinToString(separator = " · ") { amount ->
                            formatEuros(amount)
                        }

                        Text(
                            text = "${mode.label}: $text",
                            style = typography.bodySmall.copy(color = colors.onSurfaceVariant),
                        )
                    }
                }
            }
        }
    }
}

// Local copy of parseDecimalValue for this file's scope
private fun parseDecimalValue(value: String): Double? = value
    .trim()
    .replace(',', '.')
    .toDoubleOrNull()
