package com.cuentamorosos.notifications

/**
 * Codificación del registro de notificaciones ya emitidas.
 *
 * Cada entrada es `"{epochMs}|{fingerprint}"`: la huella identifica la
 * notificación y la marca de tiempo permite podar el registro para que no crezca
 * sin límite.
 *
 * Estas funciones son puras y operan sobre el conjunto de entradas; cada
 * plataforma solo aporta *dónde* se guarda ese conjunto — `SharedPreferences` en
 * Android, `NSUserDefaults` en iOS. Al compartir la codificación, un registro
 * escrito por una versión no puede volverse ilegible para la otra.
 */
object NotificationDedupEntries {

    /** Días que se conserva una huella antes de poderse podar. */
    const val DEFAULT_MAX_AGE_DAYS = 30

    private const val SEPARATOR = '|'
    private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

    /** `true` si alguna entrada corresponde a [fingerprint]. Una huella vacía nunca cuenta. */
    fun contains(entries: Set<String>, fingerprint: String): Boolean {
        if (fingerprint.isBlank()) return false
        return entries.any { fingerprintOf(it) == fingerprint }
    }

    /**
     * Devuelve el conjunto con [fingerprint] registrado en [nowMillis].
     *
     * Si la huella está en blanco devuelve [entries] sin tocar, para que un
     * evento mal formado no ensucie el registro.
     */
    fun record(entries: Set<String>, fingerprint: String, nowMillis: Long): Set<String> {
        if (fingerprint.isBlank()) return entries
        return entries + "$nowMillis$SEPARATOR$fingerprint"
    }

    /**
     * Descarta las entradas anteriores a [maxAgeDays] contados desde [nowMillis].
     * Las entradas mal formadas —sin separador o con marca no numérica— también
     * se descartan: son ilegibles y no aportan deduplicación.
     */
    fun prune(
        entries: Set<String>,
        nowMillis: Long,
        maxAgeDays: Int = DEFAULT_MAX_AGE_DAYS,
    ): Set<String> {
        if (entries.isEmpty()) return entries
        val cutoff = nowMillis - maxAgeDays.toLong() * MILLIS_PER_DAY
        return entries.filterTo(mutableSetOf()) { entry ->
            val timestamp = timestampOf(entry)
            timestamp != null && timestamp >= cutoff
        }
    }

    /** La huella de una entrada, o `null` si está mal formada. */
    fun fingerprintOf(entry: String): String? {
        val separator = entry.indexOf(SEPARATOR)
        if (separator < 0) return null
        return entry.substring(separator + 1)
    }

    /** La marca de tiempo de una entrada, o `null` si está mal formada. */
    fun timestampOf(entry: String): Long? {
        val separator = entry.indexOf(SEPARATOR)
        if (separator < 0) return null
        return entry.substring(0, separator).toLongOrNull()
    }
}
