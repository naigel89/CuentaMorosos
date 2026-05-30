package com.cuentamorosos.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.formatEuros

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    state: DashboardState,
    events: List<EventItem> = emptyList(),
    onAlertTap: (SmartAlert) -> Unit = {},
    onEventTap: (DashboardEventRow) -> Unit = {},
    onOpenCalendar: () -> Unit = {},
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

    if (state.isLoading) {
        LoadingSkeleton(modifier = modifier)
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Title + Calendar button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Panel",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(colors.primaryContainer, RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenCalendar),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("\uD83D\uDCC5", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        // Resumen: accordion cards
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DebtAccordionCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = "Te deben",
                    totalAmount = state.totalOwedToYou,
                    breakdown = state.owedToYouBreakdown,
                    accentColor = colors.primaryContainer,
                    trendIcon = { TrendLineUp(color = colors.primaryContainer) },
                )
                DebtAccordionCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = "Debes",
                    totalAmount = state.totalYouOwe,
                    breakdown = state.youOweBreakdown,
                    accentColor = colors.error,
                    trendIcon = { TrendLineDown(color = colors.error) },
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
                    text = "No hay nada de lo que preocuparse… por ahora",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.primaryContainer,
                )
            }
        } else {
            item {
                val alertCount = state.smartAlerts.size
                val allExpanded = expandedAlertIds.size == state.smartAlerts.size
                Text(
                    text = "ALERTAS INTELIGENTES ($alertCount)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primaryContainer,
                    modifier = Modifier.clickable {
                        if (allExpanded) {
                            expandedAlertIds = emptySet()
                        } else {
                            expandedAlertIds = state.smartAlerts.map { it.eventId }.toSet()
                        }
                    },
                )
            }
            items(state.smartAlerts) { alert ->
                AlertAccordionCard(
                    alert = alert,
                    onTap = { onAlertTap(alert) },
                    initiallyExpanded = alert.eventId in expandedAlertIds,
                )
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
                    text = "No tienes aún eventos disponibles",
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
            Column(modifier = Modifier.weight(1f)) {
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
            Text(
                text = "\u25B6",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LoadingSkeleton(modifier: Modifier = Modifier) {
    val colors = LocalNeoFintechColors.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .background(colors.surfaceContainerHigh, NeoFintechShapes.sm),
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(colors.surfaceContainerHigh, NeoFintechShapes.lg),
                    )
                }
            }
        }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(24.dp)
                    .background(colors.surfaceContainerHigh, NeoFintechShapes.sm),
            )
        }
        items(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(colors.surfaceContainerHigh, NeoFintechShapes.md),
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

// ── Animated sparkline trend lines ────────────────────────────────────────────

@Composable
private fun TrendLineUp(color: Color) {
    var animate by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (animate) 1f else 0f,
        animationSpec = tween(durationMillis = 1200),
        label = "trendLineUp",
    )

    LaunchedEffect(Unit) { animate = true }

    Canvas(modifier = Modifier.size(24.dp)) {
        val strokeWidth = 1.5f
        val w = size.width
        val h = size.height

        // Zigzag path trending upward with peaks
        val path = Path().apply {
            moveTo(0f, h * 0.85f)
            lineTo(w * 0.15f, h * 0.65f)  // up
            lineTo(w * 0.25f, h * 0.75f)  // dip
            lineTo(w * 0.40f, h * 0.45f)  // up
            lineTo(w * 0.50f, h * 0.55f)  // dip
            lineTo(w * 0.65f, h * 0.30f)  // up
            lineTo(w * 0.75f, h * 0.40f)  // dip
            lineTo(w * 0.90f, h * 0.15f)  // up to peak
            lineTo(w, 0f)                  // arrow tip
        }

        val pathMeasure = PathMeasure()
        pathMeasure.setPath(path, false)
        val length = pathMeasure.length

        val animatedPath = Path()
        pathMeasure.getSegment(0f, length * progress, animatedPath, true)

        drawPath(
            path = animatedPath,
            color = color,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

@Composable
private fun TrendLineDown(color: Color) {
    var animate by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (animate) 1f else 0f,
        animationSpec = tween(durationMillis = 1200),
        label = "trendLineDown",
    )

    LaunchedEffect(Unit) { animate = true }

    Canvas(modifier = Modifier.size(24.dp)) {
        val strokeWidth = 1.5f
        val w = size.width
        val h = size.height

        // Zigzag path trending downward with peaks
        val path = Path().apply {
            moveTo(0f, h * 0.15f)
            lineTo(w * 0.15f, h * 0.35f)   // down
            lineTo(w * 0.25f, h * 0.25f)   // up
            lineTo(w * 0.40f, h * 0.55f)   // down
            lineTo(w * 0.50f, h * 0.45f)   // up
            lineTo(w * 0.65f, h * 0.70f)   // down
            lineTo(w * 0.75f, h * 0.60f)   // up
            lineTo(w * 0.90f, h * 0.85f)   // down to bottom
            lineTo(w, h)                    // arrow tip
        }

        val pathMeasure = PathMeasure()
        pathMeasure.setPath(path, false)
        val length = pathMeasure.length

        val animatedPath = Path()
        pathMeasure.getSegment(0f, length * progress, animatedPath, true)

        drawPath(
            path = animatedPath,
            color = color,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}
