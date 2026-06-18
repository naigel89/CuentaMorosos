package com.cuentamorosos

import androidx.compose.runtime.Composable

/**
 * Implementación iOS: no-op. iOS no tiene botón de retroceso del sistema.
 */
@Composable
actual fun SystemBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    // No se requiere acción en iOS
}
