package com.cuentamorosos.ui

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cuentamorosos.formatDateMillis
import com.cuentamorosos.model.formatEuros

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    state: DashboardState,
    onAlertTap: (SmartAlert) -> Unit = {},
    onActivityTap: (ActivityItem) -> Unit = {},
) {
    val colors = NeoFintechColors.dark()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = "Panel de Control",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        // FIX D1: Indicator cards with top accent bar
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
                    title = "Debés",
                    amount = state.totalYouOwe,
                    borderColor = colors.error,
                    icon = "\uD83D\uDCC9",
                )
            }
        }

        // Historial de eventos
        item {
            Text(
                text = "HISTORIAL DE EVENTOS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.primaryContainer,
            )
        }

        if (state.eventHistory.isEmpty()) {
            item {
                Text(
                    text = "No hay eventos activos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(state.eventHistory) { historyItem ->
                EventHistoryRow(
                    historyItem = historyItem,
                    onTap = {
                        // Build a SmartAlert to reuse onAlertTap for navigation
                        onAlertTap(
                            SmartAlert(
                                type = AlertType.NO_EXPENSES,
                                message = historyItem.eventName,
                                icon = "\uD83D\uDCCB",
                                eventId = historyItem.eventId,
                            ),
                        )
                    },
                )
            }
        }

        // Smart Alerts
        item {
            Text(
                text = "ALERTAS INTELIGENTES",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.primaryContainer,
            )
        }

        if (state.smartAlerts.isEmpty()) {
            item {
                Text(
                    text = "\u2705 Todo en orden",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.primaryContainer,
                )
            }
        } else {
            items(state.smartAlerts) { alert ->
                AlertCard(
                    alert = alert,
                    onTap = { onAlertTap(alert) },
                )
            }
        }

        // FIX D5: Recent Activity with "Ver todo" button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "ACTIVIDAD RECIENTE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primaryContainer,
                )
                TextButton(onClick = { /* TODO: navigate to all activity */ }) {
                    Text(
                        text = "Ver todo",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primaryContainer,
                    )
                }
            }
        }

        if (state.recentActivity.isEmpty()) {
            item {
                Text(
                    text = "Sin actividad reciente",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            // FIX D6: Activity rows wrapped in a single Card container
            item {
                ActivityCardContainer(
                    activities = state.recentActivity,
                    onActivityTap = onActivityTap,
                )
            }
        }
    }
}

// ── IndicatorCard with top accent bar (FIX D1) ────────────────────────────────

@Composable
private fun IndicatorCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: Double,
    borderColor: androidx.compose.ui.graphics.Color,
    icon: String,
) {
    val colors = NeoFintechColors.dark()

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
                // FIX D4: JetBrains Mono for amounts
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

// ── EventHistoryRow ───────────────────────────────────────────────────────────

@Composable
private fun EventHistoryRow(
    historyItem: EventHistoryItem,
    onTap: () -> Unit,
) {
    val colors = NeoFintechColors.dark()
    val amountColor = if (historyItem.amount >= 0) colors.primaryContainer else colors.error
    val participantLabel = when {
        historyItem.participantCount == 0 -> "Sin participantes"
        historyItem.participantCount == 1 -> "1 participante"
        else -> "${historyItem.participantCount} participantes"
    }
    val statusLabel = when (historyItem.status) {
        EventStatus.ACTIVE -> "Activo"
        EventStatus.SETTLING -> "Calculando"
        EventStatus.CLOSED -> "Cerrado"
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
                    text = historyItem.eventName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface,
                )
                Text(
                    text = "$participantLabel · $statusLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            Text(
                text = formatEuros(historyItem.amount),
                style = MaterialTheme.typography.labelLarge,
                fontFamily = JetBrainsMonoFontFamily(),
                fontWeight = FontWeight.SemiBold,
                color = amountColor,
            )
        }
    }
}

// ── AlertCard with icon circles (FIX D3) ──────────────────────────────────────

@Composable
private fun AlertCard(
    alert: SmartAlert,
    onTap: () -> Unit,
) {
    val colors = NeoFintechColors.dark()

    // Resolve icon circle colors based on alert type
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

// ── Activity rows in card container (FIX D6) ──────────────────────────────────

@Composable
private fun ActivityCardContainer(
    activities: List<ActivityItem>,
    onActivityTap: (ActivityItem) -> Unit,
) {
    val colors = NeoFintechColors.dark()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(NeoFintechElevation.cardShadowElevation, NeoFintechElevation.cardShadowShape, clip = false)
            .border(1.dp, colors.outlineVariant, NeoFintechShapes.lg),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLowest),
        shape = NeoFintechShapes.lg,
    ) {
        Column {
            activities.forEachIndexed { index, activity ->
                ActivityRow(
                    activity = activity,
                    onTap = { onActivityTap(activity) },
                )
                if (index < activities.lastIndex) {
                    HorizontalDivider(color = colors.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(
    activity: ActivityItem,
    onTap: () -> Unit,
) {
    val colors = NeoFintechColors.dark()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = activity.eventName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = colors.onSurface,
            )
            Text(
                text = formatDateMillis(activity.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatEuros(activity.amount),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = JetBrainsMonoFontFamily(),
                color = colors.primaryContainer,
            )
            Spacer(modifier = Modifier.width(8.dp))
            StatusBadge(status = activity.status)
        }
    }
}

@Composable
private fun StatusBadge(status: EventStatus) {
    val colors = NeoFintechColors.dark()
    val (label, bgColor, textColor) = when (status) {
        EventStatus.ACTIVE -> Triple("Activo", colors.primaryContainer.copy(alpha = 0.15f), colors.primaryContainer)
        EventStatus.SETTLING -> Triple("Calculando", colors.surfaceContainer, colors.onSurfaceVariant)
        EventStatus.CLOSED -> Triple("Cerrado", colors.tertiaryContainer.copy(alpha = 0.3f), colors.onTertiaryContainer)
    }
    Surface(
        color = bgColor,
        shape = NeoFintechShapes.full,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            textAlign = TextAlign.Center,
        )
    }
}
