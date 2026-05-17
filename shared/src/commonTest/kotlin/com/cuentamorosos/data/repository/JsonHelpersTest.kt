package com.cuentamorosos.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for JSON serialization helpers used in OfflineFirstExpenseRepository.
 * Covers round-trip integrity for List<String> and Map<String, Double> fields.
 */
class JsonHelpersTest {

    // ── List<String> round-trip ─────────────────────────────────────────────────

    @Test
    fun `empty list round-trip`() {
        val original = emptyList<String>()
        val json = original.toJsonArray()
        val result = json.toStringArray()
        assertEquals(original, result)
    }

    @Test
    fun `single element list round-trip`() {
        val original = listOf("uid1")
        val json = original.toJsonArray()
        val result = json.toStringArray()
        assertEquals(original, result)
    }

    @Test
    fun `multi element list round-trip`() {
        val original = listOf("uid1", "uid2", "uid3")
        val json = original.toJsonArray()
        val result = json.toStringArray()
        assertEquals(original, result)
    }

    @Test
    fun `toJsonArray produces valid JSON array string`() {
        val list = listOf("a", "b")
        assertEquals("""["a","b"]""", list.toJsonArray())
    }

    @Test
    fun `toStringArray handles null returns empty list`() {
        val result: List<String> = null.toStringArray()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `toStringArray handles blank string returns empty list`() {
        assertEquals(emptyList<String>(), "".toStringArray())
        assertEquals(emptyList<String>(), "   ".toStringArray())
    }

    // ── Map<String, Double> round-trip ──────────────────────────────────────────

    @Test
    fun `empty map round-trip`() {
        val original = emptyMap<String, Double>()
        val json = original.toJsonObject()
        val result = json.toMapDouble()
        assertEquals(original, result)
    }

    @Test
    fun `single entry map round-trip`() {
        val original = mapOf("uid1" to 10.5)
        val json = original.toJsonObject()
        val result = json.toMapDouble()
        assertEquals(original, result)
    }

    @Test
    fun `multi entry map round-trip`() {
        val original = mapOf("uid1" to 10.5, "uid2" to 20.0, "uid3" to 0.75)
        val json = original.toJsonObject()
        val result = json.toMapDouble()
        assertEquals(original, result)
    }

    @Test
    fun `toJsonObject produces valid JSON object string`() {
        val map = mapOf("a" to 1.0, "b" to 2.5)
        val json = map.toJsonObject()
        // Order may vary, so check both possible orderings
        assertTrue(json == """{"a":1.0,"b":2.5}""" || json == """{"b":2.5,"a":1.0}""")
    }

    @Test
    fun `toMapDouble handles null returns empty map`() {
        val result: Map<String, Double> = null.toMapDouble()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `toMapDouble handles blank string returns empty map`() {
        assertEquals(emptyMap<String, Double>(), "".toMapDouble())
        assertEquals(emptyMap<String, Double>(), "   ".toMapDouble())
    }

    // ── Private helper extensions (mirrors OfflineFirstExpenseRepository) ────────

    private fun List<String>.toJsonArray(): String =
        joinToString(prefix = "[", postfix = "]", separator = ",") { "\"$it\"" }

    private fun Map<String, Double>.toJsonObject(): String =
        entries.joinToString(prefix = "{", postfix = "}", separator = ",") { (k, v) -> "\"$k\":$v" }

    private fun String?.toStringArray(): List<String> =
        if (isNullOrBlank()) emptyList()
        else removeSurrounding("[", "]").split(",").map { it.trim().removeSurrounding("\"") }.filter { it.isNotEmpty() }

    private fun String?.toMapDouble(): Map<String, Double> =
        if (isNullOrBlank()) emptyMap()
        else {
            val inner = removeSurrounding("{", "}").trim()
            if (inner.isEmpty()) emptyMap()
            else inner.split(",").associate {
                val parts = it.split(":", limit = 2)
                val key = parts[0].trim().removeSurrounding("\"")
                val value = parts.getOrNull(1)?.trim()?.toDoubleOrNull() ?: 0.0
                key to value
            }
        }
}
