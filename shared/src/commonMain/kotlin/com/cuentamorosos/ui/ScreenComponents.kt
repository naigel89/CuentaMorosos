package com.cuentamorosos.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cuentamorosos.data.ReminderMessage

// ── StatusCard ────────────────────────────────────────────────────────────────

@Composable
fun StatusCard(
    title: String,
    message: String,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(NeoFintechElevation.cardShadowElevation, NeoFintechElevation.cardShadowShape, clip = false)
            .border(1.dp, colors.outlineVariant, NeoFintechShapes.lg),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = NeoFintechShapes.lg,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// ── SuggestionCard ────────────────────────────────────────────────────────────

@Composable
fun SuggestionCard(message: String) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(NeoFintechElevation.cardShadowElevation, NeoFintechElevation.cardShadowShape, clip = false)
            .border(1.dp, colors.outlineVariant, NeoFintechShapes.lg),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = NeoFintechShapes.lg,
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(24.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// ── EmptyState ────────────────────────────────────────────────────────────────

@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    title: String,
    message: String,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(vertical = 48.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── ReminderSummaryCard ───────────────────────────────────────────────────────

@Composable
fun ReminderSummaryCard(reminders: List<ReminderMessage>) {
    var isVisible by remember { mutableStateOf(true) }
    if (!isVisible) return

    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(NeoFintechElevation.cardShadowElevation, NeoFintechElevation.cardShadowShape, clip = false)
            .border(1.dp, colors.outlineVariant, NeoFintechShapes.lg),
        colors = CardDefaults.cardColors(
            containerColor = colors.primaryContainer.copy(alpha = 0.7f)
        ),
        shape = NeoFintechShapes.lg,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🔔 Recordatorios activos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                }
                TextButton(onClick = { isVisible = false }) {
                    Text("Ocultar", style = MaterialTheme.typography.labelSmall)
                }
            }
            reminders.take(5).forEach { reminder ->
                Text(
                    text = "• ${reminder.title}: ${reminder.body}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurface
                )
            }
        }
    }
}
