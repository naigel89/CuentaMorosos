package com.cuentamorosos.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuentamorosos.model.EventState

/** Morado del estado cerrado. Absoluto a propósito: no tiene slot en el tema. */
private val ClosedAccent = Color(0xFF9C27B0)

/**
 * Returns the badge background color for a given event state.
 * Extracted from EventCard to be shared between StateBadge and EventCard.
 */
fun EventState.stateBadgeColor(colors: NeoFintechColorSet): Color =
    when (this) {
        EventState.OPEN -> colors.primaryContainer.copy(alpha = 0.25f)
        EventState.CALCULATED -> colors.tertiaryContainer.copy(alpha = 0.35f)
        EventState.CLOSED -> ClosedAccent.copy(alpha = 0.25f)
    }

/**
 * Returns the human-readable Spanish label for a given event state.
 */
fun EventState.stateBadgeLabel(): String =
    when (this) {
        EventState.OPEN -> "Abierto"
        EventState.CALCULATED -> "Calculado"
        EventState.CLOSED -> "Cerrado"
    }

/**
 * Color sólido del estado, para el raíl. [stateBadgeColor] no sirve aquí: sus
 * valores son translúcidos porque están pensados para ir de fondo de la píldora.
 */
fun EventState.stateRailColor(colors: NeoFintechColorSet): Color =
    when (this) {
        EventState.OPEN -> colors.primaryContainer
        EventState.CALCULATED -> colors.onSurfaceVariant
        EventState.CLOSED -> ClosedAccent
    }

/**
 * Posición del estado dentro de la máquina OPEN → CALCULATED → CLOSED.
 *
 * Función pura para poder comprobar en jvmTest que el orden del raíl coincide
 * con el de [StateMachine][com.cuentamorosos.model.attemptTransition].
 */
internal fun eventStateIndex(state: EventState): Int = when (state) {
    EventState.OPEN -> 0
    EventState.CALCULATED -> 1
    EventState.CLOSED -> 2
}

/** Número de paradas del raíl. */
internal const val EVENT_STATE_COUNT = 3

/**
 * A reusable badge/chip composable that displays the human-readable label
 * for an [EventState] with color-coded background.
 *
 * Color mapping:
 * - OPEN → blue/green (primaryContainer 25%)
 * - CALCULATED → green (tertiaryContainer 35%)
 * - CLOSED → purple (0xFF9C27B0 25%)
 *
 * El cambio de estado no es instantáneo: el fondo interpola con
 * [NeoFintechMotion.color], el ancho de la píldora se reajusta con un muelle
 * ([NeoFintechMotion.resize]) porque "Calculado" no mide lo mismo que "Abierto",
 * y la etiqueta que sale sube mientras la nueva entra por abajo.
 */
@Composable
fun StateBadge(
    state: EventState,
    modifier: Modifier = Modifier,
) {
    val neoColors = LocalNeoFintechColors.current
    val themeColors = MaterialTheme.colorScheme

    val background by animateColorAsState(
        targetValue = state.stateBadgeColor(neoColors),
        animationSpec = NeoFintechMotion.color,
        label = "stateBadgeColor",
    )

    Surface(
        color = background,
        shape = NeoFintechShapes.full,
        modifier = modifier.animateContentSize(NeoFintechMotion.resize),
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                val fade = tween<Float>(NeoFintechMotion.SHORT_MS, easing = NeoFintechMotion.standard)
                val forward = eventStateIndex(targetState) >= eventStateIndex(initialState)
                val enter = slideInVertically(NeoFintechMotion.placement) { h ->
                    if (forward) h else -h
                } + fadeIn(fade)
                val exit = slideOutVertically(NeoFintechMotion.placement) { h ->
                    if (forward) -h else h
                } + fadeOut(fade)
                enter togetherWith exit
            },
            label = "stateBadgeLabel",
        ) { current ->
            Text(
                text = current.stateBadgeLabel(),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                color = themeColors.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

/**
 * Raíl de tres paradas que hace visible la máquina de estados del evento.
 *
 * La píldora sola dice dónde estás, pero no que haya un camino ni cuánto queda
 * de él. El raíl lo dice sin texto: el relleno avanza con un muelle hasta la
 * parada actual, que además crece un poco respecto a las otras.
 *
 * Es solo lectura — quien valida y ejecuta las transiciones sigue siendo
 * `StateMachine`.
 */
@Composable
fun EventStateRail(
    state: EventState,
    modifier: Modifier = Modifier,
) {
    val colors = LocalNeoFintechColors.current
    val index = eventStateIndex(state)
    val accent = state.stateRailColor(colors)

    val fill by animateFloatAsState(
        targetValue = index / (EVENT_STATE_COUNT - 1).toFloat(),
        animationSpec = NeoFintechMotion.smooth(),
        label = "stateRailFill",
    )
    val fillColor by animateColorAsState(
        targetValue = accent,
        animationSpec = NeoFintechMotion.color,
        label = "stateRailColor",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(NeoFintechShapes.sm)
                    .background(colors.outlineVariant),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(fill)
                    .height(3.dp)
                    .clip(NeoFintechShapes.sm)
                    .background(fillColor),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EventState.entries.forEachIndexed { position, stop ->
                    RailStop(
                        reached = position <= index,
                        isCurrent = position == index,
                        color = stop.stateRailColor(colors),
                        fallback = colors.outlineVariant,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            EventState.entries.forEachIndexed { position, stop ->
                val isCurrent = position == index
                Text(
                    text = stop.stateBadgeLabel(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = if (isCurrent) colors.onSurface else colors.onSurfaceVariant,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    textAlign = when (position) {
                        0 -> TextAlign.Start
                        EVENT_STATE_COUNT - 1 -> TextAlign.End
                        else -> TextAlign.Center
                    },
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun RailStop(
    reached: Boolean,
    isCurrent: Boolean,
    color: Color,
    fallback: Color,
) {
    val diameter by animateDpAsState(
        targetValue = if (isCurrent) 12.dp else 9.dp,
        animationSpec = NeoFintechMotion.offsetDp,
        label = "railStopSize",
    )
    val dotColor by animateColorAsState(
        targetValue = if (reached) color else fallback,
        animationSpec = NeoFintechMotion.color,
        label = "railStopColor",
    )
    Box(
        modifier = Modifier
            .size(diameter)
            .clip(NeoFintechShapes.full)
            .background(dotColor),
    )
}
