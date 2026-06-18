package com.cuentamorosos.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

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
): String {
    val animatable = remember { Animatable(0f) }
    LaunchedEffect(targetValue) {
        animatable.animateTo(
            targetValue = targetValue.toFloat(),
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = FastOutSlowInEasing,
            ),
        )
    }
    return formatAmount(animatable.value.toDouble(), prefix, suffix, decimals)
}

// ═══════════════════════════════════════════════════════════════════
// Fade-in Staggered Modifier
// ═══════════════════════════════════════════════════════════════════

/**
 * Applies a staggered fade-in animation to a composable.
 *
 * Each item fades in from alpha 0 → 1 after a delay proportional to its
 * [index] × [delayPerItemMs], creating a cascading reveal effect.
 *
 * When [enabled] is false the modifier renders at alpha 0 (hidden).
 *
 * @param index          Zero-based position in the list (drives stagger delay).
 * @param delayPerItemMs Delay between each item's fade start (default 100).
 * @param fadeDurationMs Duration of each individual fade animation (default 300).
 * @param enabled        Whether the animation is active.
 */
fun Modifier.fadeInStaggered(
    index: Int,
    delayPerItemMs: Int = NeoFintechAnimations.FADE_IN_DELAY_PER_ITEM_MS,
    fadeDurationMs: Int = NeoFintechAnimations.FADE_IN_DURATION_MS,
    enabled: Boolean = true,
): Modifier = composed {
    val alphaAnim = remember { Animatable(if (enabled) 0f else 1f) }
    LaunchedEffect(index, enabled) {
        if (enabled) {
            kotlinx.coroutines.delay((index * delayPerItemMs).toLong())
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = fadeDurationMs),
            )
        } else {
            alphaAnim.snapTo(1f)
        }
    }
    graphicsLayer(alpha = alphaAnim.value)
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
fun Modifier.slideUp(
    distanceDp: Float = NeoFintechAnimations.SLIDE_UP_DISTANCE_DP,
    durationMs: Int = NeoFintechAnimations.SLIDE_UP_DURATION_MS,
    delayMs: Int = 0,
    enabled: Boolean = true,
): Modifier = composed {
    val density = LocalDensity.current
    val distancePx = with(density) { distanceDp.dp.toPx() }
    val offsetY = remember { Animatable(if (enabled) distancePx else 0f) }
    val alpha = remember { Animatable(if (enabled) 0f else 1f) }

    LaunchedEffect(enabled) {
        if (enabled) {
            kotlinx.coroutines.delay(delayMs.toLong())
            launch {
                offsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = durationMs, easing = FastOutSlowInEasing),
                )
            }
            launch {
                alpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = durationMs, easing = FastOutSlowInEasing),
                )
            }
        } else {
            offsetY.snapTo(0f)
            alpha.snapTo(1f)
        }
    }
    graphicsLayer(
        translationY = offsetY.value,
        alpha = alpha.value,
    )
}

// ═══════════════════════════════════════════════════════════════════
// Existing helpers (preserved — used by other screens)
// ═══════════════════════════════════════════════════════════════════

/**
 * Applies a press-scale animation to a clickable composable.
 * Scales down to [NeoFintechAnimations.BUTTON_PRESS_SCALE] when pressed,
 * using the standard short duration.
 */
fun Modifier.buttonPressAnimation(): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) NeoFintechAnimations.BUTTON_PRESS_SCALE else 1f,
        animationSpec = tween(durationMillis = NeoFintechAnimations.DURATION_SHORT_MS),
        label = "buttonPressScale",
    )
    this
        .graphicsLayer(scaleX = scale, scaleY = scale)
        .clickable(interactionSource = interactionSource, indication = null) {
            // Click handled by parent
        }
}

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
            easing = FastOutSlowInEasing,
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
        ),
        label = "proportionBar",
    )
    Box(
        modifier = modifier
            .fillMaxWidth(animatedProportion)
            .background(color, shape),
    )
}
