package com.cuentamorosos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuentamorosos.model.formatEuros

@Suppress("UNUSED_PARAMETER")
@Composable
fun BalanceSummaryCard(
    totalPending: Double,
    activeEventCount: Int,
    totalSpent: Double,
    _owedEventCount: Int = 0,
    _onSettleUp: (() -> Unit)? = null,
    _onRequest: (() -> Unit)? = null,
) {
    val colors = LocalNeoFintechColors.current
    val themeColors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left: Total balance
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "\uD83D\uDCB0",
                style = MaterialTheme.typography.headlineMedium,
            )
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(
                    text = "Balance total",
                    style = MaterialTheme.typography.labelSmall,
                    color = themeColors.onSurfaceVariant,
                )
                Text(
                    text = formatEuros(totalPending),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    fontFamily = JetBrainsMonoFontFamily(),
                    color = colors.primaryContainer,
                )
            }
        }
        // Right: Quick stats
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip(label = "$activeEventCount activos", icon = "\uD83D\uDCC5")
            StatChip(label = formatEuros(totalSpent), icon = "\uD83D\uDCB3")
        }
    }
}

@Composable
private fun StatChip(label: String, icon: String) {
    val themeColors = MaterialTheme.colorScheme

    Surface(
        color = themeColors.surfaceContainerHigh,
        shape = NeoFintechShapes.md,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = icon, style = MaterialTheme.typography.labelMedium)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = themeColors.onSurface,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
