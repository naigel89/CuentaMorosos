package com.cuentamorosos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuentamorosos.model.formatEuros

@Composable
fun BalanceSummaryCard(
    totalPending: Double,
    activeEventCount: Int,
    totalSpent: Double,
    owedEventCount: Int = 0,
    onSettleUp: (() -> Unit)? = null,
    onRequest: (() -> Unit)? = null,
) {
    BoxWithConstraints {
        val isWide = maxWidth >= 600.dp

        if (isWide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NeoFintechSpacing.md),
            ) {
                TotalBalanceCard(
                    modifier = Modifier.weight(2f),
                    totalPending = totalPending,
                    owedEventCount = owedEventCount,
                    onSettleUp = onSettleUp,
                    onRequest = onRequest,
                )
                QuickStatsColumn(
                    modifier = Modifier.weight(1f),
                    activeEventCount = activeEventCount,
                    totalSpent = totalSpent,
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(NeoFintechSpacing.sm),
            ) {
                TotalBalanceCard(
                    modifier = Modifier.fillMaxWidth(),
                    totalPending = totalPending,
                    owedEventCount = owedEventCount,
                    onSettleUp = onSettleUp,
                    onRequest = onRequest,
                )
                QuickStatsColumn(
                    modifier = Modifier.fillMaxWidth(),
                    activeEventCount = activeEventCount,
                    totalSpent = totalSpent,
                )
            }
        }
    }
}

@Composable
private fun TotalBalanceCard(
    modifier: Modifier = Modifier,
    totalPending: Double,
    owedEventCount: Int,
    onSettleUp: (() -> Unit)?,
    onRequest: (() -> Unit)?,
) {
    val colors = MaterialTheme.colorScheme
    val neoColors = NeoFintechColors.dark()
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Card(
        modifier = modifier
            .shadow(
                elevation = if (isHovered == true) NeoFintechElevation.cardShadowHoverElevation
                else NeoFintechElevation.cardShadowElevation,
                shape = NeoFintechElevation.cardShadowShape,
                clip = false,
            )
            .border(1.dp, colors.outlineVariant, NeoFintechShapes.xl)
            .clip(NeoFintechShapes.xl)
            .hoverable(interactionSource),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = NeoFintechShapes.xl,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Decorative glow (positioned in top-right corner)
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.TopEnd)
                    .background(
                        neoColors.primaryContainer.copy(alpha = if (isHovered == true) 0.10f else 0.05f),
                        shape = NeoFintechShapes.full,
                    ),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(NeoFintechSpacing.lg),
            ) {
                Text(
                    text = "Total Balance",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                    ),
                    color = colors.onSurfaceVariant,
                )
                Text(
                    text = formatEuros(totalPending),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = colors.onSurface,
                )

                Row(
                    modifier = Modifier.padding(top = NeoFintechSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(NeoFintechSpacing.xs),
                ) {
                    Surface(
                        color = neoColors.primaryContainer.copy(alpha = 0.1f),
                        shape = NeoFintechShapes.full,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = neoColors.primaryContainer,
                            )
                            Text(
                                text = "You are owed",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                color = neoColors.primaryContainer,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                    Text(
                        text = "from $owedEventCount events",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                        color = colors.onSurfaceVariant,
                    )
                }

                Row(
                    modifier = Modifier.padding(top = NeoFintechSpacing.md),
                    horizontalArrangement = Arrangement.spacedBy(NeoFintechSpacing.sm),
                ) {
                    Button(
                        onClick = { onSettleUp?.invoke() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = neoColors.primaryContainer,
                            contentColor = neoColors.onSurface,
                        ),
                        shape = NeoFintechShapes.xl,
                    ) {
                        Text("Settle Up", style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp))
                    }
                    OutlinedButton(
                        onClick = { onRequest?.invoke() },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = colors.surfaceContainer,
                            contentColor = colors.onSurface,
                        ),
                        shape = NeoFintechShapes.xl,
                    ) {
                        Text("Request", style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp))
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickStatsColumn(
    modifier: Modifier = Modifier,
    activeEventCount: Int,
    totalSpent: Double,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(NeoFintechSpacing.sm),
    ) {
        QuickStatCard(
            label = "Active Events",
            value = activeEventCount.toString(),
            icon = Icons.Default.Event,
            iconBgColor = NeoFintechColors.dark().primaryContainer,
        )
        QuickStatCard(
            label = "Total Spent",
            value = formatEuros(totalSpent),
            icon = Icons.Default.CreditCard,
            iconBgColor = NeoFintechColors.dark().secondary,
        )
    }
}

@Composable
private fun QuickStatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBgColor: androidx.compose.ui.graphics.Color,
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(NeoFintechElevation.cardShadowElevation, NeoFintechElevation.cardShadowShape, clip = false)
            .border(1.dp, colors.outlineVariant, NeoFintechShapes.xl),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = NeoFintechShapes.xl,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(NeoFintechSpacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                    color = colors.onSurfaceVariant,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.sp),
                    color = colors.onSurface,
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(NeoFintechShapes.full)
                    .background(iconBgColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = iconBgColor,
                )
            }
        }
    }
}
