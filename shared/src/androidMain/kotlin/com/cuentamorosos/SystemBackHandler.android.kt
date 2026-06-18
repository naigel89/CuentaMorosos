package com.cuentamorosos

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/**
 * Implementación Android: delega en [BackHandler] de activity-compose.
 */
@Composable
actual fun SystemBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    BackHandler(enabled = enabled, onBack = onBack)
}
