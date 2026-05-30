package com.cuentamorosos.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
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

private val spanishMonthNames = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)

private val MAX_VISIBLE_BADGES = 3

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

    val dayRangeMillis by remember(displayYear, displayMonth) {
        derivedStateOf {
            val fields = calendarFieldsForYearMonth(displayYear, displayMonth)
            val startOfMonth = dateToMillis(displayYear, displayMonth, 1)
            val endOfMonth = dateToMillis(displayYear, displayMonth, fields.daysInMonth) + 86400000L
            startOfMonth to endOfMonth
        }
    }

    val eventsByDay by remember(events, dayRangeMillis) {
        derivedStateOf {
            val (monthStart, monthEnd) = dayRangeMillis
            val map = mutableMapOf<Int, MutableList<EventItem>>()
            events.forEach { event ->
                val start = event.startDateMillis.coerceAtLeast(monthStart)
                val end = event.endDateMillis.coerceAtMost(monthEnd - 1)
                if (start > end) return@forEach
                var dayStart = start
                while (dayStart <= end) {
                    val dayOfMonth = millisToDayOfMonth(dayStart)
                    if (dayOfMonth > 0) {
                        map.getOrPut(dayOfMonth) { mutableListOf() }.add(event)
                    }
                    dayStart += 86400000L
                }
            }
            map.toMap()
        }
    }

    val selectedDayEvents by remember(eventsByDay, selectedDay) {
        derivedStateOf {
            selectedDay?.let { eventsByDay[it] } ?: emptyList()
        }
    }

    val calGrid: List<Int?> by remember(displayYear, displayMonth) {
        derivedStateOf {
            val fields = calendarFieldsForYearMonth(displayYear, displayMonth)
            val firstDow = fields.firstWeekDayOffset
            val daysInMonth = fields.daysInMonth
            val cells = mutableListOf<Int?>()
            repeat(firstDow) { cells.add(null) }
            for (d in 1..daysInMonth) cells.add(d)
            while (cells.size % 7 != 0) cells.add(null)
            cells
        }
    }

    val weekDayLabels = shortWeekDayNames()

    val todayFieldsNow = remember { currentYearMonth() }
    val todayDay by remember(displayYear, displayMonth) {
        derivedStateOf {
            if (todayFieldsNow.year == displayYear && todayFieldsNow.month == displayMonth) {
                val todayMillis = currentTimeMillis()
                millisToDayOfMonth(todayMillis)
            } else -1
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // ── Month header ────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cerrar calendario") }
            Text(
                text = "${spanishMonthNames[displayMonth - 1]} $displayYear",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = {
                    val prev = previousMonth(displayYear, displayMonth)
                    displayYear = prev.year; displayMonth = prev.month; selectedDay = null
                }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Mes anterior") }
                IconButton(onClick = {
                    val next = nextMonth(displayYear, displayMonth)
                    displayYear = next.year; displayMonth = next.month; selectedDay = null
                }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Mes siguiente") }
            }
        }

        // ── Week day headers ───────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            weekDayLabels.forEach { label ->
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

        // ── Day grid with badges ────────────────────────────────────
        calGrid.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                week.forEach { day ->
                    Box(
                        modifier = Modifier.weight(1f).aspectRatio(0.9f).padding(1.dp)
                            .then(if (day != null) Modifier.clickable {
                                selectedDay = if (selectedDay == day) null else day
                            } else Modifier),
                    ) {
                        if (day != null) {
                            val isSelected = selectedDay == day
                            val isToday = day == todayDay
                            val dayEvents = eventsByDay[day] ?: emptyList()
                            val eventCount = dayEvents.size

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // Day number circle
                                Box(
                                    modifier = Modifier.size(28.dp).then(
                                        when {
                                            isSelected -> Modifier.background(
                                                MaterialTheme.colorScheme.primary, CircleShape
                                            )
                                            isToday -> Modifier.background(
                                                MaterialTheme.colorScheme.primaryContainer, CircleShape
                                            )
                                            else -> Modifier
                                        }
                                    ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = day.toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                            else -> MaterialTheme.colorScheme.onSurface
                                        },
                                    )
                                }

                                // Badge pills
                                Spacer(Modifier.height(2.dp))
                                dayEvents.take(MAX_VISIBLE_BADGES).forEach { event ->
                                    EventBadge(event = event)
                                }
                                if (eventCount > MAX_VISIBLE_BADGES) {
                                    Text(
                                        text = "+${eventCount - MAX_VISIBLE_BADGES} más",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 8.sp,
                                            lineHeight = 10.sp,
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
            }
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

@Composable
private fun EventBadge(event: EventItem) {
    val color = event.state.statusColor()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
            .padding(horizontal = 2.dp, vertical = 1.dp),
    ) {
        Text(
            text = event.name,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                lineHeight = 10.sp,
            ),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

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

    if (selectedDay == null) {
        Text(
            text = if (events.isEmpty()) "No hay eventos este mes" else "Tocá un día para ver los eventos",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            textAlign = TextAlign.Center,
        )
        return
    }

    Text(
        text = "${selectedDay} de ${spanishMonthNames[displayMonth - 1]} de $displayYear",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(4.dp))

    if (events.isEmpty()) {
        Text(
            text = "Sin eventos este día",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            textAlign = TextAlign.Center,
        )
        return
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        events.sortedBy { it.dateMillis }.forEach { event ->
            val pending = pendingTotalsByEvent[event.id] ?: 0.0
            val isRange = event.startDateMillis != event.endDateMillis

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clickable { onOpenEvent(event) },
                colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLowest),
                shape = NeoFintechShapes.sm,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // State color strip
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
                        )
                        if (isRange) {
                            Text(
                                text = "Del ${formatDateMillis(event.startDateMillis)} al ${formatDateMillis(event.endDateMillis)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Text(
                                text = formatDateMillis(event.dateMillis),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
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
                        if (pending > 0.0 && event.state != EventState.CLOSED) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Pendiente: ${formatEuros(pending)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        } else if (event.state == EventState.CLOSED) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Saldado",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
}

// ── Date helpers (KMP-compatible) ────────────────────────────────────

private fun dateToMillis(year: Int, month: Int, day: Int): Long {
    var totalDays = 0L
    for (y in 1970 until year) {
        totalDays += if (isLeapYear(y)) 366 else 365
    }
    val monthDays = if (isLeapYear(year)) leapYearMonthStarts else commonYearMonthStarts
    totalDays += monthDays[month - 1]
    totalDays += (day - 1)
    return totalDays * 86400000L
}

private fun millisToDayOfMonth(millis: Long): Int {
    val days = millis / 86400000L
    var remaining = days
    var y = 1970
    while (true) {
        val daysInYear = if (isLeapYear(y)) 366 else 365
        if (remaining < daysInYear) break
        remaining -= daysInYear
        y++
    }
    val monthDays = if (isLeapYear(y)) leapYearMonthStarts else commonYearMonthStarts
    for (m in monthDays.indices) {
        if (remaining < monthDays[m]) return (remaining - (if (m > 0) monthDays[m - 1] else 0)).toInt() + 1
    }
    return (remaining - monthDays[11] + 1).toInt() + 1
}

private fun isLeapYear(year: Int): Boolean =
    (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)

private val commonYearMonthStarts = listOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
private val leapYearMonthStarts = listOf(0, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335)
