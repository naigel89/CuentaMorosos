package com.cuentamorosos.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.cuentamorosos.isValidEmail

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Header: collapses when keyboard is open so the CTA stays visible ──
            if (!isKeyboardVisible) {
                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Crear cuenta",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Regístrate para empezar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(16.dp))

                // Empuja el form hacia el centro cuando no hay teclado
                Spacer(Modifier.weight(1f))
            }

            // ── Registration form: always visible and reachable ──
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.trim(); errorMessage = null },
                    label = { Text("Email") },
                    singleLine = true,
                    isError = emailError != null,
                    supportingText = emailError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

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
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; errorMessage = null },
                    label = { Text("Confirmar contraseña") },
                    singleLine = true,
                    isError = confirmError != null,
                    supportingText = confirmError?.let { { Text(it) } },
                    visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { confirmVisible = !confirmVisible }) {
                            Icon(
                                imageVector = if (confirmVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(Modifier.height(16.dp))

                Button(
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
                    enabled = canSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Crear cuenta")
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("¿Ya tienes cuenta?", style = MaterialTheme.typography.bodyMedium)
                    TextButton(
                        onClick = onNavigateToLogin,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text("Inicia sesión")
                    }
                }
            }

            // Pequeño respiro entre el form y el borde cuando el teclado está visible
            if (isKeyboardVisible) {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
