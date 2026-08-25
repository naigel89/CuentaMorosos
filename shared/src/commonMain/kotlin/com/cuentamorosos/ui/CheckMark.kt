package com.cuentamorosos.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

// ═══════════════════════════════════════════════════════════════════
// Geometría pura — sin Compose, comprobable en jvmTest
// ═══════════════════════════════════════════════════════════════════

/** Vértices de la marca, en coordenadas normalizadas dentro de la casilla. */
internal val TICK_START = Offset(0.20f, 0.52f)
internal val TICK_ELBOW = Offset(0.42f, 0.73f)
internal val TICK_END = Offset(0.80f, 0.29f)

internal val TICK_FIRST_LENGTH: Float =
    hypot(TICK_ELBOW.x - TICK_START.x, TICK_ELBOW.y - TICK_START.y)
internal val TICK_SECOND_LENGTH: Float =
    hypot(TICK_END.x - TICK_ELBOW.x, TICK_END.y - TICK_ELBOW.y)

/**
 * Reparte un progreso global [0..1] entre los dos trazos de la marca,
 * proporcionalmente a su longitud.
 *
 * Es lo que hace que la marca se **trace** a velocidad constante en lugar de
 * dibujar los dos trazos a la vez: el trazo corto termina antes de que empiece
 * el largo, igual que si la dibujaras con un bolígrafo.
 *
 * @return fracción recorrida del primer trazo y del segundo, ambas en [0..1].
 */
internal fun tickProgress(
    progress: Float,
    firstLength: Float = TICK_FIRST_LENGTH,
    secondLength: Float = TICK_SECOND_LENGTH,
): Pair<Float, Float> {
    val clamped = progress.coerceIn(0f, 1f)
    val total = firstLength + secondLength
    if (total <= 0f) return 0f to 0f
    // Atajo en el extremo: dividir y volver a multiplicar deja 0.9999999 en
    // Float, y eso es una marca a la que le falta el último píxel de la punta.
    if (clamped >= 1f) return 1f to 1f
    val travelled = clamped * total
    val first = (travelled / firstLength).coerceIn(0f, 1f)
    val second = ((travelled - firstLength) / secondLength).coerceIn(0f, 1f)
    return first to second
}

// ═══════════════════════════════════════════════════════════════════
// Composable
// ═══════════════════════════════════════════════════════════════════

/**
 * Casilla de verificación cuya marca **se traza** en lugar de aparecer.
 *
 * Sustituye a [androidx.compose.material3.Checkbox] en la lista de participantes
 * de la liquidación. La diferencia no es decorativa: marcar a alguien es afirmar
 * que ya te pagó, así que el gesto merece verse ocurrir —y con [NeoFintechMotion.snappy],
 * sin rebote, porque cualquier oscilación aquí se leería como celebración de algo
 * que no ha movido dinero.
 *
 * Respeta [LocalAnimationsEnabled]: desactivado, la marca salta a su estado final.
 *
 * @param onCheckedChange null deja la casilla como indicador no interactivo; en
 *   ese caso quien maneja el clic es la fila entera.
 */
@Composable
fun CheckMark(
    checked: Boolean,
    modifier: Modifier = Modifier,
    boxSize: Dp = 20.dp,
    checkedColor: Color = MaterialTheme.colorScheme.primary,
    uncheckedColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    markColor: Color = MaterialTheme.colorScheme.surface,
    enabled: Boolean = true,
    onCheckedChange: ((Boolean) -> Unit)? = null,
) {
    val animationsEnabled = LocalAnimationsEnabled.current

    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = if (animationsEnabled) {
            NeoFintechMotion.snappy()
        } else {
            androidx.compose.animation.core.snap()
        },
        label = "checkMarkProgress",
    )
    val fill by animateColorAsState(
        targetValue = if (checked) checkedColor else Color.Transparent,
        animationSpec = NeoFintechMotion.color,
        label = "checkMarkFill",
    )
    val border by animateColorAsState(
        targetValue = if (checked) checkedColor else uncheckedColor,
        animationSpec = NeoFintechMotion.color,
        label = "checkMarkBorder",
    )

    val clickable = if (onCheckedChange != null) {
        Modifier.pressable(
            onClick = { onCheckedChange(!checked) },
            shape = NeoFintechShapes.full,
            enabled = enabled,
            role = Role.Checkbox,
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(TOUCH_TARGET)
            .then(clickable),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(boxSize)) {
            val side = size.minDimension
            val stroke = side * 0.10f
            val radius = side * 0.10f

            drawRoundRect(
                color = fill,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
            )
            drawRoundRect(
                color = border,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
                style = Stroke(width = stroke),
            )

            if (progress > 0f) {
                val (firstFraction, secondFraction) = tickProgress(progress)
                val start = Offset(TICK_START.x * side, TICK_START.y * side)
                val elbow = Offset(TICK_ELBOW.x * side, TICK_ELBOW.y * side)
                val end = Offset(TICK_END.x * side, TICK_END.y * side)

                drawLine(
                    color = markColor,
                    start = start,
                    end = Offset(
                        start.x + (elbow.x - start.x) * firstFraction,
                        start.y + (elbow.y - start.y) * firstFraction,
                    ),
                    strokeWidth = side * 0.13f,
                    cap = StrokeCap.Round,
                )
                if (secondFraction > 0f) {
                    drawLine(
                        color = markColor,
                        start = elbow,
                        end = Offset(
                            elbow.x + (end.x - elbow.x) * secondFraction,
                            elbow.y + (end.y - elbow.y) * secondFraction,
                        ),
                        strokeWidth = side * 0.13f,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}

/** Área táctil mínima. Por debajo de esto la casilla es difícil de acertar. */
private val TOUCH_TARGET: Dp = 44.dp
