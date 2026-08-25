package com.cuentamorosos.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import kotlinx.coroutines.delay

/**
 * Lleva la cuenta de qué elementos ya han reproducido su animación de entrada.
 *
 * El problema que resuelve: un item de `LazyColumn` / `LazyVerticalGrid` se destruye
 * al salir de pantalla y se vuelve a componer al volver a entrar. Si la animación de
 * entrada vive dentro del item, se reproduce **cada vez** que se hace scroll, lo que
 * se percibe como parpadeo. Un tracker a nivel de pantalla recuerda las claves ya
 * vistas, de modo que cada elemento entra una sola vez por visita a la pantalla.
 *
 * Crear con [rememberEntranceTracker] y consultar con [isFirstAppearance] o pasarlo
 * a [Modifier.appearOnce].
 */
@Stable
class EntranceTracker internal constructor() {
    private val seen = mutableSetOf<Any>()

    /** Devuelve true la primera vez que se pregunta por [key], false después. */
    internal fun claim(key: Any): Boolean = seen.add(key)
}

/**
 * Crea un [EntranceTracker] con ámbito de pantalla.
 *
 * @param resetKeys cambiar cualquiera de estos valores descarta el tracker y permite
 *   que las entradas vuelvan a reproducirse (por ejemplo, al cambiar de filtro).
 */
@Composable
fun rememberEntranceTracker(vararg resetKeys: Any?): EntranceTracker =
    remember(*resetKeys) { EntranceTracker() }

/**
 * True solo la primera vez que [key] aparece en esta pantalla. El resultado se
 * memoriza en el punto de llamada, así que es estable durante toda la vida del
 * elemento aunque se recomponga.
 */
@Composable
fun EntranceTracker.isFirstAppearance(key: Any): Boolean = remember(key) { claim(key) }

/**
 * Número máximo de items que participan en el escalonado. A partir de ahí todos
 * comparten el mismo retardo: sin este tope, el item 30 de una lista esperaría
 * tres segundos antes de aparecer.
 */
private const val MAX_STAGGERED_ITEMS = 5

/**
 * Animación de entrada (fundido + desplazamiento vertical) que se reproduce **una
 * sola vez** por elemento, aunque el elemento se recomponga al hacer scroll.
 *
 * Pensado para items de listas lazy. En contenedores no-lazy (una `Column` normal)
 * [fadeInStaggered] y [slideUp] siguen siendo suficientes, porque ahí el elemento
 * se compone una vez y no se recicla.
 *
 * Respeta [LocalAnimationsEnabled]: si las animaciones están desactivadas, devuelve
 * el modificador sin tocar y el elemento se dibuja directamente en su estado final.
 *
 * @param tracker  tracker de pantalla creado con [rememberEntranceTracker].
 * @param key      identidad estable del elemento (normalmente el mismo `id` que se
 *                 usa como `key` del item en la lista).
 * @param index    posición en la lista; determina el retardo del escalonado.
 */
@Composable
fun Modifier.appearOnce(
    tracker: EntranceTracker,
    key: Any,
    index: Int = 0,
    slideDistance: Dp = 12.dp,
    staggerMs: Int = NeoFintechAnimations.FADE_IN_DELAY_PER_ITEM_MS,
    durationMs: Int = NeoFintechMotion.LONG_MS,
): Modifier {
    val animationsEnabled = LocalAnimationsEnabled.current
    val shouldPlay = remember(key) { animationsEnabled && tracker.claim(key) }
    val progress = remember(key) { Animatable(if (shouldPlay) 0f else 1f) }
    val distancePx = with(LocalDensity.current) { slideDistance.toPx() }

    LaunchedEffectIfPlaying(shouldPlay, key) {
        delay((index.coerceAtMost(MAX_STAGGERED_ITEMS) * staggerMs).toLong())
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMs, easing = NeoFintechMotion.emphasized),
        )
    }

    return if (!shouldPlay) {
        this
    } else {
        // Lectura diferida: `progress` se lee en fase de dibujo, no de composición,
        // así que la animación no recompone el item en cada frame.
        this.graphicsLayer {
            val p = progress.value
            alpha = p
            translationY = distancePx * (1f - p)
        }
    }
}

/**
 * `LaunchedEffect` que solo arranca la corrutina cuando [playing] es true, sin
 * introducir un `if` alrededor de la llamada (lo que cambiaría la estructura de
 * grupos de la composición entre recomposiciones).
 */
@Composable
private fun LaunchedEffectIfPlaying(
    playing: Boolean,
    key: Any,
    block: suspend () -> Unit,
) {
    androidx.compose.runtime.LaunchedEffect(playing, key) {
        if (playing) block()
    }
}

/**
 * Escala hacia dentro mientras el dedo está apoyado y vuelve con un muelle al soltar.
 *
 * Es la pieza suelta de [pressable], pensada para cadenas de modificadores donde el
 * `clip` no puede ir en el mismo punto que la escala: al aplicar `pressScale` primero
 * de toda la cadena, la sombra y el borde escalan **junto con** el contenido; si la
 * escala fuera después, la tarjeta encogería dentro de un borde inmóvil.
 */
@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = NeoFintechAnimations.BUTTON_PRESS_SCALE,
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val animationsEnabled = LocalAnimationsEnabled.current
    val scale by animateFloatAsState(
        targetValue = if (isPressed && animationsEnabled) pressedScale else 1f,
        animationSpec = NeoFintechMotion.snappy(),
        label = "pressScale",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Área pulsable con feedback físico: escala al pulsar y ripple recortado a [shape].
 *
 * Sustituye al patrón `hoverable` + `collectIsHoveredAsState`, que en un dispositivo
 * táctil nunca se activa. El `clip` no es cosmético: sin él, el ripple se dibuja
 * rectangular por encima de las esquinas redondeadas.
 *
 * Para tarjetas con sombra y borde usa [pressableCard], que además ordena la cadena
 * correctamente.
 *
 * @param interactionSource pásalo si además necesitas observar el estado de pulsación
 *   desde fuera. Si es null se crea uno interno.
 */
@Composable
fun Modifier.pressable(
    onClick: () -> Unit,
    shape: Shape,
    enabled: Boolean = true,
    pressedScale: Float = NeoFintechAnimations.BUTTON_PRESS_SCALE,
    role: Role? = null,
    interactionSource: MutableInteractionSource? = null,
): Modifier {
    val fallbackSource = remember { MutableInteractionSource() }
    val source = interactionSource ?: fallbackSource

    return this
        .pressScale(source, pressedScale)
        .clip(shape)
        .clickable(
            interactionSource = source,
            indication = LocalIndication.current,
            enabled = enabled,
            role = role,
            onClick = onClick,
        )
}

/**
 * Cadena completa de tarjeta pulsable: escala → sombra (con elevación animada) →
 * borde → recorte → clic.
 *
 * El orden importa y es la razón de que esto exista como token y no se escriba a mano
 * en cada tarjeta:
 *
 *  1. la escala va **primero**, para que sombra y borde escalen con el contenido;
 *  2. la sombra va antes del recorte, o el recorte se la comería;
 *  3. el recorte va antes del clic, o el ripple se sale de las esquinas.
 *
 * @param borderColor null para una tarjeta sin borde.
 */
@Composable
fun Modifier.pressableCard(
    onClick: () -> Unit,
    shape: Shape,
    borderColor: Color? = null,
    borderWidth: Dp = 1.dp,
    restingElevation: Dp = NeoFintechElevation.cardShadowElevation,
    pressedElevation: Dp = NeoFintechElevation.cardShadowHoverElevation,
    enabled: Boolean = true,
    role: Role? = Role.Button,
    interactionSource: MutableInteractionSource? = null,
): Modifier {
    val fallbackSource = remember { MutableInteractionSource() }
    val source = interactionSource ?: fallbackSource
    val isPressed by source.collectIsPressedAsState()
    val animationsEnabled = LocalAnimationsEnabled.current

    val elevation by animateDpAsState(
        targetValue = if (isPressed && animationsEnabled) pressedElevation else restingElevation,
        animationSpec = NeoFintechMotion.offsetDp,
        label = "cardElevation",
    )

    return this
        .pressScale(source)
        .shadow(elevation = elevation, shape = shape, clip = false)
        .then(if (borderColor != null) Modifier.border(borderWidth, borderColor, shape) else Modifier)
        .clip(shape)
        .clickable(
            interactionSource = source,
            indication = LocalIndication.current,
            enabled = enabled,
            role = role,
            onClick = onClick,
        )
}

// ═══════════════════════════════════════════════════════════════════
// Shimmer — barrido de carga
// ═══════════════════════════════════════════════════════════════════

/**
 * Barrido de brillo sobre un bloque de esqueleto, para que se lea como
 * "cargando" y no como "roto".
 *
 * Es un degradado en movimiento dibujado encima del contenido, no un desenfoque:
 * dentro de Compose Multiplatform 1.6.11 `Modifier.blur` solo existe en Android,
 * la misma razón por la que existe [neonGlow].
 *
 * Va **después** del `background` en la cadena de modificadores, o el degradado
 * quedaría debajo del relleno. Recorta con `clip(shape)` antes si el bloque
 * tiene esquinas redondeadas.
 *
 * Respeta [LocalAnimationsEnabled]: desactivado devuelve el modificador intacto,
 * sin dejar una animación infinita corriendo de fondo.
 */
@Composable
fun Modifier.shimmer(
    enabled: Boolean = true,
    durationMs: Int = NeoFintechAnimations.SHIMMER_DURATION_MS,
): Modifier {
    if (!enabled || !LocalAnimationsEnabled.current) return this

    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerSweep",
    )

    return this.drawWithContent {
        drawContent()
        val width = size.width
        if (width > 0f) {
            // La banda recorre el ancho completo más su propio tamaño a cada
            // lado, así que entra y sale por fuera en vez de aparecer de golpe.
            val band = width * 0.45f
            val center = (1f - progress) * (width + band * 2f) - band
            val brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, highlight, Color.Transparent),
                start = Offset(center - band, 0f),
                end = Offset(center + band, 0f),
            )
            drawRect(brush = brush)
        }
    }
}
