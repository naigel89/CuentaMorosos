package com.cuentamorosos.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuentamorosos.calendarFieldsForYearMonth
import com.cuentamorosos.currentYearMonth
import com.cuentamorosos.currentTimeMillis
import com.cuentamorosos.formatDateMillis
import com.cuentamorosos.nextMonth
import com.cuentamorosos.previousMonth
import com.cuentamorosos.shortWeekDayNames
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.EventState
import com.cuentamorosos.model.formatEuros
import kotlinx.datetime.LocalDate

private val spanishMonthNames = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)

/** Altura de cada barra o punto de evento bajo el número del día. */
private val MARKER_HEIGHT = 5.dp

@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    events: List<EventItem>,
    pendingTotalsByEvent: Map<String, Double>,
    onOpenEvent: (EventItem) -> Unit,
    onClose: () -> Unit = {},
) {
    val todayFields = remember { currentYearMonth() }
    var displayYear by remember { mutableStateOf(todayFields.year) }
    var displayMonth by remember { mutableStateOf(todayFields.month) }
    var selectedDay by remember { mutableStateOf<Int?>(null) }
    // +1 avanza en el tiempo, -1 retrocede. Es lo que decide hacia qué lado
    // entra el mes nuevo: sin esto, cambiar de mes era una sustitución sin
    // dirección y no se sabía si ibas hacia delante o hacia atrás.
    var monthDirection by remember { mutableStateOf(1) }

    val selectedDayEvents by remember(events, selectedDay, displayYear, displayMonth) {
        derivedStateOf {
            val day = selectedDay ?: return@derivedStateOf emptyList()
            val epochDay = LocalDate(displayYear, displayMonth, day).toEpochDays()
            events.filter { event ->
                epochDay >= epochDayOf(event.startDateMillis) &&
                    epochDay <= epochDayOf(event.endDateMillis)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // ── Month header ────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cerrar calendario")
            }
            Text(
                text = "${spanishMonthNames[displayMonth - 1]} $displayYear",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = {
                    val prev = previousMonth(displayYear, displayMonth)
                    monthDirection = -1
                    displayYear = prev.year; displayMonth = prev.month; selectedDay = null
                }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Mes anterior") }
                IconButton(onClick = {
                    val next = nextMonth(displayYear, displayMonth)
                    monthDirection = 1
                    displayYear = next.year; displayMonth = next.month; selectedDay = null
                }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Mes siguiente") }
            }
        }

        // ── Week day headers ─────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            shortWeekDayNames().forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        // ── Day grid ─────────────────────────────────────────────────
        AnimatedContent(
            targetState = displayYear to displayMonth,
            transitionSpec = {
                val slide = tween<androidx.compose.ui.unit.IntOffset>(
                    durationMillis = NeoFintechMotion.LONG_MS,
                    easing = NeoFintechMotion.emphasized,
                )
                val fade = tween<Float>(
                    durationMillis = NeoFintechMotion.MEDIUM_MS,
                    easing = NeoFintechMotion.standard,
                )
                val direction = monthDirection
                val enter = slideInHorizontally(slide) { width ->
                    (width * 0.18f * direction).toInt()
                } + fadeIn(fade)
                val exit = slideOutHorizontally(slide) { width ->
                    (-width * 0.18f * direction).toInt()
                } + fadeOut(fade)
                // Sin esto el contenido se recorta cuando dos meses tienen
                // distinto número de semanas.
                (enter togetherWith exit).using(SizeTransform(clip = false))
            },
            label = "calendarMonth",
        ) { (year, month) ->
            MonthGrid(
                year = year,
                month = month,
                events = events,
                todayYear = todayFields.year,
                todayMonth = todayFields.month,
                selectedDay = selectedDay,
                onSelectDay = { day -> selectedDay = if (selectedDay == day) null else day },
            )
        }

        // ── Day Detail Panel ────────────────────────────────────────
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(4.dp))

        DayDetailPanel(
            selectedDay = selectedDay,
            displayMonth = displayMonth,
            displayYear = displayYear,
            events = selectedDayEvents,
            pendingTotalsByEvent = pendingTotalsByEvent,
            onOpenEvent = onOpenEvent,
        )
    }
}

// ── Month grid ───────────────────────────────────────────────────────────────

@Composable
private fun MonthGrid(
    year: Int,
    month: Int,
    events: List<EventItem>,
    todayYear: Int,
    todayMonth: Int,
    selectedDay: Int?,
    onSelectDay: (Int) -> Unit,
) {
    val fields = remember(year, month) { calendarFieldsForYearMonth(year, month) }

    val cells = remember(year, month) {
        val list = mutableListOf<Int?>()
        repeat(fields.firstWeekDayOffset) { list.add(null) }
        for (day in 1..fields.daysInMonth) list.add(day)
        while (list.size % 7 != 0) list.add(null)
        list.toList()
    }

    // Eventos que tocan este mes, con su carril ya asignado. El carril se
    // calcula una vez por mes y no por celda: es lo que mantiene recta la barra
    // de un evento de varios días.
    val monthEvents = remember(events, year, month) {
        val first = LocalDate(year, month, 1).toEpochDays()
        val last = LocalDate(year, month, fields.daysInMonth).toEpochDays()
        events.filter { event ->
            epochDayOf(event.startDateMillis) <= last && epochDayOf(event.endDateMillis) >= first
        }
    }
    val lanes = remember(monthEvents) { assignEventLanes(monthEvents) }

    val todayDay = remember(year, month) {
        if (todayYear == year && todayMonth == month) {
            LocalDate.fromEpochDays(epochDayOf(currentTimeMillis())).dayOfMonth
        } else {
            -1
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                week.forEachIndexed { column, day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(0.9f)
                            .then(
                                if (day != null) {
                                    Modifier.clickable { onSelectDay(day) }
                                } else {
                                    Modifier
                                }
                            ),
                    ) {
                        if (day != null) {
                            val epochDay = LocalDate(year, month, day).toEpochDays()
                            val dayEvents = monthEvents.filter { event ->
                                epochDay >= epochDayOf(event.startDateMillis) &&
                                    epochDay <= epochDayOf(event.endDateMillis)
                            }
                            DayCell(
                                day = day,
                                isSelected = selectedDay == day,
                                isToday = day == todayDay,
                                markers = markersForDay(dayEvents, epochDay, lanes),
                                isWeekStart = column == 0,
                                isWeekEnd = column == 6,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isSelected: Boolean,
    isToday: Boolean,
    markers: DayMarkers,
    isWeekStart: Boolean,
    isWeekEnd: Boolean,
) {
    val colors = LocalNeoFintechColors.current
    val animationsEnabled = LocalAnimationsEnabled.current

    // snappy(): la selección responde al dedo, y cualquier rebote aquí se leería
    // como imprecisión sobre qué día has tocado.
    val selection = animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = if (animationsEnabled) {
            NeoFintechMotion.snappy()
        } else {
            androidx.compose.animation.core.snap()
        },
        label = "daySelection",
    )

    val numberColor = when {
        isSelected -> colors.onPrimaryContainer
        isToday -> colors.primaryContainer
        else -> colors.onSurface
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(3.dp))

        Box(
            modifier = Modifier.size(30.dp),
            contentAlignment = Alignment.Center,
        ) {
            // "Hoy" es un aro, no un relleno: así no compite con el día
            // seleccionado, que sí va relleno.
            if (isToday) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .border(1.5.dp, colors.primaryContainer, CircleShape),
                )
            }
            Box(
                modifier = Modifier
                    .size(30.dp)
                    // Lectura diferida: la escala se lee en fase de dibujo, así
                    // que animar la selección no recompone las 42 celdas.
                    .graphicsLayer {
                        val value = selection.value
                        scaleX = value
                        scaleY = value
                        alpha = value
                    }
                    .clip(CircleShape)
                    .background(colors.primaryContainer),
            )
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = numberColor,
            )
        }

        Spacer(Modifier.height(3.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            markers.visible.forEach { marker ->
                EventMarker(
                    marker = marker,
                    isWeekStart = isWeekStart,
                    isWeekEnd = isWeekEnd,
                )
            }
            if (markers.overflow > 0) {
                Text(
                    text = "+${markers.overflow}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                    ),
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * Barra de rango o punto suelto.
 *
 * Los días intermedios de un rango llegan a ras de celda para empalmar con la
 * barra de al lado; los extremos reales se redondean y se separan un poco. Al
 * cambiar de semana la barra se corta con un margen mínimo, porque ahí sí hay
 * un salto de línea de verdad.
 */
@Composable
private fun EventMarker(
    marker: DayMarker,
    isWeekStart: Boolean,
    isWeekEnd: Boolean,
) {
    val color = marker.state.statusColor()

    if (!marker.isRange) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(MARKER_HEIGHT)
                    .clip(CircleShape)
                    .background(color),
            )
        }
        return
    }

    val startRadius = if (marker.roundedStart) 3.dp else 0.dp
    val endRadius = if (marker.roundedEnd) 3.dp else 0.dp
    val startPadding = when {
        marker.roundedStart -> 5.dp
        isWeekStart -> 2.dp
        else -> 0.dp
    }
    val endPadding = when {
        marker.roundedEnd -> 5.dp
        isWeekEnd -> 2.dp
        else -> 0.dp
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = startPadding, end = endPadding)
            .height(MARKER_HEIGHT)
            .clip(
                RoundedCornerShape(
                    topStart = startRadius,
                    bottomStart = startRadius,
                    topEnd = endRadius,
                    bottomEnd = endRadius,
                )
            )
            .background(color),
    )
}

// ── Day detail panel ─────────────────────────────────────────────────────────

@Composable
private fun DayDetailPanel(
    selectedDay: Int?,
    displayMonth: Int,
    displayYear: Int,
    events: List<EventItem>,
    pendingTotalsByEvent: Map<String, Double>,
    onOpenEvent: (EventItem) -> Unit,
) {
    val colors = LocalNeoFintechColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            // El panel crece con muelle en lugar de dar el salto de altura que
            // daba al elegir un día con eventos.
            .animateContentSize(NeoFintechMotion.resize),
    ) {
        if (selectedDay == null) {
            Text(
                text = if (events.isEmpty()) "No hay eventos este mes" else "Toca un día para ver los eventos",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                textAlign = TextAlign.Center,
            )
            return@Column
        }

        Text(
            text = "$selectedDay de ${spanishMonthNames[displayMonth - 1]} de $displayYear",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            color = colors.onSurface,
        )
        Spacer(Modifier.height(4.dp))

        if (events.isEmpty()) {
            Text(
                text = "Sin eventos este día",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                textAlign = TextAlign.Center,
            )
            return@Column
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            events.sortedBy { it.dateMillis }.forEach { event ->
                val pending = pendingTotalsByEvent[event.id] ?: 0.0
                val isRange = event.startDateMillis != event.endDateMillis

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .pressableCard(
                            onClick = { onOpenEvent(event) },
                            shape = NeoFintechShapes.sm,
                        ),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLowest),
                    shape = NeoFintechShapes.sm,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(40.dp)
                                .background(event.state.statusColor(), RoundedCornerShape(2.dp)),
                        )
                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = event.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = colors.onSurface,
                            )
                            Text(
                                text = if (isRange) {
                                    "Del ${formatDateMillis(event.startDateMillis)} al ${formatDateMillis(event.endDateMillis)}"
                                } else {
                                    formatDateMillis(event.dateMillis)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                            )
                        }

                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(event.state.statusColor(), CircleShape)
                                )
                                Text(
                                    text = event.state.statusLabel(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = event.state.statusColor(),
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = when {
                                    event.state == EventState.CLOSED -> "Saldado"
                                    pending > 0.0 -> "Pendiente: ${formatEuros(pending)}"
                                    else -> ""
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (event.state == EventState.CLOSED) {
                                    colors.onSurfaceVariant
                                } else {
                                    colors.error
                                },
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
