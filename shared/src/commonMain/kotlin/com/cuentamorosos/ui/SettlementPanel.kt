package com.cuentamorosos.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuentamorosos.model.EventDebtItem
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.EventRole
import com.cuentamorosos.model.EventState
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.displayNameFor
import com.cuentamorosos.model.formatEuros
import com.cuentamorosos.model.toCalculationSnapshot

/**
 * Checkbox state for a profile's debts in the settlement panel.
 */
data class ProfileCheckState(
    val hasDebt: Boolean,
    val isPaid: Boolean,
)

/**
 * Computes the display state of a profile's checkbox from their debts.
 * A profile "has debt" if the list is non-empty. A profile is "paid"
 * only when ALL their debts are paid.
 */
internal fun computeProfileCheckState(
    profileDebts: List<EventDebtItem>,
): ProfileCheckState {
    val hasDebt = profileDebts.isNotEmpty()
    val isPaid = hasDebt && profileDebts.all { it.paid }
    return ProfileCheckState(hasDebt, isPaid)
}

/**
 * Whether the checkbox should be shown for a profile.
 * Visible only when the profile has debts AND the event is not OPEN.
 */
internal fun computeShouldShowCheckbox(
    profileDebts: List<EventDebtItem>,
    eventState: EventState,
): Boolean {
    return profileDebts.isNotEmpty() && eventState != EventState.OPEN
}

/**
 * Computes the list of debts after toggling ALL profile debts atomically.
 * Uses an immutable snapshot of the list to prevent concurrent modification issues.
 *
 * The returned debts have the new paid state pre-computed. The caller
 * iterates and calls onTogglePaid for each one.
 *
 * @return A snapshot-based list where each debt has `paid = !isAllPaid`.
 *         Empty list when [profileDebts] is empty.
 */
internal fun computeMultiDebtToggleActions(
    profileDebts: List<EventDebtItem>,
): List<EventDebtItem> {
    if (profileDebts.isEmpty()) return emptyList()
    val snapshot = profileDebts.toList()
    val isAllPaid = snapshot.all { it.paid }
    val newPaid = !isAllPaid
    return snapshot.map { it.copy(paid = newPaid) }
}

/**
 * Texto que resume el alcance de una marca: cuántas deudas abarca y por cuánto.
 *
 * Existe porque [computeMultiDebtToggleActions] marca **todas** las deudas del
 * perfil de una sola vez. Un toque puede cambiar tres registros, y hasta ahora
 * la pantalla no lo decía en ninguna parte.
 */
internal fun settlementScopeLabel(debtCount: Int, total: Double): String =
    if (debtCount == 1) "1 deuda · ${formatEuros(total)}"
    else "$debtCount deudas · ${formatEuros(total)}"

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
    onViewReceipt: () -> Unit = {},
    onHowCalculated: () -> Unit = {},
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

                // Calculate + Receipt button row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Calculate Totals button
                    Button(
                        onClick = onCalculateTotals,
                        enabled = canCalculate,
                        modifier = Modifier.weight(1f),
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

                    // Receipt button — circular, visible only when event is CALCULATED or CLOSED
                    val receiptEnabled = (eventState == EventState.CALCULATED || eventState == EventState.CLOSED) && snapshot != null
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                    ) {
                        IconButton(
                            onClick = onViewReceipt,
                            enabled = receiptEnabled,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = "Ver recibo del evento",
                                tint = if (receiptEnabled) colors.primaryContainer
                                       else themeColors.onSurfaceVariant.copy(alpha = 0.38f),
                            )
                        }
                    }

                    // How calculated button — circular, visible only when trace exists
                    val currentSnapshot = snapshot
                    val howCalculatedEnabled = (eventState == EventState.CALCULATED || eventState == EventState.CLOSED) &&
                        currentSnapshot != null && currentSnapshot.trace.isNotEmpty()
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                    ) {
                        IconButton(
                            onClick = onHowCalculated,
                            enabled = howCalculatedEnabled,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "¿Cómo se calculó esto?",
                                tint = if (howCalculatedEnabled) colors.primaryContainer
                                       else themeColors.onSurfaceVariant.copy(alpha = 0.38f),
                            )
                        }
                    }
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
                        val checkState = computeProfileCheckState(profileDebts)
                        val showCheckbox = computeShouldShowCheckbox(profileDebts, eventState)
                        val participantRole = _event.participants.firstOrNull { it.profileId == profile.id }?.role
                            ?: if (profile.id == _event.ownerId) EventRole.OWNER else EventRole.READER

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .then(
                                    if (showCheckbox) Modifier.clickable {
                                        val toggled = computeMultiDebtToggleActions(profileDebts)
                                        toggled.forEach { onTogglePaid(it) }
                                    }
                                    else Modifier
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                // La marca abarca TODAS las deudas del perfil
                                // (computeMultiDebtToggleActions), no solo una.
                                // El clic lo maneja la fila entera, así que la
                                // casilla es aquí un indicador.
                                if (showCheckbox) {
                                    CheckMark(
                                        checked = checkState.isPaid,
                                        checkedColor = colors.primaryContainer,
                                        uncheckedColor = themeColors.onSurfaceVariant,
                                        markColor = colors.onPrimaryContainer,
                                    )
                                }

                                ProfileAvatar(
                                    name = profile.name,
                                    photoUrl = profile.photoUrl,
                                    size = 32.dp,
                                )
                                Column(modifier = Modifier.animateContentSize(NeoFintechMotion.resize)) {
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
                                        RoleBadge(
                                            role = participantRole,
                                            themeColors = themeColors,
                                        )
                                    }
                                    ParticipantDebtStatus(
                                        isPaid = checkState.isPaid,
                                        totalOwed = totalOwed,
                                        debtCount = profileDebts.size,
                                        scopeTotal = profileDebts.sumOf { it.amountEuros },
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                if (onRemoveMember != null && profile.id != currentUserUid && canManageParticipants) {
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

                    // El grafo no calcula nada: dibuja lo que ya decidió
                    // SettlementEngine, para que se vea por qué las
                    // transferencias son esas y no otras.
                    SettlementGraph(
                        transfers = snapshot.transfers,
                        nameById = profileNameById,
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

// ── RoleBadge ──────────────────────────────────────────────────────────────────

/**
 * Small colored pill showing the participant's role in the event.
 */
@Composable
private fun RoleBadge(
    role: EventRole,
    themeColors: androidx.compose.material3.ColorScheme,
) {
    val (label, containerColor) = when (role) {
        EventRole.OWNER -> Pair(
            "Dueño",
            themeColors.primary,
        )
        EventRole.CONTRIBUTOR -> Pair(
            "Colaborador",
            themeColors.secondary,
        )
        EventRole.READER -> Pair(
            "Lector",
            themeColors.tertiary,
        )
    }

    Surface(
        color = containerColor.copy(alpha = 0.15f),
        shape = NeoFintechShapes.full,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = containerColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
        )
    }
}


/**
 * Línea de estado de un participante dentro de la liquidación.
 *
 * "Debe: X" y "Pagado ✓" no se sustituyen: la que sale se va hacia arriba y la
 * que entra llega por abajo, para que el cambio se lea como una transición y no
 * como un texto distinto que apareció ahí.
 *
 * Debajo, cuando está marcado, aparece el alcance real de esa marca.
 */
@Composable
private fun ParticipantDebtStatus(
    isPaid: Boolean,
    totalOwed: Double,
    debtCount: Int,
    scopeTotal: Double,
) {
    val colors = LocalNeoFintechColors.current

    AnimatedContent(
        targetState = isPaid,
        transitionSpec = {
            val fade = tween<Float>(NeoFintechMotion.SHORT_MS, easing = NeoFintechMotion.standard)
            val enter = slideInVertically(NeoFintechMotion.placement) { it } + fadeIn(fade)
            val exit = slideOutVertically(NeoFintechMotion.placement) { -it } + fadeOut(fade)
            enter togetherWith exit
        },
        label = "participantDebtStatus",
    ) { paid ->
        when {
            paid -> Text(
                text = "Pagado ✓",
                style = MaterialTheme.typography.bodySmall,
                color = colors.primaryContainer,
                fontWeight = FontWeight.Medium,
            )
            totalOwed > 0.0 -> Text(
                text = "Debe: ${formatEuros(totalOwed)}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.error,
                fontWeight = FontWeight.Medium,
                fontFamily = JetBrainsMonoFontFamily(),
            )
            else -> Box(modifier = Modifier)
        }
    }

    AnimatedVisibility(
        visible = isPaid && debtCount > 0,
        enter = expandVertically(NeoFintechMotion.resize) + fadeIn(
            tween(NeoFintechMotion.SHORT_MS, easing = NeoFintechMotion.standard),
        ),
        exit = shrinkVertically(NeoFintechMotion.resize) + fadeOut(
            tween(NeoFintechMotion.QUICK_MS, easing = NeoFintechMotion.standard),
        ),
    ) {
        Text(
            text = settlementScopeLabel(debtCount, scopeTotal),
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant,
            fontFamily = JetBrainsMonoFontFamily(),
        )
    }
}
