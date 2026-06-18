package com.cuentamorosos.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.displayNameFor
import com.cuentamorosos.model.formatEuros

/**
 * Profile card with avatar, name, balance, and balance-based badges.
 *
 * @param profile The profile data to display.
 * @param isOwnProfile Whether this profile belongs to the current user.
 * @param currentUid The current user's profile ID (used for display name resolution).
 * @param onClick Callback when the card is tapped.
 * @param modifier Optional modifier for layout positioning.
 */
@Composable
fun ProfileCard(
    profile: ProfileItem,
    isOwnProfile: Boolean,
    currentUid: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalNeoFintechColors.current
    val displayName = profile.displayNameFor(currentUid)
    val balanceColor by remember(profile.totalPendingEuros) {
        derivedStateOf {
            if (profile.totalPendingEuros >= 0) colors.primaryContainer else colors.error
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(NeoFintechElevation.cardShadowElevation, NeoFintechElevation.cardShadowShape, clip = false)
            .border(1.dp, colors.outlineVariant, NeoFintechShapes.lg)
            .clickable(onClick = onClick)
            .then(
                if (isOwnProfile) {
                    Modifier.border(
                        width = 2.dp,
                        color = colors.primaryContainer,
                        shape = NeoFintechShapes.lg,
                    )
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = NeoFintechShapes.lg,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProfileAvatar(name = profile.name, emoji = profile.icon, photoUrl = profile.photoUrl)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface,
                        )
                        if (!profile.username.isNullOrBlank()) {
                            Text(
                                text = "@${profile.username}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                    }
                    BalanceBadges(
                        balance = profile.totalPendingEuros,
                        isOwnProfile = isOwnProfile,
                    )
                }

                Text(
                    text = formatEuros(profile.totalPendingEuros),
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = JetBrainsMonoFontFamily(),
                    fontWeight = FontWeight.Bold,
                    color = balanceColor,
                )
            }
        }
    }
}

/**
 * Balance-based badges: "Te debe" / "Debes" / "Saldado".
 * Plus an optional "Tú" marker for own profiles.
 */
@Composable
private fun BalanceBadges(
    balance: Double,
    isOwnProfile: Boolean,
) {
    val colors = LocalNeoFintechColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (isOwnProfile) {
            BalanceBadge(text = "Tú", bgColor = colors.onSurfaceVariant.copy(alpha = 0.15f), textColor = colors.onSurfaceVariant)
        }
        if (balance > 0) {
            BalanceBadge(text = "Te debe", bgColor = colors.primaryContainer.copy(alpha = 0.15f), textColor = colors.primaryContainer)
        } else if (balance < 0) {
            BalanceBadge(text = "Debes", bgColor = colors.error.copy(alpha = 0.15f), textColor = colors.error)
        } else {
            BalanceBadge(text = "Saldado", bgColor = colors.onSurfaceVariant.copy(alpha = 0.15f), textColor = colors.onSurfaceVariant)
        }
    }
}

/**
 * Individual badge chip with small rounded corners (not pill).
 */
@Composable
private fun BalanceBadge(text: String, bgColor: androidx.compose.ui.graphics.Color, textColor: androidx.compose.ui.graphics.Color) {
    Surface(
        shape = NeoFintechShapes.sm,
        color = bgColor,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            textAlign = TextAlign.Center,
        )
    }
}
