package com.cuentamorosos.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

        // Indicator cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IndicatorCard(
                    modifier = Modifier.weight(1f),
                    title = "Te deben",
                    amount = state.totalOwedToYou,
                    borderColor = NeoFintechColors.dark().primaryContainer,
                    icon = "\uD83D\uDCC8",
                )
                IndicatorCard(
                    modifier = Modifier.weight(1f),
                    title = "Debés",
                    amount = state.totalYouOwe,
                    borderColor = NeoFintechColors.dark().error,
                    icon = "\uD83D\uDCC9",
                )
            }
        }

        // Smart Alerts
        item {
            Text(
                text = "Alertas inteligentes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (state.smartAlerts.isEmpty()) {
            item {
                Text(
                    text = "\u2705 Todo en orden",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeoFintechColors.dark().primaryContainer,
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

        // Recent Activity
        item {
            Text(
                text = "Actividad reciente",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
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
            items(state.recentActivity) { activity ->
                ActivityRow(
                    activity = activity,
                    onTap = { onActivityTap(activity) },
                )
            }
        }
    }
}

@Composable
private fun IndicatorCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: Double,
    borderColor: androidx.compose.ui.graphics.Color,
    icon: String,
) {
    Card(
        modifier = modifier
            .border(
                width = 4.dp,
                color = borderColor,
                shape = NeoFintechShapes.lg,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = icon, style = MaterialTheme.typography.headlineMedium)
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatEuros(amount),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = borderColor,
            )
        }
    }
}

@Composable
private fun AlertCard(
    alert: SmartAlert,
    onTap: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = alert.icon, style = MaterialTheme.typography.headlineMedium)
            Column {
                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "Tocar para ver detalles",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ActivityRow(
    activity: ActivityItem,
    onTap: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = activity.eventName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = formatDateMillis(activity.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatEuros(activity.amount),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = JetBrainsMonoFontFamily(),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            StatusBadge(status = activity.status)
        }
    }
    HorizontalDivider()
}

@Composable
private fun StatusBadge(status: EventStatus) {
    val (label, color) = when (status) {
        EventStatus.ACTIVE -> "Activo" to NeoFintechColors.dark().primaryContainer
        EventStatus.SETTLING -> "Calculando" to NeoFintechColors.dark().secondary
        EventStatus.CLOSED -> "Cerrado" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = NeoFintechShapes.full,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            textAlign = TextAlign.Center,
        )
    }
}
