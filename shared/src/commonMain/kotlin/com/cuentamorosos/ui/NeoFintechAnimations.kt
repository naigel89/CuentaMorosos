package com.cuentamorosos.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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

    // Button press scale factor (subtle press feedback)
    const val BUTTON_PRESS_SCALE = 0.98f
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
