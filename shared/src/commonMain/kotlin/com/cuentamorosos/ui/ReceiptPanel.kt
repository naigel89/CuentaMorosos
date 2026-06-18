package com.cuentamorosos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuentamorosos.formatDateMillis
import com.cuentamorosos.model.CalculationSnapshot
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.formattedDate
import kotlinx.coroutines.launch

/**
 * Full receipt breakdown panel shown as a ModalBottomSheet.
 * Displays event info, total, per-profile balances, settlement transfers,
 * and calculation metadata from a persisted [CalculationSnapshot].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptPanel(
    event: EventItem,
    snapshot: CalculationSnapshot,
    profiles: List<ProfileItem>,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val colors = LocalNeoFintechColors.current
    val typography = NeoFintechTypography()
    val profileNameResolver: (String) -> String = { id ->
        profiles.find { it.id == id }?.name ?: id
    }

    fun dismiss() {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Header ──────────────────────────────────────────────────────
            Text(
                text = "🧾 Recibo del Evento",
                style = typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.onSurface,
            )

            Text(
                text = event.name,
                style = typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = colors.primaryContainer,
            )

            Text(
                text = "📅 ${event.formattedDate()}",
                style = typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )

            HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.3f))

            // ── Total ───────────────────────────────────────────────────────
            TotalHero(snapshot = snapshot)

            HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.3f))

            // ── Per-profile balances ────────────────────────────────────────
            if (snapshot.participantBalances.isNotEmpty()) {
                SectionLabel(text = "Saldos por perfil")

                val maxAbs = snapshot.participantBalances.values
                    .maxOfOrNull { kotlin.math.abs(it) }
                    ?.takeIf { it > 0.0 } ?: 1.0

                snapshot.participantBalances.forEach { (profileId, balance) ->
                    val name = profileNameResolver(profileId)
                    val profile = profiles.find { it.id == profileId }
                    BalanceRow(
                        name = name,
                        profile = profile,
                        balance = balance,
                        maxAbs = maxAbs,
                        currentProfileId = null,
                        cardBg = colors.surface,
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.3f))
            }

            // ── Settlement transfers ────────────────────────────────────────
            if (snapshot.transfers.isNotEmpty()) {
                SectionLabel(text = "Transferencias")

                snapshot.transfers.forEachIndexed { index, transfer ->
                    TransferRow(
                        transfer = transfer,
                        profileNameResolver = profileNameResolver,
                        profiles = profiles,
                        isPaid = false,
                        onTogglePaid = {},
                        index = index,
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.3f))
            }

            // ── Calculation mode and timestamp ─────────────────────────────
            SectionLabel(text = "Metadatos del cálculo")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Modo de cálculo",
                    style = typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
                Text(
                    text = event.lastCalculationMode ?: "—",
                    style = typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.onSurface,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Calculado el",
                    style = typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
                Text(
                    text = formatDateMillis(snapshot.calculatedAtMillis),
                    style = typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.onSurface,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Close button ────────────────────────────────────────────────
            Button(
                onClick = { dismiss() },
                modifier = Modifier.fillMaxWidth(),
                shape = NeoFintechShapes.lg,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primaryContainer,
                    contentColor = colors.onPrimaryContainer,
                ),
            ) {
                Text(
                    text = "Cerrar",
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

