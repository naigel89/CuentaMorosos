package com.cuentamorosos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuentamorosos.model.SplitMode

/**
 * Horizontal segmented control for selecting split modes.
 * Uses FilterChip with Neo-Fintech styling: neon green container when selected,
 * outline variant when unselected.
 */
@Composable
fun ModeSelectorChip(
    selectedMode: SplitMode,
    onModeSelected: (SplitMode) -> Unit,
) {
    val shapes = NeoFintechShapes
    val typography = NeoFintechTypography()

    val modes = listOf(
        SplitMode.REAL_CONSUMPTION,
        SplitMode.SIMPLE_AVG,
        SplitMode.BY_CATEGORY,
        SplitMode.CUSTOM_PERCENTAGE,
    )

    val modeIcons = mapOf(
        SplitMode.REAL_CONSUMPTION to "\uD83D\uDC65",    // 👥 shared
        SplitMode.SIMPLE_AVG to "\u2797",               // ➗ avg
        SplitMode.BY_CATEGORY to "\uD83D\uDCC2",        // 📂 category
        SplitMode.CUSTOM_PERCENTAGE to "\uD83D\uDCCA",  // 📊 percentage
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        modes.forEach { mode ->
            val isSelected = mode == selectedMode
            FilterChip(
                selected = isSelected,
                onClick = { onModeSelected(mode) },
                label = {
                    Text(
                        text = "${modeIcons[mode]} ${mode.label}",
                        style = typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        ),
                    )
                },
                shape = shapes.md,
                modifier = Modifier.padding(vertical = 2.dp),
            )
        }
    }
}
