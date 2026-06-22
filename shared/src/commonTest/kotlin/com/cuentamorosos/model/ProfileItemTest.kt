package com.cuentamorosos.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for ProfileItem backward compatibility and displayNameFor resolution.
 *
 * Covers:
 * - 7-field construction (original fields only) → new fields default correctly
 * - displayNameFor resolution: customName > name
 */
class ProfileItemTest {

    // ── Backward compatibility ────────────────────────────────────────────────

    @Test
    fun `backward compatibility - new fields default to null or empty`() {
        val profile = ProfileItem(
            id = "1",
            name = "Test",
        )

        assertEquals(null, profile.photoUrl, "photoUrl should be null by default")
        assertEquals(null, profile.username, "username should be null by default")
        assertEquals(emptyMap(), profile.customNames, "customNames should be empty by default")
    }

    // ── displayNameFor resolution ─────────────────────────────────────────────

    @Test
    fun `displayNameFor returns customName when present`() {
        val profile = ProfileItem(
            id = "1", name = "Original",
            customNames = mapOf("viewer1" to "CustomName", "viewer2" to "OtherName"),
        )

        assertEquals("CustomName", profile.displayNameFor("viewer1"))
    }

    @Test
    fun `displayNameFor falls back to name when no customName`() {
        val profile = ProfileItem(
            id = "1", name = "Original Name",
        )

        assertEquals("Original Name", profile.displayNameFor("anyViewer"))
    }

    @Test
    fun `displayNameFor with empty customNames falls back to name`() {
        val profile = ProfileItem(
            id = "1", name = "Original Name",
            customNames = emptyMap(),
        )
        assertEquals("Original Name", profile.displayNameFor("anyViewer"))
    }

    // ── Ghost profile fields (GPS-REQ-004: toMigrationMap coverage) ─────────

    @Test
    fun `ghost profile has isGhost true and linkedEmail set`() {
        val profile = ProfileItem(
            id = "ghost-1",
            name = "Ghost User",
            isGhost = true,
            linkedEmail = "ghost@example.com",
        )

        assertTrue(profile.isGhost, "isGhost should be true for ghost profiles")
        assertEquals("ghost@example.com", profile.linkedEmail)
    }

    @Test
    fun `regular profile has isGhost false and linkedEmail null by default`() {
        val profile = ProfileItem(
            id = "real-1",
            name = "Real User",
        )

        assertFalse(profile.isGhost, "isGhost should be false by default")
        assertNull(profile.linkedEmail, "linkedEmail should be null by default")
    }
}
