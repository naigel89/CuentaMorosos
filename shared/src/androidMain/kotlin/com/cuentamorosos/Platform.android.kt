package com.cuentamorosos

import android.util.Patterns
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

actual fun generateUuid(): String = UUID.randomUUID().toString()

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun formatDateMillis(millis: Long): String = synchronized(dateFormatter) {
    dateFormatter.format(Date(millis))
}

actual fun parseDateString(value: String): Long? = synchronized(dateFormatter) {
    runCatching {
        dateFormatter.isLenient = false
        dateFormatter.parse(value)?.time
    }.getOrNull()
}

actual fun currentDateText(): String = synchronized(dateFormatter) {
    dateFormatter.format(Date())
}

actual fun isValidEmail(email: String): Boolean =
    Patterns.EMAIL_ADDRESS.matcher(email).matches()

actual fun shortWeekDayNames(): List<String> {
    val symbols = DateFormatSymbols(Locale("es", "ES"))
    // Java short weekday names: index 0 unused, 1=Sun, 2=Mon, ... 7=Sat
    val raw = symbols.shortWeekdays // length 8
    // Reorder to Mon-Sun and take first letter uppercase
    val mondayFirst = listOf(2, 3, 4, 5, 6, 7, 1).map { raw[it].first().uppercaseChar().toString() }
    return mondayFirst
}

actual fun calendarFieldsForYearMonth(year: Int, month: Int): CalendarFields {
    val cal = Calendar.getInstance(Locale("es", "ES")).apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, 1)
        firstDayOfWeek = Calendar.MONDAY
    }
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    // DAY_OF_WEEK: 1=Sun, 2=Mon, ..., 7=Sat — convert to 0=Mon offset
    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    val offset = (dayOfWeek - Calendar.MONDAY + 7) % 7
    return CalendarFields(year = year, month = month, firstWeekDayOffset = offset, daysInMonth = daysInMonth)
}

actual fun previousMonth(year: Int, month: Int): CalendarFields {
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MONTH, -1)
    }
    return calendarFieldsForYearMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
}

actual fun nextMonth(year: Int, month: Int): CalendarFields {
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MONTH, 1)
    }
    return calendarFieldsForYearMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
}

actual fun currentYearMonth(): CalendarFields {
    val cal = Calendar.getInstance()
    return calendarFieldsForYearMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
}
