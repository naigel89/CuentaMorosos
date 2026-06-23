package com.cuentamorosos.ui.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the email verification gating logic that controls whether
 * a user must verify their email before accessing the main app.
 *
 * Note: Full Compose UI rendering of EmailVerificationScreen requires
 * Compose instrumentation tests (androidTest). This test suite covers
 * the pure state logic extracted as VerificationGate.
 */
class EmailVerificationGateTest {

    // ── VerificationGate pure function tests ────────────────────────────

    @Test
    fun `unverified email requires verification`() {
        assertTrue(
            VerificationGate.requiresVerification(isEmailVerified = false),
            "unverified email must require verification"
        )
    }

    @Test
    fun `verified email does not require verification`() {
        assertFalse(
            VerificationGate.requiresVerification(isEmailVerified = true),
            "verified email must NOT require verification"
        )
    }

    @Test
    fun `null verification status requires verification for safety`() {
        assertTrue(
            VerificationGate.requiresVerification(isEmailVerified = null),
            "null/unknown verification status must require verification as safety default"
        )
    }

    // ── VerificationGate state resolution tests ─────────────────────────

    @Test
    fun `resolveVerificationState returns needsVerification when unverified`() {
        val state = VerificationGate.resolveVerificationState(isEmailVerified = false)
        assertEquals(
            VerificationState.NeedsVerification,
            state,
            "unverified email must resolve to NeedsVerification state"
        )
    }

    @Test
    fun `resolveVerificationState returns verified when verified`() {
        val state = VerificationGate.resolveVerificationState(isEmailVerified = true)
        assertEquals(
            VerificationState.Verified,
            state,
            "verified email must resolve to Verified state"
        )
    }

    @Test
    fun `resolveVerificationState returns needsVerification when null`() {
        val state = VerificationGate.resolveVerificationState(isEmailVerified = null)
        assertEquals(
            VerificationState.NeedsVerification,
            state,
            "null verification status must default to NeedsVerification state"
        )
    }
}
