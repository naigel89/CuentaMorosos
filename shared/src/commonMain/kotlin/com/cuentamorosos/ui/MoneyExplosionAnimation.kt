package com.cuentamorosos.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ─── Bill particle data ───────────────────────────────────────────────────────

/**
 * Describes a single bill particle in the money explosion animation.
 * All values are pre-computed at composition time for consistent animation.
 */
internal data class BillParticle(
    val angle: Float,
    val velocity: Float,
    val rotation: Float,
    val sizeScale: Float,
    val hueIndex: Int,
    val delayMs: Int,
)

/** Green euro-bill color palette — NeoFintech friendly. */
private val BILL_COLORS = listOf(
    Color(0xFF00A651),  // NeoFintech green
    Color(0xFF00C853),  // Lighter accent
    Color(0xFF66BB6A),  // Highlight
)

// Particle visual size in dp (landscape rectangle like a bill)
private const val BILL_WIDTH_DP = 28f
private const val BILL_HEIGHT_DP = 14f
private const val BILL_CORNER_DP = 3f

// ─── Private helpers ──────────────────────────────────────────────────────────

/**
 * Generates [count] random bill particles with varied trajectories.
 */
internal fun generateBillParticles(count: Int): List<BillParticle> {
    val rng = Random
    return List(count) { index ->
        BillParticle(
            angle = rng.nextFloat() * 360f,
            velocity = 0.4f + rng.nextFloat() * 1.2f,
            rotation = rng.nextFloat() * 360f,
            sizeScale = 0.6f + rng.nextFloat() * 0.5f,
            hueIndex = rng.nextInt(BILL_COLORS.size),
            delayMs = index * 30,
        )
    }
}

/**
 * Computes the current alpha for a particle given normalised animation progress [t] (0..1).
 * Fades from 1.0 at start to 0.0 at ~70% progress, stays at 0 thereafter.
 */
internal fun particleAlpha(t: Float): Float {
    if (t <= 0f) return 0f
    val fadeEnd = 0.7f
    return if (t < fadeEnd) 1f else (1f - (t - fadeEnd) / (1f - fadeEnd)).coerceIn(0f, 1f)
}

/**
 * Computes the current scale for a particle given normalised progress [t].
 * Scales from 1.0 down to 0.3 linearly.
 */
internal fun particleScale(t: Float): Float {
    if (t <= 0f) return 0f
    return (1f - t * 0.7f).coerceIn(0.3f, 1f)
}

// ─── Composable ───────────────────────────────────────────────────────────────

/**
 * Full-screen celebration overlay with animated euro-bill particles exploding
 * from the center. Auto-dismisses after [NeoFintechAnimations.EXPLOSION_DURATION_MS]
 * or on tap.
 *
 * When [isVisible] becomes `true`, the animation plays. When it completes or
 * the user taps, [onDismiss] is called.
 *
 * Respects [LocalAnimationsEnabled] — if disabled, calls [onDismiss] immediately.
 */
@Composable
fun MoneyExplosionAnimation(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animationsEnabled = LocalAnimationsEnabled.current

    // Bail out early if animations are disabled
    if (!animationsEnabled) {
        if (isVisible) {
            LaunchedEffect(Unit) { onDismiss() }
        }
        return
    }

    if (!isVisible) return

    val density = LocalDensity.current
    val billWidthPx = with(density) { BILL_WIDTH_DP.dp.toPx() }
    val billHeightPx = with(density) { BILL_HEIGHT_DP.dp.toPx() }
    val cornerPx = with(density) { BILL_CORNER_DP.dp.toPx() }

    // Generate particles once at composition
    val particles = remember {
        generateBillParticles(NeoFintechAnimations.EXPLOSION_MAX_PARTICLES)
    }

    // Single progress driver: 0 → 1 over EXPLOSION_DURATION_MS
    // Use Animatable + asState() so Canvas recomposes on each frame
    val progressAnim = remember { Animatable(0f) }

    // Launch animation when isVisible
    LaunchedEffect(isVisible) {
        progressAnim.snapTo(0f)
        progressAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = NeoFintechAnimations.EXPLOSION_DURATION_MS,
            ),
        )
        onDismiss()
    }

    // Read via asState() to trigger Canvas recomposition
    val progress by progressAnim.asState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
            // Early dismiss on tap
            onDismiss()
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val maxRadius = maxOf(size.width, size.height) * 0.7f
            val t = progress.coerceIn(0f, 1f)

            // Semi-transparent dark background
            drawRect(
                color = Color.Black.copy(alpha = 0.3f),
                size = size,
            )

            // Draw each particle
            for (particle in particles) {
                drawBillParticle(
                    particle = particle,
                    t = t,
                    cx = cx,
                    cy = cy,
                    maxRadius = maxRadius,
                    billWidth = billWidthPx,
                    billHeight = billHeightPx,
                    cornerRadius = cornerPx,
                )
            }
        }
    }
}

/**
 * Draws a single bill particle at animation progress [t].
 *
 * The particle launches from near the center with a staggered delay,
 * flies outward in [particle.angle] direction, rotates, scales down, and fades.
 */
private fun DrawScope.drawBillParticle(
    particle: BillParticle,
    t: Float,
    cx: Float,
    cy: Float,
    maxRadius: Float,
    billWidth: Float,
    billHeight: Float,
    cornerRadius: Float,
) {
    // Stagger: shift the effective progress for this particle
    val staggerOffset = particle.delayMs.toFloat() / NeoFintechAnimations.EXPLOSION_DURATION_MS
    val effectiveT = ((t - staggerOffset) / (1f - staggerOffset)).coerceIn(0f, 1f)

    if (effectiveT <= 0f) return

    val angleRad = particle.angle * (kotlin.math.PI.toFloat() / 180f)
    val distance = effectiveT * maxRadius * particle.velocity
    val px = cx + cos(angleRad) * distance
    val py = cy + sin(angleRad) * distance

    val alpha = particleAlpha(effectiveT)
    val scale = particleScale(effectiveT)
    val rotationDeg = particle.rotation * effectiveT * 3f

    val color = BILL_COLORS[particle.hueIndex].copy(alpha = alpha)

    rotate(rotationDeg, pivot = Offset(px, py)) {
        scale(scale, scale, pivot = Offset(px, py)) {
            drawRoundRect(
                color = color,
                topLeft = Offset(px - billWidth / 2f, py - billHeight / 2f),
                size = Size(billWidth, billHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
            )
            // Subtle lighter stripe in the middle for "bill fold" effect
            drawRect(
                color = Color.White.copy(alpha = 0.15f * alpha),
                topLeft = Offset(px - billWidth * 0.3f, py - billHeight * 0.3f),
                size = Size(billWidth * 0.6f, billHeight * 0.6f),
            )
        }
    }
}
