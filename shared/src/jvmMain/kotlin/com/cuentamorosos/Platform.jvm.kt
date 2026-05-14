package com.cuentamorosos

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.DayOfWeek
import java.util.UUID

actual fun generateUuid(): String = UUID.randomUUID().toString()

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

actual fun formatDateMillis(millis: Long): String =
    LocalDateTime.ofInstant(
        java.time.Instant.ofEpochMilli(millis),
        java.time.ZoneId.systemDefault()
    ).format(dateFormatter)

actual fun parseDateString(value: String): Long? =
    runCatching {
        LocalDate.parse(value, dateFormatter)
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }.getOrNull()

actual fun currentDateText(): String = LocalDate.now().format(dateFormatter)

private val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()

actual fun isValidEmail(email: String): Boolean = emailRegex.matches(email)

actual fun shortWeekDayNames(): List<String> {
    val fmt = java.time.format.TextStyle.SHORT
    val locale = java.util.Locale.getDefault()
    return listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).map { it.getDisplayName(fmt, locale) }
}

actual fun calendarFieldsForYearMonth(year: Int, month: Int): CalendarFields {
    val ym = YearMonth.of(year, month)
    val firstDay = LocalDate.of(year, month, 1).dayOfWeek
    val offset = (firstDay.value - 1) % 7 // 0=Monday
    return CalendarFields(year, month, offset, ym.lengthOfMonth())
}

actual fun previousMonth(year: Int, month: Int): CalendarFields {
    val ym = YearMonth.of(year, month).minusMonths(1)
    return calendarFieldsForYearMonth(ym.year, ym.monthValue)
}

actual fun nextMonth(year: Int, month: Int): CalendarFields {
    val ym = YearMonth.of(year, month).plusMonths(1)
    return calendarFieldsForYearMonth(ym.year, ym.monthValue)
}

actual fun currentYearMonth(): CalendarFields {
    val now = LocalDate.now()
    return calendarFieldsForYearMonth(now.year, now.monthValue)
}
