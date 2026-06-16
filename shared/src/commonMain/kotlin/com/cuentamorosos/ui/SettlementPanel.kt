package com.cuentamorosos.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuentamorosos.model.EventDebtItem
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.EventState
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.displayNameFor
import com.cuentamorosos.model.formatEuros
import com.cuentamorosos.model.toCalculationSnapshot

/**
 * Formats a euro amount with comma as decimal separator (Spanish locale style).
 * E.g., 23.0 → "23,00", 13.5 → "13,50".
 */
private fun formatEuroAmount(amount: Double): String {
    val intPart = amount.toLong()
    val decPart = ((amount - intPart) * 100).toLong().let { if (it < 0) -it else it }
    val decStr = if (decPart < 10) "0$decPart" else "$decPart"
    return "$intPart,$decStr"
}

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
    eventState: EventState = EventState.OPEN,
    canClose: Boolean = false,
    onCloseEvent: (() -> Unit)? = null,
    onRemoveMember: ((String) -> Unit)? = null,
    lastCalculationSummary: String? = null,
) {
    val colors = LocalNeoFintechColors.current
    val themeColors = MaterialTheme.colorScheme

    // Deserialize persisted calculation snapshot (R008)
    val snapshot = remember(lastCalculationSummary) {
        lastCalculationSummary?.toCalculationSnapshot()
    }

    // Pre-compute profile name resolver from profiles list
    val profileNameById = remember(profiles) {
        profiles.associate { it.id to (it.name.ifBlank { it.id }) }
    }

    // Group transfers by debtor for display (R010)
    val debtorTransfers = remember(snapshot) {
        if (snapshot == null || snapshot.transfers.isEmpty()) {
            emptyMap()
        } else {
            snapshot.transfers
                .groupBy { it.fromProfileId }
                .mapValues { (_, transfers) ->
                    val total = transfers.sumOf { it.amount }
                    val creditors = transfers.map { t -> t.toProfileId to t.amount }
                    total to creditors
                }
        }
    }

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
                        contentColor = colors.onPrimaryContainer,
                    ),
                    shape = NeoFintechShapes.lg,
                ) {
                    Text(
                        text = "Calcular Totales",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }

                // Close Event button — visible only for CALCULATED events
                if (eventState == EventState.CALCULATED && canClose && onCloseEvent != null) {
                    Button(
                        onClick = onCloseEvent,
                        enabled = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                        shape = NeoFintechShapes.lg,
                    ) {
                        Text(
                            text = "Cerrar evento",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }

                HorizontalDivider(color = themeColors.outlineVariant.copy(alpha = 0.3f))

                // ── Total event cost (R009) — read-only, from snapshot ─────────
                if (snapshot != null) {
                    Text(
                        text = "Coste total: ${formatEuroAmount(snapshot.totalExpense)} €",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.onSurface,
                    )

                    HorizontalDivider(color = themeColors.outlineVariant.copy(alpha = 0.2f))
                }

                // Participants Status
                Text(
                    text = "Estado de participantes",
                    style = MaterialTheme.typography.labelSmall,
                    color = themeColors.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )

                // Participants list (from event.effectiveMemberIds)
                val eventMembers = profiles.filter { it.id in _event.effectiveMemberIds }
                if (eventMembers.isNotEmpty()) {
                    Text(
                        text = "Participantes (${eventMembers.size})",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = themeColors.onSurface,
                    )
                    eventMembers.forEach { profile ->
                        val profileDebts = debts.filter { it.profileId == profile.id }
                        val totalOwed = profileDebts.filter { !it.paid }.sumOf { it.amountEuros }
                        val hasDebt = profileDebts.isNotEmpty()
                        val isPaid = profileDebts.all { it.paid } && profileDebts.isNotEmpty()
                        val debt = profileDebts.firstOrNull()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .then(
                                    if (hasDebt) Modifier.clickable { debt?.let { onTogglePaid(it) } }
                                    else Modifier
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                // Checkbox (only if has debt)
                                if (hasDebt) {
                                    Checkbox(
                                        checked = isPaid,
                                        onCheckedChange = { debt?.let { onTogglePaid(it) } },
                                    )
                                }

                                ProfileAvatar(
                                    name = profile.name,
                                    emoji = profile.icon,
                                    photoUrl = profile.photoUrl,
                                    size = 32.dp,
                                )
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            text = profile.displayNameFor(currentUserUid),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = themeColors.onSurface,
                                        )
                                        if (profile.id == currentUserUid && currentUserUid.isNotBlank()) {
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
                                    if (totalOwed > 0.0) {
                                        Text(
                                            text = "Debe: ${formatEuros(totalOwed)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.error,
                                            fontWeight = FontWeight.Medium,
                                            fontFamily = JetBrainsMonoFontFamily(),
                                        )
                                    } else if (isPaid) {
                                        Text(
                                            text = "Pagado ✓",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.primaryContainer,
                                            fontWeight = FontWeight.Medium,
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                if (onRemoveMember != null && profile.id != currentUserUid) {
                                    IconButton(
                                        onClick = { onRemoveMember(profile.id) },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Eliminar participante",
                                            tint = colors.error,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = themeColors.outlineVariant.copy(alpha = 0.2f))
                    }
                }

                if (debts.isEmpty() && eventMembers.isEmpty()) {
                    Text(
                        text = "Sin participantes aún",
                        style = MaterialTheme.typography.bodySmall,
                        color = themeColors.onSurfaceVariant,
                    )
                }

                // ── Transfer details from persisted calculation (R008, R010) ──
                if (snapshot != null && debtorTransfers.isNotEmpty()) {
                    HorizontalDivider(color = themeColors.outlineVariant.copy(alpha = 0.3f))

                    Text(
                        text = "Transferencias sugeridas",
                        style = MaterialTheme.typography.labelSmall,
                        color = themeColors.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                    )

                    debtorTransfers.forEach { (debtorId, pair) ->
                        val (total, creditors) = pair
                        val debtorName = profileNameById[debtorId] ?: debtorId
                        val parts = creditors.joinToString(", ") { (creditorId, amount) ->
                            val credName = profileNameById[creditorId] ?: creditorId
                            "${formatEuroAmount(amount)} a $credName"
                        }
                        Text(
                            text = "$debtorName debe ${formatEuroAmount(total)}€ ($parts)",
                            style = MaterialTheme.typography.bodySmall,
                            color = themeColors.onSurface,
                            fontWeight = FontWeight.Normal,
                        )
                    }
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
