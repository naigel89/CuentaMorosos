package com.cuentamorosos.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.delay

// ═══════════════════════════════════════════════════════════════════
// Geometría pura — sin Compose, comprobable en jvmTest
// ═══════════════════════════════════════════════════════════════════

/**
 * Interpola un rectángulo hacia otro. [fraction] fuera de [0..1] se recorta:
 * un muelle puede sobrepasar y aquí eso dibujaría fuera de la pantalla.
 */
internal fun lerpRect(from: Rect, to: Rect, fraction: Float): Rect {
    val f = fraction.coerceIn(0f, 1f)
    return Rect(
        left = from.left + (to.left - from.left) * f,
        top = from.top + (to.top - from.top) * f,
        right = from.right + (to.right - from.right) * f,
        bottom = from.bottom + (to.bottom - from.bottom) * f,
    )
}

/**
 * Progreso del color de fondo a partir del progreso geométrico.
 *
 * Va tres veces más rápido a propósito: el verde del botón tiene que haberse
 * convertido en fondo de pantalla mucho antes de que la superficie termine de
 * crecer, o durante media transición la pantalla entera sería verde.
 */
internal fun originColorFraction(progress: Float): Float =
    (progress * 3f).coerceIn(0f, 1f)

// ═══════════════════════════════════════════════════════════════════
// Composable
// ═══════════════════════════════════════════════════════════════════

/**
 * Revela una pantalla completa a partir del rectángulo del control que la abrió,
 * y la repliega al mismo sitio al cerrarse.
 *
 * **No escala el contenido.** Lo que se anima es un recorte: el contenido se
 * dispone desde el primer fotograma a su tamaño final y se va destapando. Un
 * `scaleIn` haría que el texto empezara diminuto y creciera, que es justo lo que
 * delata una transición falsa.
 *
 * Compose Multiplatform 1.6.11 no tiene transiciones de elemento compartido, así
 * que esto es lo más cerca que se puede estar sin ellas.
 *
 * El orden importa: al cerrar, el contenido se funde **primero** y la superficie
 * se repliega después. Al revés, la tarjeta encogería con la pantalla dentro.
 *
 * @param origin rectángulo de partida en coordenadas de la raíz (el
 *   `boundsInRoot()` del botón). Si es null la transición es solo un fundido.
 * @param expanded lo controla quien abre y cierra.
 * @param onCollapsed se invoca cuando el repliegue ha terminado del todo; es la
 *   señal para dejar de componer el overlay.
 */
@Composable
fun ExpandFromBounds(
    origin: Rect?,
    expanded: Boolean,
    onCollapsed: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.background,
    originColor: Color = containerColor,
    originCornerRadius: Dp = 12.dp,
    content: @Composable () -> Unit,
) {
    val animationsEnabled = LocalAnimationsEnabled.current
    val density = LocalDensity.current
    val originRadiusPx = with(density) { originCornerRadius.toPx() }

    var containerOrigin by remember { mutableStateOf(Offset.Zero) }
    var containerRect by remember { mutableStateOf(Rect.Zero) }

    val progress = remember { Animatable(if (expanded) 1f else 0f) }
    val contentAlpha = remember { Animatable(if (expanded) 1f else 0f) }

    LaunchedEffect(expanded, animationsEnabled) {
        if (!animationsEnabled) {
            progress.snapTo(if (expanded) 1f else 0f)
            contentAlpha.snapTo(if (expanded) 1f else 0f)
            if (!expanded) onCollapsed()
            return@LaunchedEffect
        }
        if (expanded) {
            progress.animateTo(1f, NeoFintechMotion.gentle())
        } else {
            delay(NeoFintechMotion.QUICK_MS.toLong())
            progress.animateTo(0f, NeoFintechMotion.gentle())
            onCollapsed()
        }
    }

    LaunchedEffect(expanded, animationsEnabled) {
        if (!animationsEnabled) return@LaunchedEffect
        if (expanded) {
            // Espera a que la superficie haya crecido lo bastante como para que
            // el contenido no aparezca dentro de un cuadrado de 40 dp.
            delay(NeoFintechMotion.QUICK_MS.toLong())
            contentAlpha.animateTo(
                1f,
                tween(NeoFintechMotion.MEDIUM_MS, easing = NeoFintechMotion.standard),
            )
        } else {
            contentAlpha.animateTo(
                0f,
                tween(NeoFintechMotion.QUICK_MS, easing = NeoFintechMotion.standard),
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                containerOrigin = coordinates.positionInRoot()
                containerRect = Rect(Offset.Zero, coordinates.size.toSize())
            }
            .drawWithContent {
                val full = Rect(Offset.Zero, size)
                // El origen llega en coordenadas de la raíz; este contenedor
                // puede estar desplazado (statusBarsPadding, por ejemplo), así
                // que hay que pasarlo a coordenadas locales o el recorte
                // arrancaría desplazado hacia abajo.
                val from = origin
                    ?.translate(-containerOrigin.x, -containerOrigin.y)
                    ?: full
                val fraction = progress.value
                val rect = lerpRect(from, full, fraction)
                val radius = originRadiusPx * (1f - fraction.coerceIn(0f, 1f))
                val path = Path().apply {
                    addRoundRect(RoundRect(rect, CornerRadius(radius, radius)))
                }
                clipPath(path) { this@drawWithContent.drawContent() }
            },
    ) {
        Box(modifier = Modifier.fillMaxSize().background(containerColor))

        if (originColor != containerColor) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val alpha = 1f - originColorFraction(progress.value)
                        if (alpha > 0f) drawRect(originColor, alpha = alpha)
                    },
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = contentAlpha.value },
        ) {
            content()
        }
    }
}
