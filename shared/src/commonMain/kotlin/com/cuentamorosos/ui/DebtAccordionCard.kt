package com.cuentamorosos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuentamorosos.model.formatEuros

// ── Color tokens for the avatar circles ─────────────────────────────────────
// These are intentional absolute colours to guarantee the "semáforo visual"
// effect the design requires, regardless of light/dark theme.
private val OwedToYouAvatarBg = Color(0xFF1B5E20)  // dark green
private val OwedToYouAvatarFg = Color(0xFF00C853)  // bright green
private val YouOweAvatarBg = Color(0xFFB71C1C)     // dark red
private val YouOweAvatarFg = Color(0xFFEF5350)     // red/salmon

/**
 * Unified debt card that shows ALL profiles with active balances in a single list.
 *
 * Design (left to right):
 *   [Avatar circle with initial]  Name + subtitle  |  +Amount/-Amount  >
 *
 * Each row is tappable — delegates to [onProfileTap] with the profile ID.
 * Zero-balance items are hidden (filtered upstream), and a hint is shown when
 * the list is empty.
 */
@Composable
fun UnifiedDebtsCard(
    modifier: Modifier = Modifier,
    items: List<UnifiedDebtItem>,
    onProfileTap: (String) -> Unit = {},
) {
    val colors = LocalNeoFintechColors.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLowest),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ── Header ──
            Text(
                text = "Saldos por Perfil",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
                modifier = Modifier.padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 4.dp),
            )

            if (items.isEmpty()) {
                EmptyStateHint(modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp))
            } else {
                // ── Rows ──
                items.forEachIndexed { index, item ->
                    UnifiedDebtRow(
                        item = item,
                        onProfileTap = onProfileTap,
                        staggerIndex = index,
                    )
                    if (index < items.lastIndex) {
                        ThinDivider(
                            color = colors.outlineVariant,
                            modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                        )
                    }
                }
            }

            // ── Educational footer ──
            if (items.isNotEmpty()) {
                Text(
                    text = "Solo se muestran los perfiles con saldos activos. Si ajustaste cuentas con alguien, su perfil desaparece de esta lista automáticamente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

// ── Single row ───────────────────────────────────────────────────────────────

@Composable
private fun UnifiedDebtRow(
    item: UnifiedDebtItem,
    onProfileTap: (String) -> Unit,
    staggerIndex: Int,
) {
    val colors = LocalNeoFintechColors.current
    val isOwedToYou = item.direction == DebtDirection.OWED_TO_YOU
    val avatarBg = if (isOwedToYou) OwedToYouAvatarBg else YouOweAvatarBg
    val avatarFg = if (isOwedToYou) OwedToYouAvatarFg else YouOweAvatarFg
    val amountColor = if (isOwedToYou) OwedToYouAvatarFg else YouOweAvatarFg
    val subtitle = if (isOwedToYou) "Te debe dinero" else "Le debes dinero"
    val amountPrefix = if (isOwedToYou) "+" else "-"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fadeInStaggered(index = staggerIndex)
            .clickable(onClick = { onProfileTap(item.profileName) })
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ── Avatar circle with initial ──
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(avatarBg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = item.profileName.first().uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = avatarFg,
            )
        }

        // ── Spacing ──
        Box(modifier = Modifier.width(12.dp))

        // ── Name + subtitle ──
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.profileName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = colors.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }

        // ── Amount with sign ──
        Text(
            text = "$amountPrefix${formatEuros(item.amount)}",
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = JetBrainsMonoFontFamily(),
            fontWeight = FontWeight.SemiBold,
            color = amountColor,
        )

        // ── Arrow indicator ──
        Text(
            text = "›",
            style = MaterialTheme.typography.headlineSmall,
            color = colors.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

// ── Empty / footer helpers ───────────────────────────────────────────────────

@Composable
private fun EmptyStateHint(modifier: Modifier = Modifier) {
    val colors = LocalNeoFintechColors.current
    Text(
        text = "No hay deudas pendientes con nadie. ¡Estás al día!",
        style = MaterialTheme.typography.bodyMedium,
        color = colors.onSurfaceVariant,
        modifier = modifier,
    )
}

/** Thin horizontal line used as a row separator. */
@Composable
private fun ThinDivider(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color.copy(alpha = 0.25f)),
    )
}
