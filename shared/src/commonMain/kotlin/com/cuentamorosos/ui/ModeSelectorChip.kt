package com.cuentamorosos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
 *
 * Sin emojis: el sistema usa solo tipografía e iconos vectoriales, y aquellos
 * glifos renderizaban distinto en cada plataforma.
 */
@Composable
fun ModeSelectorChip(
    selectedMode: SplitMode,
    onModeSelected: (SplitMode) -> Unit,
) {
    val colors = LocalNeoFintechColors.current
    val shapes = NeoFintechShapes
    val typography = NeoFintechTypography()

    val modes = listOf(
        SplitMode.REAL_CONSUMPTION,
        SplitMode.SIMPLE_AVG,
        SplitMode.BY_CATEGORY,
        SplitMode.CUSTOM_PERCENTAGE,
        SplitMode.EXACT,
        SplitMode.PARTS,
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
                        text = mode.label,
                        style = typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        ),
                    )
                },
                shape = shapes.md,
                modifier = Modifier.padding(vertical = 2.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colors.primaryContainer,
                    selectedLabelColor = colors.onPrimaryContainer,
                ),
            )
        }
    }
}
