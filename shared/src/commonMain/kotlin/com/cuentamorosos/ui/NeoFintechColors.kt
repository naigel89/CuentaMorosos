package com.cuentamorosos.ui

import androidx.compose.ui.graphics.Color

data class NeoFintechColorSet(
    val background: Color,
    val surface: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerLowest: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val primaryContainer: Color,
    val primaryFixedDim: Color,
    val error: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val outlineVariant: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val secondary: Color,
    val buttonContainer: Color,
    val onButton: Color,
    val warning: Color,
)

object NeoFintechColors {
    fun light() = NeoFintechColorSet(
        background = Color(0xFFF8F9FA),
        surface = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF3F4F5),
        surfaceContainer = Color(0xFFEDEEEF),
        surfaceContainerHigh = Color(0xFFE7E8E9),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        onSurface = Color(0xFF191C1D),
        onSurfaceVariant = Color(0xFF3C4B35),
        primaryContainer = Color(0xFF39FF14),
        primaryFixedDim = Color(0xFF2AE500),
        error = Color(0xFFBA1A1A),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF93000A),
        outlineVariant = Color(0xFFBACCB0),
        tertiaryContainer = Color(0xFFD6DDED),
        onTertiaryContainer = Color(0xFF5A616F),
        secondary = Color(0xFF5E5E5E),
        buttonContainer = Color(0xFF191C1D),
        onButton = Color(0xFFFFFFFF),
        warning = Color(0xFFFFA000),
    )

    fun dark() = NeoFintechColorSet(
        background = Color(0xFF131313),
        surface = Color(0xFF201F1F),
        surfaceContainerLow = Color(0xFF1C1B1B),
        surfaceContainer = Color(0xFF201F1F),
        surfaceContainerHigh = Color(0xFF2A2A2A),
        surfaceContainerLowest = Color(0xFF0E0E0E),
        onSurface = Color(0xFFE5E2E1),
        onSurfaceVariant = Color(0xFFBACCB0),
        primaryContainer = Color(0xFF39FF14),
        primaryFixedDim = Color(0xFF2AE500),
        error = Color(0xFFFFB4AB),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        outlineVariant = Color(0xFF3C4B35),
        tertiaryContainer = Color(0xFFDFDCE1),
        onTertiaryContainer = Color(0xFF404754),
        secondary = Color(0xFFC8C5CB),
        buttonContainer = Color(0xFFE5E2E1),
        onButton = Color(0xFF191C1D),
        warning = Color(0xFFFFB74D),
    )
}

/**
 * Maps Neo-Fintech color tokens to Material 3 ColorScheme slots.
 */
fun NeoFintechColorSet.toColorScheme(isLight: Boolean): androidx.compose.material3.ColorScheme {
    return if (isLight) {
        androidx.compose.material3.lightColorScheme(
            background = background,
            surface = surface,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerLowest = surfaceContainerLowest,
            onSurface = onSurface,
            onSurfaceVariant = onSurfaceVariant,
            primary = primaryContainer,
            secondary = secondary,
            tertiary = tertiaryContainer,
            error = error,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
            onTertiaryContainer = onTertiaryContainer,
            outlineVariant = outlineVariant,
        )
    } else {
        androidx.compose.material3.darkColorScheme(
            background = background,
            surface = surface,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerLowest = surfaceContainerLowest,
            onSurface = onSurface,
            onSurfaceVariant = onSurfaceVariant,
            primary = primaryContainer,
            secondary = secondary,
            tertiary = tertiaryContainer,
            error = error,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
            onTertiaryContainer = onTertiaryContainer,
            outlineVariant = outlineVariant,
        )
    }
}
