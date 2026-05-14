package com.cuentamorosos.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Neon glow effect using layered alpha shadows.
 *
 * Compose Multiplatform 1.6 does not support RenderEffect.createBlurEffect,
 * so we approximate a glow using concentric circles with decreasing alpha.
 *
 * This modifier is a no-op when not in dark mode context.
 */
fun Modifier.neonGlow(
    color: Color = NeoFintechColors.dark().primaryContainer,
    blurRadius: Dp = 8.dp,
    intensity: Float = 0.6f,
    isDarkMode: Boolean = true,
): Modifier = if (!isDarkMode) {
    this
} else {
    this.drawBehind {
        val radiusPx = blurRadius.toPx()
        val center = Offset(size.width / 2, size.height / 2)
        val maxDim = maxOf(size.width, size.height) / 2

        // 3-layer shadow approximation
        drawGlowCircle(center, maxDim + radiusPx, color, intensity * 0.15f)
        drawGlowCircle(center, maxDim + radiusPx * 0.7f, color, intensity * 0.3f)
        drawGlowCircle(center, maxDim + radiusPx * 0.4f, color, intensity * 0.5f)
    }
}

private fun DrawScope.drawGlowCircle(
    center: Offset,
    radius: Float,
    color: Color,
    alpha: Float,
) {
    drawCircle(
        color = color.copy(alpha = alpha.coerceIn(0f, 1f)),
        radius = radius,
        center = center,
    )
}
