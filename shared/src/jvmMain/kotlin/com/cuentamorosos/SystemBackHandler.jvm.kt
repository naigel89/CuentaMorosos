package com.cuentamorosos

import androidx.compose.runtime.Composable

/**
 * Implementación JVM: no-op (sin botón de retroceso del sistema).
 */
@Composable
actual fun SystemBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    // No-op en JVM — solo se usa para tests
}
