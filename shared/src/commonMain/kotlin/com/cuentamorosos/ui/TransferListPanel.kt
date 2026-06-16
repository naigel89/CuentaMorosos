package com.cuentamorosos.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cuentamorosos.model.CalculationSnapshot
import com.cuentamorosos.model.CalculationStatus
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.formatEuros

// ─── Design tokens ────────────────────────────────────────────────────────────

private val ShapeCard   = RoundedCornerShape(20.dp)
private val ShapeRow    = RoundedCornerShape(8.dp)
private val ShapeBadge  = RoundedCornerShape(4.dp)
private val ShapePill   = RoundedCornerShape(99.dp)

/** Warning/accent amber — keeps its identity in light/dark. */
private val AmberAccent = Color(0xFFFFB347)

// ─── Root composable ──────────────────────────────────────────────────────────

/**
 * Displays the results of a settlement calculation: status banner, transfer rows,
 * per-profile net balances, and total expense summary.
 */
@Composable
fun TransferListPanel(
    snapshot: CalculationSnapshot,
    status: CalculationStatus?,
    profileNameResolver: (String) -> String,
    profiles: List<ProfileItem> = emptyList(),
    paidTransferIndices: Set<Int> = emptySet(),
    onTogglePaid: (Int) -> Unit = {},
    currentProfileId: String? = null,
    modifier: Modifier = Modifier,
) {
    val colors = LocalNeoFintechColors.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .cardShadow()
            .slideUp(),
        shape = ShapeCard,
        color = colors.surfaceContainerLowest,
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ── 1. Status banner ──────────────────────────────────────────────
            status?.let { StatusBanner(status = it) }

            // ── 2. Total hero ─────────────────────────────────────────────────
            TotalHero(snapshot = snapshot)

            // ── 3. Transfer rows ──────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SectionLabel(text = "Transferencias sugeridas")

                if (snapshot.transfers.isEmpty()) {
                    Text(
                        text = "No hay transferencias pendientes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                } else {
                    snapshot.transfers.forEachIndexed { index, transfer ->
                        TransferRow(
                            transfer = transfer,
                            profileNameResolver = profileNameResolver,
                            profiles = profiles,
                            isPaid = index in paidTransferIndices,
                            onTogglePaid = { onTogglePaid(index) },
                            index = index,
                        )
                    }
                }
            }

            HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.3f), thickness = 1.dp)

            // ── 4. Per-profile balance rows ───────────────────────────────────
            if (snapshot.participantBalances.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                ) {
                    SectionLabel(text = "Saldos por perfil")
                    Spacer(modifier = Modifier.height(4.dp))

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
                            currentProfileId = currentProfileId,
                            cardBg = colors.surfaceContainerLowest,
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
 * Uses theme-aware accent colors.
 */
@Composable
private fun StatusBanner(status: CalculationStatus) {
    val colors = LocalNeoFintechColors.current

    val (bgColor, dotColor) = when (status) {
        is CalculationStatus.Success ->
            colors.primaryContainer.copy(alpha = 0.08f) to colors.primaryContainer
        is CalculationStatus.ZeroBalance ->
            colors.primaryContainer.copy(alpha = 0.08f) to colors.primaryContainer
        is CalculationStatus.EdgeCaseWarning ->
            AmberAccent.copy(alpha = 0.08f) to AmberAccent
        is CalculationStatus.Error ->
            colors.error.copy(alpha = 0.08f) to colors.error
    }
    val borderColor = dotColor.copy(alpha = 0.15f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
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
            text = status.message,
            style = MaterialTheme.typography.bodySmall,
            color = dotColor,
            fontWeight = FontWeight.Medium,
        )
    }

    HorizontalDivider(color = borderColor, thickness = 1.dp)
}

// ─── Total hero ───────────────────────────────────────────────────────────────

@Composable
private fun TotalHero(snapshot: CalculationSnapshot) {
    val colors = LocalNeoFintechColors.current

    val participantCount = snapshot.participantBalances.size
    val transferCount = snapshot.transfers.size
    val totalDisplay = rememberAnimatedAmount(
        targetValue = snapshot.totalExpense,
        prefix = "",
        suffix = "",
        decimals = 2,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Eyebrow label
        Text(
            text = "TOTAL DEL EVENTO",
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )

        // Large mono amount with animated count-up
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = totalDisplay,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = JetBrainsMonoFontFamily(),
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                ),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "€",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = colors.onSurfaceVariant,
                    fontWeight = FontWeight.Normal,
                ),
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }

        // Metadata line
        Text(
            text = "$participantCount participantes · $transferCount transferencias pendientes",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }

    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.3f), thickness = 1.dp)
}

// ─── Section label ────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    val colors = LocalNeoFintechColors.current

    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = colors.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 10.dp),
    )
}

// ─── Transfer row ─────────────────────────────────────────────────────────────

/**
 * Single transfer row with staggered fade-in animation.
 * Paid transfers: lower alpha + check icon.
 */
@Composable
private fun TransferRow(
    transfer: com.cuentamorosos.model.SettlementTransfer,
    profileNameResolver: (String) -> String,
    profiles: List<ProfileItem>,
    isPaid: Boolean,
    onTogglePaid: () -> Unit,
    index: Int,
) {
    val colors = LocalNeoFintechColors.current

    val fromName = profileNameResolver(transfer.fromProfileId)
    val toName = profileNameResolver(transfer.toProfileId)
    val fromProfile = profiles.find { it.id == transfer.fromProfileId }
    val toProfile = profiles.find { it.id == transfer.toProfileId }
    val rowAlpha = if (isPaid) 0.45f else 1f

    Surface(
        onClick = onTogglePaid,
        shape = ShapeRow,
        color = colors.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .fadeInStaggered(index = index)
            .then(
                if (isPaid) Modifier.background(
                    colors.surfaceContainer.copy(alpha = 0.45f),
                    ShapeRow,
                ) else Modifier
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Check icon for paid state
            if (isPaid) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.primaryContainer.copy(alpha = rowAlpha),
                    fontWeight = FontWeight.Bold,
                )
            }

            // FROM avatar + name
            SmallAvatar(profile = fromProfile, fallbackLabel = fromName.take(1))
            Text(
                text = fromName,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface.copy(alpha = rowAlpha),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )

            // Arrow
            Text(
                text = "→",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant.copy(alpha = 0.5f),
            )

            // TO avatar + name
            SmallAvatar(profile = toProfile, fallbackLabel = toName.take(1))
            Text(
                text = toName,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface.copy(alpha = rowAlpha),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )

            // Amount
            Text(
                text = formatEuros(transfer.amount),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = JetBrainsMonoFontFamily(),
                    fontWeight = FontWeight.SemiBold,
                ),
                color = if (isPaid) AmberAccent.copy(alpha = 0.5f) else AmberAccent,
            )
        }
    }
}

// ─── Balance row ──────────────────────────────────────────────────────────────

/**
 * Per-profile balance row with an animated proportional bar under the name.
 *
 * Shows a "Tú" marker next to the current user's profile.
 * Names longer than 7 characters fade out on the right edge.
 *
 * creditor (balance > 0) → green (primaryContainer)
 * debtor   (balance < 0) → red (error)
 * settled  (balance ≈ 0)  → neutral
 *
 * [maxAbs] is the largest absolute balance in the list, used to scale the bars.
 */
@Composable
private fun BalanceRow(
    name: String,
    profile: ProfileItem?,
    balance: Double,
    maxAbs: Double,
    currentProfileId: String?,
    cardBg: Color,
) {
    val colors = LocalNeoFintechColors.current

    val isCreditor = balance > 0.01
    val isDebtor = balance < -0.01
    val isCurrentUser = profile?.id != null && profile.id == currentProfileId

    val accentColor = when {
        isCreditor -> colors.primaryContainer
        isDebtor -> colors.error
        else -> colors.onSurfaceVariant
    }
    val badgeBg = when {
        isCreditor -> colors.primaryContainer.copy(alpha = 0.1f)
        isDebtor -> colors.error.copy(alpha = 0.1f)
        else -> colors.surfaceContainer
    }
    val badgeLabel = when {
        isCreditor -> "Acreedor"
        isDebtor -> "Deudor"
        else -> "Saldado"
    }
    val barFraction = (kotlin.math.abs(balance) / maxAbs).toFloat().coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = barFraction,
        animationSpec = tween(durationMillis = NeoFintechAnimations.PROPORTION_BAR_DURATION_MS),
        label = "balanceBar",
    )

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Avatar (36 dp)
            LargeAvatar(profile = profile, fallbackLabel = name.take(1))

            // Name + Tú marker + animated bar
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Name with optional fade if > 7 chars
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.weight(1f).then(
                            if (name.length > 7) Modifier.drawWithContent {
                                drawContent()
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Transparent,
                                            cardBg,
                                        ),
                                        startX = size.width * 0.6f,
                                        endX = size.width,
                                    ),
                                    size = size,
                                )
                            } else Modifier
                        ),
                    )

                    // "Tú" badge for current user
                    if (isCurrentUser) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = ShapeBadge,
                            color = colors.primaryContainer.copy(alpha = 0.15f),
                        ) {
                            Text(
                                text = "Tú",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = colors.primaryContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(5.dp))
                // Track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(ShapePill)
                        .background(colors.outlineVariant.copy(alpha = 0.25f)),
                ) {
                    // Animated fill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedFraction)
                            .height(3.dp)
                            .clip(ShapePill)
                            .background(accentColor),
                    )
                }
            }

            // Amount + badge
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatEuros(balance),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily(),
                        fontWeight = FontWeight.Bold,
                    ),
                    color = accentColor,
                )
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape = ShapeBadge,
                    color = badgeBg,
                ) {
                    Text(
                        text = badgeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }

        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.2f), thickness = 1.dp)
    }
}

// ─── Avatar helpers ───────────────────────────────────────────────────────────

/** 28 dp avatar used in transfer rows. */
@Composable
private fun SmallAvatar(profile: ProfileItem?, fallbackLabel: String) {
    ProfileAvatar(
        name = profile?.name ?: fallbackLabel,
        emoji = profile?.icon ?: "",
        photoUrl = profile?.photoUrl,
        size = 28.dp,
    )
}

/** 36 dp avatar used in balance rows. */
@Composable
private fun LargeAvatar(profile: ProfileItem?, fallbackLabel: String) {
    ProfileAvatar(
        name = profile?.name ?: fallbackLabel,
        emoji = profile?.icon ?: "",
        photoUrl = profile?.photoUrl,
        size = 36.dp,
    )
}
