package com.cuentamorosos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuentamorosos.model.formatEuros

@Composable
fun DebtAccordionCard(
    modifier: Modifier = Modifier,
    title: String,
    breakdown: List<DebtBreakdownItem>,
    accentColor: Color,
    trendIcon: @Composable () -> Unit,
    onProfileTap: (String) -> Unit = {},
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
                    .background(accentColor),
            )

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                trendIcon()
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
            }

            // Profile list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (breakdown.isEmpty()) {
                    Text(
                        text = "No hay deudas pendientes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                } else {
                    breakdown.forEachIndexed { index, item ->
                        ProfileDebtRow(
                            item = item,
                            accentColor = accentColor,
                            onProfileTap = onProfileTap,
                            staggerIndex = index,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileDebtRow(
    item: DebtBreakdownItem,
    accentColor: Color,
    onProfileTap: (String) -> Unit,
    staggerIndex: Int = 0,
) {
    val colors = LocalNeoFintechColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fadeInStaggered(index = staggerIndex)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.profileName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = colors.onSurface,
            )
            // Event indicator
            if (item.events.isNotEmpty()) {
                Text(
                    text = item.events.first().eventName,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }

        Text(
            text = formatEuros(item.amount),
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = JetBrainsMonoFontFamily(),
            fontWeight = FontWeight.SemiBold,
            color = accentColor,
        )
    }
}
