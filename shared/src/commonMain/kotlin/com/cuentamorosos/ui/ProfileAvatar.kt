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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Circular avatar component displaying an emoji centered inside.
 *
 * @param emoji The emoji character to display.
 * @param size Diameter of the avatar (default 48dp).
 * @param modifier Optional modifier for layout positioning.
 */
@Composable
fun ProfileAvatar(
    emoji: String,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
) {
    val colors = NeoFintechColors.dark()
    Surface(
        shape = NeoFintechShapes.full,
        color = colors.surfaceContainerHigh,
        modifier = modifier
            .size(size)
            .border(
                width = 1.dp,
                color = colors.outlineVariant,
                shape = NeoFintechShapes.full,
            ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}
