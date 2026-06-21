package com.cuentamorosos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuentamorosos.model.EventState
import com.cuentamorosos.model.formatEuros

// Fix 5: Pure functions for total display logic ────────────────────────────────

/**
 * Returns the label for the "pending" row based on event state.
 * - OPEN: "Pendiente" (current behavior)
 * - CALCULATED/CLOSED: "Transferencias netas" (settlement transfers, not debts)
 */
internal fun computeTotalDisplayLabel(eventState: EventState): String {
    return when (eventState) {
        EventState.OPEN -> "Pendiente"
        EventState.CALCULATED, EventState.CLOSED -> "Transferencias netas"
    }
}

/**
 * Returns the total amount to display as the main "TOTAL DEL EVENTO" value.
 * - CALCULATED: uses [calculationTotal] (from snapshot) when available,
 *   falls back to [totalExpenses]
 * - OPEN/CLOSED: uses [totalExpenses] (immutable event cost)
 */
internal fun computeTotalAmountForDisplay(
    eventState: EventState,
    totalExpenses: Double,
    calculationTotal: Double?,
): Double {
    return when (eventState) {
        EventState.CALCULATED -> calculationTotal ?: totalExpenses
        EventState.OPEN, EventState.CLOSED -> totalExpenses
    }
}

// ── Composable ────────────────────────────────────────────────────────────────

/**
 * Compact total cost card for the EventDetail header.
 * Shows the total event cost prominently with a neon green accent.
 * Fix 5: Accepts [eventState] and [calculationTotal] to correctly display
 * post-settlement totals.
 */
@Composable
fun TotalCostCard(
    totalExpenses: Double,
    totalPending: Double,
    expenseCount: Int,
    eventState: EventState = EventState.OPEN,
    calculationTotal: Double? = null,
) {
    val colors = LocalNeoFintechColors.current
    val themeColors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .cardShadow(),
        colors = CardDefaults.cardColors(containerColor = themeColors.surfaceContainerLow),
        shape = NeoFintechShapes.xl,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "TOTAL DEL EVENTO",
                style = MaterialTheme.typography.labelSmall,
                color = themeColors.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
            val displayTotal = computeTotalAmountForDisplay(eventState, totalExpenses, calculationTotal)
            Text(
                text = formatEuros(displayTotal),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = colors.primaryContainer,
            )
            HorizontalDivider(color = themeColors.outlineVariant.copy(alpha = 0.3f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Gastos", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                Text(
                    "$expenseCount",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    fontFamily = JetBrainsMonoFontFamily(),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val pendingLabel = computeTotalDisplayLabel(eventState)
                Text(pendingLabel, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                Text(
                    formatEuros(totalPending),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    fontFamily = JetBrainsMonoFontFamily(),
                    color = if (totalPending > 0) colors.error else colors.primaryContainer,
                )
            }
        }
    }
}
