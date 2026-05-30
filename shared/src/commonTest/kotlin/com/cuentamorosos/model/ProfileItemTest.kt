package com.cuentamorosos.model

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for ProfileItem backward compatibility and displayNameFor resolution.
 *
 * Covers:
 * - 7-field construction (original fields only) → new fields default correctly
 * - displayNameFor resolution: customName > displayName > name
 */
class ProfileItemTest {

    // ── Backward compatibility ────────────────────────────────────────────────

    @Test
    fun `backward compatibility - new fields default to null or empty`() {
        val profile = ProfileItem(
            id = "1",
            name = "Test",
            icon = "\uD83D\uDE42",
        )

        assertEquals(null, profile.photoUrl, "photoUrl should be null by default")
        assertEquals(null, profile.username, "username should be null by default")
        assertEquals(null, profile.displayName, "displayName should be null by default")
        assertEquals(emptyMap(), profile.customNames, "customNames should be empty by default")
    }

    // ── displayNameFor resolution ─────────────────────────────────────────────

    @Test
    fun `displayNameFor returns customName when present`() {
        val profile = ProfileItem(
            id = "1", name = "Original", icon = "\uD83D\uDE42",
            displayName = "Display",
            customNames = mapOf("viewer1" to "CustomName", "viewer2" to "OtherName"),
        )

        assertEquals("CustomName", profile.displayNameFor("viewer1"))
    }

    @Test
    fun `displayNameFor falls back to displayName when no customName`() {
        val profile = ProfileItem(
            id = "1", name = "Original", icon = "\uD83D\uDE42",
            displayName = "DisplayName",
        )

        assertEquals("DisplayName", profile.displayNameFor("anyViewer"))
    }

    @Test
    fun `displayNameFor falls back to name when no displayName or customName`() {
        val profile = ProfileItem(
            id = "1", name = "Original Name", icon = "\uD83D\uDE42",
        )

        assertEquals("Original Name", profile.displayNameFor("anyViewer"))
    }

    @Test
    fun `displayNameFor with empty customNames falls back to displayName then name`() {
        val profileWithDisplay = ProfileItem(
            id = "1", name = "Original", icon = "\uD83D\uDE42",
            displayName = "Display",
            customNames = emptyMap(),
        )
        assertEquals("Display", profileWithDisplay.displayNameFor("anyViewer"))

        val profileWithoutDisplay = ProfileItem(
            id = "1", name = "Original Name", icon = "\uD83D\uDE42",
            customNames = emptyMap(),
        )
        assertEquals("Original Name", profileWithoutDisplay.displayNameFor("anyViewer"))
    }
}
