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
import androidx.compose.material3.Surface
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

@Suppress("UNUSED_PARAMETER")
@Composable
fun SettlementPanel(
    _event: EventItem,
    debts: List<EventDebtItem>,
    profiles: List<ProfileItem>,
    _pendingTotal: Double,
    _expenseTotal: Double,
    currentUserUid: String = "",
    onCalculateTotals: () -> Unit,
    onTogglePaid: (EventDebtItem) -> Unit,
    onAddProfile: () -> Unit,
    onInviteMember: () -> Unit,
    canCalculate: Boolean = true,
    canManageParticipants: Boolean = true,
    canInvite: Boolean = true,
) {
    val profileById = profiles.associateBy { it.id }
    val pendingDebts = debts.filter { !it.paid }
    val paidDebts = debts.filter { it.paid }
    val colors = LocalNeoFintechColors.current
    val themeColors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Settlement card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .cardShadow(),
            colors = CardDefaults.cardColors(containerColor = themeColors.surfaceContainerLowest),
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
                    color = themeColors.onSurface,
                )

                // Calculate Totals button (full width, neon green, bold)
                Button(
                    onClick = onCalculateTotals,
                    enabled = canCalculate,
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

                HorizontalDivider(color = themeColors.outlineVariant.copy(alpha = 0.3f))

                // Participants Status
                Text(
                    text = "Estado de participantes",
                    style = MaterialTheme.typography.labelSmall,
                    color = themeColors.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )

                // Pending
                if (pendingDebts.isNotEmpty()) {
                    pendingDebts.forEach { debt ->
                        val profile = profileById[debt.profileId]
                        DebtRow(
                            profile = profile,
                            debt = debt,
                            currentUserUid = currentUserUid,
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
                            currentUserUid = currentUserUid,
                            onTogglePaid = onTogglePaid,
                            isPaid = true,
                        )
                    }
                }

                if (debts.isEmpty()) {
                    Text(
                        text = "Sin participantes aún",
                        style = MaterialTheme.typography.bodySmall,
                        color = themeColors.onSurfaceVariant,
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
                enabled = canManageParticipants,
                modifier = Modifier.weight(1f),
                shape = NeoFintechShapes.md,
            ) {
                Text("Añadir perfil")
            }
            Button(
                onClick = onInviteMember,
                enabled = canInvite,
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
    currentUserUid: String,
    onTogglePaid: (EventDebtItem) -> Unit,
    isPaid: Boolean,
) {
    val colors = LocalNeoFintechColors.current
    val themeColors = MaterialTheme.colorScheme
    val isCurrentUser = debt.profileId == currentUserUid && currentUserUid.isNotBlank()
    val displayName = when {
        profile != null -> profile.name
        isCurrentUser -> "Vos"
        else -> "Desconocido"
    }
    val initials = profile?.name?.take(2)?.uppercase() ?: if (isCurrentUser) "V" else "?"

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
                        if (isPaid) themeColors.secondary.copy(alpha = 0.3f)
                        else colors.primaryContainer.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.onSurface,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = themeColors.onSurface,
                )
                if (isCurrentUser) {
                    Surface(
                        color = themeColors.primaryContainer.copy(alpha = 0.2f),
                        shape = NeoFintechShapes.full,
                    ) {
                        Text(
                            text = "Tú",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    }
                }
            }
        }
        Text(
            text = formatEuros(debt.amountEuros),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            fontFamily = JetBrainsMonoFontFamily(),
            color = if (isPaid) themeColors.onSurfaceVariant else colors.error,
        )
    }
    HorizontalDivider(color = themeColors.outlineVariant.copy(alpha = 0.2f))
}
