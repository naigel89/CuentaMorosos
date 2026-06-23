package com.cuentamorosos.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Screen shown when the user is authenticated but has not verified their email.
 *
 * Blocks access to the main app until email verification is confirmed.
 * Provides controls to resend the verification email, check verification
 * status, or sign out.
 *
 * @param email The user's email address for display
 * @param isResending Whether a verification email resend is in progress
 * @param errorMessage Error to display (from resend or check failures)
 * @param onResendVerification Triggered when user taps "Reenviar correo"
 * @param onCheckAgain Triggered when user taps "Ya verifiqué" to reload user
 * @param onSignOut Triggered when user taps "Cerrar sesión"
 */
@Composable
fun EmailVerificationScreen(
    email: String,
    isResending: Boolean,
    errorMessage: String?,
    onResendVerification: () -> Unit,
    onCheckAgain: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Verificá tu email",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Te enviamos un correo de verificación a",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = email,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Revisá tu bandeja de entrada y hacé clic en el enlace " +
                    "de verificación. Si no lo encontrás, revisá la carpeta de spam.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onResendVerification,
            enabled = !isResending,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isResending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.size(8.dp))
                Text("Enviando...")
            } else {
                Text("Reenviar correo de verificación")
            }
        }
        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = onCheckAgain,
            enabled = !isResending,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Ya verifiqué mi email")
        }
        Spacer(Modifier.height(4.dp))

        TextButton(
            onClick = onSignOut,
            enabled = !isResending,
        ) {
            Text("Cerrar sesión")
        }

        if (errorMessage != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
