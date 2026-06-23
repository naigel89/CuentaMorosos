package com.cuentamorosos.model.validation

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for Unicode control character stripping in validators.
 *
 * Covers:
 * - RTL override (U+202E) stripping (spec data-leak R003)
 * - ZWJ/ZWNJ removal
 * - Other Unicode control characters
 * - Internal whitespace collapsing
 * - Leading/trailing whitespace trimming
 */
class ValidatorSanitizeTest {

    // ── Unicode control character stripping ────────────────────────────────

    @Test
    fun `strip RTL override from text`() {
        // "Na\u202Eme" — 'm' with RTL override before 'e'
        val input = "Na\u202Eme"
        val sanitized = com.cuentamorosos.model.validation.sanitize(input)
        assertEquals("Name", sanitized)
    }

    @Test
    fun `strip LTR override from text`() {
        val input = "He\u202Dllo"
        val sanitized = com.cuentamorosos.model.validation.sanitize(input)
        assertEquals("Hello", sanitized)
    }

    @Test
    fun `strip zero-width joiner`() {
        // ZWJ (U+200D) is commonly used in emoji sequences
        val input = "ab\u200Dcd"
        val sanitized = com.cuentamorosos.model.validation.sanitize(input)
        assertEquals("abcd", sanitized)
    }

    @Test
    fun `strip zero-width non-joiner`() {
        val input = "ab\u200Ccd"
        val sanitized = com.cuentamorosos.model.validation.sanitize(input)
        assertEquals("abcd", sanitized)
    }

    @Test
    fun `strip zero-width space`() {
        val input = "ab\u200Bcd"
        val sanitized = com.cuentamorosos.model.validation.sanitize(input)
        assertEquals("abcd", sanitized)
    }

    @Test
    fun `strip BOM character`() {
        val input = "\uFEFFHello"
        val sanitized = com.cuentamorosos.model.validation.sanitize(input)
        assertEquals("Hello", sanitized)
    }

    @Test
    fun `strip line separator`() {
        val input = "Hello\u2028World"
        val sanitized = com.cuentamorosos.model.validation.sanitize(input)
        assertEquals("HelloWorld", sanitized)
    }

    @Test
    fun `strip paragraph separator`() {
        val input = "Hello\u2029World"
        val sanitized = com.cuentamorosos.model.validation.sanitize(input)
        assertEquals("HelloWorld", sanitized)
    }

    @Test
    fun `strip null character`() {
        val input = "Hello\u0000World"
        val sanitized = com.cuentamorosos.model.validation.sanitize(input)
        assertEquals("HelloWorld", sanitized)
    }

    @Test
    fun `strip DEL character`() {
        val input = "Hello\u007FWorld"
        val sanitized = com.cuentamorosos.model.validation.sanitize(input)
        assertEquals("HelloWorld", sanitized)
    }

    @Test
    fun `strip multiple control chars`() {
        val input = "\u202ENa\u200Bme\u200D\uFEFF!\u0000"
        val sanitized = com.cuentamorosos.model.validation.sanitize(input)
        assertEquals("Name!", sanitized)
    }

    // ── Whitespace normalization ───────────────────────────────────────────

    @Test
    fun `collapse multiple internal spaces`() {
        val input = "Fiesta    2026"
        val sanitized = com.cuentamorosos.model.validation.sanitize(input)
        assertEquals("Fiesta 2026", sanitized)
    }

    @Test
    fun `collapse tabs to spaces`() {
        val input = "Event\t\tName"
        val sanitized = com.cuentamorosos.model.validation.sanitize(input)
        assertEquals("Event Name", sanitized)
    }

    @Test
    fun `trim leading whitespace`() {
        val input = "   Hello World"
        val sanitized = com.cuentamorosos.model.validation.sanitize(input)
        assertEquals("Hello World", sanitized)
    }

    @Test
    fun `trim trailing whitespace`() {
        val input = "Hello World   "
        val sanitized = com.cuentamorosos.model.validation.sanitize(input)
        assertEquals("Hello World", sanitized)
    }

    @Test
    fun `trim and collapse combined`() {
        val input = "   Fiesta   2026   "
        val sanitized = com.cuentamorosos.model.validation.sanitize(input)
        assertEquals("Fiesta 2026", sanitized)
    }

    // ── Normal text passthrough ────────────────────────────────────────────

    @Test
    fun `clean text passes through unchanged`() {
        val input = "Normal Event Name"
        val sanitized = com.cuentamorosos.model.validation.sanitize(input)
        assertEquals("Normal Event Name", sanitized)
    }

    @Test
    fun `single character passes through`() {
        val input = "A"
        val sanitized = com.cuentamorosos.model.validation.sanitize(input)
        assertEquals("A", sanitized)
    }

    @Test
    fun `empty string stays empty`() {
        val input = ""
        val sanitized = com.cuentamorosos.model.validation.sanitize(input)
        assertEquals("", sanitized)
    }

    @Test
    fun `only whitespace becomes empty`() {
        val input = "   \t   "
        val sanitized = com.cuentamorosos.model.validation.sanitize(input)
        assertEquals("", sanitized)
    }

    @Test
    fun `accented characters preserved`() {
        val input = "Café Résumé"
        val sanitized = com.cuentamorosos.model.validation.sanitize(input)
        assertEquals("Café Résumé", sanitized)
    }
}
