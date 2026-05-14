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

@Composable
fun GeistFontFamily(): FontFamily = FontFamily(
    Font(Res.font.geist_regular, FontWeight.Normal),
    Font(Res.font.geist_medium, FontWeight.Medium),
    Font(Res.font.geist_semibold, FontWeight.SemiBold),
    Font(Res.font.geist_bold, FontWeight.Bold),
)

@Composable
fun JetBrainsMonoFontFamily(): FontFamily = FontFamily(
    Font(Res.font.jetbrains_mono_regular, FontWeight.Normal),
)

@Composable
fun NeoFintechTypography(): Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = GeistFontFamily(),
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = (-0.02).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = GeistFontFamily(),
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        letterSpacing = (-0.02).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = GeistFontFamily(),
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
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
    labelSmall = TextStyle(
        fontFamily = JetBrainsMonoFontFamily(),
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.1.sp,
    ),
)
