package com.cuentamorosos.ui.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.cuentamorosos.isValidEmail
import com.cuentamorosos.ui.LocalAnimationsEnabled
import com.cuentamorosos.ui.NeoFintechColors
import com.cuentamorosos.ui.slideUp

/**
 * Animated splash + login screen in one.
 *
 * The logo starts centered (fade-in), then slides to the top.
 * The title "CuentaMorosos" slides in below the logo.
 * The REAL login form (email, password, button) staggers in.
 * After the animation (~3s), the form is fully interactive.
 *
 * No transition — this IS the login screen.
 */
@Composable
fun SplashAuthScreen(
    logo: @Composable (modifier: Modifier) -> Unit,
    modifier: Modifier = Modifier,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onLogin: (email: String, password: String, onResult: (error: String?) -> Unit) -> Unit,
) {
    val animationsEnabled = LocalAnimationsEnabled.current
    val isDark = isSystemInDarkTheme()
    val colors = remember(isDark) { if (isDark) NeoFintechColors.dark() else NeoFintechColors.light() }
    val density = LocalDensity.current
    val logoSlideDistancePx = remember { with(density) { 60.dp.toPx() } }

    // ── Logo animation state ──
    val logoAlpha = remember { Animatable(if (animationsEnabled) 0f else 1f) }
    val logoOffsetY = remember { Animatable(0f) }

    // ── Login form state ──
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val emailError = if (email.isNotBlank() && !isValidEmail(email))
        "Formato de email incorrecto" else null
    val passwordError = if (password.isNotBlank() && password.length < 8)
        "Mínimo 8 caracteres" else null
    val canSubmit = email.isNotBlank() && password.isNotBlank() &&
            emailError == null && passwordError == null && !isLoading

    // ── Animation orchestration ──
    LaunchedEffect(Unit) {
        if (!animationsEnabled) return@LaunchedEffect

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

        // Phases 3-4 handled by slideUp modifiers on title + form elements
        // No onAnimationComplete needed — the form IS the destination
        kotlinx.coroutines.delay(1600)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // ── Logo ──
        Box(
            modifier = Modifier.graphicsLayer(
                alpha = logoAlpha.value,
                translationY = logoOffsetY.value,
            ),
        ) {
            logo(Modifier.size(164.dp))
        }

        Spacer(Modifier.height(24.dp))

        // ── Title ──
        Text(
            text = "CuentaMorosos",
            style = MaterialTheme.typography.headlineLarge,
            color = colors.primaryContainer,
            modifier = Modifier.slideUp(delayMs = 1400, durationMs = 400),
        )

        Spacer(Modifier.height(8.dp))

        // ── Subtitle ──
        Text(
            text = "Inicia sesión para continuar",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.slideUp(delayMs = 1800),
        )

        Spacer(Modifier.height(24.dp))

        // ── Email field (REAL, interactive) ──
        OutlinedTextField(
            value = email,
            onValueChange = { email = it.trim(); errorMessage = null },
            label = { Text("Email") },
            singleLine = true,
            isError = emailError != null,
            supportingText = emailError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier
                .fillMaxWidth()
                .slideUp(delayMs = 1900),
        )

        Spacer(Modifier.height(8.dp))

        // ── Password field (REAL, interactive) ──
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMessage = null },
            label = { Text("Contraseña") },
            singleLine = true,
            isError = passwordError != null,
            supportingText = passwordError?.let { { Text(it) } },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .slideUp(delayMs = 2000),
        )

        // ── Error message ──
        if (errorMessage != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.slideUp(delayMs = 2100),
            )
        }

        Spacer(Modifier.height(24.dp))

        // ── Login button (REAL, interactive) ──
        Button(
            onClick = {
                isLoading = true
                errorMessage = null
                onLogin(email, password) { error ->
                    isLoading = false
                    if (error == null) onLoginSuccess()
                    else errorMessage = error
                }
            },
            enabled = canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .slideUp(delayMs = 2100),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("Iniciar sesión")
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Forgot password link ──
        TextButton(
            onClick = onNavigateToForgotPassword,
            modifier = Modifier.slideUp(delayMs = 2100),
        ) {
            Text("¿Olvidaste tu contraseña?")
        }

        Spacer(Modifier.height(16.dp))

        // ── Register link ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.slideUp(delayMs = 2100),
        ) {
            Text("¿No tienes cuenta?", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onNavigateToRegister) {
                Text("Regístrate")
            }
        }
    }
}
