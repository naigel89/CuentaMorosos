package com.cuentamorosos.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuentamorosos.model.SettlementTransfer
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// ═══════════════════════════════════════════════════════════════════
// Lógica pura — sin Compose, comprobable en jvmTest
// ═══════════════════════════════════════════════════════════════════

/**
 * Separa a los participantes en quien paga y quien cobra, conservando el orden
 * en que aparecen en [transfers] para que el dibujo sea estable entre recomposiciones.
 *
 * Los roles salen de las propias transferencias y no de `participantBalances`,
 * cuyo signo significa lo contrario en `SettlementEngine` que en `CalculatorEngine`.
 */
internal fun settlementRoles(
    transfers: List<SettlementTransfer>,
): Pair<List<String>, List<String>> {
    val debtors = LinkedHashSet<String>()
    val creditors = LinkedHashSet<String>()
    transfers.forEach { transfer ->
        debtors.add(transfer.fromProfileId)
        creditors.add(transfer.toProfileId)
    }
    return debtors.toList() to creditors.toList()
}

/**
 * Centros verticales de las filas de una columna de [count] elementos, centrada
 * dentro de un alto de [totalRows] filas.
 *
 * Centrar cada columna por separado es lo que hace que tres deudores y un solo
 * acreedor no queden desalineados: el acreedor cae a la altura del bloque, no
 * pegado arriba.
 */
internal fun columnRowCenters(count: Int, totalRows: Int, rowHeight: Float): List<Float> {
    if (count <= 0) return emptyList()
    val offset = (totalRows - count) * rowHeight / 2f
    return (0 until count).map { index -> offset + index * rowHeight + rowHeight / 2f }
}

/**
 * Recorrido trazado de la flecha [index] de [count].
 *
 * Las franjas se solapan al 60 % para que salga en cascada —se sigue con la
 * vista quién paga a quién— y la duración se calcula de modo que la última
 * **termine exactamente** al final: con franjas de tamaño fijo, a partir de
 * cierto número de transferencias la última se quedaba a medio dibujar.
 */
internal fun arrowProgress(progress: Float, index: Int, count: Int): Float {
    if (count <= 0) return 0f
    val duration = 1f / (0.6f * (count - 1) + 1f)
    val start = index * duration * 0.6f
    return ((progress - start) / duration).coerceIn(0f, 1f)
}

/** Los dos extremos de la punta de flecha que apunta a [tip] viniendo de [from]. */
internal fun arrowHead(
    tip: Offset,
    from: Offset,
    length: Float,
    spread: Float = 0.55f,
): Pair<Offset, Offset> {
    val angle = atan2(tip.y - from.y, tip.x - from.x)
    return Offset(
        tip.x - length * cos(angle - spread),
        tip.y - length * sin(angle - spread),
    ) to Offset(
        tip.x - length * cos(angle + spread),
        tip.y - length * sin(angle + spread),
    )
}

// ═══════════════════════════════════════════════════════════════════
// Composable
// ═══════════════════════════════════════════════════════════════════

private val ROW_HEIGHT: Dp = 52.dp
private val AVATAR_SIZE: Dp = 30.dp
private val CHANNEL_WIDTH: Dp = 92.dp
private val LINE_WIDTH: Dp = 2.dp
private val ARROW_HEAD: Dp = 7.dp
private const val GRAPH_DURATION_MS = 1400

/**
 * Quién paga a quién, en dos columnas: deudores a la izquierda, acreedores a la
 * derecha, y las flechas trazándose una a una.
 *
 * No calcula nada: dibuja lo que ya decidió `SettlementEngine`. La lista de
 * texto que va debajo dice lo mismo con palabras; esto hace visible la
 * **forma** del resultado —que tres personas paguen a la misma se ve de un
 * vistazo y en la lista hay que reconstruirlo leyendo.
 *
 * Los nombres son composables de verdad y no texto dibujado en el lienzo, para
 * que un usuario largo se recorte con puntos suspensivos en lugar de salirse
 * por el borde.
 *
 * Respeta [LocalAnimationsEnabled]: desactivado se dibuja el resultado completo.
 */
@Composable
fun SettlementGraph(
    transfers: List<SettlementTransfer>,
    nameById: Map<String, String>,
    modifier: Modifier = Modifier,
) {
    if (transfers.isEmpty()) return

    val colors = LocalNeoFintechColors.current
    val animationsEnabled = LocalAnimationsEnabled.current
    val measurer = rememberTextMeasurer()

    val (debtors, creditors) = remember(transfers) { settlementRoles(transfers) }
    if (debtors.isEmpty() || creditors.isEmpty()) return

    val totalRows = maxOf(debtors.size, creditors.size)

    val progress = remember { Animatable(0f) }
    LaunchedEffect(transfers, animationsEnabled) {
        if (!animationsEnabled) {
            progress.snapTo(1f)
        } else {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = GRAPH_DURATION_MS,
                    easing = NeoFintechMotion.emphasized,
                ),
            )
        }
    }

    val amountStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = colors.primaryContainer,
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT * totalRows),
    ) {
        PersonColumn(
            ids = debtors,
            nameById = nameById,
            totalRows = totalRows,
            accent = colors.error,
            avatarFirst = false,
            modifier = Modifier.weight(1f),
        )

        Canvas(
            modifier = Modifier
                .width(CHANNEL_WIDTH)
                .fillMaxHeight(),
        ) {
            // DrawScope trabaja en píxeles: todo lo que venga en dp hay que
            // convertirlo aquí o se dibuja a 1/densidad del tamaño previsto.
            val rowHeightPx = ROW_HEIGHT.toPx()
            val debtorCenters = columnRowCenters(debtors.size, totalRows, rowHeightPx)
            val creditorCenters = columnRowCenters(creditors.size, totalRows, rowHeightPx)

            transfers.forEachIndexed { index, transfer ->
                val fromIndex = debtors.indexOf(transfer.fromProfileId)
                val toIndex = creditors.indexOf(transfer.toProfileId)
                if (fromIndex < 0 || toIndex < 0) return@forEachIndexed

                drawTransferArrow(
                    start = Offset(0f, debtorCenters[fromIndex]),
                    end = Offset(size.width, creditorCenters[toIndex]),
                    drawn = arrowProgress(progress.value, index, transfers.size),
                    color = colors.primaryContainer,
                    label = formatAmount(transfer.amount, suffix = "€"),
                    labelStyle = amountStyle,
                    labelBackground = colors.surfaceContainerLowest,
                    measurer = measurer,
                )
            }
        }

        PersonColumn(
            ids = creditors,
            nameById = nameById,
            totalRows = totalRows,
            accent = colors.primaryContainer,
            avatarFirst = true,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Dibuja una transferencia: la línea se traza, y la punta y el importe solo
 * aparecen cuando ha llegado. Si aparecieran antes, la flecha llegaría antes
 * que el dinero.
 */
private fun DrawScope.drawTransferArrow(
    start: Offset,
    end: Offset,
    drawn: Float,
    color: Color,
    label: String,
    labelStyle: TextStyle,
    labelBackground: Color,
    measurer: TextMeasurer,
) {
    if (drawn <= 0f) return

    val tip = Offset(
        start.x + (end.x - start.x) * drawn,
        start.y + (end.y - start.y) * drawn,
    )
    drawLine(
        color = color,
        start = start,
        end = tip,
        strokeWidth = LINE_WIDTH.toPx(),
        cap = StrokeCap.Round,
    )

    if (drawn < 1f) return

    val (left, right) = arrowHead(end, start, ARROW_HEAD.toPx())
    drawLine(color, left, end, strokeWidth = LINE_WIDTH.toPx(), cap = StrokeCap.Round)
    drawLine(color, right, end, strokeWidth = LINE_WIDTH.toPx(), cap = StrokeCap.Round)

    // El importe va sobre una pastilla opaca: sin ella la línea lo cruza por
    // detrás y a 11 sp deja de leerse.
    val layout = measurer.measure(label, labelStyle)
    val center = Offset((start.x + end.x) / 2f, (start.y + end.y) / 2f)
    val padX = 4.dp.toPx()
    val padY = 1.dp.toPx()
    val boxWidth = layout.size.width + padX * 2f
    val boxHeight = layout.size.height + padY * 2f
    drawRoundRect(
        color = labelBackground,
        topLeft = Offset(center.x - boxWidth / 2f, center.y - boxHeight / 2f),
        size = Size(boxWidth, boxHeight),
        cornerRadius = CornerRadius(boxHeight / 2f, boxHeight / 2f),
    )
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(
            center.x - layout.size.width / 2f,
            center.y - layout.size.height / 2f,
        ),
    )
}

/**
 * Columna de personas. El avatar va siempre del lado del canal por el que salen
 * las líneas, de modo que la flecha parece nacer de la persona.
 */
@Composable
private fun PersonColumn(
    ids: List<String>,
    nameById: Map<String, String>,
    totalRows: Int,
    accent: Color,
    avatarFirst: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
    ) {
        ids.forEach { id ->
            val name = nameById[id] ?: id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ROW_HEIGHT),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (avatarFirst) Arrangement.Start else Arrangement.End,
            ) {
                if (avatarFirst) {
                    InitialAvatar(name = name, accent = accent)
                    PersonName(name = name, alignEnd = false, modifier = Modifier.weight(1f, fill = false))
                } else {
                    PersonName(name = name, alignEnd = true, modifier = Modifier.weight(1f, fill = false))
                    InitialAvatar(name = name, accent = accent)
                }
            }
        }
    }
}

@Composable
private fun InitialAvatar(name: String, accent: Color) {
    Box(
        modifier = Modifier
            .size(AVATAR_SIZE)
            .clip(NeoFintechShapes.full)
            .background(accent.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = accent,
        )
    }
}

@Composable
private fun PersonName(name: String, alignEnd: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = name,
        modifier = modifier.padding(horizontal = 6.dp),
        style = MaterialTheme.typography.bodySmall,
        color = LocalNeoFintechColors.current.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
    )
}
