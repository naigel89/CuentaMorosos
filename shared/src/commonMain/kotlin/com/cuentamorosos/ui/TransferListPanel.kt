package com.cuentamorosos.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuentamorosos.model.CalculationSnapshot
import com.cuentamorosos.model.CalculationStatus
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.formatEuros

/**
 * Displays the results of a settlement calculation: status banner, transfer rows,
 * per-profile net balances, and total expense summary.
 *
 * Replaces the old SettlementCard usage in CalculatorSheet.
 */
@Composable
fun TransferListPanel(
    snapshot: CalculationSnapshot,
    status: CalculationStatus?,
    profileNameResolver: (String) -> String,
    profiles: List<ProfileItem> = emptyList(),
    paidTransferIndices: Set<Int> = emptySet(),
    onTogglePaid: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val themeColors = MaterialTheme.colorScheme
    val shapes = NeoFintechShapes
    val typography = NeoFintechTypography()
    val monoFont = JetBrainsMonoFontFamily()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = NeoFintechElevation.cardShadowElevation,
                shape = NeoFintechElevation.cardShadowShape,
                clip = false,
            ),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        shape = shapes.lg,
        border = BorderStroke(1.dp, themeColors.outline),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Section 1: Status banner
            status?.let {
                StatusBanner(status = it, typography = typography, themeColors = themeColors)
            }

            // Section 2: Total expense header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Total del evento",
                    style = typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    text = formatEuros(snapshot.totalExpense),
                    style = typography.titleMedium.copy(
                        fontFamily = monoFont,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.primary,
                    ),
                )
            }

            // Section 3: Transfer rows
            if (snapshot.transfers.isEmpty()) {
                Text(
                    text = "No hay transferencias pendientes",
                    style = typography.bodyMedium.copy(
                        color = themeColors.onSurfaceVariant,
                    ),
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                Text(
                    text = "Transferencias sugeridas",
                    style = typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )
                snapshot.transfers.forEachIndexed { index, transfer ->
                    val isPaid = index in paidTransferIndices
                    TransferRow(
                        transfer = transfer,
                        profileNameResolver = profileNameResolver,
                        profiles = profiles,
                        isPaid = isPaid,
                        _onTogglePaid = { onTogglePaid(index) },
                        typography = typography,
                        themeColors = themeColors,
                        monoFont = monoFont,
                    )
                }
            }

            // Section 4: Per-profile net balances
            if (snapshot.participantBalances.isNotEmpty()) {
                Text(
                    text = "Saldos por perfil",
                    style = typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )
                snapshot.participantBalances.forEach { (profileId, balance) ->
                    val name = profileNameResolver(profileId)
                    val profile = profiles.find { it.id == profileId }
                    BalanceRow(
                        name = name,
                        profile = profile,
                        balance = balance,
                        typography = typography,
                        themeColors = themeColors,
                        monoFont = monoFont,
                    )
                }
            }
        }
    }
}

/**
 * Color-coded status banner for CalculationStatus variants.
 *
 * Coverage: All 4 CalculationStatus variants are handled:
 * - Success: tertiaryContainer + ✅ checkmark + "Cálculo completado"
 * - ZeroBalance: primaryContainer + 🎉 celebration + "Todo está saldado"
 * - EdgeCaseWarning: secondaryContainer + ⚠️ warning + custom message
 * - Error: errorContainer + ❌ error icon + custom message
 */
@Composable
private fun StatusBanner(
    status: CalculationStatus,
    typography: Typography,
    themeColors: androidx.compose.material3.ColorScheme,
) {
    val (backgroundColor, contentColor, icon) = when (status) {
        is CalculationStatus.Success ->
            Triple(themeColors.tertiaryContainer, themeColors.onTertiaryContainer, "\u2705")
        is CalculationStatus.ZeroBalance ->
            Triple(themeColors.primaryContainer, themeColors.onPrimaryContainer, "\uD83C\uDF89")
        is CalculationStatus.EdgeCaseWarning ->
            Triple(themeColors.secondaryContainer, themeColors.onSecondaryContainer, "\u26A0\uFE0F")
        is CalculationStatus.Error ->
            Triple(themeColors.errorContainer, themeColors.onErrorContainer, "\u274C")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = backgroundColor, shape = NeoFintechShapes.md)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = icon,
            style = typography.bodyMedium,
        )
        Text(
            text = status.message,
            style = typography.bodyMedium.copy(color = contentColor),
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Single transfer row: "Perfil A → Perfil B: XX,XX €"
 * Paid transfers show checkmark + muted styling.
 * ProfileAvatar (24dp) rendered before each profile name.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
private fun TransferRow(
    transfer: com.cuentamorosos.model.SettlementTransfer,
    profileNameResolver: (String) -> String,
    profiles: List<ProfileItem>,
    isPaid: Boolean,
    _onTogglePaid: () -> Unit,
    typography: Typography,
    themeColors: androidx.compose.material3.ColorScheme,
    monoFont: androidx.compose.ui.text.font.FontFamily,
) {
    val fromName = profileNameResolver(transfer.fromProfileId)
    val toName = profileNameResolver(transfer.toProfileId)
    val fromProfile = profiles.find { it.id == transfer.fromProfileId }
    val toProfile = profiles.find { it.id == transfer.toProfileId }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isPaid) 0.5f else 1f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            if (isPaid) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Pagado",
                    modifier = Modifier.size(16.dp),
                    tint = themeColors.tertiary,
                )
            }
            if (fromProfile != null) {
                ProfileAvatar(
                    name = fromProfile.name,
                    emoji = fromProfile.icon,
                    photoUrl = fromProfile.photoUrl,
                    size = 24.dp,
                )
            }
            Text(
                text = fromName,
                style = typography.bodyMedium.copy(
                    color = if (isPaid) themeColors.onSurfaceVariant else themeColors.onSurface,
                ),
            )
            Text(
                text = "→",
                style = typography.bodyMedium.copy(
                    color = themeColors.onSurfaceVariant,
                ),
            )
            if (toProfile != null) {
                ProfileAvatar(
                    name = toProfile.name,
                    emoji = toProfile.icon,
                    photoUrl = toProfile.photoUrl,
                    size = 24.dp,
                )
            }
            Text(
                text = toName,
                style = typography.bodyMedium.copy(
                    color = if (isPaid) themeColors.onSurfaceVariant else themeColors.onSurface,
                ),
            )
        }
        Text(
            text = formatEuros(transfer.amount),
            style = typography.labelSmall.copy(
                fontFamily = monoFont,
                color = if (isPaid) themeColors.tertiary else themeColors.primary,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

/**
 * Per-profile balance row: positive = creditor (green), negative = debtor (red).
 * ProfileAvatar (24dp) rendered before the profile name.
 */
@Composable
private fun BalanceRow(
    name: String,
    profile: ProfileItem?,
    balance: Double,
    typography: Typography,
    themeColors: androidx.compose.material3.ColorScheme,
    monoFont: androidx.compose.ui.text.font.FontFamily,
) {
    val balanceColor = when {
        balance > 0.01 -> themeColors.tertiary // creditor = green
        balance < -0.01 -> themeColors.error   // debtor = red
        else -> themeColors.onSurfaceVariant     // zero = neutral
    }
    val balanceLabel = when {
        balance > 0.01 -> "Acreedor"
        balance < -0.01 -> "Deudor"
        else -> "Saldado"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (profile != null) {
                ProfileAvatar(
                    name = profile.name,
                    emoji = profile.icon,
                    photoUrl = profile.photoUrl,
                    size = 24.dp,
                )
            }
            Text(
                text = name,
                style = typography.bodySmall.copy(color = themeColors.onSurfaceVariant),
            )
            Text(
                text = "($balanceLabel)",
                style = typography.labelSmall.copy(
                    color = balanceColor,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
        Text(
            text = formatEuros(balance),
            style = typography.labelSmall.copy(
                fontFamily = monoFont,
                color = balanceColor,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}
