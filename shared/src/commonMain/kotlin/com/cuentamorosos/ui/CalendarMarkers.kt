package com.cuentamorosos.ui

import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.EventState

/** Milisegundos en un día. */
internal const val DAY_MS: Long = 86_400_000L

/** Carriles de marcas que caben bajo el número del día antes de resumir en "+N". */
internal const val MAX_MARKER_LANES = 3

/** Día epoch (días desde 1970-01-01) al que pertenece un instante. */
internal fun epochDayOf(millis: Long): Int = (millis / DAY_MS).toInt()

/**
 * Cómo se dibuja un evento dentro de una celda del calendario.
 *
 * Un evento de varios días es una **barra continua** que solo redondea sus
 * extremos reales. Antes cada día del rango repetía el nombre del evento
 * recortado a 8 sp, que en una celda de ~56 dp no se lee: la misma información
 * cuatro veces y ninguna legible.
 */
internal data class DayMarker(
    val eventId: String,
    val state: EventState,
    val lane: Int,
    val isRange: Boolean,
    val roundedStart: Boolean,
    val roundedEnd: Boolean,
)

/**
 * Reparte los eventos en carriles horizontales de modo que dos que se solapan
 * nunca compartan carril.
 *
 * Es lo que mantiene **recta** la barra de un evento de varios días: si el
 * carril se eligiera celda a celda, un evento suelto que cayera en medio del
 * rango empujaría la barra a otra altura y la partiría por la mitad.
 *
 * Coloreado voraz sobre intervalos: se ordena por día de inicio (a igualdad, el
 * más largo primero, y luego por id para que el resultado sea determinista) y
 * cada evento cae en el primer carril libre.
 */
internal fun assignEventLanes(events: List<EventItem>): Map<String, Int> {
    val spans = events
        .map { Triple(it.id, epochDayOf(it.startDateMillis), epochDayOf(it.endDateMillis)) }
        .sortedWith(
            compareBy(
                { it.second },
                { -(it.third - it.second) },
                { it.first },
            ),
        )

    val laneLastDay = mutableListOf<Int>()
    val lanes = mutableMapOf<String, Int>()
    for ((id, start, end) in spans) {
        val free = laneLastDay.indexOfFirst { it < start }
        val lane = if (free >= 0) {
            laneLastDay[free] = end
            free
        } else {
            laneLastDay.add(end)
            laneLastDay.size - 1
        }
        lanes[id] = lane
    }
    return lanes
}

/**
 * Marca que le corresponde a [event] en el día [epochDay].
 *
 * [roundedStart] y [roundedEnd] indican si ese día es el principio o el final
 * reales del evento; en los días intermedios la barra llega a ras de celda para
 * empalmar con la de al lado.
 */
internal fun markerFor(event: EventItem, epochDay: Int, lane: Int): DayMarker {
    val start = epochDayOf(event.startDateMillis)
    val end = epochDayOf(event.endDateMillis)
    return DayMarker(
        eventId = event.id,
        state = event.state,
        lane = lane,
        isRange = end > start,
        roundedStart = epochDay <= start,
        roundedEnd = epochDay >= end,
    )
}

/**
 * Marcas visibles de un día, ya ordenadas por carril, y cuántas se han quedado
 * fuera por no caber en [MAX_MARKER_LANES].
 */
internal data class DayMarkers(
    val visible: List<DayMarker>,
    val overflow: Int,
)

internal fun markersForDay(
    dayEvents: List<EventItem>,
    epochDay: Int,
    lanes: Map<String, Int>,
): DayMarkers {
    val all = dayEvents.map { markerFor(it, epochDay, lanes[it.id] ?: 0) }
    val visible = all.filter { it.lane < MAX_MARKER_LANES }.sortedBy { it.lane }
    return DayMarkers(visible = visible, overflow = all.size - visible.size)
}
