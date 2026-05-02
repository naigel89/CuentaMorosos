package com.cuentamorosos

/** Genera un identificador único universal. */
expect fun generateUuid(): String

/** Devuelve el tiempo actual en milisegundos desde epoch. */
expect fun currentTimeMillis(): Long

/** Formatea un [millis] timestamp a "dd/MM/yyyy". */
expect fun formatDateMillis(millis: Long): String

/** Parsea una fecha en formato "dd/MM/yyyy" y devuelve millis, o null si es inválida. */
expect fun parseDateString(value: String): Long?

/** Devuelve la fecha actual en formato "dd/MM/yyyy". */
expect fun currentDateText(): String
