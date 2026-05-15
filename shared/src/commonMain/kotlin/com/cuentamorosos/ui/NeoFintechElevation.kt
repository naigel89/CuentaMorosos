package com.cuentamorosos.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp

/**
 * Shadow/elevation tokens for the Neo-Fintech Precision design system.
 *
 * Design reference:
 *   - Card shadow:  0px 4px 20px rgba(0,0,0,0.04)
 *   - Hover shadow: 0px 8px 30px rgba(0,0,0,0.08)
 *
 * Compose does not use CSS box-shadow syntax. Instead, `Modifier.shadow()`
 * approximates the effect via elevation + ambient/spot color. The values
 * below are tuned to match the design intent as closely as possible.
 */
object NeoFintechElevation {
    // Card shadow — subtle diffused shadow for resting state
    val cardShadowElevation = 4.dp

    // Hover shadow — increased elevation for interactive hover state
    val cardShadowHoverElevation = 8.dp

    // Shadow shape — matches NeoFintechShapes.lg
    val cardShadowShape = RoundedCornerShape(12.dp)

    // Shadow alpha for ambient/spot color (design: rgba(0,0,0,0.04))
    const val SHADOW_ALPHA_RESTING = 0.04f
    const val SHADOW_ALPHA_HOVER = 0.08f
}

/**
 * Applies the standard Neo-Fintech card shadow (resting state).
 * Uses [NeoFintechElevation.cardShadowShape] so the shadow follows rounded corners.
 */
fun Modifier.cardShadow(): Modifier = this.shadow(
    elevation = NeoFintechElevation.cardShadowElevation,
    shape = NeoFintechElevation.cardShadowShape,
    clip = false,
)

/**
 * Applies the Neo-Fintech card hover shadow (elevated state).
 */
fun Modifier.cardShadowHover(): Modifier = this.shadow(
    elevation = NeoFintechElevation.cardShadowHoverElevation,
    shape = NeoFintechElevation.cardShadowShape,
    clip = false,
)
