package com.cuentamorosos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cuentamorosos.CalendarFields
import com.cuentamorosos.calendarFieldsForYearMonth
import com.cuentamorosos.currentYearMonth
import com.cuentamorosos.currentTimeMillis
import com.cuentamorosos.nextMonth
import com.cuentamorosos.previousMonth
import com.cuentamorosos.shortWeekDayNames
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.formatEuros

// Spanish month names (KMP-compatible, no java.text.DateFormatSymbols)
private val spanishMonthNames = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)

@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    events: List<EventItem>,
    pendingTotalsByEvent: Map<String, Double>,
    onOpenEvent: (EventItem) -> Unit,
) {
    // State for the displayed month using KMP-compatible utilities
    val todayFields = remember { currentYearMonth() }
    var displayYear by remember { mutableStateOf(todayFields.year) }
    var displayMonth by remember { mutableStateOf(todayFields.month) } // 1-12
    var selectedDay by remember { mutableStateOf<Int?>(null) }

    // Map day-of-month → list of events for that month/year
    val eventsByDay by remember(events, displayYear, displayMonth) {
        derivedStateOf {
            val map = mutableMapOf<Int, MutableList<EventItem>>()
            events.forEach { event ->
                val eventFields = calendarFieldsForYearMonth(
                    year = event.dateMillis.toYear(),
                    month = event.dateMillis.toMonth()
                )
                if (eventFields.year == displayYear && eventFields.month == displayMonth) {
                    val day = event.dateMillis.toDayOfMonth()
                    map.getOrPut(day) { mutableListOf() }.add(event)
                }
            }
            map.toMap()
        }
    }

    // Events for the selected day
    val selectedDayEvents by remember(eventsByDay, selectedDay) {
        derivedStateOf {
            selectedDay?.let { eventsByDay[it] } ?: emptyList()
        }
    }

    // Calculate the month grid using KMP-compatible utilities
    val calGrid: List<Int?> by remember(displayYear, displayMonth) {
        derivedStateOf {
            val fields = calendarFieldsForYearMonth(displayYear, displayMonth)
            val firstDow = fields.firstWeekDayOffset // 0 = Monday
            val daysInMonth = fields.daysInMonth
            // Build list of cells: null = empty, Int = day
            val cells = mutableListOf<Int?>()
            repeat(firstDow) { cells.add(null) }
            for (d in 1..daysInMonth) cells.add(d)
            // Fill until multiple of 7
            while (cells.size % 7 != 0) cells.add(null)
            cells
        }
    }

    // Week day labels from KMP utility
    val weekDayLabels = shortWeekDayNames()

    // Today's day if we're displaying the current month
    val todayDayFields = currentYearMonth()
    val todayDay by remember {
        derivedStateOf {
            val now = currentTimeMillis()
            if (todayDayFields.year == displayYear && todayDayFields.month == displayMonth &&
                now.toYear() == displayYear && now.toMonth() == displayMonth
            ) now.toDayOfMonth() else -1
        }
    }

    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        // ── Month header ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = {
                val prev = previousMonth(displayYear, displayMonth)
                displayYear = prev.year
                displayMonth = prev.month
                selectedDay = null
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Mes anterior")
            }
            Text(
                text = "${spanishMonthNames[displayMonth - 1]} $displayYear",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = {
                val next = nextMonth(displayYear, displayMonth)
                displayYear = next.year
                displayMonth = next.month
                selectedDay = null
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Mes siguiente")
            }
        }

        // ── Week day headers ──────────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth()) {
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

        // ── Day grid ─────────────────────────────────────────────────────────
        calGrid.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .then(
                                if (day != null) Modifier.clickable {
                                    selectedDay = if (selectedDay == day) null else day
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (day != null) {
                            val isSelected = selectedDay == day
                            val isToday = day == todayDay
                            val hasEvents = eventsByDay.containsKey(day)

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .then(
                                        if (isSelected) Modifier.background(
                                            MaterialTheme.colorScheme.primary,
                                            CircleShape,
                                        )
                                        else if (isToday) Modifier.background(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            CircleShape,
                                        )
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = day.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                            else -> MaterialTheme.colorScheme.onSurface
                                        },
                                    )
                                    if (hasEvents) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                    else MaterialTheme.colorScheme.primary,
                                                    CircleShape,
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Events list for selected day ──────────────────────────────────────
        val dayEvents = selectedDayEvents
        if (dayEvents.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Eventos del día $selectedDay",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            dayEvents.forEach { event ->
                val pending = pendingTotalsByEvent[event.id] ?: 0.0
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { onOpenEvent(event) },
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = event.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (pending > 0.0) {
                            Text(
                                text = "Pendiente: ${formatEuros(pending)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        } else {
                            Text(
                                text = "Sin deuda pendiente",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        } else if (selectedDay != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Sin eventos el día $selectedDay",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
        } else {
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (eventsByDay.isEmpty()) "No hay eventos este mes"
                       else "Toca un día para ver los eventos",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── KMP-compatible date extraction helpers ────────────────────────────────────

/** Extract year from millis using the same logic as the Platform implementations. */
private fun Long.toYear(): Int {
    // Days since epoch
    val days = this / (24L * 60L * 60L * 1000L)
    var y = 1970
    while (true) {
        val daysInYear = if (isLeapYear(y)) 366 else 365
        if (days < daysInYear) break
        // We need to track remaining days
        return@toYear computeYearFromDays(this)
    }
    return y
}

private fun computeYearFromDays(millis: Long): Int {
    var remaining = millis / (24L * 60L * 60L * 1000L)
    var y = 1970
    while (true) {
        val daysInYear = if (isLeapYear(y)) 366 else 365
        if (remaining < daysInYear) return y
        remaining -= daysInYear
        y++
    }
}

private fun Long.toMonth(): Int {
    val days = this / (24L * 60L * 60L * 1000L)
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
        if (remaining < monthDays[m]) return m + 1
    }
    return 12
}

private fun Long.toDayOfMonth(): Int {
    val days = this / (24L * 60L * 60L * 1000L)
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
