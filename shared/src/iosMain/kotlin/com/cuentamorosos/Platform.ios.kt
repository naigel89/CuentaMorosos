@file:OptIn(ExperimentalForeignApi::class)

package com.cuentamorosos

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSUUID
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.localeWithLocaleIdentifier
import platform.Foundation.timeIntervalSince1970

private val dateFormatter: NSDateFormatter by lazy {
    NSDateFormatter().apply {
        dateFormat = "dd/MM/yyyy"
        locale = NSLocale.currentLocale
        lenient = false
    }
}

private val esLocale: NSLocale by lazy {
    NSLocale.localeWithLocaleIdentifier("es_ES")
}

actual fun generateUuid(): String = NSUUID().UUIDString()

actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

actual fun formatDateMillis(millis: Long): String {
    val date = NSDate.dateWithTimeIntervalSince1970(millis / 1000.0)
    return dateFormatter.stringFromDate(date)
}

actual fun parseDateString(value: String): Long? {
    val date = dateFormatter.dateFromString(value) ?: return null
    return (date.timeIntervalSince1970 * 1000).toLong()
}

actual fun currentDateText(): String {
    return dateFormatter.stringFromDate(NSDate())
}

actual fun isValidEmail(email: String): Boolean {
    // Simple RFC-5322 compatible regex for KMP
    val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    return emailRegex.matches(email)
}

actual fun shortWeekDayNames(): List<String> {
    // L M X J V S D (Spanish abbreviations, Monday-first)
    return listOf("L", "M", "X", "J", "V", "S", "D")
}

actual fun calendarFieldsForYearMonth(year: Int, month: Int): CalendarFields {
    val cal = NSCalendar(NSCalendarIdentifierGregorian)
    val comps = NSDateComponents().apply {
        this.year = year.toLong()
        this.month = month.toLong()
        this.day = 1
    }
    val date = cal.dateFromComponents(comps) ?: return CalendarFields(year, month, 0, 30)

    // Days in month
    val rangeResult = cal.rangeOfUnit(NSCalendarUnitDay, NSCalendarUnitMonth, date)
    val daysInMonth = rangeResult.length.toInt()

    // First weekday offset (Monday = 0)
    val startComps = cal.components(
        NSCalendarUnitDay or NSCalendarUnitMonth or NSCalendarUnitYear,
        date
    )
    // weekday: 1=Sun in Gregorian; we want 0=Mon
    val weekdayComps = cal.components(
        platform.Foundation.NSCalendarUnitWeekday,
        date
    )
    val weekday = weekdayComps.weekday.toInt() // 1=Sun, 2=Mon, ..., 7=Sat
    val offset = (weekday - 2 + 7) % 7 // shift so Monday=0

    return CalendarFields(year = year, month = month, firstWeekDayOffset = offset, daysInMonth = daysInMonth)
}

actual fun previousMonth(year: Int, month: Int): CalendarFields {
    return if (month == 1) calendarFieldsForYearMonth(year - 1, 12)
    else calendarFieldsForYearMonth(year, month - 1)
}

actual fun nextMonth(year: Int, month: Int): CalendarFields {
    return if (month == 12) calendarFieldsForYearMonth(year + 1, 1)
    else calendarFieldsForYearMonth(year, month + 1)
}

actual fun currentYearMonth(): CalendarFields {
    val cal = NSCalendar(NSCalendarIdentifierGregorian)
    val now = NSDate()
    val comps = cal.components(NSCalendarUnitYear or NSCalendarUnitMonth, now)
    val year = comps.year.toInt()
    val month = comps.month.toInt()
    return calendarFieldsForYearMonth(year, month)
}
