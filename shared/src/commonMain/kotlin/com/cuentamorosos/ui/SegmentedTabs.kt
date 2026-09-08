package com.cuentamorosos.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Control segmentado del sistema: opciones en una píldora con el pulgar (el
 * botón de marca) deslizándose al seleccionar. Sustituye al TabRow de M3 en
 * los paneles y diálogos — el elemento más "Material genérico" que quedaba.
 */
@Composable
internal fun SegmentedTabs(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalNeoFintechColors.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(NeoFintechShapes.pill)
            .background(colors.surfaceContainerHigh)
            .padding(3.dp),
    ) {
        val segmentWidth = maxWidth / options.size
        val thumbOffset by animateDpAsState(
            targetValue = segmentWidth * selected,
            animationSpec = NeoFintechMotion.smooth(),
            label = "segmentedThumb",
        )

        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .width(segmentWidth)
                .height(36.dp)
                .clip(NeoFintechShapes.pill)
                .background(colors.buttonContainer),
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, label ->
                val labelColor by animateColorAsState(
                    targetValue = if (index == selected) colors.onButton else colors.onSurfaceVariant,
                    animationSpec = NeoFintechMotion.color,
                    label = "segmentedLabel$index",
                )
                Box(
                    modifier = Modifier
                        .width(segmentWidth)
                        .height(36.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (index == selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = labelColor,
                    )
                }
            }
        }
    }
}
