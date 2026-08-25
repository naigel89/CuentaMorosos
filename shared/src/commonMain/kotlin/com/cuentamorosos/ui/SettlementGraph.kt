package com.cuentamorosos.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuentamorosos.model.SettlementTransfer
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

// ═══════════════════════════════════════════════════════════════════
// Geometría y fases — puras, comprobables en jvmTest
// ═══════════════════════════════════════════════════════════════════

/** Fracción del recorrido a partir de la cual empiezan a trazarse las flechas. */
internal const val ARROW_WINDOW_START = 0.5f

/**
 * Reparte [count] nodos sobre una elipse inscrita en [size], empezando arriba y
 * girando en el sentido de las agujas del reloj.
 */
internal fun ringPositions(count: Int, size: Size, inset: Float): List<Offset> {
    if (count <= 0) return emptyList()
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val radiusX = (size.width / 2f - inset).coerceAtLeast(1f)
    val radiusY = (size.height / 2f - inset).coerceAtLeast(1f)
    if (count == 1) return listOf(Offset(centerX, centerY))
    return (0 until count).map { index ->
        val angle = -PI.toFloat() / 2f + 2f * PI.toFloat() * index / count
        Offset(centerX + radiusX * cos(angle), centerY + radiusY * sin(angle))
    }
}

/**
 * Acorta un segmento por los dos extremos para que no se meta debajo de los
 * nodos. Si el hueco es menor que el recorte, devuelve el segmento intacto: un
 * recorte mayor que la distancia daría un segmento invertido.
 */
internal fun trimSegment(from: Offset, to: Offset, pad: Float): Pair<Offset, Offset> {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val distance = hypot(dx, dy)
    if (distance <= pad * 2f || distance == 0f) return from to to
    val ux = dx / distance
    val uy = dy / distance
    return Offset(from.x + ux * pad, from.y + uy * pad) to
        Offset(to.x - ux * pad, to.y - uy * pad)
}

/**
 * Opacidad de la maraña de deudas cruzadas a lo largo del recorrido.
 *
 * Entra, se sostiene un momento —el tiempo de que se lea "esto es lo que hay"—
 * y se retira justo antes de que empiecen a dibujarse las transferencias.
 */
internal fun webAlpha(progress: Float): Float = when {
    progress <= 0f -> 0f
    progress < 0.28f -> progress / 0.28f
    progress < 0.42f -> 1f
    progress < ARROW_WINDOW_START -> 1f - (progress - 0.42f) / (ARROW_WINDOW_START - 0.42f)
    else -> 0f
}

/**
 * Recorrido trazado de la flecha [index] de [count].
 *
 * Las franjas se solapan al 60 % para que salga en cascada, y la duración se
 * calcula de modo que la última **termine exactamente** al final del recorrido:
 * con franjas de tamaño fijo, a partir de cierto número de transferencias la
 * última se quedaba a medio dibujar.
 */
internal fun arrowProgress(progress: Float, index: Int, count: Int): Float {
    if (count <= 0) return 0f
    val window = 1f - ARROW_WINDOW_START
    val duration = window / (0.6f * (count - 1) + 1f)
    val start = ARROW_WINDOW_START + index * duration * 0.6f
    return ((progress - start) / duration).coerceIn(0f, 1f)
}

/** Los dos extremos de la punta de flecha que apunta a [tip] viniendo de [from]. */
internal fun arrowHead(
    tip: Offset,
    from: Offset,
    length: Float,
    spread: Float = PI.toFloat() / 2.6f,
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

/**
 * Participantes implicados en un conjunto de transferencias, en orden estable.
 *
 * Los roles salen de las propias transferencias —quien aparece como origen debe,
 * quien aparece como destino cobra— y no de `participantBalances`, cuyo signo
 * significa lo contrario en `SettlementEngine` y en `CalculatorEngine`.
 */
internal fun graphParticipants(transfers: List<SettlementTransfer>): List<String> {
    val seen = LinkedHashSet<String>()
    transfers.forEach { transfer ->
        seen.add(transfer.fromProfileId)
        seen.add(transfer.toProfileId)
    }
    return seen.toList()
}

/** True si [id] paga en alguna de las transferencias. */
internal fun isDebtor(id: String, transfers: List<SettlementTransfer>): Boolean =
    transfers.any { it.fromProfileId == id }

// ═══════════════════════════════════════════════════════════════════
// Composable
// ═══════════════════════════════════════════════════════════════════

/**
 * Hace visible lo que hace `SettlementEngine`: la maraña de deudas cruzadas
 * entre todos los participantes se absorbe y quedan solo las transferencias
 * mínimas, que se trazan una a una.
 *
 * Hasta ahora el resultado sustituía al planteamiento sin transición —aparecía
 * una lista de transferencias y no se veía por qué eran esas—. El grafo no
 * calcula nada: solo dibuja lo que el motor ya decidió.
 *
 * Respeta [LocalAnimationsEnabled]: desactivado se dibuja directamente el
 * resultado, sin maraña previa.
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

    val participants = remember(transfers) { graphParticipants(transfers) }
    val debtorFlags = remember(transfers, participants) {
        participants.associateWith { isDebtor(it, transfers) }
    }

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
                    easing = NeoFintechMotion.standard,
                ),
            )
        }
    }

    val creditorColor = colors.primaryContainer
    val debtorColor = colors.error
    val webColor = colors.onSurfaceVariant
    val arrowColor = colors.primaryContainer
    val labelStyle = TextStyle(fontSize = 10.sp, color = colors.onSurface)
    val amountStyle = TextStyle(fontSize = 10.sp, color = colors.primaryContainer)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(GRAPH_HEIGHT),
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(GRAPH_HEIGHT)) {
            val nodes = ringPositions(participants.size, size, NODE_INSET_PX)
            if (nodes.isEmpty()) return@Canvas
            val value = progress.value

            drawNaiveWeb(nodes, webColor, webAlpha(value))
            drawTransfers(
                transfers = transfers,
                participants = participants,
                nodes = nodes,
                value = value,
                color = arrowColor,
                measurer = measurer,
                amountStyle = amountStyle,
            )
            drawNodes(
                participants = participants,
                nodes = nodes,
                debtorFlags = debtorFlags,
                nameById = nameById,
                creditorColor = creditorColor,
                debtorColor = debtorColor,
                measurer = measurer,
                labelStyle = labelStyle,
            )
        }
    }
}

private const val GRAPH_DURATION_MS = 2200
private val GRAPH_HEIGHT = 210.dp
private const val NODE_INSET_PX = 42f
private const val NODE_RADIUS_PX = 17f
private const val EDGE_PAD_PX = 24f

private fun DrawScope.drawNaiveWeb(nodes: List<Offset>, color: androidx.compose.ui.graphics.Color, alpha: Float) {
    if (alpha <= 0f) return
    for (i in nodes.indices) {
        for (j in i + 1 until nodes.size) {
            val (start, end) = trimSegment(nodes[i], nodes[j], EDGE_PAD_PX)
            drawLine(
                color = color,
                start = start,
                end = end,
                strokeWidth = 1f,
                alpha = alpha * 0.55f,
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun DrawScope.drawTransfers(
    transfers: List<SettlementTransfer>,
    participants: List<String>,
    nodes: List<Offset>,
    value: Float,
    color: androidx.compose.ui.graphics.Color,
    measurer: TextMeasurer,
    amountStyle: TextStyle,
) {
    transfers.forEachIndexed { index, transfer ->
        val fromIndex = participants.indexOf(transfer.fromProfileId)
        val toIndex = participants.indexOf(transfer.toProfileId)
        if (fromIndex < 0 || toIndex < 0) return@forEachIndexed

        val drawn = arrowProgress(value, index, transfers.size)
        if (drawn <= 0f) return@forEachIndexed

        val (start, end) = trimSegment(nodes[fromIndex], nodes[toIndex], EDGE_PAD_PX)
        val tip = Offset(
            start.x + (end.x - start.x) * drawn,
            start.y + (end.y - start.y) * drawn,
        )
        drawLine(
            color = color,
            start = start,
            end = tip,
            strokeWidth = 2.5f,
            cap = StrokeCap.Round,
        )

        // La punta y el importe solo cuando la línea ha llegado: si aparecieran
        // antes, la flecha llegaría antes que el dinero.
        if (drawn >= 1f) {
            val (left, right) = arrowHead(end, start, 11f)
            drawLine(color = color, start = left, end = end, strokeWidth = 2.5f, cap = StrokeCap.Round)
            drawLine(color = color, start = right, end = end, strokeWidth = 2.5f, cap = StrokeCap.Round)

            val label = formatAmount(transfer.amount, suffix = "€")
            val layout = measurer.measure(label, amountStyle)
            val midX = (start.x + end.x) / 2f
            val midY = (start.y + end.y) / 2f
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    midX - layout.size.width / 2f,
                    midY - layout.size.height / 2f,
                ),
            )
        }
    }
}

private fun DrawScope.drawNodes(
    participants: List<String>,
    nodes: List<Offset>,
    debtorFlags: Map<String, Boolean>,
    nameById: Map<String, String>,
    creditorColor: androidx.compose.ui.graphics.Color,
    debtorColor: androidx.compose.ui.graphics.Color,
    measurer: TextMeasurer,
    labelStyle: TextStyle,
) {
    participants.forEachIndexed { index, id ->
        val center = nodes[index]
        val accent = if (debtorFlags[id] == true) debtorColor else creditorColor

        drawCircle(color = accent.copy(alpha = 0.16f), radius = NODE_RADIUS_PX, center = center)
        drawCircle(
            color = accent,
            radius = NODE_RADIUS_PX,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f),
        )

        val name = nameById[id] ?: id
        val initial = name.firstOrNull()?.uppercase() ?: "?"
        val initialLayout = measurer.measure(initial, labelStyle.copy(color = accent, fontSize = 12.sp))
        drawText(
            textLayoutResult = initialLayout,
            topLeft = Offset(
                center.x - initialLayout.size.width / 2f,
                center.y - initialLayout.size.height / 2f,
            ),
        )

        val nameLayout = measurer.measure(name, labelStyle)
        drawText(
            textLayoutResult = nameLayout,
            topLeft = Offset(
                center.x - nameLayout.size.width / 2f,
                center.y + NODE_RADIUS_PX + 4f,
            ),
        )
    }
}
