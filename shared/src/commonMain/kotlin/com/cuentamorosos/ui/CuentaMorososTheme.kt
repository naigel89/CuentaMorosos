package com.cuentamorosos.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.cuentamorosos.model.UserPreferences

enum class ThemeModeOption(val id: String, val label: String) {
    SYSTEM("system", "Sistema"),
    LIGHT("light", "Claro"),
    DARK("dark", "Oscuro");

    companion object {
        fun fromId(id: String): ThemeModeOption = entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}

/**
 * CompositionLocal providing the current Neo-Fintech color set.
 * Access via `LocalNeoFintechColors.current` to get tokens like
 * `buttonContainer` and `onButton` that are not mapped to Material 3 slots.
 */
val LocalNeoFintechColors = staticCompositionLocalOf<NeoFintechColorSet> {
    error("No NeoFintechColorSet provided — wrap your content in CuentaMorososTheme")
}

@Composable
fun CuentaMorososTheme(
    preferences: UserPreferences,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val themeMode = ThemeModeOption.fromId(preferences.themeMode)
    val isDark = when (themeMode) {
        ThemeModeOption.SYSTEM -> systemDark
        ThemeModeOption.LIGHT -> false
        ThemeModeOption.DARK -> true
    }

    val colorSet = if (isDark) NeoFintechColors.dark() else NeoFintechColors.light()
    val typography = NeoFintechTypography()

    CompositionLocalProvider(LocalNeoFintechColors provides colorSet) {
        MaterialTheme(
            colorScheme = colorSet.toColorScheme(isLight = !isDark),
            typography = typography,
            content = content,
        )
    }
}
