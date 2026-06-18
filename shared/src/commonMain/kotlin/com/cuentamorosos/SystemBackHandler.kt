package com.cuentamorosos

import androidx.compose.runtime.Composable

/**
 * Intercepta el botón de retroceso del sistema (hardware/gesture back).
 *
 * En Android: usa [androidx.activity.compose.BackHandler] real.
 * En iOS: no-op (no hay botón de retroceso del sistema).
 *
 * @param enabled Si `false`, el handler se ignora.
 * @param onBack  Acción a ejecutar cuando se presiona el botón de retroceso.
 */
@Composable
expect fun SystemBackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit,
)
