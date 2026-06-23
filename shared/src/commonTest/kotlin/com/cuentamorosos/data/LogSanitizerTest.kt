package com.cuentamorosos.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests LogSanitizer redaction rules and debug/release gating.
 *
 * Covers:
 * - Email redaction (spec data-leak R001)
 * - UID redaction (last 6 chars)
 * - Quoted name redaction
 * - Release no-op behavior
 */
class LogSanitizerTest {

    private fun captureLog(block: () -> Unit): List<String> {
        val output = mutableListOf<String>()
        val originalDebug = LogSanitizer.isDebug
        val originalOutput = LogSanitizer.output
        LogSanitizer.isDebug = true
        LogSanitizer.output = { output.add(it) }
        try {
            block()
        } finally {
            LogSanitizer.isDebug = originalDebug
            LogSanitizer.output = originalOutput
        }
        return output
    }

    // ── Email redaction ────────────────────────────────────────────────────

    @Test
    fun `redact emails — standard email`() {
        val result = LogSanitizer.redact("user@domain.com")
        assertEquals("use***@domain.com", result)
    }

    @Test
    fun `redact emails — short local part`() {
        val result = LogSanitizer.redact("ab@domain.com")
        assertEquals("a***@domain.com", result)
    }

    @Test
    fun `redact emails — email in context`() {
        val result = LogSanitizer.redact("linkedEmail=john.doe@gmail.com, other text")
        assertEquals("linkedEmail=joh***@gmail.com, other text", result)
    }

    @Test
    fun `redact emails — multiple emails`() {
        val result = LogSanitizer.redact("from: a@b.com to: xyz@foo.org")
        assertEquals("from: a***@b.com to: xyz***@foo.org", result)
    }

    @Test
    fun `redact emails — no email in text`() {
        val result = LogSanitizer.redact("no email here, just text")
        assertEquals("no email here, just text", result)
    }

    // ── UID redaction ──────────────────────────────────────────────────────

    @Test
    fun `redact UID — standard Firebase UID`() {
        val result = LogSanitizer.redact("uid=abcdef1234567890abcdef")
        assertEquals("uid=***abcdef", result)
    }

    @Test
    fun `redact UID — short UID preserved`() {
        val result = LogSanitizer.redact("uid=abc123")
        assertEquals("uid=abc123", result)
    }

    @Test
    fun `redact UID — UID in context`() {
        val result = LogSanitizer.redact("observeProfiles called, uid=xyzABC1234567890 end")
        assertEquals("observeProfiles called, uid=***567890 end", result)
    }

    // ── Quoted name redaction ──────────────────────────────────────────────

    @Test
    fun `redact quoted name — standard name`() {
        val result = LogSanitizer.redact("name='John Doe'")
        assertEquals("name='J*******'", result)
    }

    @Test
    fun `redact quoted name — short name`() {
        val result = LogSanitizer.redact("name='Al'")
        assertEquals("name='A*'", result)
    }

    @Test
    fun `redact quoted username`() {
        val result = LogSanitizer.redact("username='cooluser123'")
        assertEquals("username='c**********'", result)
    }

    @Test
    fun `redact quoted displayName`() {
        val result = LogSanitizer.redact("displayName='Jane Smith'")
        assertEquals("displayName='J*********'", result)
    }

    @Test
    fun `redact quoted name — preserves non-name fields`() {
        val result = LogSanitizer.redact("status='active' role='admin'")
        // status and role should NOT be redacted (only name-like keys)
        assertEquals("status='active' role='admin'", result)
    }

    @Test
    fun `redact quoted name — composite log line`() {
        val result = LogSanitizer.redact("newName='Alice' and username='alice99'")
        assertEquals("newName='A****' and username='a******'", result)
    }

    // ── Full redact() composition ──────────────────────────────────────────

    @Test
    fun `redact — all types in one line`() {
        val input = "uid=abcdef1234567890 name='Test User' linkedEmail=test@example.com"
        val result = LogSanitizer.redact(input)
        // UID redacted, email redacted
        assertTrue(result.contains("***567890"), "UID should be redacted, got: $result")
        assertTrue(result.contains("tes***@example.com"),
            "Email should be redacted, got: $result")
        assertFalse(result.contains("abcdef1234567890abcdef"), "Full UID must not appear")
        assertFalse(result.contains("test@example.com"), "Full email must not appear")
    }

    // ── Debug/release gate ─────────────────────────────────────────────────

    @Test
    fun `log — debug mode produces output`() {
        val output = captureLog {
            LogSanitizer.log("Test", "hello world")
        }
        assertEquals(1, output.size)
        assertTrue(output[0].contains("[Test] hello world"))
    }

    @Test
    fun `log — release mode produces no output`() {
        val outputs = mutableListOf<String>()
        val originalOutput = LogSanitizer.output
        LogSanitizer.isDebug = false
        LogSanitizer.output = { outputs.add(it) }
        try {
            LogSanitizer.log("Test", "should not appear")
        } finally {
            LogSanitizer.isDebug = true
            LogSanitizer.output = originalOutput
        }
        assertTrue(outputs.isEmpty(), "Release mode should produce no output")
    }

    @Test
    fun `log — redacts PII in output`() {
        val output = captureLog {
            LogSanitizer.log("Repo", "uid=abcdef1234567890 user@test.com")
        }
        assertEquals(1, output.size)
        assertFalse(output[0].contains("abcdef1234567890"), "UID must be redacted in output")
        assertFalse(output[0].contains("user@test.com"), "Email must be redacted in output")
    }
}
