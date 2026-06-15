package com.cuentamorosos.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.formatEuros

/**
 * Per-profile breakdown display with monospace amounts in a table layout.
 * Each row has: ProfileAvatar (32dp, photo or initial fallback) + name + amount.
 * Dividers between rows. Total row at bottom with bold label + separator above.
 */
@Composable
fun PreviewBreakdown(
    profiles: List<ProfileItem>,
    amounts: List<Double>,
    summary: String,
) {
    val themeColors = MaterialTheme.colorScheme
    val shapes = NeoFintechShapes
    val typography = NeoFintechTypography()
    val monoFont = JetBrainsMonoFontFamily()

    Card(
        modifier = Modifier
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
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Vista previa por perfil",
                style = typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )

            profiles.zip(amounts).forEachIndexed { index, (profile, amount) ->
                // Divider between rows (not before first)
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = themeColors.outlineVariant,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ProfileAvatar(
                            name = profile.name,
                            emoji = profile.icon,
                            photoUrl = profile.photoUrl,
                            size = 32.dp,
                        )
                        Text(
                            text = profile.name,
                            style = typography.bodyMedium,
                        )
                    }
                    Text(
                        text = formatEuros(amount),
                        style = typography.headlineMedium.copy(
                            fontFamily = monoFont,
                            color = themeColors.primary,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }

            // Total separator + summary row
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = themeColors.onSurfaceVariant,
                thickness = 2.dp,
            )

            Text(
                text = "Total: $summary",
                style = typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = themeColors.onSurface,
                ),
            )
        }
    }
}
