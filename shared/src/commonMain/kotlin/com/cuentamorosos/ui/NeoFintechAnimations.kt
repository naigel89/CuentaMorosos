package com.cuentamorosos.ui

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

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
}

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
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxWidth(animatedProportion)
            .background(color, shape),
    )
}
