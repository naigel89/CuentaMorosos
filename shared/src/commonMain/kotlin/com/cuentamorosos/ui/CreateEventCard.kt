package com.cuentamorosos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CreateEventCard(
    onCreate: () -> Unit,
) {
    val neoColors = NeoFintechColors.dark()

    Button(
        onClick = onCreate,
        colors = ButtonDefaults.buttonColors(
            containerColor = neoColors.primaryContainer,
            contentColor = neoColors.onSurface,
        ),
        shape = NeoFintechShapes.xl,
        modifier = Modifier.padding(vertical = 4.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(NeoFintechSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "Crear evento",
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}
