package com.cuentamorosos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.formatEuros

/**
 * Per-profile breakdown display with monospace amounts.
 * Shows avatar emoji + name + amount (JetBrains Mono, neon green).
 * Total row at bottom with separator above.
 */
@Composable
fun PreviewBreakdown(
    profiles: List<ProfileItem>,
    amounts: List<Double>,
    summary: String,
) {
    val colors = NeoFintechColors.light()
    val shapes = NeoFintechShapes
    val typography = NeoFintechTypography()
    val monoFont = JetBrainsMonoFontFamily()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceContainerLow,
        ),
        shape = shapes.lg,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Vista previa por perfil",
                style = typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )

            profiles.zip(amounts).forEach { (profile, amount) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Avatar circle
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(NeoFintechShapes.full)
                                .background(colors.secondary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = profile.icon,
                                fontSize = 14.sp,
                            )
                        }
                        Text(
                            text = profile.name,
                            style = typography.bodyMedium,
                        )
                    }
                    Text(
                        text = formatEuros(amount),
                        style = typography.labelSmall.copy(
                            fontFamily = monoFont,
                            color = colors.primaryContainer,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = colors.outlineVariant,
            )

            // Summary row
            Text(
                text = summary,
                style = typography.bodySmall.copy(color = colors.onSurfaceVariant),
            )
        }
    }
}
