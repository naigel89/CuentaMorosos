package com.cuentamorosos.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.formatEuros

/**
 * Global balance summary showing total owed to you and total you owe.
 *
 * Displays two indicator cards side by side:
 * - "Total a cobrar" (green) — positive balances owed to the current user
 * - "Total a pagar" (red) — negative balances the current user owes
 *
 * @param profiles The list of all profiles to aggregate balances from.
 * @param currentUid The ID of the current user's profile (used to determine owed vs owing).
 * @param modifier Optional modifier for layout positioning.
 */
@Composable
fun ProfileBalanceSummary(
    profiles: List<ProfileItem>,
    currentUid: String?,
    modifier: Modifier = Modifier,
) {
    val colors = NeoFintechColors.dark()

    val totalOwedToYou by remember(profiles, currentUid) {
        derivedStateOf {
            profiles
                .filter { it.id == currentUid }
                .sumOf { if (it.totalPendingEuros > 0) it.totalPendingEuros else 0.0 }
        }
    }

    val totalYouOwe by remember(profiles, currentUid) {
        derivedStateOf {
            profiles
                .filter { it.id == currentUid }
                .sumOf { if (it.totalPendingEuros < 0) -it.totalPendingEuros else 0.0 }
        }
    }

    val owedCount by remember(profiles, currentUid) {
        derivedStateOf {
            profiles
                .filter { it.id == currentUid }
                .count { it.totalPendingEuros > 0 }
        }
    }

    val oweCount by remember(profiles, currentUid) {
        derivedStateOf {
            profiles
                .filter { it.id == currentUid }
                .count { it.totalPendingEuros < 0 }
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SummaryIndicator(
            modifier = Modifier.weight(1f),
            icon = "\u2193",
            label = "Total a cobrar",
            amount = totalOwedToYou,
            count = owedCount,
            amountColor = colors.primaryContainer,
            iconColor = colors.primaryContainer,
        )
        SummaryIndicator(
            modifier = Modifier.weight(1f),
            icon = "\u2191",
            label = "Total a pagar",
            amount = totalYouOwe,
            count = oweCount,
            amountColor = colors.error,
            iconColor = colors.error,
        )
    }
}

/**
 * Individual summary indicator card used inside ProfileBalanceSummary.
 */
@Composable
private fun SummaryIndicator(
    modifier: Modifier = Modifier,
    icon: String,
    label: String,
    amount: Double,
    count: Int,
    amountColor: androidx.compose.ui.graphics.Color,
    iconColor: androidx.compose.ui.graphics.Color,
) {
    val colors = NeoFintechColors.dark()
    Card(
        modifier = modifier
            .shadow(NeoFintechElevation.cardShadowElevation, NeoFintechElevation.cardShadowShape, clip = false)
            .border(1.dp, colors.outlineVariant, NeoFintechShapes.lg),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLowest),
        shape = NeoFintechShapes.lg,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = icon,
                    style = MaterialTheme.typography.bodyLarge,
                    color = iconColor,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
            }
            Text(
                text = formatEuros(amount),
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = JetBrainsMonoFontFamily(),
                fontWeight = FontWeight.Bold,
                color = amountColor,
            )
            Text(
                text = "$count perfiles activos",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}
