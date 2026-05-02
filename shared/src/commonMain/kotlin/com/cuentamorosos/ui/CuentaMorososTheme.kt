package com.cuentamorosos.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.cuentamorosos.model.UserPreferences

enum class ThemeModeOption(val id: String, val label: String) {
    SYSTEM("system", "Sistema"),
    LIGHT("light", "Claro"),
    DARK("dark", "Oscuro");

    companion object {
        fun fromId(id: String): ThemeModeOption = entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}

enum class AccentColorOption(val id: String, val label: String, val color: Color) {
    ROSE("rose", "Rosa", Color(0xFFD16BA5)),
    BLUE("blue", "Azul", Color(0xFF4F83FF)),
    GREEN("green", "Verde", Color(0xFF2EAF7D)),
    AMBER("amber", "Ámbar", Color(0xFFE0A106));

    companion object {
        fun fromId(id: String): AccentColorOption = entries.firstOrNull { it.id == id } ?: ROSE
    }
}

@Composable
fun CuentaMorososTheme(
    preferences: UserPreferences,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (ThemeModeOption.fromId(preferences.themeMode)) {
        ThemeModeOption.SYSTEM -> systemDark
        ThemeModeOption.LIGHT -> false
        ThemeModeOption.DARK -> true
    }

    val accent = AccentColorOption.fromId(preferences.accentColorId).color
    val colors = if (isDark) {
        darkColorScheme(
            primary = accent,
            secondary = accent,
            tertiary = accent,
        )
    } else {
        lightColorScheme(
            primary = accent,
            secondary = accent,
            tertiary = accent,
        )
    }

    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
        content = content,
    )
}
