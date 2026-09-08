package com.cuentamorosos.ui.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.cuentamorosos.isValidEmail
import com.cuentamorosos.ui.CuentaMorososLogo
import com.cuentamorosos.ui.NeoFintechColorSet
import com.cuentamorosos.ui.NeoFintechColors
import com.cuentamorosos.ui.NeoFintechMotion
import com.cuentamorosos.ui.NeoFintechShapes
import com.cuentamorosos.ui.slideUp

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    /**
     * Platform provides the actual account creation.
     * Call [onResult] with null on success, or an error message on failure.
     */
    onRegister: (email: String, password: String, onResult: (error: String?) -> Unit) -> Unit,
) {
    // Pantalla previa al tema (aún no hay preferencias de usuario): resuelve
    // los tokens directamente del tema del sistema, como SplashAuthScreen.
    val isDark = isSystemInDarkTheme()
    val colors = remember(isDark) { if (isDark) NeoFintechColors.dark() else NeoFintechColors.light() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val emailError = if (email.isNotBlank() && !isValidEmail(email))
        "Formato de email incorrecto" else null
    val passwordError = if (password.isNotBlank() && password.length < 8)
        "Mínimo 8 caracteres" else null
    val confirmError = if (confirmPassword.isNotBlank() && confirmPassword != password)
        "Las contraseñas no coinciden" else null
    val canSubmit = email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank()
            && emailError == null && passwordError == null && confirmError == null && !isLoading

    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val isKeyboardVisible = imeBottom > 0

    val fieldColors = authFieldColors(colors)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        // Los campos van en una zona desplazable y el CTA queda anclado abajo,
        // fuera del scroll: visible siempre, en pantallas bajas y con teclado.
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          // El bloque cabecera+formulario se centra verticalmente en el hueco
          // disponible (heightIn(min = viewport) + Center): en pantallas altas
          // ya no queda pegado arriba con un vacío hasta el CTA, y cuando no
          // cabe sigue haciendo scroll con normalidad.
          BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
          ) {
            val viewportHeight = maxHeight
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = viewportHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
            // ── Header: collapses when keyboard is open so the CTA stays visible ──
            if (!isKeyboardVisible) {
                Spacer(Modifier.height(16.dp))

                // Wordmark: la marca también existe fuera del login
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CuentaMorososLogo(Modifier.size(32.dp))
                    Text(
                        text = "CuentaMorosos",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface,
                    )
                }

                Spacer(Modifier.height(24.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Crear cuenta",
                        style = MaterialTheme.typography.headlineLarge,
                        color = colors.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Regístrate para empezar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Registration form: always visible and reachable ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .slideUp(distanceDp = 16f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
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
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    label = { Text("Contraseña") },
                    singleLine = true,
                    isError = passwordError != null,
                    supportingText = passwordError?.let { { Text(it) } },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next,
                    ),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                            )
                        }
                    },
                    shape = NeoFintechShapes.md,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                PasswordStrengthMeter(
                    password = password,
                    colors = colors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; errorMessage = null },
                    label = { Text("Confirmar contraseña") },
                    singleLine = true,
                    isError = confirmError != null,
                    supportingText = confirmError?.let { { Text(it) } },
                    visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    trailingIcon = {
                        IconButton(onClick = { confirmVisible = !confirmVisible }) {
                            Icon(
                                imageVector = if (confirmVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (confirmVisible) "Ocultar contraseña" else "Mostrar contraseña",
                            )
                        }
                    },
                    shape = NeoFintechShapes.md,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(Modifier.height(12.dp))
                    AuthErrorBanner(message = errorMessage!!, colors = colors)
                }

                Spacer(Modifier.height(8.dp))
            }
            }
          }

            // ── CTA anclado: fuera del scroll, visible siempre ──
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(8.dp))

                AuthPrimaryButton(
                    text = "Crear cuenta",
                    enabled = canSubmit,
                    loading = isLoading,
                    colors = colors,
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        onRegister(email, password) { error ->
                            isLoading = false
                            if (error == null) {
                                password = ""
                                confirmPassword = ""
                                onRegisterSuccess()
                            }
                            else errorMessage = error
                        }
                    },
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(
                        text = "¿Ya tienes cuenta?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                    TextButton(
                        onClick = onNavigateToLogin,
                        modifier = Modifier.heightIn(min = 48.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = colors.primaryContainer),
                    ) {
                        Text("Inicia sesión", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/**
 * Medidor de fortaleza: cuatro segmentos de 3 dp que se van tiñendo según
 * [passwordStrength], con la etiqueta del nivel a la derecha. Convierte el
 * error pasivo "mínimo 8 caracteres" en feedback progresivo mientras se
 * escribe. Cada segmento anima su color con la curva estándar del sistema.
 */
@Composable
private fun PasswordStrengthMeter(
    password: String,
    colors: NeoFintechColorSet,
    modifier: Modifier = Modifier,
) {
    val strength = passwordStrength(password)
    val accent = when {
        strength <= 1 -> colors.error
        strength == 2 -> colors.warning
        else -> colors.primaryContainer
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(4) { index ->
                    val segmentColor by animateColorAsState(
                        targetValue = if (index < strength) accent else colors.surfaceContainerHigh,
                        animationSpec = NeoFintechMotion.color,
                        label = "strengthSegment$index",
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(NeoFintechShapes.full)
                            .background(segmentColor),
                    )
                }
            }
            val label = passwordStrengthLabel(strength)
            if (label.isNotEmpty()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                )
            }
        }
    }
}
