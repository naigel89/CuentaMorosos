package com.cuentamorosos.ui

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import cuentamorosos.shared.generated.resources.Res
import cuentamorosos.shared.generated.resources.geist_bold
import cuentamorosos.shared.generated.resources.geist_medium
import cuentamorosos.shared.generated.resources.geist_regular
import cuentamorosos.shared.generated.resources.geist_semibold
import cuentamorosos.shared.generated.resources.jetbrains_mono_regular
import org.jetbrains.compose.resources.Font

// ── Font families ─────────────────────────────────────────────────────────────
// NOTE: Hanken Grotesk and Inter are NOT available as font resources.
// Using Geist (closest available sans-serif) for headlines and body.
// JetBrains Mono for data/amounts/labels.

@Composable
fun GeistFontFamily(): FontFamily = FontFamily(
    Font(Res.font.geist_regular, FontWeight.Normal),
    Font(Res.font.geist_medium, FontWeight.Medium),
    Font(Res.font.geist_semibold, FontWeight.SemiBold),
    Font(Res.font.geist_bold, FontWeight.Bold),
)

@Composable
fun JetBrainsMonoFontFamily(): FontFamily = FontFamily(
    Font(Res.font.jetbrains_mono_regular, FontWeight.Medium),
)

// ── Typography ────────────────────────────────────────────────────────────────
// Headlines: Geist Bold, tight letter-spacing (Hanken Grotesk substitute)
// Body: Geist Regular (Inter substitute)
// Data/Labels: JetBrains Mono Medium 500 weight

@Composable
fun NeoFintechTypography(): Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = GeistFontFamily(),
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = (-0.03).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = GeistFontFamily(),
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = (-0.025).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = GeistFontFamily(),
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        letterSpacing = (-0.02).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = GeistFontFamily(),
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = (-0.015).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = GeistFontFamily(),
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        letterSpacing = (-0.01).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = GeistFontFamily(),
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = GeistFontFamily(),
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = GeistFontFamily(),
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = GeistFontFamily(),
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = GeistFontFamily(),
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = JetBrainsMonoFontFamily(),
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = JetBrainsMonoFontFamily(),
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = JetBrainsMonoFontFamily(),
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.05.sp,
    ),
)
