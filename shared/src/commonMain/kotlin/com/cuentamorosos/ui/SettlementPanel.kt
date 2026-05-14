package com.cuentamorosos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuentamorosos.model.EventDebtItem
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.formatEuros

@Composable
fun SettlementPanel(
    event: EventItem,
    debts: List<EventDebtItem>,
    profiles: List<ProfileItem>,
    pendingTotal: Double,
    expenseTotal: Double,
    onCalculateTotals: () -> Unit,
    onTogglePaid: (EventDebtItem) -> Unit,
    onAddProfile: () -> Unit,
    onInviteMember: () -> Unit,
) {
    val profileById = profiles.associateBy { it.id }
    val pendingDebts = debts.filter { !it.paid }
    val paidDebts = debts.filter { it.paid }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Total cost card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Costo total del evento",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatEuros(expenseTotal),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeoFintechColors.dark().primaryContainer,
                )
                Text(
                    text = "Pendiente: ${formatEuros(pendingTotal)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (pendingTotal > 0) Color(0xFFFFB4AB) else NeoFintechColors.dark().primaryContainer,
                )
            }
        }

        // Calculate button
        Button(
            onClick = onCalculateTotals,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Calcular totales")
        }

        // Participants section
        Text(
            text = "Participantes (${debts.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        // Pending
        if (pendingDebts.isNotEmpty()) {
            Text(
                text = "Pendientes",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFFB4AB),
                fontWeight = FontWeight.Medium,
            )
            pendingDebts.forEach { debt ->
                val profile = profileById[debt.profileId]
                DebtRow(
                    profile = profile,
                    debt = debt,
                    onTogglePaid = onTogglePaid,
                )
            }
        }

        // Paid
        if (paidDebts.isNotEmpty()) {
            Text(
                text = "Han pagado",
                style = MaterialTheme.typography.bodyMedium,
                color = NeoFintechColors.dark().primaryContainer,
                fontWeight = FontWeight.Medium,
            )
            paidDebts.forEach { debt ->
                val profile = profileById[debt.profileId]
                DebtRow(
                    profile = profile,
                    debt = debt,
                    onTogglePaid = onTogglePaid,
                )
            }
        }

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onAddProfile,
                modifier = Modifier.weight(1f),
            ) {
                Text("Añadir perfil")
            }
            Button(
                onClick = onInviteMember,
                modifier = Modifier.weight(1f),
            ) {
                Text("Invitar")
            }
        }
    }
}

@Composable
private fun DebtRow(
    profile: ProfileItem?,
    debt: EventDebtItem,
    onTogglePaid: (EventDebtItem) -> Unit,
) {
    val isHighDebt = debt.amountEuros > 50.0
    val debtColor = if (isHighDebt) Color(0xFFFFB4AB) else NeoFintechColors.dark().primaryContainer

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTogglePaid(debt) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = debt.paid,
                onCheckedChange = { onTogglePaid(debt) },
            )
            // Avatar
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(NeoFintechShapes.full)
                    .background(NeoFintechColors.dark().secondary.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = profile?.name?.take(1)?.uppercase() ?: "?",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = profile?.name ?: "Desconocido",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            text = formatEuros(debt.amountEuros),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            fontFamily = JetBrainsMonoFontFamily(),
            color = debtColor,
        )
    }
    HorizontalDivider()
}
