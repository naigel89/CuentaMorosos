package com.cuentamorosos.notifications

import com.cuentamorosos.currentTimeMillis
import platform.Foundation.NSUserDefaults

/**
 * Implementación iOS de [NotificationDedupStore], respaldada por `NSUserDefaults`.
 *
 * La codificación de las entradas y la poda viven en [NotificationDedupEntries],
 * compartidas con Android: solo cambia dónde se guarda el conjunto.
 *
 * `NSUserDefaults` es adecuado aquí porque el volumen es mínimo (una huella corta
 * por notificación, podada a los 30 días) y porque debe sobrevivir al reinicio de
 * la app sin depender de que la base SQLDelight esté abierta — las push llegan
 * también con la app cerrada.
 */
class IosNotificationDedupStore(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : NotificationDedupStore {

    override fun hasBeenSent(fingerprint: String): Boolean =
        NotificationDedupEntries.contains(readEntries(), fingerprint)

    override fun recordSent(fingerprint: String) {
        val now = currentTimeMillis()
        // Se poda al escribir: es el único momento en que el registro crece.
        val pruned = NotificationDedupEntries.prune(readEntries(), now)
        val updated = NotificationDedupEntries.record(pruned, fingerprint, now)
        if (updated != readEntries()) writeEntries(updated)
    }

    private fun readEntries(): Set<String> {
        val stored = defaults.stringArrayForKey(KEY) ?: return emptySet()
        return stored.filterIsInstance<String>().toSet()
    }

    private fun writeEntries(entries: Set<String>) {
        defaults.setObject(entries.toList(), forKey = KEY)
    }

    private companion object {
        const val KEY = "cuenta_morosos_sent_fingerprints"
    }
}
