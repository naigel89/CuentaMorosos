package com.cuentamorosos.data

import com.cuentamorosos.isDebug as platformIsDebug

/**
 * Debug-only logging wrapper with PII redaction.
 *
 * In release builds ([Platform.isDebug] == false), [log] is a no-op.
 * In debug builds, messages are redacted for emails, UIDs, and names
 * before being printed.
 *
 * The [output] function is swappable for testing.
 */
object LogSanitizer {

    /** Gate: set to true in tests to simulate debug mode. Defaults to [platformIsDebug]. */
    var isDebug: Boolean = platformIsDebug

    /** Output sink — swap in tests to capture log output. */
    var output: (String) -> Unit = { msg -> println(msg) }

    /**
     * Logs a message in debug mode, with PII redacted.
     * In release mode, this method does nothing.
     */
    fun log(tag: String, msg: String) {
        if (!isDebug) return
        output("[$tag] ${redact(msg)}")
    }

    /**
     * Redacts PII from a log message string.
     *
     * Redaction rules:
     * - Emails: `user@domain.com` → `use***@domain.com` (first 3 chars + `***` + domain)
     * - UIDs: hex strings with context `uid=` → last 6 chars only
     * - Names: `name='...'`, `username='...'`, etc. → first char + asterisks
     */
    internal fun redact(msg: String): String {
        var result = msg
        result = redactEmails(result)
        result = redactUids(result)
        result = redactQuotedNames(result)
        return result
    }

    // ── Email redaction ─────────────────────────────────────────────────────
    // Matches email-like patterns: local@domain
    private val EMAIL_REGEX = Regex("[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}")

    private fun redactEmails(input: String): String {
        return EMAIL_REGEX.replace(input) { match ->
            val email = match.value
            val atIndex = email.indexOf('@')
            if (atIndex >= 3) {
                "${email.take(3)}***${email.substring(atIndex)}"
            } else if (atIndex > 0) {
                "${email.take(1)}***${email.substring(atIndex)}"
            } else {
                email // no @, leave as-is (shouldn't happen)
            }
        }
    }

    // ── UID redaction ───────────────────────────────────────────────────────
    // Matches `uid=` followed by a hex-ish identifier
    private val UID_PATTERN = Regex("uid=([A-Za-z0-9_-]{8,})")

    private fun redactUids(input: String): String {
        return UID_PATTERN.replace(input) { match ->
            val full = match.value
            val uid = match.groupValues[1]
            if (uid.length > 6) {
                "uid=***${uid.takeLast(6)}"
            } else {
                full
            }
        }
    }

    // ── Quoted name redaction ───────────────────────────────────────────────
    // Matches key='value' patterns where key is name-like
    private val QUOTED_NAME_REGEX = Regex("(name|username|displayName|finalName|existingName|fbDisplayName|newName|oldName|newUsername|inviteeName|invitedByName)='([^']*)'")

    private fun redactQuotedNames(input: String): String {
        return QUOTED_NAME_REGEX.replace(input) { match ->
            val prefix = match.groupValues[1]
            val name = match.groupValues[2]
            val redactedName = if (name.length > 1) {
                "${name.take(1)}${"*".repeat(name.length - 1)}"
            } else {
                name
            }
            "$prefix='$redactedName'"
        }
    }
}
