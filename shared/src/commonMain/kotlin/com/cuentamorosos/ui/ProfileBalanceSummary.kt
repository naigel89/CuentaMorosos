package com.cuentamorosos.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.formatEuros

/**
 * Global balance summary showing total pending, total to collect, and total to pay.
 *
 * Displays three indicator cards in a row, each with an icon, label, and computed amount.
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

    val totalPending by remember(profiles) {
        derivedStateOf {
            profiles.sumOf { it.totalPendingEuros }
        }
    }

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

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SummaryIndicator(
            modifier = Modifier.weight(1f),
            icon = "\uD83D\uDCCB",
            label = "Total pendiente",
            amount = totalPending,
            borderColor = colors.primaryContainer,
        )
        SummaryIndicator(
            modifier = Modifier.weight(1f),
            icon = "\uD83D\uDCC8",
            label = "Total a cobrar",
            amount = totalOwedToYou,
            borderColor = colors.error,
        )
        SummaryIndicator(
            modifier = Modifier.weight(1f),
            icon = "\uD83D\uDCC9",
            label = "Total a pagar",
            amount = totalYouOwe,
            borderColor = colors.secondary,
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
    borderColor: androidx.compose.ui.graphics.Color,
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
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = icon, style = MaterialTheme.typography.headlineMedium)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatEuros(amount),
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = JetBrainsMonoFontFamily(),
                fontWeight = FontWeight.Bold,
                color = borderColor,
            )
        }
    }
}
