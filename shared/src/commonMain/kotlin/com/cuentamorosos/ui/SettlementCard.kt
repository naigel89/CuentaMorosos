package com.cuentamorosos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import com.cuentamorosos.model.SettlementTransfer
import com.cuentamorosos.model.formatEuros

/**
 * Settlement transfer suggestions card.
 * Shows "From → To: amount" with arrow emoji and monospace amounts.
 * Includes transfer count badge.
 */
@Composable
fun SettlementCard(
    transfers: List<SettlementTransfer>,
) {
    val colors = NeoFintechColors.light()
    val shapes = NeoFintechShapes
    val typography = NeoFintechTypography()
    val monoFont = JetBrainsMonoFontFamily()

    if (transfers.isEmpty()) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = NeoFintechElevation.cardShadowElevation,
                shape = NeoFintechElevation.cardShadowShape,
                clip = false,
            ),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = shapes.lg,
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Liquidación sugerida",
                    style = typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Badge(
                    containerColor = colors.tertiaryContainer,
                ) {
                    Text(
                        text = "${transfers.size}",
                        style = typography.labelSmall,
                    )
                }
            }

            Text(
                text = "Transferencias mínimas necesarias:",
                style = typography.bodySmall.copy(color = colors.onSurfaceVariant),
            )

            transfers.forEach { transfer ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "${transfer.fromName} \u2192 ${transfer.toName}",
                        style = typography.bodyMedium,
                    )
                    Text(
                        text = formatEuros(transfer.amount),
                        style = typography.labelSmall.copy(
                            fontFamily = monoFont,
                            color = colors.primaryContainer,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }
        }
    }
}
