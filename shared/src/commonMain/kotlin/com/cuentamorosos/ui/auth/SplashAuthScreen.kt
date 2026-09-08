package com.cuentamorosos.ui.auth

import androidx.compose.animation.AnimatedVisibility
import kotlinx.coroutines.launch
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.cuentamorosos.isValidEmail
import com.cuentamorosos.ui.LocalAnimationsEnabled
import com.cuentamorosos.ui.NeoFintechColors
import com.cuentamorosos.ui.NeoFintechShapes

/**
 * Splash + login screen.
 *
 * Layout de tres zonas:
 *  - Branding (logo + título + subtítulo): wrap content. Se oculta con
 *    fade cuando el teclado está visible.
 *  - Spacer con weight(1f): SIEMPRE presente, empuja el formulario
 *    hacia abajo sin importar si el branding está visible u oculto.
 *  - Formulario: siempre visible, anclado a la parte inferior.
 *
 * La animación del logo usa graphicsLayer.translationY (no modifica
 * el layout) para moverlo desde el centro visual de la pantalla hasta
 * su posición final, mientras encoge su escala. No hay salto brusco
 * al abrir/cerrar el teclado porque el Spacer con weight NO está
 * dentro del AnimatedVisibility.
 */
@Composable
fun SplashAuthScreen(
    logo: @Composable (modifier: Modifier) -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onLogin: (email: String, password: String, onResult: (error: String?) -> Unit) -> Unit,
) {
    val animationsEnabled = LocalAnimationsEnabled.current
    val isDark = isSystemInDarkTheme()
    val colors = remember(isDark) { if (isDark) NeoFintechColors.dark() else NeoFintechColors.light() }
    val density = LocalDensity.current

    // ─── Animatables ────────────────────────────────────────────────────────
    val logoAlpha = remember { Animatable(if (animationsEnabled) 0f else 1f) }
    val logoScale = remember { Animatable(if (animationsEnabled) 2f else 1f) }
    val logoTranslateY = remember { Animatable(if (animationsEnabled) Float.NaN else 0f) }
    val titleAlpha = remember { Animatable(if (animationsEnabled) 0f else 1f) }
    val formAlpha = remember { Animatable(if (animationsEnabled) 0f else 1f) }

    // ─── Form state ─────────────────────────────────────────────────────────
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var localLoading by remember { mutableStateOf(false) }
    val showLoading = isLoading || localLoading
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val emailError = if (email.isNotBlank() && !isValidEmail(email))
        "Formato de email incorrecto" else null
    val passwordError = if (password.isNotBlank() && password.length < 8)
        "Mínimo 8 caracteres" else null
    val canSubmit = email.isNotBlank() && password.isNotBlank() &&
            emailError == null && passwordError == null && !localLoading

    val imeBottom = WindowInsets.ime.getBottom(density)
    val isKeyboardVisible = imeBottom > 0

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        val logoEndSizeDp = 96.dp
        val logoSizePx = with(density) { logoEndSizeDp.toPx() }
        // Posición central (visual) del logo en el viewport
        val centerOffsetPx = with(density) { (maxHeight.toPx() - logoSizePx) / 2f }

        // ─── Halo radial tras la marca ──────────────────────────────────────
        // Profundidad sin blur (RenderEffect no existe en CMP 1.6.11): un
        // degradado radial del verde de marca que "respira" lentamente.
        // Aparece con el logo (comparte su alpha) y se oculta con el teclado.
        val haloBreath = if (animationsEnabled) {
            rememberInfiniteTransition(label = "halo").animateFloat(
                initialValue = 0.6f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3500, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "haloBreath",
            ).value
        } else {
            0.8f
        }

        AnimatedVisibility(
            visible = !isKeyboardVisible,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(maxWidth)
                    .graphicsLayer { alpha = logoAlpha.value * haloBreath }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                colors.primaryContainer.copy(alpha = 0.16f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
        }

        LaunchedEffect(Unit) {
            // Inicializar estado inicial UNA vez
            if (logoTranslateY.value.isNaN()) {
                logoTranslateY.snapTo(centerOffsetPx)
            }
            if (logoAlpha.value == 0f && !animationsEnabled) {
                logoAlpha.snapTo(1f)
                logoScale.snapTo(1f)
                titleAlpha.snapTo(1f)
                formAlpha.snapTo(1f)
            }

            if (!animationsEnabled) return@LaunchedEffect

            // 1. Logo fade-in
            logoAlpha.animateTo(1f, tween(400))

            // 2. Logo sube y encoge: centro → posición final
            launch {
                logoScale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(650, easing = FastOutSlowInEasing),
                )
            }
            logoTranslateY.animateTo(
                targetValue = 0f,
                animationSpec = tween(800, easing = FastOutSlowInEasing),
            )

            // 3. Título + subtítulo aparecen
            titleAlpha.animateTo(1f, tween(350))

            // 4. Formulario aparece
            formAlpha.animateTo(1f, tween(350))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ═══════════════════════════════════════════════════════════════
            //  ZONA BRANDING — wrap content.
            //  El logo se mueve visualmente con translationY sin afectar el
            //  layout. Se oculta con fade cuando el teclado está visible.
            // ═══════════════════════════════════════════════════════════════
            AnimatedVisibility(
                visible = !isKeyboardVisible,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(150)),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Logo: graphicsLayer con translationY (no desplaza el layout)
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                alpha = logoAlpha.value
                                scaleX = logoScale.value
                                scaleY = logoScale.value
                                translationY = logoTranslateY.value
                            },
                    ) {
                        logo(Modifier.size(logoEndSizeDp))
                    }

                    Spacer(Modifier.height(16.dp))

                    // La marca en onSurface: el verde se reserva para el dinero.
                    // Solo el punto final lleva el acento.
                    Text(
                        text = buildAnnotatedString {
                            append("CuentaMorosos")
                            withStyle(SpanStyle(color = colors.primaryContainer)) { append(".") }
                        },
                        style = MaterialTheme.typography.headlineLarge,
                        color = colors.onSurface,
                        modifier = Modifier.graphicsLayer(alpha = titleAlpha.value),
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Inicia sesión para continuar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.graphicsLayer(alpha = titleAlpha.value),
                    )
                }
            }

            // ══════════════════════════════════════════════════════════════
            //  SPACER FIJO — separación controlada entre branding y form.
            //  Al no usar weight, el formulario queda cerca del branding
            //  cuando el teclado está cerrado. Cuando el teclado se abre,
            //  el imePadding() del Box raíz empuja todo el Column hacia
            //  arriba, dejando el formulario justo encima del teclado.
            // ═══════════════════════════════════════════════════════════════
            Spacer(Modifier.height(32.dp))

            // ═══════════════════════════════════════════════════════════════
            //  FORMULARIO — siempre visible, anclado abajo.
            // ═══════════════════════════════════════════════════════════════
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(alpha = formAlpha.value),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val fieldColors = authFieldColors(colors)

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.trim(); errorMessage = null },
                    label = { Text("Email") },
                    singleLine = true,
                    isError = emailError != null,
                    supportingText = emailError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                    shape = NeoFintechShapes.md,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    label = { Text("Contraseña") },
                    singleLine = true,
                    isError = passwordError != null,
                    supportingText = passwordError?.let { { Text(it) } },
                    visualTransformation = if (passwordVisible)
                        VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible)
                                    Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible)
                                    "Ocultar contraseña" else "Mostrar contraseña",
                            )
                        }
                    },
                    shape = NeoFintechShapes.md,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (errorMessage != null) {
                    Spacer(Modifier.height(12.dp))
                    AuthErrorBanner(message = errorMessage!!, colors = colors)
                }

                Spacer(Modifier.height(16.dp))

                AuthPrimaryButton(
                    text = "Iniciar sesión",
                    enabled = canSubmit,
                    loading = showLoading,
                    colors = colors,
                    onClick = {
                        localLoading = true
                        errorMessage = null
                        onLogin(email, password) { error ->
                            localLoading = false
                            if (error == null) {
                                password = ""
                                onLoginSuccess()
                            } else {
                                errorMessage = error
                            }
                        }
                    },
                )

                TextButton(
                    onClick = onNavigateToForgotPassword,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.primaryContainer),
                ) {
                    Text("¿Olvidaste tu contraseña?")
                }

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "¿No tienes cuenta?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                    TextButton(
                        onClick = onNavigateToRegister,
                        modifier = Modifier.heightIn(min = 48.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = colors.primaryContainer),
                    ) {
                        Text("Regístrate", fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
