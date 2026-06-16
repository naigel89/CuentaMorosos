package com.cuentamorosos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuentamorosos.model.CalculationSnapshot
import com.cuentamorosos.model.CalculationStatus
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.formatEuros

// ─── Design tokens ────────────────────────────────────────────────────────────

private val SurfaceCard    = Color(0xFF181C27)
private val SurfaceRow     = Color(0xFF1E2335)
private val SurfaceTrack   = Color(0xFF252B40)
private val BorderSubtle   = Color(0xFF2A2F45)
private val BorderEmphasis = Color(0xFF363D5A)
private val TextPrimary    = Color(0xFFF0F2F8)
private val TextSecondary  = Color(0xFF8B91A8)
private val TextHint       = Color(0xFF555E7A)
private val GreenAccent    = Color(0xFF3DFFA0)
private val GreenBg        = Color(0x143DFFA0)
private val GreenBorder    = Color(0x333DFFA0)
private val RedAccent      = Color(0xFFFF6B6B)
private val RedBg          = Color(0x14FF6B6B)
private val RedBorder      = Color(0x33FF6B6B)
private val AmberAccent    = Color(0xFFFFB347)

private val ShapeCard   = RoundedCornerShape(20.dp)
private val ShapeRow    = RoundedCornerShape(8.dp)
private val ShapeBadge  = RoundedCornerShape(4.dp)
private val ShapePill   = RoundedCornerShape(99.dp)

// ─── Root composable ──────────────────────────────────────────────────────────

/**
 * Displays the results of a settlement calculation: status banner, transfer rows,
 * per-profile net balances, and total expense summary.
 *
 * Replaces the old SettlementCard usage in CalculatorSheet.
 * Signature is unchanged — drop-in replacement.
 */
@Composable
fun TransferListPanel(
    snapshot: CalculationSnapshot,
    status: CalculationStatus?,
    profileNameResolver: (String) -> String,
    profiles: List<ProfileItem> = emptyList(),
    paidTransferIndices: Set<Int> = emptySet(),
    onTogglePaid: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val typography = NeoFintechTypography()
    val monoFont   = JetBrainsMonoFontFamily()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, BorderEmphasis, ShapeCard),
        shape    = ShapeCard,
        color    = SurfaceCard,
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ── 1. Status banner ──────────────────────────────────────────────
            status?.let {
                StatusBanner(status = it, typography = typography)
            }

            // ── 2. Total hero ─────────────────────────────────────────────────
            TotalHero(
                snapshot   = snapshot,
                typography = typography,
                monoFont   = monoFont,
            )

            // ── 3. Transfer rows ──────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SectionLabel(text = "Transferencias sugeridas", typography = typography)

                if (snapshot.transfers.isEmpty()) {
                    Text(
                        text  = "No hay transferencias pendientes",
                        style = typography.bodyMedium.copy(color = TextSecondary),
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                } else {
                    snapshot.transfers.forEachIndexed { index, transfer ->
                        TransferRow(
                            transfer            = transfer,
                            profileNameResolver = profileNameResolver,
                            profiles            = profiles,
                            isPaid              = index in paidTransferIndices,
                            onTogglePaid        = { onTogglePaid(index) },
                            typography          = typography,
                            monoFont            = monoFont,
                        )
                    }
                }
            }

            HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

            // ── 4. Per-profile balance rows ───────────────────────────────────
            if (snapshot.participantBalances.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                ) {
                    SectionLabel(text = "Saldos por perfil", typography = typography)
                    Spacer(modifier = Modifier.height(4.dp))

                    val maxAbs = snapshot.participantBalances.values
                        .maxOfOrNull { kotlin.math.abs(it) }
                        ?.takeIf { it > 0.0 } ?: 1.0

                    snapshot.participantBalances.forEach { (profileId, balance) ->
                        val name    = profileNameResolver(profileId)
                        val profile = profiles.find { it.id == profileId }
                        BalanceRow(
                            name       = name,
                            profile    = profile,
                            balance    = balance,
                            maxAbs     = maxAbs,
                            typography = typography,
                            monoFont   = monoFont,
                        )
                    }
                }
            }
        }
    }
}

// ─── Status banner ────────────────────────────────────────────────────────────

/**
 * Slim top banner with a glowing dot and a tinted background.
 *
 * Success / ZeroBalance → green
 * EdgeCaseWarning       → amber
 * Error                 → red
 */
@Composable
private fun StatusBanner(
    status: CalculationStatus,
    typography: Typography,
) {
    val (bgColor, dotColor, textColor) = when (status) {
        is CalculationStatus.Success ->
            Triple(Color(0x113DFFA0), GreenAccent, GreenAccent)
        is CalculationStatus.ZeroBalance ->
            Triple(Color(0x113DFFA0), GreenAccent, GreenAccent)
        is CalculationStatus.EdgeCaseWarning ->
            Triple(Color(0x11FFB347), AmberAccent, AmberAccent)
        is CalculationStatus.Error ->
            Triple(Color(0x11FF6B6B), RedAccent, RedAccent)
    }
    val borderColor = dotColor.copy(alpha = 0.15f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment   = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Glowing dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Text(
            text  = status.message,
            style = typography.bodySmall.copy(
                color      = textColor,
                fontWeight = FontWeight.Medium,
                fontSize   = 13.sp,
            ),
        )
    }

    HorizontalDivider(color = borderColor, thickness = 1.dp)
}

// ─── Total hero ───────────────────────────────────────────────────────────────

@Composable
private fun TotalHero(
    snapshot: CalculationSnapshot,
    typography: Typography,
    monoFont: androidx.compose.ui.text.font.FontFamily,
) {
    val participantCount = snapshot.participantBalances.size
    val transferCount    = snapshot.transfers.size
    val formattedAmount  = formatEuros(snapshot.totalExpense)
        .replace(" €", "").replace("€", "").trim()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Eyebrow label
        Text(
            text  = "TOTAL DEL EVENTO",
            style = typography.labelSmall.copy(
                color          = TextHint,
                fontWeight     = FontWeight.SemiBold,
                fontSize       = 11.sp,
                letterSpacing  = 0.1.sp,
            ),
        )

        // Large mono amount
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text  = formattedAmount,
                style = typography.displaySmall.copy(
                    fontFamily    = monoFont,
                    fontWeight    = FontWeight.Bold,
                    color         = TextPrimary,
                    fontSize      = 38.sp,
                    letterSpacing = (-0.5).sp,
                    lineHeight    = 38.sp,
                ),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text  = "€",
                style = typography.titleLarge.copy(
                    color      = TextSecondary,
                    fontWeight = FontWeight.Normal,
                    fontSize   = 20.sp,
                ),
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }

        // Metadata line
        Text(
            text  = "$participantCount participantes · $transferCount transferencias pendientes",
            style = typography.bodySmall.copy(
                color    = TextHint,
                fontSize = 12.sp,
            ),
        )
    }

    HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
}

// ─── Section label ────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String, typography: Typography) {
    Text(
        text     = text.uppercase(),
        style    = typography.labelSmall.copy(
            color         = TextHint,
            fontWeight    = FontWeight.SemiBold,
            fontSize      = 11.sp,
            letterSpacing = 0.1.sp,
        ),
        modifier = Modifier.padding(bottom = 10.dp),
    )
}

// ─── Transfer row ─────────────────────────────────────────────────────────────

/**
 * Single transfer row inside a tinted surface pill.
 * Paid transfers: check icon + muted alpha.
 */
@Composable
private fun TransferRow(
    transfer: com.cuentamorosos.model.SettlementTransfer,
    profileNameResolver: (String) -> String,
    profiles: List<ProfileItem>,
    isPaid: Boolean,
    onTogglePaid: () -> Unit,
    typography: Typography,
    monoFont: androidx.compose.ui.text.font.FontFamily,
) {
    val fromName    = profileNameResolver(transfer.fromProfileId)
    val toName      = profileNameResolver(transfer.toProfileId)
    val fromProfile = profiles.find { it.id == transfer.fromProfileId }
    val toProfile   = profiles.find { it.id == transfer.toProfileId }
    val rowAlpha    = if (isPaid) 0.45f else 1f

    Surface(
        onClick = onTogglePaid,
        shape   = ShapeRow,
        color   = SurfaceRow,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isPaid) Modifier.background(
                    SurfaceRow.copy(alpha = 0.45f), ShapeRow,
                ) else Modifier
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Check icon for paid state
            if (isPaid) {
                Icon(
                    imageVector        = Icons.Default.Check,
                    contentDescription = "Pagado",
                    tint               = GreenAccent.copy(alpha = rowAlpha),
                    modifier           = Modifier.size(14.dp),
                )
            }

            // FROM avatar + name
            SmallAvatar(profile = fromProfile, fallbackLabel = fromName.take(1))
            Text(
                text     = fromName,
                style    = typography.bodyMedium.copy(
                    color      = TextPrimary.copy(alpha = rowAlpha),
                    fontWeight = FontWeight.Medium,
                    fontSize   = 13.sp,
                ),
                maxLines = 1,
            )

            // Arrow
            Text(
                text  = "→",
                style = typography.bodyMedium.copy(
                    color    = TextHint,
                    fontSize = 11.sp,
                ),
            )

            // TO avatar + name
            SmallAvatar(profile = toProfile, fallbackLabel = toName.take(1))
            Text(
                text     = toName,
                style    = typography.bodyMedium.copy(
                    color      = TextPrimary.copy(alpha = rowAlpha),
                    fontWeight = FontWeight.Medium,
                    fontSize   = 13.sp,
                ),
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )

            // Amount
            Text(
                text  = formatEuros(transfer.amount),
                style = typography.bodyMedium.copy(
                    fontFamily = monoFont,
                    color      = if (isPaid) AmberAccent.copy(alpha = 0.5f) else AmberAccent,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 13.sp,
                ),
            )
        }
    }
}

// ─── Balance row ──────────────────────────────────────────────────────────────

/**
 * Per-profile balance row with a mini proportional bar under the name.
 *
 * creditor (balance > 0) → green
 * debtor   (balance < 0) → red
 * settled  (balance ≈ 0) → neutral
 *
 * [maxAbs] is the largest absolute balance in the list, used to scale the bars.
 */
@Composable
private fun BalanceRow(
    name: String,
    profile: ProfileItem?,
    balance: Double,
    maxAbs: Double,
    typography: Typography,
    monoFont: androidx.compose.ui.text.font.FontFamily,
) {
    val isCreditor = balance > 0.01
    val isDebtor   = balance < -0.01

    val accentColor = when {
        isCreditor -> GreenAccent
        isDebtor   -> RedAccent
        else       -> TextHint
    }
    val badgeBg     = when {
        isCreditor -> GreenBg
        isDebtor   -> RedBg
        else       -> SurfaceRow
    }
    val badgeBorder = when {
        isCreditor -> GreenBorder
        isDebtor   -> RedBorder
        else       -> BorderSubtle
    }
    val badgeLabel  = when {
        isCreditor -> "Acreedor"
        isDebtor   -> "Deudor"
        else       -> "Saldado"
    }
    val barFraction = (kotlin.math.abs(balance) / maxAbs).toFloat().coerceIn(0f, 1f)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Avatar (36 dp)
            LargeAvatar(profile = profile, fallbackLabel = name.take(1))

            // Name + mini bar
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = name,
                    style = typography.bodyMedium.copy(
                        color      = TextPrimary,
                        fontWeight = FontWeight.Medium,
                        fontSize   = 14.sp,
                    ),
                    maxLines = 1,
                )
                Spacer(Modifier.height(5.dp))
                // Track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(ShapePill)
                        .background(SurfaceTrack),
                ) {
                    // Fill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(barFraction)
                            .height(3.dp)
                            .clip(ShapePill)
                            .background(accentColor),
                    )
                }
            }

            // Amount + badge
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text  = formatEuros(balance),
                    style = typography.titleMedium.copy(
                        fontFamily = monoFont,
                        color      = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp,
                    ),
                )
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape = ShapeBadge,
                    color = badgeBg,
                    modifier = Modifier.border(1.dp, badgeBorder, ShapeBadge),
                ) {
                    Text(
                        text     = badgeLabel,
                        style    = typography.labelSmall.copy(
                            color      = accentColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 10.sp,
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }

        HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
    }
}

// ─── Avatar helpers ───────────────────────────────────────────────────────────

/** 28 dp avatar used in transfer rows. */
@Composable
private fun SmallAvatar(profile: ProfileItem?, fallbackLabel: String) {
    ProfileAvatar(
        name     = profile?.name ?: fallbackLabel,
        emoji    = profile?.icon ?: "",
        photoUrl = profile?.photoUrl,
        size     = 28.dp,
    )
}

/** 36 dp avatar used in balance rows. */
@Composable
private fun LargeAvatar(profile: ProfileItem?, fallbackLabel: String) {
    ProfileAvatar(
        name     = profile?.name ?: fallbackLabel,
        emoji    = profile?.icon ?: "",
        photoUrl = profile?.photoUrl,
        size     = 36.dp,
    )
}
