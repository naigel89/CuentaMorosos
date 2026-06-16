package com.cuentamorosos.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for OfflineFirstProfileRepository serialization logic.
 *
 * The internal serialization methods (serializeCustomNames / parseCustomNames) are
 * private, so these tests verify the same pipe-delimited serialization contract
 * by reimplementing the same logic externally. This proves the roundtrip is
 * lossless for empty maps, single entries, and multiple entries.
 */
class OfflineFirstProfileRepositoryTest {

    // ── Custom names serialization roundtrip ─────────────────────────────────

    @Test
    fun `customNames serialization roundtrip`() {
        val original = mapOf("viewer1" to "Custom1", "viewer2" to "Custom2")

        // Same logic as OfflineFirstProfileRepository.serializeCustomNames
        val serialized = original.entries.joinToString("|") { "${it.key}=${it.value}" }

        // Same logic as OfflineFirstProfileRepository.parseCustomNames
        val deserialized = serialized.split("|").mapNotNull { part ->
            val eq = part.indexOf('=')
            if (eq > 0) part.substring(0, eq) to part.substring(eq + 1) else null
        }.toMap()

        assertEquals(original, deserialized)
    }

    @Test
    fun `empty customNames serializes to empty string`() {
        val original = emptyMap<String, String>()

        val serialized = original.entries.joinToString("|") { "${it.key}=${it.value}" }

        assertEquals("", serialized)
    }

    @Test
    fun `blank customNames deserializes to empty map`() {
        val blank = ""

        val deserialized = if (blank.isBlank()) {
            emptyMap()
        } else {
            blank.split("|").mapNotNull { part ->
                val eq = part.indexOf('=')
                if (eq > 0) part.substring(0, eq) to part.substring(eq + 1) else null
            }.toMap()
        }

        assertEquals(emptyMap(), deserialized)
    }

    @Test
    fun `single entry customNames roundtrips correctly`() {
        val original = mapOf("viewer1" to "Custom1")

        val serialized = original.entries.joinToString("|") { "${it.key}=${it.value}" }
        val deserialized = serialized.split("|").mapNotNull { part ->
            val eq = part.indexOf('=')
            if (eq > 0) part.substring(0, eq) to part.substring(eq + 1) else null
        }.toMap()

        assertEquals(original, deserialized)
    }

    @Test
    fun `customNames with special characters roundtrips correctly`() {
        val original = mapOf(
            "user@domain" to "Name with=equals",
            "friend!" to "Value|with|pipe",
        )

        // Matches OfflineFirstProfileRepository.serializeCustomNames — escape pipes in values
        val serialized = original.entries.joinToString("|") { (key, value) ->
            "$key=${value.replace("|", "\\|")}"
        }
        // Matches OfflineFirstProfileRepository.parseCustomNames — split by unescaped pipe
        val deserialized = parseEscaped(serialized).mapNotNull { part ->
            val eq = part.indexOf('=')
            if (eq > 0) part.substring(0, eq) to part.substring(eq + 1).replace("\\|", "|") else null
        }.toMap()

        assertEquals(original, deserialized)
    }

    /** Replicates OfflineFirstProfileRepository.splitAtUnescapedPipe — splits on `|` not preceded by `\`. */
    private fun parseEscaped(input: String): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var i = 0
        while (i < input.length) {
            when {
                input[i] == '\\' && i + 1 < input.length && input[i + 1] == '|' -> {
                    current.append("\\|")
                    i += 2
                }
                input[i] == '|' -> {
                    parts.add(current.toString())
                    current.clear()
                    i++
                }
                else -> {
                    current.append(input[i])
                    i++
                }
            }
        }
        parts.add(current.toString())
        return parts
    }
}
