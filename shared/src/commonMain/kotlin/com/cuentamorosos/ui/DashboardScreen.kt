package com.cuentamorosos.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ── Geometría compartida entre el esqueleto y el contenido ────────────────────
//
// El esqueleto no es un relleno: es la misma tarjeta sin datos. Que las alturas
// salgan de aquí y no de dos sitios distintos es lo que permite que el relevo
// sea una fundición corta en vez de un salto de layout.
//
// En la tarjeta real es un mínimo, no una altura fija: con la escala de fuente
// del sistema al máximo el texto tiene que poder crecer sin recortarse.
private val SUMMARY_CARD_HEIGHT: Dp = 96.dp
private val NET_BALANCE_CARD_HEIGHT: Dp = 80.dp
private val TITLE_ROW_HEIGHT: Dp = 40.dp
private val DEBT_ROW_HEIGHT: Dp = 64.dp

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    state: DashboardState,
    onOpenCalendar: (Rect) -> Unit = {},
    onProfileTap: (String) -> Unit = {},
) {
    // Antes era `if (isLoading) { LoadingSkeleton(); return }`: el esqueleto
    // desaparecía y el contenido aparecía en el mismo fotograma.
    Crossfade(
        targetState = state.isLoading,
        animationSpec = tween(
            durationMillis = NeoFintechMotion.MEDIUM_MS,
            easing = NeoFintechMotion.standard,
        ),
        label = "dashboardLoading",
    ) { loading ->
        if (loading) {
            LoadingSkeleton(modifier = modifier)
        } else {
            DashboardContent(
                modifier = modifier,
                state = state,
                onOpenCalendar = onOpenCalendar,
                onProfileTap = onProfileTap,
            )
        }
    }
}

@Composable
private fun DashboardContent(
    modifier: Modifier,
    state: DashboardState,
    onOpenCalendar: (Rect) -> Unit,
    onProfileTap: (String) -> Unit,
) {
    val colors = LocalNeoFintechColors.current
    val summary = state.toFinancialSummary()
    // Una sola entrada por visita a la pestaña: sin esto, cada tarjeta repite su
    // fundido cada vez que vuelve a entrar en pantalla al hacer scroll.
    val entrance = rememberEntranceTracker()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Title + Calendar button
        item(key = "title") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TITLE_ROW_HEIGHT),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Panel",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                // El calendario se abre desde este botón, así que hay que
                // saber dónde está: ExpandFromBounds destapa la pantalla a
                // partir de este rectángulo en lugar de hacerla aparecer.
                var calendarButtonBounds by remember { mutableStateOf<Rect?>(null) }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .onGloballyPositioned { calendarButtonBounds = it.boundsInRoot() }
                        .background(colors.primaryContainer, RoundedCornerShape(12.dp))
                        .pressable(
                            onClick = { onOpenCalendar(calendarButtonBounds ?: Rect.Zero) },
                            shape = RoundedCornerShape(12.dp),
                            role = Role.Button,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = "Abrir calendario",
                        modifier = Modifier.size(24.dp),
                        tint = colors.onPrimaryContainer,
                    )
                }
            }
        }

        // Financial Summary Row — siempre visible
        item(key = "financial-summary") {
            FinancialSummaryRow(
                debes = summary.debes,
                teDeben = summary.teDeben,
                debesCount = summary.debesCount,
                teDebenCount = summary.teDebenCount,
                entrance = entrance,
            )
        }

        // Net Balance Card — siempre visible
        item(key = "net-balance") {
            NetBalanceCard(balance = summary.netBalance, entrance = entrance)
        }

        // Unified debts card (all profiles in one list)
        item(key = "unified-debts") {
            UnifiedDebtsCard(
                items = state.unifiedBreakdown,
                onProfileTap = onProfileTap,
            )
        }
    }
}

// ── Financial Summary Row ─────────────────────────────────────────────────────

@Composable
private fun FinancialSummaryRow(
    debes: Double,
    teDeben: Double,
    debesCount: Int,
    teDebenCount: Int,
    entrance: EntranceTracker,
) {
    val colors = LocalNeoFintechColors.current
    val amountStyle = MaterialTheme.typography.headlineSmall.copy(
        fontFamily = JetBrainsMonoFontFamily(),
        fontWeight = FontWeight.Bold,
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Left card: "DEBES"
        Card(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = SUMMARY_CARD_HEIGHT)
                .appearOnce(entrance, key = "summary-debes", index = 0),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLowest),
            shape = NeoFintechShapes.lg,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    text = "DEBES",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.secondary,
                )
                AnimatedAmount(
                    value = debes,
                    style = amountStyle,
                    color = colors.secondary,
                    countUp = entrance.isFirstAppearance("count-debes"),
                )
                Text(
                    text = "$debesCount perfil${if (debesCount != 1) "es" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }

        // Right card: "TE DEBEN"
        Card(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = SUMMARY_CARD_HEIGHT)
                .appearOnce(entrance, key = "summary-te-deben", index = 1),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLowest),
            shape = NeoFintechShapes.lg,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    text = "TE DEBEN",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.primaryContainer,
                )
                AnimatedAmount(
                    value = teDeben,
                    style = amountStyle,
                    color = colors.primaryContainer,
                    countUp = entrance.isFirstAppearance("count-te-deben"),
                )
                Text(
                    text = "$teDebenCount perfil${if (teDebenCount != 1) "es" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Net Balance Card ──────────────────────────────────────────────────────────

@Composable
private fun NetBalanceCard(balance: Double, entrance: EntranceTracker) {
    val colors = LocalNeoFintechColors.current
    val isPositive = balance >= 0
    val isZero = balance == 0.0
    val accentColor = when {
        isZero -> colors.onSurfaceVariant
        isPositive -> colors.primaryContainer
        else -> colors.error
    }
    val label = when {
        isZero -> "Sin deudas pendientes"
        isPositive -> "Balance a tu favor"
        else -> "Debes saldar"
    }
    val prefix = when {
        isZero -> ""
        isPositive -> "+"
        else -> ""
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = NET_BALANCE_CARD_HEIGHT)
            .appearOnce(entrance, key = "net-balance", index = 2),
        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.12f)),
        shape = NeoFintechShapes.lg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = accentColor,
            )
            AnimatedAmount(
                value = kotlin.math.abs(balance),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = JetBrainsMonoFontFamily(),
                    fontWeight = FontWeight.Bold,
                ),
                color = accentColor,
                prefix = prefix,
                countUp = entrance.isFirstAppearance("count-net-balance"),
            )
        }
    }
}

// ── Loading Skeleton ──────────────────────────────────────────────────────────
//
// Misma geometría que el contenido real: mismos altos, mismos radios, mismo
// fondo de tarjeta. Solo faltan los datos. Así el paso a contenido no recoloca
// nada y puede ser una fundición de MEDIUM_MS.

@Composable
private fun LoadingSkeleton(modifier: Modifier = Modifier) {
    val colors = LocalNeoFintechColors.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        userScrollEnabled = false,
    ) {
        // Título + botón de calendario
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TITLE_ROW_HEIGHT),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SkeletonBlock(width = 78.dp, height = 28.dp)
                SkeletonBlock(width = 40.dp, height = 40.dp, shape = RoundedCornerShape(12.dp))
            }
        }

        // Las dos tarjetas del resumen
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                repeat(2) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(SUMMARY_CARD_HEIGHT),
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLowest),
                        shape = NeoFintechShapes.lg,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            SkeletonBlock(width = 74.dp, height = 13.dp)
                            SkeletonBlock(width = 106.dp, height = 24.dp)
                            SkeletonBlock(width = 56.dp, height = 11.dp)
                        }
                    }
                }
            }
        }

        // Balance neto
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(NET_BALANCE_CARD_HEIGHT),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLowest),
                shape = NeoFintechShapes.lg,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SkeletonBlock(width = 118.dp, height = 13.dp)
                    SkeletonBlock(width = 132.dp, height = 24.dp)
                }
            }
        }

        // Tarjeta de saldos por perfil
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLowest),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 4.dp)) {
                        SkeletonBlock(width = 132.dp, height = 18.dp)
                    }
                    repeat(3) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(DEBT_ROW_HEIGHT)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            SkeletonBlock(width = 40.dp, height = 40.dp, shape = NeoFintechShapes.full)
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                SkeletonBlock(width = 96.dp, height = 15.dp)
                                SkeletonBlock(width = 74.dp, height = 11.dp)
                            }
                            SkeletonBlock(width = 68.dp, height = 15.dp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Bloque gris de esqueleto con el barrido de [Modifier.shimmer].
 *
 * El `clip` va antes del fondo y del barrido: sin él el degradado se dibujaría
 * rectangular por encima de las esquinas redondeadas.
 */
@Composable
private fun SkeletonBlock(
    width: Dp,
    height: Dp,
    shape: androidx.compose.ui.graphics.Shape = NeoFintechShapes.sm,
) {
    val colors = LocalNeoFintechColors.current
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(shape)
            .background(colors.surfaceContainerHigh)
            .shimmer(),
    )
}
