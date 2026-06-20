package com.cuentamorosos.ui.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.cuentamorosos.ui.LocalAnimationsEnabled
import com.cuentamorosos.ui.NeoFintechColors
import com.cuentamorosos.ui.neonGlow

/**
 * Animated splash/auth entrance screen with a 5-phase choreographed sequence.
 *
 * Phases:
 * 1. Logo fade-in (0ms, ~400ms duration)
 * 2. Logo slide-up (800ms, ~500ms duration, 120dp upward)
 * 3. Title slide-up (1400ms, via slideUp modifier)
 * 4. Form elements staggered (1800ms+, via slideUp modifiers with 100ms inter-item delay)
 * 5. Complete callback (~3000ms)
 */
@Composable
fun SplashAuthScreen(
    logo: @Composable (modifier: Modifier) -> Unit,
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animationsEnabled = LocalAnimationsEnabled.current
    val isDark = isSystemInDarkTheme()
    val colors = remember(isDark) { if (isDark) NeoFintechColors.dark() else NeoFintechColors.light() }
    val density = LocalDensity.current
    val logoSlideDistancePx = remember { with(density) { 120.dp.toPx() } }

    val logoAlpha = remember { Animatable(if (animationsEnabled) 0f else 1f) }
    val logoOffsetY = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (!animationsEnabled) {
            onAnimationComplete()
            return@LaunchedEffect
        }

        // Phase 1: Logo fade-in (0ms)
        logoAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 400),
        )

        // Phase 2: Logo slide-up (800ms)
        kotlinx.coroutines.delay(400)
        logoOffsetY.animateTo(
            targetValue = -logoSlideDistancePx,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        )

        // Phase 3 (1400ms) and Phase 4 (1800ms+) are handled by slideUp modifiers
        // Phase 5: Complete callback at ~3000ms total
        kotlinx.coroutines.delay(1600)
        onAnimationComplete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Logo with phase 1-2 animations
            Box(
                modifier = Modifier.graphicsLayer(
                    alpha = logoAlpha.value,
                    translationY = logoOffsetY.value,
                ),
            ) {
                logo(Modifier)
            }

            Spacer(Modifier.height(24.dp))

            // Title with phase 3 animation
            Text(
                text = "CuentaMorosos",
                style = MaterialTheme.typography.headlineLarge,
                color = colors.primaryContainer,
                modifier = Modifier
                    .slideUp(delayMs = 1400)
                    .then(
                        if (isDark) {
                            Modifier.neonGlow(
                                color = colors.primaryContainer,
                                blurRadius = 8.dp,
                                intensity = 0.6f,
                                isDarkMode = true,
                            )
                        } else {
                            Modifier
                        },
                    ),
            )

            Spacer(Modifier.height(8.dp))

            // Subtitle with phase 4 animation
            Text(
                text = "Inicia sesión para continuar",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.slideUp(delayMs = 1800),
            )

            Spacer(Modifier.height(24.dp))

            // Email placeholder with phase 4 animation
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("Email") },
                singleLine = true,
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .slideUp(delayMs = 1900),
            )

            Spacer(Modifier.height(8.dp))

            // Password placeholder with phase 4 animation
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("Contraseña") },
                singleLine = true,
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .slideUp(delayMs = 2000),
            )

            Spacer(Modifier.height(24.dp))

            // Login button with phase 4 animation
            Button(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .slideUp(delayMs = 2100),
            ) {
                Text("Iniciar sesión")
            }
        }
    }
}
