package com.cuentamorosos.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuentamorosos.model.CalculationSnapshot
import com.cuentamorosos.model.EventExpenseItem
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.SettlementEngine
import com.cuentamorosos.model.SettlementTraceStep
import com.cuentamorosos.model.SettlementTransfer
import com.cuentamorosos.model.SplitMode
import com.cuentamorosos.model.formatEuros
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowCalculatedPanel(
    event: EventItem,
    snapshot: CalculationSnapshot,
    expenses: List<EventExpenseItem>,
    profiles: List<ProfileItem>,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val colors = LocalNeoFintechColors.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val profileNameResolver: (String) -> String = { id ->
        profiles.find { it.id == id }?.name ?: id
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "¿Cómo se calculó esto?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                )
                IconButton(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) onDismiss()
                        }
                    },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = colors.onSurfaceVariant,
                    )
                }
            }

            // Control segmentado con pulgar deslizante (sustituye al TabRow)
            SegmentedTabs(
                options = listOf("Desglose", "Optimización"),
                selected = selectedTab,
                onSelect = { selectedTab = it },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )

            // Tab content
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState),
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        val dir = if (targetState > initialState) 1 else -1
                        (
                            slideInHorizontally(
                                tween(NeoFintechMotion.MEDIUM_MS, easing = NeoFintechMotion.standard),
                            ) { width -> dir * width / 8 } +
                                fadeIn(tween(NeoFintechMotion.MEDIUM_MS))
                            ) togetherWith (
                            slideOutHorizontally(
                                tween(NeoFintechMotion.SHORT_MS, easing = NeoFintechMotion.standard),
                            ) { width -> -dir * width / 8 } +
                                fadeOut(tween(NeoFintechMotion.SHORT_MS))
                            )
                    },
                    label = "howCalculatedTab",
                ) { tab ->
                    when (tab) {
                        0 -> DesgloseTab(
                            expenses = expenses,
                            snapshot = snapshot,
                            profileNameResolver = profileNameResolver,
                            profiles = profiles,
                            onGoToOptimization = { selectedTab = 1 },
                        )
                        else -> OptimizacionTab(
                            snapshot = snapshot,
                            expenses = expenses,
                            profileNameResolver = profileNameResolver,
                            profiles = profiles,
                        )
                    }
                }
            }
        }
    }
}

// ─── Tab 1: Desglose ────────────────────────────────────────────────────────

@Composable
private fun DesgloseTab(
    expenses: List<EventExpenseItem>,
    snapshot: CalculationSnapshot,
    profileNameResolver: (String) -> String,
    profiles: List<ProfileItem>,
    onGoToOptimization: () -> Unit,
) {
    val colors = LocalNeoFintechColors.current
    val themeColors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Context text
        Text(
            text = "Cada gasto suma o resta en la cuenta de cada uno. Así llegamos a los saldos finales.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )

        // Expense cards
        expenses.forEachIndexed { index, expense ->
            ExpenseBreakdownCard(
                expense = expense,
                profileNameResolver = profileNameResolver,
                colors = colors,
                themeColors = themeColors,
                index = index,
            )
        }

        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.3f))

        // Paid vs consumed per person
        SectionLabel(text = "De lo que pagaste vs. lo que consumiste")
        Spacer(modifier = Modifier.height(4.dp))

        PaidVsConsumedSection(
            expenses = expenses,
            profileNameResolver = profileNameResolver,
            colors = colors,
            themeColors = themeColors,
        )

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.3f))

        // Accumulated balance summary
        SectionLabel(text = "Saldo acumulado")
        Spacer(modifier = Modifier.height(8.dp))

        val maxAbs = snapshot.participantBalances.values
            .maxOfOrNull { abs(it) }
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

        // CTA to optimization tab
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.clickable { onGoToOptimization() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Ver cómo se optimiza esto",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.primaryContainer,
                fontWeight = FontWeight.Medium,
            )
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = colors.primaryContainer,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun ExpenseBreakdownCard(
    expense: EventExpenseItem,
    profileNameResolver: (String) -> String,
    colors: NeoFintechColorSet,
    themeColors: androidx.compose.material3.ColorScheme,
    index: Int = 0,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fadeInStaggered(index = index),
        colors = CardDefaults.cardColors(containerColor = themeColors.surfaceContainerLowest),
        shape = NeoFintechShapes.md,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Header: name + amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = expense.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = themeColors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatEuros(expense.amountEuros),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily(),
                        fontWeight = FontWeight.Bold,
                    ),
                    color = colors.onSurfaceVariant,
                )
            }

            // Who paid + split mode badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val payerName = profileNameResolver(expense.paidByProfileId)
                Text(
                    text = "Pagó: $payerName",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
                SplitModeBadge(
                    modeId = expense.splitMode,
                    colors = colors,
                )
            }

            HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.2f))

            // Per-person breakdown
            val debtorAmounts = SettlementEngine.computeDebtorAmounts(expense)
            debtorAmounts.forEach { (profileId, amount) ->
                val name = profileNameResolver(profileId)
                val isCreditor = amount < 0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodySmall,
                        color = themeColors.onSurface,
                    )
                    Text(
                        text = if (isCreditor) "-${formatEuros(abs(amount))}" else formatEuros(amount),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = JetBrainsMonoFontFamily(),
                        ),
                        color = if (isCreditor) colors.primaryContainer else colors.error,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun PaidVsConsumedSection(
    expenses: List<EventExpenseItem>,
    profileNameResolver: (String) -> String,
    colors: NeoFintechColorSet,
    themeColors: androidx.compose.material3.ColorScheme,
) {
    val paid = mutableMapOf<String, Double>()
    val consumed = mutableMapOf<String, Double>()

    for (expense in expenses) {
        if (expense.payerContributions.isNotEmpty()) {
            for ((payerId, amount) in expense.payerContributions) {
                paid[payerId] = (paid[payerId] ?: 0.0) + amount
            }
        } else {
            paid[expense.paidByProfileId] = (paid[expense.paidByProfileId] ?: 0.0) + expense.amountEuros
        }

        val debtorAmounts = SettlementEngine.computeDebtorAmounts(expense)
        for ((profileId, amount) in debtorAmounts) {
            consumed[profileId] = (consumed[profileId] ?: 0.0) + amount
        }
    }

    val allProfileIds = (paid.keys + consumed.keys).toSet().sorted()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        allProfileIds.forEach { profileId ->
            val paidValue = paid[profileId] ?: 0.0
            val consumedValue = consumed[profileId] ?: 0.0
            val net = paidValue - consumedValue
            val name = profileNameResolver(profileId)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = themeColors.surfaceContainerLowest,
                ),
                shape = NeoFintechShapes.sm,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = themeColors.onSurface,
                        )
                        Text(
                            text = if (net >= -0.005) "Le deben ${formatEuros(net)}" else "Debe ${formatEuros(abs(net))}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = JetBrainsMonoFontFamily(),
                                fontWeight = FontWeight.Bold,
                            ),
                            color = if (net >= -0.005) colors.primaryContainer else colors.error,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = "Pagó: ${formatEuros(paidValue)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                        )
                        Text(
                            text = "Consumió: ${formatEuros(consumedValue)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SplitModeBadge(modeId: String, colors: NeoFintechColorSet) {
    val mode = SplitMode.fromId(modeId)
    Surface(
        color = colors.primaryContainer.copy(alpha = 0.15f),
        shape = NeoFintechShapes.full,
    ) {
        Text(
            text = mode.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = colors.primaryContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

// ─── Tab 2: Cómo se optimizó ────────────────────────────────────────────────

@Composable
private fun OptimizacionTab(
    snapshot: CalculationSnapshot,
    expenses: List<EventExpenseItem>,
    profileNameResolver: (String) -> String,
    profiles: List<ProfileItem>,
) {
    val colors = LocalNeoFintechColors.current
    val themeColors = MaterialTheme.colorScheme

    val balances = snapshot.participantBalances
    val naiveTransfers = remember(expenses) { computeNaiveTransfers(expenses) }
    val totalDebt = abs(balances.values.filter { it < 0 }.sum())
    val savings = naiveTransfers.size - snapshot.transfers.size
    val nameById = remember(profiles) { profiles.associate { it.id to it.name } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (savings > 0) {
            // Héroe: el resultado estrella, legible en un segundo
            SavingsHero(
                naiveCount = naiveTransfers.size,
                optimizedCount = snapshot.transfers.size,
                savings = savings,
                colors = colors,
                themeColors = themeColors,
            )

            // Antes / después como grafos reales de liquidación
            BeforeAfterGraphs(
                naiveTransfers = naiveTransfers,
                optimizedTransfers = snapshot.transfers,
                nameById = nameById,
                colors = colors,
                themeColors = themeColors,
            )
        } else if (naiveTransfers.size == snapshot.transfers.size && naiveTransfers.isNotEmpty()) {
            InfoNote(
                text = "En este caso todos los gastos los pagó una sola persona, así que reparto " +
                    "ingenuo y optimizado coinciden. La optimización se nota cuando hay varios " +
                    "pagadores distintos en un evento.",
                colors = colors,
            )
        }

        // Netting explanation: show profiles that paid but consumed more
        NettingNote(
            expenses = expenses,
            profileNameResolver = profileNameResolver,
            colors = colors,
        )

        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.3f))

        // Step-by-step
        SectionLabel(text = "Paso a paso")

        DetailedStepByStep(
            snapshot = snapshot,
            balances = balances,
            totalDebt = totalDebt,
            profileNameResolver = profileNameResolver,
            profiles = profiles,
            colors = colors,
            themeColors = themeColors,
        )

        // Rounding note
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = colors.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "Si sobra algún céntimo, se asigna siguiendo un criterio fijo determinista para que el cálculo sea siempre reproducible.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

/**
 * "9 → 2": el conteo ingenuo tachado en rojo, una flecha, y el optimizado en
 * verde con un rebote de confirmación ([NeoFintechMotion.bouncy] — reservado a
 * momentos positivos, y ahorrarse transferencias lo es).
 */
@Composable
private fun SavingsHero(
    naiveCount: Int,
    optimizedCount: Int,
    savings: Int,
    colors: NeoFintechColorSet,
    themeColors: androidx.compose.material3.ColorScheme,
) {
    val animationsEnabled = LocalAnimationsEnabled.current
    val mono = JetBrainsMonoFontFamily()

    val optimizedScale = remember { Animatable(if (animationsEnabled) 0.6f else 1f) }
    LaunchedEffect(Unit) {
        if (animationsEnabled) {
            optimizedScale.animateTo(1f, NeoFintechMotion.bouncy())
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .slideUp(),
        shape = NeoFintechShapes.xl,
        color = themeColors.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$naiveCount",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontFamily = mono,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.LineThrough,
                        ),
                        color = colors.error.copy(alpha = 0.85f),
                    )
                    Text(
                        text = "sin optimizar",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(26.dp),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$optimizedCount",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontFamily = mono,
                            fontSize = 46.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = colors.primaryContainer,
                        modifier = Modifier.graphicsLayer {
                            scaleX = optimizedScale.value
                            scaleY = optimizedScale.value
                        },
                    )
                    Text(
                        text = if (optimizedCount == 1) "transferencia" else "transferencias",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = "$savings transferencia${if (savings > 1) "s" else ""} menos, con el mismo dinero en juego.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

/**
 * La maraña "todos contra todos" frente a la ruta mínima, dibujadas con el
 * mismo [SettlementGraph] del panel de liquidación: la de arriba en rojo y sin
 * importes, la de abajo en verde con sus flechas animadas.
 */
@Composable
private fun BeforeAfterGraphs(
    naiveTransfers: List<NaiveTransferEntry>,
    optimizedTransfers: List<SettlementTransfer>,
    nameById: Map<String, String>,
    colors: NeoFintechColorSet,
    themeColors: androidx.compose.material3.ColorScheme,
) {
    val naiveAsTransfers = remember(naiveTransfers) {
        naiveTransfers.map { SettlementTransfer(it.fromProfileId, it.toProfileId, it.totalAmount) }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = NeoFintechShapes.lg,
            color = colors.error.copy(alpha = 0.05f),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                GraphHeader(
                    label = "Sin optimizar",
                    detail = "${naiveAsTransfers.size} transferencias",
                    accent = colors.error,
                )
                SettlementGraph(
                    transfers = naiveAsTransfers,
                    nameById = nameById,
                    arrowColor = colors.error.copy(alpha = 0.55f),
                    creditorAccent = colors.error,
                    showAmounts = false,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.ArrowDownward,
                contentDescription = null,
                tint = colors.primaryContainer,
                modifier = Modifier.size(18.dp),
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = NeoFintechShapes.lg,
            color = colors.primaryContainer.copy(alpha = 0.06f),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                GraphHeader(
                    label = "Optimizado",
                    detail = "${optimizedTransfers.size} transferencia${if (optimizedTransfers.size != 1) "s" else ""} · ${formatEuros(optimizedTransfers.sumOf { it.amount })}",
                    accent = colors.primaryContainer,
                )
                SettlementGraph(
                    transfers = optimizedTransfers,
                    nameById = nameById,
                )
            }
        }
    }
}

@Composable
private fun GraphHeader(label: String, detail: String, accent: androidx.compose.ui.graphics.Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accent),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "· $detail",
            style = MaterialTheme.typography.labelSmall,
            color = accent.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun InfoNote(text: String, colors: NeoFintechColorSet) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = colors.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun NettingNote(
    expenses: List<EventExpenseItem>,
    profileNameResolver: (String) -> String,
    colors: NeoFintechColorSet,
) {
    val paid = mutableMapOf<String, Double>()
    val consumed = mutableMapOf<String, Double>()

    for (expense in expenses) {
        if (expense.payerContributions.isNotEmpty()) {
            for ((payerId, amount) in expense.payerContributions) {
                paid[payerId] = (paid[payerId] ?: 0.0) + amount
            }
        } else {
            paid[expense.paidByProfileId] = (paid[expense.paidByProfileId] ?: 0.0) + expense.amountEuros
        }
        val debtorAmounts = SettlementEngine.computeDebtorAmounts(expense)
        for ((profileId, amount) in debtorAmounts) {
            consumed[profileId] = (consumed[profileId] ?: 0.0) + amount
        }
    }

    val nettedProfiles = paid.keys.filter { profileId ->
        val p = paid[profileId] ?: 0.0
        val c = consumed[profileId] ?: 0.0
        p > 0.005 && (p - c) < -0.005
    }

    if (nettedProfiles.isEmpty()) return

    // Banner con radio del sistema: era el único rectángulo de esquinas rectas
    // que quedaba en la app.
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = NeoFintechShapes.md,
        color = colors.primaryContainer.copy(alpha = 0.08f),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = colors.primaryContainer,
                modifier = Modifier.size(18.dp),
            )
            val names = nettedProfiles.joinToString(" y ") { profileNameResolver(it) }
            val paidSum = nettedProfiles.sumOf { paid[it] ?: 0.0 }
            val consumedSum = nettedProfiles.sumOf { consumed[it] ?: 0.0 }
            val isPlural = nettedProfiles.size > 1
            Text(
                text = "$names ${if (isPlural) "pagaron" else "pagó"} ${formatEuros(paidSum)} pero ${if (isPlural) "consumieron" else "consumió"} ${formatEuros(consumedSum)}. En vez de que todos ${if (isPlural) "les paguen" else "le paguen"} y después ${names} ${if (isPlural) "paguen" else "pague"} a ${if (isPlural) "otros" else "otro"}, el algoritmo simplifica todo a una transferencia neta.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface,
            )
        }
    }
}

@Composable
private fun DetailedStepByStep(
    snapshot: CalculationSnapshot,
    balances: Map<String, Double>,
    totalDebt: Double,
    profileNameResolver: (String) -> String,
    profiles: List<ProfileItem>,
    colors: NeoFintechColorSet,
    themeColors: androidx.compose.material3.ColorScheme,
) {
    val debtors = balances.filterValues { it < -0.01 }
    val creditors = balances.filterValues { it > 0.01 }

    // Los pasos cuelgan de una línea vertical continua, estilo timeline: la
    // línea se dibuja detrás y los círculos numerados (fondo sólido) la tapan.
    val lineColor = colors.outlineVariant.copy(alpha = 0.6f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val x = 12.dp.toPx()
                drawLine(
                    color = lineColor,
                    start = Offset(x, 12.dp.toPx()),
                    end = Offset(x, size.height - 20.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                )
            },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Step 1: Saldo final — with concrete data
        StepItem(
            number = 1,
            title = "Miramos los saldos finales",
            description = "Cada persona tiene un saldo neto: lo que pagó menos lo que le toca consumir. Positivo = le deben. Negativo = debe.",
            colors = colors,
            themeColors = themeColors,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 28.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            balances.forEach { (profileId, balance) ->
                val name = profileNameResolver(profileId)
                val isCreditor = balance > 0.01
                val isDebtor = balance < -0.01
                if (isCreditor || isDebtor) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodySmall,
                            color = themeColors.onSurface,
                        )
                        Text(
                            text = if (isCreditor) "Le deben ${formatEuros(balance)}" else "Debe ${formatEuros(abs(balance))}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = JetBrainsMonoFontFamily(),
                            ),
                            color = if (isCreditor) colors.primaryContainer else colors.error,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }

        // Step 2: Bote imaginario
        StepItem(
            number = 2,
            title = "Creamos un bote imaginario",
            description = "Sumamos todo lo que deben los ${debtors.size} deudor${if (debtors.size != 1) "es" else ""}: ${formatEuros(totalDebt)}. Ese es el monto que hay que repartir entre los ${creditors.size} acreedor${if (creditors.size != 1) "es" else ""}.",
            colors = colors,
            themeColors = themeColors,
        )

        // Step 3: Repartir la hucha — with concrete trace steps
        StepItem(
            number = 3,
            title = "Repartimos la hucha",
            description = "En cada paso, tomamos al mayor deudor y al mayor acreedor. El deudor le paga lo que debe o lo que le deben al acreedor, lo que sea menor. Así saldamos a uno de los dos y seguimos.",
            colors = colors,
            themeColors = themeColors,
        )

        // Show trace steps with detailed reasoning
        if (snapshot.trace.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 28.dp, end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                snapshot.trace.forEachIndexed { index, step ->
                    DetailedTraceStep(
                        step = step,
                        index = index + 1,
                        totalSteps = snapshot.trace.size,
                        profileNameResolver = profileNameResolver,
                        colors = colors,
                        themeColors = themeColors,
                    )
                }
            }
        }

        // Multiple creditors note
        if (creditors.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = colors.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "Como hay ${creditors.size} acreedores, la hucha se repartiría entre varios. El algoritmo siempre empieza por quien más le deben, así se minimizan las transferencias.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DetailedTraceStep(
    step: SettlementTraceStep,
    index: Int,
    totalSteps: Int,
    profileNameResolver: (String) -> String,
    colors: NeoFintechColorSet,
    themeColors: androidx.compose.material3.ColorScheme,
) {
    val fromName = profileNameResolver(step.fromProfileId)
    val toName = profileNameResolver(step.toProfileId)
    val debtorSaldado = abs(step.debtorRemaining) < 0.01
    val creditorSaldado = abs(step.creditorRemaining) < 0.01

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fadeInStaggered(index = index - 1),
        colors = CardDefaults.cardColors(
            containerColor = themeColors.surfaceContainerLowest,
        ),
        shape = NeoFintechShapes.sm,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Header: step number + transfer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = colors.primaryContainer,
                    modifier = Modifier.size(20.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$index",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Text(
                    text = fromName,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.error,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = toName,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.primaryContainer,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = formatEuros(step.amount),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = JetBrainsMonoFontFamily(),
                        fontWeight = FontWeight.Bold,
                    ),
                    color = colors.onSurfaceVariant,
                )
            }

            // Explanation
            val explanation = buildString {
                append("$fromName le pagaría ${formatEuros(step.amount)} a $toName")
                when {
                    debtorSaldado && creditorSaldado -> {
                        append(" y ambos quedarían saldados")
                    }
                    debtorSaldado -> {
                        append(". Su deuda quedaría saldada")
                        append("; a $toName aún le quedarían por cobrar ${formatEuros(step.creditorRemaining)}")
                    }
                    creditorSaldado -> {
                        append(". $toName quedaría saldado")
                        append("; a $fromName aún le quedaría por pagar ${formatEuros(abs(step.debtorRemaining))}")
                    }
                    else -> {
                        append(". A $fromName aún le quedaría por pagar ${formatEuros(abs(step.debtorRemaining))}")
                    }
                }
                append(".")
            }

            Text(
                text = explanation,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )

            // Remaining balances
            if (index < totalSteps) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = if (abs(step.debtorRemaining) < 0.01) "$fromName quedaría saldado" else "A $fromName le quedaría por pagar ${formatEuros(abs(step.debtorRemaining))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (abs(step.debtorRemaining) < 0.01) colors.primaryContainer else colors.error.copy(alpha = 0.7f),
                    )
                    Text(
                        text = if (abs(step.creditorRemaining) < 0.01) "$toName quedaría saldado" else "A $toName le quedarían por cobrar ${formatEuros(step.creditorRemaining)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (abs(step.creditorRemaining) < 0.01) colors.primaryContainer else colors.primaryContainer.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@Composable
private fun StepItem(
    number: Int,
    title: String,
    description: String,
    colors: NeoFintechColorSet,
    themeColors: androidx.compose.material3.ColorScheme,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Step number circle
        Surface(
            shape = CircleShape,
            color = colors.primaryContainer,
            modifier = Modifier.size(24.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "$number",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = themeColors.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

// ─── Per-expense breakdown for naive transfers ────────────────────────────

private data class NaiveExpenseBreakdown(
    val expenseName: String,
    val amount: Double,
)

private data class NaiveTransferEntry(
    val fromProfileId: String,
    val toProfileId: String,
    val totalAmount: Double,
    val breakdowns: List<NaiveExpenseBreakdown>,
)

/**
 * Computes naive transfers expense-by-expense: each person pays the payer
 * of each expense their share. Results are accumulated by (debtor, payer)
 * pair to show the intuitive "everyone pays their part" approach.
 */
private fun computeNaiveTransfers(expenses: List<EventExpenseItem>): List<NaiveTransferEntry> {
    data class Accum(val total: MutableList<Double>, val breakdowns: MutableList<NaiveExpenseBreakdown>)

    val accum = mutableMapOf<Pair<String, String>, Accum>()

    for (expense in expenses) {
        val payerId = expense.paidByProfileId
        val debtorAmounts = SettlementEngine.computeDebtorAmounts(expense)

        for ((debtorId, amount) in debtorAmounts) {
            if (debtorId == payerId) continue
            if (amount <= 0.005) continue

            val pair = debtorId to payerId
            val entry = accum.getOrPut(pair) { Accum(mutableListOf(), mutableListOf()) }
            entry.total.add(amount)
            entry.breakdowns.add(NaiveExpenseBreakdown(expense.name, amount))
        }
    }

    return accum.map { (pair, acc) ->
        NaiveTransferEntry(
            fromProfileId = pair.first,
            toProfileId = pair.second,
            totalAmount = acc.total.sum(),
            breakdowns = acc.breakdowns,
        )
    }.filter { it.totalAmount > 0.005 }
}
