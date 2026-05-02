package com.cuentamorosos

import java.text.SimpleDateFormat
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
