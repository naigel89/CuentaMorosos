package com.cuentamorosos.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    state: DashboardState,
    onOpenCalendar: () -> Unit = {},
    onProfileTap: (String) -> Unit = {},
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
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = "Abrir calendario",
                        modifier = Modifier.size(24.dp),
                        tint = colors.onPrimaryContainer,
                    )
                }
            }
        }

        // Financial Summary Row — only show when there are amounts
        if (summary.debes > 0.0 || summary.teDeben > 0.0) {
            item(key = "financial-summary") {
                FinancialSummaryRow(
                    debes = summary.debes,
                    teDeben = summary.teDeben,
                    debesCount = summary.debesCount,
                    teDebenCount = summary.teDebenCount,
                )
            }
        }

        // Net Balance Card — only show when non-zero
        if (summary.netBalance != 0.0) {
            item(key = "net-balance") {
                NetBalanceCard(balance = summary.netBalance)
            }
        }

        // Unified debts card (all profiles in one list)
        item(key = "unified-debts") {
            UnifiedDebtsCard(
                items = state.unifiedBreakdown,
                onProfileTap = onProfileTap,
            )
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
        repeat(3) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(colors.surfaceContainerHigh, NeoFintechShapes.md),
                )
            }
        }
    }
}
