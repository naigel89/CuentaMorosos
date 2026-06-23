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

/** Valida si [email] tiene formato de dirección de correo electrónico válida. */
expect fun isValidEmail(email: String): Boolean

/** `true` cuando la app se ejecuta en modo debug; `false` en release. */
expect val isDebug: Boolean

/**
 * Devuelve los nombres abreviados de los días de la semana empezando en lunes (L, M, X, J, V, S, D).
 * El orden es: [0]=Lun, [1]=Mar, [2]=Mié, [3]=Jue, [4]=Vie, [5]=Sáb, [6]=Dom.
 */
expect fun shortWeekDayNames(): List<String>

/**
 * Devuelve los campos del mes/año para construir el calendario:
 * [year, month (1-12), firstDayOfWeekOffset (0=Mon), daysInMonth]
 */
expect fun calendarFieldsForYearMonth(year: Int, month: Int): CalendarFields

data class CalendarFields(
    val year: Int,
    val month: Int,       // 1–12
    val firstWeekDayOffset: Int,  // 0 = Monday, 6 = Sunday
    val daysInMonth: Int
)

/** Navega al mes anterior y devuelve [CalendarFields] del resultado. */
expect fun previousMonth(year: Int, month: Int): CalendarFields

/** Navega al mes siguiente y devuelve [CalendarFields] del resultado. */
expect fun nextMonth(year: Int, month: Int): CalendarFields

/** Devuelve el año y mes actuales como [CalendarFields] (día ignorado). */
expect fun currentYearMonth(): CalendarFields

