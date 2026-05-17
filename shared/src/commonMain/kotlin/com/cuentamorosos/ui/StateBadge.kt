package com.cuentamorosos.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuentamorosos.model.EventState

/**
 * Returns the badge background color for a given event state.
 * Extracted from EventCard to be shared between StateBadge and EventCard.
 */
fun EventState.stateBadgeColor(colors: NeoFintechColorSet): Color =
    when (this) {
        EventState.DRAFT -> colors.onSurfaceVariant.copy(alpha = 0.3f)
        EventState.OPEN -> colors.primaryContainer.copy(alpha = 0.25f)
        EventState.CALCULATED -> colors.tertiaryContainer.copy(alpha = 0.35f)
        EventState.CLOSED -> Color(0xFF9C27B0).copy(alpha = 0.25f)
    }

/**
 * Returns the human-readable Spanish label for a given event state.
 */
fun EventState.stateBadgeLabel(): String =
    when (this) {
        EventState.DRAFT -> "Borrador"
        EventState.OPEN -> "Abierto"
        EventState.CALCULATED -> "Calculado"
        EventState.CLOSED -> "Cerrado"
    }

/**
 * A reusable badge/chip composable that displays the human-readable label
 * for an [EventState] with color-coded background.
 *
 * Color mapping:
 * - DRAFT → gray (onSurfaceVariant 30%)
 * - OPEN → blue/green (primaryContainer 25%)
 * - CALCULATED → green (tertiaryContainer 35%)
 * - CLOSED → purple (0xFF9C27B0 25%)
 */
@Composable
fun StateBadge(
    state: EventState,
    modifier: Modifier = Modifier,
) {
    val neoColors = LocalNeoFintechColors.current
    val themeColors = MaterialTheme.colorScheme

    Surface(
        color = state.stateBadgeColor(neoColors),
        shape = NeoFintechShapes.full,
        modifier = modifier,
    ) {
        Text(
            text = state.stateBadgeLabel(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
            color = themeColors.onSurface,
            fontWeight = FontWeight.Medium,
        )
    }
}
