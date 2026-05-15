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
import androidx.compose.material3.ButtonDefaults
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
    val colors = NeoFintechColors.light()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Settlement card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .cardShadow(),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLowest),
            shape = NeoFintechShapes.lg,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Liquidación",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                )

                // Calculate Totals button (full width, neon green, bold)
                Button(
                    onClick = onCalculateTotals,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primaryContainer,
                        contentColor = colors.onSurface,
                    ),
                    shape = NeoFintechShapes.lg,
                ) {
                    Text(
                        text = "Calcular Totales",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }

                HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.3f))

                // Participants Status
                Text(
                    text = "Estado de participantes",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )

                // Pending
                if (pendingDebts.isNotEmpty()) {
                    pendingDebts.forEach { debt ->
                        val profile = profileById[debt.profileId]
                        DebtRow(
                            profile = profile,
                            debt = debt,
                            onTogglePaid = onTogglePaid,
                            isPaid = false,
                        )
                    }
                }

                // Paid
                if (paidDebts.isNotEmpty()) {
                    paidDebts.forEach { debt ->
                        val profile = profileById[debt.profileId]
                        DebtRow(
                            profile = profile,
                            debt = debt,
                            onTogglePaid = onTogglePaid,
                            isPaid = true,
                        )
                    }
                }

                if (debts.isEmpty()) {
                    Text(
                        text = "Sin participantes aún",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
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
                shape = NeoFintechShapes.md,
            ) {
                Text("Añadir perfil")
            }
            Button(
                onClick = onInviteMember,
                modifier = Modifier.weight(1f),
                shape = NeoFintechShapes.md,
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
    isPaid: Boolean,
) {
    val colors = NeoFintechColors.light()
    val initials = profile?.name?.take(2)?.uppercase() ?: "?"

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
            // Avatar with initials
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(NeoFintechShapes.full)
                    .background(
                        if (isPaid) colors.secondary.copy(alpha = 0.3f)
                        else colors.primaryContainer.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                )
            }
            Text(
                text = profile?.name ?: "Desconocido",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface,
            )
        }
        Text(
            text = formatEuros(debt.amountEuros),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            fontFamily = JetBrainsMonoFontFamily(),
            color = if (isPaid) colors.onSurfaceVariant else colors.error,
        )
    }
    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.2f))
}
