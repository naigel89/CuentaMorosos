package com.cuentamorosos.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Animation and transition tokens for the Neo-Fintech Precision design system.
 *
 * Compose does not use CSS transitions; these constants document the intended
 * timing and easing so every animated element stays consistent.
 */
object NeoFintechAnimations {
    // Standard card hover transition duration (matches CSS "duration-300")
    const val DURATION_MS = 300
    const val DURATION_SHORT_MS = 150

    // Navigation pager transition token (HorizontalPager uses ease-out snap by default)
    const val EASING = "ease-out"
    const val NAV_TRANSITION_DURATION_MS = 300

    // Button press scale factor (subtle press feedback)
    const val BUTTON_PRESS_SCALE = 0.98f

    // Count-up animation tokens (spec: ~1.2s with ease-out easing)
    const val COUNT_UP_DURATION_MS = 1200

    // Proportion bar animation tokens
    const val PROPORTION_BAR_DURATION_MS = 600
    const val STAGGER_DELAY_MS = 80

    // Staggered fade-in defaults
    const val FADE_IN_DELAY_PER_ITEM_MS = 100
    const val FADE_IN_DURATION_MS = 300

    // Slide-up defaults
    const val SLIDE_UP_DISTANCE_DP = 24f
    const val SLIDE_UP_DURATION_MS = 400

    // Animated counter defaults
    const val ANIMATED_COUNTER_DURATION_MS = 800

    // Max simultaneous animations (performance guard)
    const val MAX_SIMULTANEOUS_ANIMATIONS = 4

    // Money explosion celebration animation tokens
    const val EXPLOSION_DURATION_MS = 1500
    const val EXPLOSION_MAX_PARTICLES = 25
}

// ═══════════════════════════════════════════════════════════════════
// Pure utility functions (testable without Compose)
// ═══════════════════════════════════════════════════════════════════

/**
 * Formats a numeric [value] into a human-readable monetary string using Spanish
 * locale conventions (period as thousands separator, comma as decimal separator).
 *
 * This is a pure, non-Composable function suitable for direct unit testing.
 *
 * @param value    The numeric amount.
 * @param prefix   String prepended before the number (e.g. "$", "€").
 * @param suffix   String appended after the number (default "€").
 * @param decimals Number of decimal places (default 2).
 */
fun formatAmount(value: Double, prefix: String = "", suffix: String = "€", decimals: Int = 2): String {
    val factor = when (decimals) {
        0 -> 1.0
        1 -> 10.0
        2 -> 100.0
        3 -> 1000.0
        4 -> 10000.0
        5 -> 100000.0
        6 -> 1000000.0
        else -> {
            var f = 1.0
            repeat(decimals) { f *= 10 }
            f
        }
    }
    val factorLong = factor.toLong()

    // Scale to integer to avoid floating-point drift in the split below
    val scaled = kotlin.math.round(value * factor)
    val isNegative = scaled < 0
    val absScaled = kotlin.math.abs(scaled).toLong()

    val integerPart = absScaled / factorLong
    val fractionalPart = absScaled % factorLong

    val integerStr = if (integerPart == 0L) "0" else {
        integerPart.toString().reversed().chunked(3).joinToString(".").reversed()
    }

    val fractionalStr = if (decimals > 0) {
        "," + fractionalPart.toString().padStart(decimals, '0')
    } else {
        ""
    }

    val signed = if (isNegative) "-" else ""
    return "$prefix$signed$integerStr$fractionalStr$suffix"
}

// ═══════════════════════════════════════════════════════════════════
// Animation Coordinator (testable class)
// ═══════════════════════════════════════════════════════════════════

/**
 * Manages a fixed pool of animation slots to limit simultaneous animations.
 * This is a plain Kotlin class — no Compose dependency — making it fully
 * unit-testable.
 *
 * @param maxSimultaneous Maximum concurrent animations allowed.
 */
class AnimationCoordinator(private val maxSimultaneous: Int) {
    private val slots = BooleanArray(maxSimultaneous)

    /**
     * Attempts to reserve a free animation slot.
     * @return the slot index (0..max-1) or -1 if all slots are occupied.
     */
    fun requestSlot(): Int {
        for (i in 0 until maxSimultaneous) {
            if (!slots[i]) {
                slots[i] = true
                return i
            }
        }
        return -1
    }

    /**
     * Releases a previously reserved slot so it can be reused.
     * Out-of-bounds indices are silently ignored.
     */
    fun releaseSlot(slot: Int) {
        if (slot in 0 until maxSimultaneous) {
            slots[slot] = false
        }
    }

    /** @return true if at least one slot is free. */
    fun hasAvailableSlot(): Boolean = slots.any { !it }
}

/**
 * Remembers an [AnimationCoordinator] with the given capacity.
 * The coordinator survives recompositions but is re-created if
 * [maxSimultaneous] changes.
 */
@Composable
fun rememberAnimationCoordinator(maxSimultaneous: Int = NeoFintechAnimations.MAX_SIMULTANEOUS_ANIMATIONS): AnimationCoordinator {
    return remember(maxSimultaneous) { AnimationCoordinator(maxSimultaneous) }
}

// ═══════════════════════════════════════════════════════════════════
// Accessibility / Animation toggle
// ═══════════════════════════════════════════════════════════════════

/**
 * CompositionLocal that globally controls whether animations are rendered.
 * When set to `false`, all animated modifiers short-circuit to their final
 * static state. Defaults to `true`.
 */
val LocalAnimationsEnabled = staticCompositionLocalOf { true }

/**
 * Pure function that resolves whether animations should run.
 *
 * @param systemAnimationsEnabled  Result of the platform accessibility check
 *                                 (e.g., Android's `AnimatorDurationScale`).
 * @param appAnimationsEnabled     The app-level toggle ([LocalAnimationsEnabled]).
 * @return true only when both the system and the app allow animations.
 */
fun shouldAnimate(systemAnimationsEnabled: Boolean, appAnimationsEnabled: Boolean): Boolean {
    return systemAnimationsEnabled && appAnimationsEnabled
}

// ═══════════════════════════════════════════════════════════════════
// Animated Amount Counter (composable)
// ═══════════════════════════════════════════════════════════════════

/**
 * Animates a monetary [targetValue] from 0 (or the current animated value)
 * to the target, formatting the result as a human-readable string via
 * [formatAmount].
 *
 * Starts from 0 on initial render. On subsequent target changes, animates
 * from the current displayed value to the new target.
 *
 * Edge cases:
 * - Zero value → animates to/from 0 (no visible movement, but functional).
 * - Negative values → formats with a leading minus sign.
 *
 * @param targetValue   The final monetary amount to animate toward.
 * @param durationMillis Duration of the animation in ms (default 800).
 * @param prefix        String prepended before the number.
 * @param suffix        String appended after the number (default "€").
 * @param decimals      Number of decimal places (default 2).
 */
@Composable
fun rememberAnimatedAmount(
    targetValue: Double,
    durationMillis: Int = NeoFintechAnimations.ANIMATED_COUNTER_DURATION_MS,
    prefix: String = "",
    suffix: String = "€",
    decimals: Int = 2,
    countUp: Boolean = true,
): String {
    val animationsEnabled = LocalAnimationsEnabled.current
    val shouldCountUp = countUp && animationsEnabled
    // Sembrar en el valor final cuando no toca contar evita que el importe vuelva
    // a subir desde 0 cada vez que la tarjeta se recompone (p. ej. al hacer scroll).
    val animatable = remember { Animatable(if (shouldCountUp) 0f else targetValue.toFloat()) }
    LaunchedEffect(targetValue, shouldCountUp) {
        if (shouldCountUp) {
            animatable.animateTo(
                targetValue = targetValue.toFloat(),
                animationSpec = tween(
                    durationMillis = durationMillis,
                    easing = NeoFintechMotion.emphasized,
                ),
            )
        } else {
            animatable.snapTo(targetValue.toFloat())
        }
    }
    return formatAmount(animatable.value.toDouble(), prefix, suffix, decimals)
}

// ═══════════════════════════════════════════════════════════════════
// Fade-in Staggered Modifier
// ═══════════════════════════════════════════════════════════════════

/**
 * Tope de items que participan en el escalonado. Sin él, el item 30 de una lista
 * esperaría tres segundos antes de aparecer.
 */
private const val MAX_STAGGERED_INDEX = 5

/**
 * Applies a staggered fade-in animation to a composable.
 *
 * Each item fades in from alpha 0 → 1 after a delay proportional to its
 * [index] × [delayPerItemMs], creating a cascading reveal effect.
 *
 * Cuando [enabled] es false (o las animaciones están desactivadas vía
 * [LocalAnimationsEnabled]) el elemento se dibuja directamente opaco, sin animar.
 *
 * @param index          Zero-based position in the list (drives stagger delay).
 * @param delayPerItemMs Delay between each item's fade start (default 100).
 * @param fadeDurationMs Duration of each individual fade animation (default 300).
 * @param enabled        Whether the animation is active.
 *
 * En listas lazy usa [Modifier.appearOnce] en su lugar: este modificador se
 * reproduce de nuevo cada vez que el item se recicla al hacer scroll.
 */
@Composable
fun Modifier.fadeInStaggered(
    index: Int,
    delayPerItemMs: Int = NeoFintechAnimations.FADE_IN_DELAY_PER_ITEM_MS,
    fadeDurationMs: Int = NeoFintechAnimations.FADE_IN_DURATION_MS,
    enabled: Boolean = true,
): Modifier {
    val shouldPlay = enabled && LocalAnimationsEnabled.current
    val alphaAnim = remember { Animatable(if (shouldPlay) 0f else 1f) }
    LaunchedEffect(index, shouldPlay) {
        if (shouldPlay) {
            kotlinx.coroutines.delay((index.coerceAtMost(MAX_STAGGERED_INDEX) * delayPerItemMs).toLong())
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = fadeDurationMs, easing = NeoFintechMotion.standard),
            )
        } else {
            alphaAnim.snapTo(1f)
        }
    }
    // Lectura diferida del valor animado: solo invalida la capa de dibujo.
    return this.graphicsLayer { alpha = alphaAnim.value }
}

// ═══════════════════════════════════════════════════════════════════
// Slide-up Modifier
// ═══════════════════════════════════════════════════════════════════

/**
 * Applies a slide-up + fade-in entrance animation.
 *
 * The composable slides up by [distanceDp] (from below) while fading in
 * from alpha 0 → 1. An optional [delayMs] postpones the animation start.
 *
 * When [enabled] is false the modifier renders at the rest position
 * (translationY = 0, alpha = 1) — i.e., fully visible without animation.
 *
 * @param distanceDp Vertical distance the element travels (default 24 dp).
 * @param durationMs Duration of the slide + fade animation (default 400).
 * @param delayMs    Initial delay before the animation begins (default 0).
 * @param enabled    Whether the entrance animation plays.
 */
@Composable
fun Modifier.slideUp(
    distanceDp: Float = NeoFintechAnimations.SLIDE_UP_DISTANCE_DP,
    durationMs: Int = NeoFintechAnimations.SLIDE_UP_DURATION_MS,
    delayMs: Int = 0,
    enabled: Boolean = true,
): Modifier {
    val shouldPlay = enabled && LocalAnimationsEnabled.current
    val distancePx = with(LocalDensity.current) { distanceDp.dp.toPx() }
    val progress = remember { Animatable(if (shouldPlay) 0f else 1f) }

    LaunchedEffect(shouldPlay) {
        if (shouldPlay) {
            kotlinx.coroutines.delay(delayMs.toLong())
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = durationMs, easing = NeoFintechMotion.emphasized),
            )
        } else {
            progress.snapTo(1f)
        }
    }
    // Un solo Animatable para desplazamiento y opacidad: van siempre acompasados,
    // y así no hay dos corrutinas que puedan desincronizarse.
    return this.graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = distancePx * (1f - p)
    }
}

// ═══════════════════════════════════════════════════════════════════
// Existing helpers (preserved — used by other screens)
// ═══════════════════════════════════════════════════════════════════

// `Modifier.buttonPressAnimation()` se ha retirado: no tenía ningún uso y añadía un
// `clickable` vacío propio, de modo que se tragaba el clic del componente sobre el que
// se aplicara. Su sustituto es `Modifier.pressable(onClick, shape)` en
// EntranceAnimation.kt, que sí propaga el clic y recorta el ripple a la forma.

/**
 * Remembers an animated double value that transitions smoothly from 0 to [targetValue]
 * when [targetValue] changes. Uses a 1.2s tween with FastOutSlowInEasing for a count-up
 * effect suitable for monetary amounts.
 *
 * When [targetValue] is 0.0, the animation is a no-op (0 → 0).
 */
@Composable
fun rememberAnimatedDouble(targetValue: Double): Double {
    val animatedValue by animateFloatAsState(
        targetValue = targetValue.toFloat(),
        animationSpec = tween(
            durationMillis = NeoFintechAnimations.COUNT_UP_DURATION_MS,
            easing = NeoFintechMotion.emphasized,
        ),
        label = "animatedDouble",
    )
    return animatedValue.toDouble()
}

/**
 * A composable that renders a horizontal proportion bar with animated width.
 * The [proportion] value (0.0..1.0) is animated using [PROPORTION_BAR_DURATION_MS].
 * Optional [delayMillis] adds a stagger delay before the animation starts.
 */
@Composable
fun AnimatedProportionBar(
    proportion: Float,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color,
    shape: androidx.compose.foundation.shape.CornerBasedShape = NeoFintechShapes.sm,
    delayMillis: Int = 0,
) {
    val animatedProportion by animateFloatAsState(
        targetValue = proportion,
        animationSpec = tween(
            durationMillis = NeoFintechAnimations.PROPORTION_BAR_DURATION_MS,
            delayMillis = delayMillis,
            easing = NeoFintechMotion.emphasized,
        ),
        label = "proportionBar",
    )
    Box(
        modifier = modifier
            .fillMaxWidth(animatedProportion)
            .background(color, shape),
    )
}
