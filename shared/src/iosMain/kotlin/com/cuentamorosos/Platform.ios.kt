package com.cuentamorosos

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSUUID
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSince1970

private val dateFormatter: NSDateFormatter by lazy {
    NSDateFormatter().apply {
        dateFormat = "dd/MM/yyyy"
        locale = NSLocale.currentLocale
        lenient = false
    }
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
