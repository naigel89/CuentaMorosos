package com.cuentamorosos.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuentamorosos.formatDateMillis
import com.cuentamorosos.model.CalculationSnapshot
import com.cuentamorosos.model.EventExpenseItem
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.SplitMode
import com.cuentamorosos.model.formatEuros
import com.cuentamorosos.model.formattedDate
import cuentamorosos.shared.generated.resources.Res
import cuentamorosos.shared.generated.resources.logo_cuentamorosos
import kotlin.math.abs
import kotlin.math.min
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

// ── Paleta de papel térmico ───────────────────────────────────────────────────
// Fija a propósito, como AmberAccent: el recibo es un objeto físico y conserva
// su identidad de papel tanto en tema claro como oscuro.
private val PaperBg = Color(0xFFF5F3EE)
private val PaperInk = Color(0xFF1C1B18)
private val PaperMuted = Color(0xFF7A766B)
private val PaperGreen = Color(0xFF008F47)
private val PaperRed = Color(0xFFBA1A1A)
private val PaperLine = Color(0xFFC9C4B8)

/**
 * El recibo como texto plano para el share sheet: mismas secciones que el
 * ticket, legible en cualquier chat. Pura y testeable.
 */
internal fun buildReceiptShareText(
    event: EventItem,
    snapshot: CalculationSnapshot,
    profiles: List<ProfileItem>,
    expenses: List<EventExpenseItem>,
): String {
    fun name(id: String): String = profiles.find { it.id == id }?.name ?: id

    return buildString {
        appendLine("CUENTAMOROSOS · Recibo")
        appendLine(event.name.uppercase())
        appendLine(event.formattedDate())
        appendLine()
        appendLine("TOTAL: ${formatEuros(snapshot.totalExpense)}")
        appendLine("${snapshot.participantBalances.size} participantes · ${snapshot.transfers.size} transferencias")
        if (expenses.isNotEmpty()) {
            appendLine()
            appendLine("GASTOS")
            expenses.forEach { expense ->
                appendLine("· ${expense.name} (${name(expense.paidByProfileId)}): ${formatEuros(expense.amountEuros)}")
            }
        }
        if (snapshot.participantBalances.isNotEmpty()) {
            appendLine()
            appendLine("SALDOS")
            snapshot.participantBalances.forEach { (profileId, balance) ->
                val prefix = if (balance > 0.005) "+" else ""
                appendLine("· ${name(profileId)}: $prefix${formatEuros(balance)}")
            }
        }
        if (snapshot.transfers.isNotEmpty()) {
            appendLine()
            appendLine("TRANSFERENCIAS")
            snapshot.transfers.forEach { transfer ->
                appendLine("· ${name(transfer.fromProfileId)} → ${name(transfer.toProfileId)}: ${formatEuros(transfer.amount)}")
            }
        }
        appendLine()
        appendLine("Modo: ${event.lastCalculationMode?.let { SplitMode.fromId(it).label } ?: "—"}")
        appendLine("Calculado el ${formatDateMillis(snapshot.calculatedAtMillis)}")
        append("N.º ${receiptCode(snapshot.calculatedAtMillis)}")
    }
}

/**
 * Pseudo-identificador del recibo, determinista a partir del instante del
 * cálculo: el mismo snapshot imprime siempre el mismo número.
 */
internal fun receiptCode(calculatedAtMillis: Long): String {
    val h = calculatedAtMillis.hashCode()
    val hex = (h.toLong() and 0xFFFF).toString(16).uppercase().padStart(4, '0')
    return "CM-$hex"
}

/**
 * Recibo del evento como ticket de papel térmico: cabecera centrada, todo en
 * JetBrains Mono, divisores punteados, código de barras y borde dentado
 * inferior. Muestra total, gastos, saldos, transferencias y metadatos del
 * [CalculationSnapshot] persistido.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptPanel(
    event: EventItem,
    snapshot: CalculationSnapshot,
    profiles: List<ProfileItem>,
    expenses: List<EventExpenseItem> = emptyList(),
    // Puerto del host: comparte el recibo como texto (share sheet nativo).
    // Null = el host no lo soporta y el botón no se muestra.
    onShare: ((String) -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val colors = LocalNeoFintechColors.current
    val typography = NeoFintechTypography()
    val mono = JetBrainsMonoFontFamily()
    val profileNameResolver: (String) -> String = { id ->
        profiles.find { it.id == id }?.name ?: id
    }

    // Estilos base del papel
    val paperBody = typography.bodySmall.copy(fontFamily = mono, color = PaperInk, fontSize = 13.sp)
    val paperLabel = typography.labelSmall.copy(
        fontFamily = mono,
        color = PaperMuted,
        letterSpacing = 2.5.sp,
        fontSize = 11.sp,
    )
    val paperFootnote = typography.labelSmall.copy(fontFamily = mono, color = PaperMuted, fontSize = 11.sp)

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
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // ── El ticket ───────────────────────────────────────────────────
            Column(modifier = Modifier.fillMaxWidth().slideUp()) {
              Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(PaperBg)
                        .padding(horizontal = 22.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Cabecera centrada, como un ticket de compra
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(text = "CUENTAMOROSOS", style = paperLabel.copy(letterSpacing = 5.sp))
                        Text(
                            text = event.name.uppercase(),
                            style = paperBody.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 5.dp),
                        )
                        Text(text = event.formattedDate(), style = paperFootnote)
                    }

                    DashedDivider()

                    // Total
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(text = "TOTAL", style = paperLabel)
                        AnimatedAmount(
                            value = snapshot.totalExpense,
                            style = paperBody.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold),
                            color = PaperInk,
                            suffix = " €",
                        )
                        Text(
                            text = "${snapshot.participantBalances.size} participantes · ${snapshot.transfers.size} transferencias",
                            style = paperFootnote,
                        )
                    }

                    // Gastos
                    if (expenses.isNotEmpty()) {
                        DashedDivider()
                        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(text = "GASTOS", style = paperLabel)
                            expenses.forEach { expense ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(text = expense.name, style = paperBody, maxLines = 1)
                                        Text(
                                            text = " · ${profileNameResolver(expense.paidByProfileId)}",
                                            style = paperFootnote,
                                            maxLines = 1,
                                        )
                                    }
                                    Text(text = formatEuros(expense.amountEuros), style = paperBody)
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(text = "${expenses.size} ítems", style = paperFootnote)
                                Text(
                                    text = formatEuros(expenses.sumOf { it.amountEuros }),
                                    style = paperBody.copy(fontWeight = FontWeight.Bold),
                                )
                            }
                        }
                    }

                    // Saldos
                    if (snapshot.participantBalances.isNotEmpty()) {
                        DashedDivider()
                        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(text = "SALDOS", style = paperLabel)
                            snapshot.participantBalances.forEach { (profileId, balance) ->
                                val settled = abs(balance) <= 0.005
                                val amountStyle: TextStyle = when {
                                    settled -> paperBody.copy(color = PaperMuted)
                                    balance > 0 -> paperBody.copy(color = PaperGreen, fontWeight = FontWeight.Bold)
                                    else -> paperBody.copy(color = PaperRed, fontWeight = FontWeight.Bold)
                                }
                                val prefix = if (!settled && balance > 0) "+" else ""
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(text = profileNameResolver(profileId), style = paperBody, maxLines = 1)
                                    Text(text = prefix + formatEuros(balance), style = amountStyle)
                                }
                            }
                        }
                    }

                    // Transferencias
                    if (snapshot.transfers.isNotEmpty()) {
                        DashedDivider()
                        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(text = "TRANSFERENCIAS", style = paperLabel)
                            snapshot.transfers.forEach { transfer ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = profileNameResolver(transfer.fromProfileId),
                                        style = paperBody,
                                        maxLines = 1,
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint = PaperMuted,
                                        modifier = Modifier
                                            .padding(horizontal = 6.dp)
                                            .size(13.dp),
                                    )
                                    Text(
                                        text = profileNameResolver(transfer.toProfileId),
                                        style = paperBody,
                                        maxLines = 1,
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        text = formatEuros(transfer.amount),
                                        style = paperBody.copy(fontWeight = FontWeight.Bold),
                                    )
                                }
                            }
                        }
                    }

                    DashedDivider()

                    // Pie de ticket: metadatos, código de barras y número
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Modo: ${event.lastCalculationMode?.let { SplitMode.fromId(it).label } ?: "—"}\n" +
                                "Calculado el ${formatDateMillis(snapshot.calculatedAtMillis)}",
                            style = paperFootnote.copy(lineHeight = 18.sp),
                            textAlign = TextAlign.Center,
                        )
                        ReceiptBarcode(
                            seed = snapshot.calculatedAtMillis,
                            modifier = Modifier.fillMaxWidth(0.64f),
                        )
                        Text(
                            text = "N.º ${receiptCode(snapshot.calculatedAtMillis)}",
                            style = paperFootnote.copy(letterSpacing = 3.sp, fontSize = 10.sp),
                        )
                    }
                }

                // El sello de la casa: tinta verde oscura sobre el papel,
                // ligeramente girado y solapando el pie del ticket.
                BrandStamp(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 18.dp, bottom = 170.dp),
                )
              }

                ZigzagEdge(color = PaperBg)
            }

            // ── Compartir / Cerrar ──────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (onShare != null) {
                    Button(
                        onClick = {
                            onShare(buildReceiptShareText(event, snapshot, profiles, expenses))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                        shape = NeoFintechShapes.lg,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.buttonContainer,
                            contentColor = colors.onButton,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Compartir recibo", fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = { dismiss() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = NeoFintechShapes.lg,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = colors.onSurfaceVariant,
                        ),
                        border = BorderStroke(1.dp, colors.outlineVariant),
                    ) {
                        Text(text = "Cerrar")
                    }
                } else {
                    Button(
                        onClick = { dismiss() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                        shape = NeoFintechShapes.lg,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.buttonContainer,
                            contentColor = colors.onButton,
                        ),
                    ) {
                        Text(text = "Cerrar", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

// ── Piezas del papel ──────────────────────────────────────────────────────────

/**
 * Sello de la marca: el logo tintado en el verde oscuro del papel, dentro de
 * un marco de tampón redondeado, girado unos grados y con tinta translúcida.
 * Entra con efecto de estampado: aparece grande y cae con el muelle [NeoFintechMotion.bouncy]
 * hasta asentarse, un momento después de que el ticket haya entrado.
 *
 * El vector del logo trae el lienzo cuadrado del launcher (la figura ocupa
 * ~56 % del ancho, centrada), así que se dibuja sobredimensionado dentro de un
 * marco recortado para que el sello no quede lleno de aire.
 */
@Composable
private fun BrandStamp(modifier: Modifier = Modifier) {
    val animationsEnabled = LocalAnimationsEnabled.current
    val inkAlpha = 0.36f

    val progress = remember { Animatable(if (animationsEnabled) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (animationsEnabled) {
            delay(500)
            progress.animateTo(1f, NeoFintechMotion.bouncy())
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                val p = progress.value
                alpha = (inkAlpha * p).coerceIn(0f, 1f)
                val scale = 1.9f - 0.9f * p
                scaleX = scale
                scaleY = scale
                rotationZ = -12f
            }
            .border(2.5.dp, PaperGreen, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 40.dp)
                    .clipToBounds(),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(Res.drawable.logo_cuentamorosos),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(PaperGreen),
                    modifier = Modifier.requiredSize(114.dp),
                )
            }
            Text(
                text = "CUENTAMOROSOS",
                style = NeoFintechTypography().labelSmall.copy(
                    fontFamily = JetBrainsMonoFontFamily(),
                    fontSize = 8.sp,
                    letterSpacing = 2.sp,
                ),
                color = PaperGreen,
            )
        }
    }
}

/** Divisor punteado, como la línea de corte de un ticket. */
@Composable
private fun DashedDivider() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp),
    ) {
        val y = size.height / 2f
        drawLine(
            color = PaperLine,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = size.height,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx())),
        )
    }
}

/** Borde dentado inferior: triángulos del color del papel sobre el fondo. */
@Composable
private fun ZigzagEdge(color: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp),
    ) {
        val tooth = 14.dp.toPx()
        val path = Path().apply {
            moveTo(0f, 0f)
            var x = 0f
            while (x < size.width) {
                lineTo(x + tooth / 2f, size.height)
                lineTo(min(x + tooth, size.width), 0f)
                x += tooth
            }
            close()
        }
        drawPath(path = path, color = color)
    }
}

/**
 * Pseudo-código de barras: barras de ancho variable generadas con un LCG
 * sembrado por el instante del cálculo — decorativo pero determinista.
 */
@Composable
private fun ReceiptBarcode(seed: Long, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.height(34.dp)) {
        val unit = 1.3.dp.toPx()
        var state = seed
        var x = 0f
        while (x < size.width) {
            state = state * 6364136223846793005L + 1442695040888963407L
            val barUnits = ((state ushr 33) % 3L + 1L).toInt()
            val gapUnits = ((state ushr 43) % 3L + 1L).toInt()
            val barWidth = min(barUnits * unit, size.width - x)
            drawRect(
                color = PaperInk,
                topLeft = Offset(x, 0f),
                size = Size(barWidth, size.height),
            )
            x += barWidth + gapUnits * unit
        }
    }
}
