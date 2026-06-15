package com.cuentamorosos.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    state: DashboardState,
    onOpenCalendar: () -> Unit = {},
) {
    val colors = LocalNeoFintechColors.current
    val summary = state.toFinancialSummary()

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
        item(key = "title") {
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

        // Financial Summary Row
        item(key = "financial-summary") {
            FinancialSummaryRow(
                debes = summary.debes,
                teDeben = summary.teDeben,
                debesCount = summary.debesCount,
                teDebenCount = summary.teDebenCount,
            )
        }

        // Net Balance Card
        item(key = "net-balance") {
            NetBalanceCard(balance = summary.netBalance)
        }

        // Debt accordion cards (Te deben + Debes)
        item(key = "debt-accordions") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .slideUp(),
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
    }
}

// ── Financial Summary Row ─────────────────────────────────────────────────────

@Composable
private fun FinancialSummaryRow(
    debes: Double,
    teDeben: Double,
    debesCount: Int,
    teDebenCount: Int,
) {
    val colors = LocalNeoFintechColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Left card: "DEBES"
        Card(
            modifier = Modifier
                .weight(1f)
                .fadeInStaggered(index = 0),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLowest),
            shape = NeoFintechShapes.lg,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    text = "DEBES",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.secondary,
                )
                Text(
                    text = rememberAnimatedAmount(targetValue = debes),
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = JetBrainsMonoFontFamily(),
                    fontWeight = FontWeight.Bold,
                    color = colors.secondary,
                )
                Text(
                    text = "$debesCount perfil${if (debesCount != 1) "es" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }

        // Right card: "TE DEBEN"
        Card(
            modifier = Modifier
                .weight(1f)
                .fadeInStaggered(index = 1),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLowest),
            shape = NeoFintechShapes.lg,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    text = "TE DEBEN",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.primaryContainer,
                )
                Text(
                    text = rememberAnimatedAmount(targetValue = teDeben),
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = JetBrainsMonoFontFamily(),
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryContainer,
                )
                Text(
                    text = "$teDebenCount perfil${if (teDebenCount != 1) "es" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Net Balance Card ──────────────────────────────────────────────────────────

@Composable
private fun NetBalanceCard(balance: Double) {
    val colors = LocalNeoFintechColors.current
    val isPositive = balance >= 0
    val accentColor = if (isPositive) colors.primaryContainer else colors.error
    val label = if (isPositive) "Balance a tu favor" else "Debes saldar"
    val prefix = if (isPositive) "+" else ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fadeInStaggered(index = 2),
        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.12f)),
        shape = NeoFintechShapes.lg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = accentColor,
            )
            Text(
                text = "$prefix${rememberAnimatedAmount(targetValue = kotlin.math.abs(balance))}",
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = JetBrainsMonoFontFamily(),
                fontWeight = FontWeight.Bold,
                color = accentColor,
            )
        }
    }
}

// ── Loading Skeleton ──────────────────────────────────────────────────────────

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
