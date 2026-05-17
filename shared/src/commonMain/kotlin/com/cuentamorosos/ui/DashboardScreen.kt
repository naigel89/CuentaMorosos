package com.cuentamorosos.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cuentamorosos.model.formatEuros

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    state: DashboardState,
    onAlertTap: (SmartAlert) -> Unit = {},
    onEventTap: (DashboardEventRow) -> Unit = {},
) {
    val colors = LocalNeoFintechColors.current
    var expandedAlertIds by remember { mutableStateOf(setOf<String>()) }

    fun toggleAlert(alertId: String) {
        expandedAlertIds = if (alertId in expandedAlertIds) {
            expandedAlertIds - alertId
        } else {
            expandedAlertIds + alertId
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Title
        item {
            Text(
                text = "Panel",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        // Resumen: indicator cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IndicatorCard(
                    modifier = Modifier.weight(1f),
                    title = "Te deben",
                    amount = state.totalOwedToYou,
                    borderColor = colors.primaryContainer,
                    icon = "\uD83D\uDCC8",
                )
                IndicatorCard(
                    modifier = Modifier.weight(1f),
                    title = "Debes",
                    amount = state.totalYouOwe,
                    borderColor = colors.error,
                    icon = "\uD83D\uDCC9",
                )
            }
        }

        // Alertas Inteligentes (collapsible)
        if (state.smartAlerts.isEmpty()) {
            item {
                Text(
                    text = "ALERTAS INTELIGENTES",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primaryContainer,
                )
                Text(
                    text = "\u2705 Todo en orden",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.primaryContainer,
                )
            }
        } else {
            item {
                val alertCount = state.smartAlerts.size
                Text(
                    text = "ALERTAS INTELIGENTES ($alertCount)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primaryContainer,
                    modifier = Modifier.clickable {
                        // Toggle all alerts at once
                        if (expandedAlertIds.isEmpty()) {
                            expandedAlertIds = state.smartAlerts.map { it.eventId }.toSet()
                        } else {
                            expandedAlertIds = emptySet()
                        }
                    },
                )
            }
            items(state.smartAlerts) { alert ->
                val isExpanded = alert.eventId in expandedAlertIds
                AnimatedVisibility(visible = isExpanded) {
                    AlertCard(
                        alert = alert,
                        onTap = { onAlertTap(alert) },
                    )
                }
            }
        }

        // TODOS MIS EVENTOS (unified list)
        item {
            Text(
                text = "TODOS MIS EVENTOS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.primaryContainer,
            )
        }

        if (state.allEvents.isEmpty()) {
            item {
                Text(
                    text = "No tienes eventos aún",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(state.allEvents) { eventRow ->
                DashboardEventRow(
                    row = eventRow,
                    onTap = { onEventTap(eventRow) },
                )
            }
        }
    }
}

// ── IndicatorCard with top accent bar ─────────────────────────────────────────

@Composable
private fun IndicatorCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: Double,
    borderColor: androidx.compose.ui.graphics.Color,
    icon: String,
) {
    val colors = LocalNeoFintechColors.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(NeoFintechElevation.cardShadowElevation, NeoFintechElevation.cardShadowShape, clip = false)
            .border(1.dp, colors.outlineVariant, NeoFintechShapes.lg),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLowest),
        shape = NeoFintechShapes.lg,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top accent bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(borderColor),
            )
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                    )
                    Text(text = icon, style = MaterialTheme.typography.headlineMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatEuros(amount),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    fontFamily = JetBrainsMonoFontFamily(),
                    color = colors.onSurface,
                )
            }
        }
    }
}

// ── AlertCard with icon circles ───────────────────────────────────────────────

@Composable
private fun AlertCard(
    alert: SmartAlert,
    onTap: () -> Unit,
) {
    val colors = LocalNeoFintechColors.current

    val (iconBgColor, iconColor) = when (alert.type) {
        AlertType.NO_PARTICIPANTS -> colors.errorContainer to colors.onErrorContainer
        AlertType.NO_EXPENSES -> colors.surfaceContainerHigh to colors.onSurfaceVariant
        AlertType.PENDING_CALCULATIONS -> colors.tertiaryContainer to colors.onTertiaryContainer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLowest),
        shape = NeoFintechShapes.md,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Left: icon circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBgColor, NeoFintechShapes.full),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = alert.icon,
                    style = MaterialTheme.typography.bodyLarge,
                    color = iconColor,
                )
            }
            // Center: alert text
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface,
                )
                Text(
                    text = "Tocar para ver detalles",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            // Right: chevron
            Text(
                text = "\u25B6",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

// ── DashboardEventRow (unified event row) ─────────────────────────────────────

@Composable
private fun DashboardEventRow(
    row: DashboardEventRow,
    onTap: () -> Unit,
) {
    val colors = LocalNeoFintechColors.current
    val amountColor = if (row.amount >= 0) colors.primaryContainer else colors.error
    val participantLabel = when {
        row.participantCount == 0 -> "Sin participantes"
        row.participantCount == 1 -> "1 participante"
        else -> "${row.participantCount} participantes"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLowest),
        shape = NeoFintechShapes.md,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.eventName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface,
                )
                Text(
                    text = participantLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatEuros(row.amount),
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = JetBrainsMonoFontFamily(),
                    fontWeight = FontWeight.SemiBold,
                    color = amountColor,
                )
                StateBadge(state = row.state)
            }
        }
    }
}
