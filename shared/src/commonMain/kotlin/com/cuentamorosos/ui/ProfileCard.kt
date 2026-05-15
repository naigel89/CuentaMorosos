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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.formatEuros

/**
 * Profile card with avatar, name, balance, and state badges.
 *
 * @param profile The profile data to display.
 * @param isOwnProfile Whether this profile belongs to the current user.
 * @param onClick Callback when the card is tapped.
 * @param modifier Optional modifier for layout positioning.
 */
@Composable
fun ProfileCard(
    profile: ProfileItem,
    isOwnProfile: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NeoFintechColors.dark()
    val balanceColor by remember(profile.totalPendingEuros) {
        derivedStateOf {
            if (profile.totalPendingEuros >= 0) colors.primaryContainer else colors.error
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (isOwnProfile) {
                    Modifier.border(
                        width = 4.dp,
                        color = colors.primaryContainer,
                        shape = NeoFintechShapes.lg,
                    )
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProfileAvatar(emoji = profile.icon)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface,
                    )
                    ProfileBadges(
                        isOwnProfile = isOwnProfile,
                        isGhost = profile.isGhost,
                        hasLinkedEmail = profile.linkedEmail != null,
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
 * Row of state badges for a profile card.
 */
@Composable
private fun ProfileBadges(
    isOwnProfile: Boolean,
    isGhost: Boolean,
    hasLinkedEmail: Boolean,
) {
    val colors = NeoFintechColors.dark()
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (isOwnProfile) {
            ProfileBadge(text = "Tú", color = colors.primaryContainer)
        }
        if (isGhost) {
            ProfileBadge(text = "Local", color = colors.secondary)
        }
        if (hasLinkedEmail) {
            ProfileBadge(text = "Vinculado", color = colors.tertiaryContainer)
        }
    }
}

/**
 * Individual badge chip.
 */
@Composable
private fun ProfileBadge(text: String, color: androidx.compose.ui.graphics.Color) {
    Surface(
        shape = NeoFintechShapes.sm,
        color = color.copy(alpha = 0.15f),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            textAlign = TextAlign.Center,
        )
    }
}
