package com.cuentamorosos.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp

@Composable
fun AlertAccordionCard(
    alert: SmartAlert,
    onTap: () -> Unit,
    initiallyExpanded: Boolean = false,
) {
    val colors = LocalNeoFintechColors.current
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    val (iconBgColor, iconColor) = when (alert.type) {
        AlertType.NO_PARTICIPANTS -> colors.errorContainer to colors.onErrorContainer
        AlertType.NO_EXPENSES -> colors.surfaceContainerHigh to colors.onSurfaceVariant
        AlertType.PENDING_CALCULATIONS -> colors.tertiaryContainer to colors.onTertiaryContainer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLowest),
        shape = NeoFintechShapes.md,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(iconBgColor, NeoFintechShapes.full),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = alert.icon,
                        style = MaterialTheme.typography.bodyLarge,
                        color = iconColor,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = alert.message,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                        color = colors.onSurface,
                    )
                }
                Text(
                    text = if (expanded) "\u25BC" else "\u25B6",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }

            // Collapsible body
            if (expanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onTap)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Tocar para ver detalles",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                    Text(
                        text = "\u25B6",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
