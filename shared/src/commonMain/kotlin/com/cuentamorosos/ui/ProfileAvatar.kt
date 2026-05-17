package com.cuentamorosos.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * Circular avatar component displaying the first letter of a name on a colored background.
 * Falls back to emoji if no name is provided.
 *
 * @param name The profile name used to derive the initial and background color.
 * @param emoji Fallback emoji to display when name is blank.
 * @param size Diameter of the avatar (default 48dp).
 * @param modifier Optional modifier for layout positioning.
 */
@Composable
fun ProfileAvatar(
    name: String = "",
    emoji: String = "",
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
) {
    val colors = LocalNeoFintechColors.current
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString().orEmpty()
    val avatarColor = if (initial.isNotBlank()) colorForName(name) else colors.surfaceContainerHigh

    Surface(
        shape = NeoFintechShapes.full,
        color = avatarColor,
        modifier = modifier
            .size(size)
            .border(
                width = 1.dp,
                color = colors.outlineVariant,
                shape = NeoFintechShapes.full,
            ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (initial.isNotBlank()) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.42).sp,
                        color = Color.White,
                    ),
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * Predefined palette of colors for avatar backgrounds.
 * Picked by hashing the profile name for deterministic assignment.
 */
private val AvatarPalette = listOf(
    Color(0xFF7C4DFF), // Purple
    Color(0xFF00BCD4), // Cyan
    Color(0xFFFF7043), // Orange
    Color(0xFF66BB6A), // Green
    Color(0xFFEC407A), // Pink
    Color(0xFF42A5F5), // Blue
    Color(0xFFAB47BC), // Deep Purple
    Color(0xFF26A69A), // Teal
    Color(0xFFEF5350), // Red
    Color(0xFF5C6BC0), // Indigo
    Color(0xFF8D6E63), // Brown
    Color(0xFF78909C), // Blue Grey
)

/**
 * Returns a deterministic background color for a given profile name.
 */
fun colorForName(name: String): Color {
    val hash = abs(name.hashCode())
    return AvatarPalette[hash % AvatarPalette.size]
}
