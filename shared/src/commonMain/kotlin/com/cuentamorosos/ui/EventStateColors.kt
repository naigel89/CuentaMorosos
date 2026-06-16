package com.cuentamorosos.ui

import androidx.compose.ui.graphics.Color
import com.cuentamorosos.model.EventState

val STATUS_COLORS: Map<EventState, Color> = mapOf(
    EventState.OPEN to Color(0xFF4CAF50),
    EventState.CALCULATED to Color(0xFF2196F3),
    EventState.CLOSED to Color(0xFFF44336),
)

fun EventState.statusColor(): Color = STATUS_COLORS[this] ?: Color.Gray

fun EventState.statusLabel(): String = when (this) {
    EventState.OPEN -> "Abierto"
    EventState.CALCULATED -> "Calculado"
    EventState.CLOSED -> "Cerrado"
}
