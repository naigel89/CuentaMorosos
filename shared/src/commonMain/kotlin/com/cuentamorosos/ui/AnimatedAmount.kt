package com.cuentamorosos.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay

// ═══════════════════════════════════════════════════════════════════
// Lógica pura — sin Compose, comprobable en jvmTest
// ═══════════════════════════════════════════════════════════════════

/**
 * Alinea dos importes ya formateados por la derecha y devuelve, para cada
 * posición, el retardo que le toca al rodar.
 *
 * La regla es que **una columna que no cambia no se mueve**: recibe [NO_ROLL].
 * Las que sí cambian se numeran de izquierda a derecha y se escalonan, de modo
 * que el importe se resuelve de las unidades de mayor peso a las de menor y se
 * puede seguir leyendo mientras cambia.
 *
 * La alineación es por la derecha porque las cifras crecen por la izquierda:
 * al pasar de `99,00` a `101,00` la columna de las unidades tiene que seguir
 * siendo la misma, o el número entero parecería otro.
 *
 * @param staggerMs separación entre columnas consecutivas que sí cambian.
 */
internal fun rollDelays(from: String, to: String, staggerMs: Int): IntArray {
    val n = maxOf(from.length, to.length)
    val f = from.padStart(n, ' ')
    val t = to.padStart(n, ' ')
    val out = IntArray(n)
    var order = 0
    for (i in 0 until n) {
        if (f[i] == t[i]) {
            out[i] = NO_ROLL
        } else {
            out[i] = order * staggerMs
            order += 1
        }
    }
    return out
}

/** Marca de "esta columna no rueda" devuelta por [rollDelays]. */
internal const val NO_ROLL = -1

/**
 * Recuerda el importe que hay en pantalla ahora mismo para poder comparar con
 * el siguiente. Es una clase y no un `mutableStateOf` a propósito: cambiarla no
 * debe provocar recomposición, solo alimentar el cálculo de retardos.
 */
@Stable
internal class AmountRoller(initial: String, initialValue: Double) {
    var current: String = initial
        private set

    /** Sentido del último cambio: hacia arriba, la cifra nueva entra por abajo. */
    var goingUp: Boolean = true
        private set

    private var lastValue: Double = initialValue

    fun advance(next: String, nextValue: Double, staggerMs: Int): IntArray {
        goingUp = nextValue >= lastValue
        lastValue = nextValue
        val delays = rollDelays(current, next, staggerMs)
        current = next
        return delays
    }
}

// ═══════════════════════════════════════════════════════════════════
// Composable
// ═══════════════════════════════════════════════════════════════════

/**
 * Importe monetario en el que **cada dígito es una columna independiente**.
 *
 * Sustituye al patrón anterior ([rememberAnimatedAmount]), que interpolaba el
 * `Double` y volvía a formatear la cadena entera en cada fotograma: eso hacía
 * girar las seis columnas a la vez —las de menor peso, decenas de veces— y el
 * importe dejaba de leerse durante toda la animación. Aquí solo se mueve lo que
 * de verdad cambia.
 *
 * **Requiere una tipografía monoespaciada** ([JetBrainsMonoFontFamily]): con una
 * proporcional cada columna tendría un ancho distinto y el número bailaría al
 * rodar.
 *
 * El ancho no encoge dentro de una misma visita a la pantalla: se recuerda el
 * número de columnas más alto que se ha llegado a mostrar y las sobrantes se
 * dibujan como espacio. Sin eso, bajar de `1.284,50` a `984,50` movería de sitio
 * todo lo que hubiera al lado.
 *
 * Respeta [LocalAnimationsEnabled]: desactivado, el importe se dibuja
 * directamente en su valor final.
 *
 * @param countUp si true, la primera aparición rueda desde cero. Pásalo a false
 *   (por ejemplo con [EntranceTracker.isFirstAppearance]) cuando el elemento se
 *   recicla al hacer scroll, o el importe volverá a contar en cada reaparición.
 */
@Composable
fun AnimatedAmount(
    value: Double,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    prefix: String = "",
    suffix: String = "€",
    decimals: Int = 2,
    countUp: Boolean = true,
    staggerMs: Int = NeoFintechMotion.DIGIT_STAGGER_MS,
) {
    val animationsEnabled = LocalAnimationsEnabled.current
    val shouldRoll = countUp && animationsEnabled

    val target = formatAmount(value, prefix, suffix, decimals)
    val zero = formatAmount(0.0, prefix, suffix, decimals)

    // El ancho solo crece. Si el importe encoge, las columnas sobrantes quedan
    // en blanco en lugar de desaparecer y arrastrar el layout de al lado.
    var widest by remember { mutableStateOf(0) }
    val naturalWidth = maxOf(target.length, if (shouldRoll) zero.length else 0)
    if (naturalWidth > widest) widest = naturalWidth
    val padded = target.padStart(widest, ' ')

    // Se captura antes de que el roller avance: es lo que cada columna muestra
    // en su primera composición, y de ahí sale el conteo inicial.
    val startFrom = remember { if (shouldRoll) zero else target }
    val roller = remember { AmountRoller(startFrom.padStart(widest, ' '), if (shouldRoll) 0.0 else value) }
    val delays = remember(padded) { roller.advance(padded, value, staggerMs) }
    val goingUp = roller.goingUp
    val startPadded = startFrom.padStart(padded.length, ' ')

    if (!animationsEnabled) {
        Text(text = padded, modifier = modifier, style = style, color = color, maxLines = 1)
        return
    }

    Row(modifier = modifier) {
        padded.forEachIndexed { index, char ->
            // La clave cuenta desde la derecha: así una columna conserva su
            // identidad —y su estado— aunque aparezca una cifra nueva delante.
            key(padded.length - index) {
                AmountColumn(
                    from = startPadded.getOrElse(index) { ' ' },
                    to = char,
                    delayMs = delays.getOrElse(index) { NO_ROLL },
                    goingUp = goingUp,
                    style = style,
                    color = color,
                )
            }
        }
    }
}

@Composable
private fun AmountColumn(
    from: Char,
    to: Char,
    delayMs: Int,
    goingUp: Boolean,
    style: TextStyle,
    color: Color,
) {
    var shown by remember { mutableStateOf(from) }

    LaunchedEffect(to, delayMs) {
        if (shown == to) return@LaunchedEffect
        if (delayMs > 0) delay(delayMs.toLong())
        shown = to
    }

    AnimatedContent(
        targetState = shown,
        modifier = Modifier.clipToBounds(),
        transitionSpec = {
            val fade = tween<Float>(NeoFintechMotion.SHORT_MS, easing = NeoFintechMotion.standard)
            val enter = slideInVertically(NeoFintechMotion.placement) { h ->
                if (goingUp) h else -h
            } + fadeIn(fade)
            val exit = slideOutVertically(NeoFintechMotion.placement) { h ->
                if (goingUp) -h else h
            } + fadeOut(fade)
            enter togetherWith exit
        },
        label = "amountColumn",
    ) { c ->
        Text(text = c.toString(), style = style, color = color, maxLines = 1)
    }
}
