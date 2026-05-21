package com.cuentamorosos.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuentamorosos.model.formatEuros

@Composable
fun DebtAccordionCard(
    modifier: Modifier = Modifier,
    title: String,
    totalAmount: Double,
    breakdown: List<DebtBreakdownItem>,
    accentColor: Color,
    trendIcon: @Composable () -> Unit,
    onProfileTap: (String) -> Unit = {},
) {
    val colors = LocalNeoFintechColors.current
    var expanded by remember { mutableStateOf(false) }
    val animatedTotal = rememberAnimatedDouble(totalAmount)

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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable { expanded = !expanded },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
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
                    // Chevron
                    Text(
                        text = if (expanded) "\u25BC" else "\u25B6",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = formatEuros(animatedTotal),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    fontFamily = JetBrainsMonoFontFamily(),
                    color = colors.onSurface,
                )
            }

            // Collapsible content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .padding(horizontal = 16.dp, vertical = if (expanded) 8.dp else 0.dp),
            ) {
                if (expanded) {
                    if (breakdown.isEmpty()) {
                        Text(
                            text = "No hay deudas pendientes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            breakdown.forEachIndexed { index, item ->
                                ProfileDebtRow(
                                    item = item,
                                    accentColor = accentColor,
                                    maxAmount = breakdown.maxOfOrNull { it.amount } ?: 0.0,
                                    onProfileTap = onProfileTap,
                                    staggerIndex = index,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileDebtRow(
    item: DebtBreakdownItem,
    accentColor: androidx.compose.ui.graphics.Color,
    maxAmount: Double,
    onProfileTap: (String) -> Unit,
    staggerIndex: Int = 0,
) {
    val colors = LocalNeoFintechColors.current
    val proportion = if (maxAmount > 0) (item.amount / maxAmount).toFloat() else 0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProfileTap(item.profileId) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.profileName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = colors.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Proportion bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(colors.outlineVariant.copy(alpha = 0.3f), NeoFintechShapes.sm),
            ) {
                AnimatedProportionBar(
                    proportion = proportion,
                    color = accentColor,
                    shape = NeoFintechShapes.sm,
                    delayMillis = staggerIndex * NeoFintechAnimations.STAGGER_DELAY_MS,
                )
            }
        }

        Text(
            text = formatEuros(item.amount),
            style = MaterialTheme.typography.labelLarge,
            fontFamily = JetBrainsMonoFontFamily(),
            fontWeight = FontWeight.SemiBold,
            color = accentColor,
        )
    }
}
