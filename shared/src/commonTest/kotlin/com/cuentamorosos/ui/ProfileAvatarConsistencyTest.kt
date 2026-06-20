package com.cuentamorosos.ui

import com.cuentamorosos.model.ProfileItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for ProfileAvatar infrastructure used by PreviewBreakdown,
 * TransferListPanel, and CalculatorSheet for consistent avatar rendering.
 *
 * Note: Compose UI rendering tests are not available in commonTest (no
 * compose.ui.test dependency). These unit tests validate the pure functions
 * and data contracts that the ProfileAvatar composable depends on.
 */
class ProfileAvatarConsistencyTest {

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun testProfile(
        id: String = "prof-1",
        name: String = "Test",
        photoUrl: String? = null,
    ) = ProfileItem(
        id = id,
        name = name,
        photoUrl = photoUrl,
    )

    /**
     * Simulates the initial extraction logic used by ProfileAvatar:
     * first non-blank char of trimmed name, uppercased.
     */
    private fun extractInitial(name: String): String =
        name.trim().firstOrNull()?.uppercaseChar()?.toString().orEmpty()

    // ── colorForName — deterministic avatar background colors ────────────────

    @Test
    fun `colorForName returns same color for same name`() {
        val c1 = colorForName("Ana")
        val c2 = colorForName("Ana")
        assertEquals(c1, c2, "Same name must produce same color")
    }

    @Test
    fun `colorForName returns consistent color across calls`() {
        val color = colorForName("Carlos")
        for (i in 1..100) {
            assertEquals(color, colorForName("Carlos"), "colorForName must be deterministic")
        }
    }

    @Test
    fun `colorForName handles empty string`() {
        // Should not throw; returns a color from the palette
        val color = colorForName("")
        assertNotNull(color, "colorForName must return a color even for empty input")
    }

    @Test
    fun `colorForName handles whitespace name`() {
        val color = colorForName("   ")
        assertNotNull(color, "colorForName must handle whitespace-only names")
    }

    @Test
    fun `colorForName different names typically produce different colors`() {
        val names = listOf("Ana", "Bob", "Carlos", "Diana", "Elena", "Fran")
        val colors = names.map { colorForName(it) }
        val uniqueColors = colors.distinct()
        // With 6 names and 12 palette colors, most should be unique
        assertTrue(uniqueColors.size >= 4, "Expected at least 4 unique colors for 6 names")
    }

    // ── Initial extraction logic ─────────────────────────────────────────────

    @Test
    fun `extractInitial returns first uppercase letter for simple name`() {
        assertEquals("A", extractInitial("Ana"))
        assertEquals("C", extractInitial("Carlos"))
    }

    @Test
    fun `extractInitial returns first letter for compound name`() {
        // ProfileAvatar uses first char only, not initials from all words
        assertEquals("J", extractInitial("Juan Pérez"))
        assertEquals("M", extractInitial("María José"))
    }

    @Test
    fun `extractInitial returns empty for blank name`() {
        assertEquals("", extractInitial(""))
        assertEquals("", extractInitial("   "))
    }

    @Test
    fun `extractInitial trims whitespace before extracting`() {
        assertEquals("L", extractInitial("  Luis"))
        assertEquals("P", extractInitial("Pepe  "))
    }

    // ── ProfileItem data contract for ProfileAvatar ──────────────────────────

    @Test
    fun `profileItem with photoUrl provides all avatar data`() {
        val profile = testProfile(
            name = "Pepe",
            photoUrl = "https://example.com/photo.jpg",
        )
        assertEquals("Pepe", profile.name)
        assertEquals("https://example.com/photo.jpg", profile.photoUrl)
    }

    @Test
    fun `profileItem without photoUrl has null photoUrl for fallback`() {
        val profile = testProfile(
            name = "Luis",
            photoUrl = null,
        )
        assertEquals("Luis", profile.name)
        assertNull(profile.photoUrl, "photoUrl must be null when not provided")
    }

    @Test
    fun `profileItem with empty name falls back to initial extraction`() {
        val profile = testProfile(
            name = "",
            photoUrl = null,
        )
        assertEquals("", profile.name)
        assertNull(profile.photoUrl)
        // When name is empty, initial extraction returns empty
        assertEquals("", extractInitial(profile.name))
    }

    // ── Multi-profile scenario (PreviewBreakdown context) ────────────────────

    @Test
    fun `multiple profiles each have distinct avatar data`() {
        val profiles = listOf(
            testProfile("p1", "Ana", "https://photos.example/ana.jpg"),
            testProfile("p2", "Bob", null),
            testProfile("p3", "Carlos", null),
        )

        // PhotoUrl present → should load photo
        assertNotNull(profiles[0].photoUrl)
        assertEquals("https://photos.example/ana.jpg", profiles[0].photoUrl)

        // PhotoUrl null → should fall back to initial
        assertNull(profiles[1].photoUrl)
        assertEquals("B", extractInitial(profiles[1].name))

        assertNull(profiles[2].photoUrl)
        assertEquals("C", extractInitial(profiles[2].name))

        // All initials are distinct
        val initials = profiles.map { extractInitial(it.name) }
        assertEquals(listOf("A", "B", "C"), initials)
    }

    // ── TransferListPanel data contract (profile IDs → ProfileItem) ──────────

    @Test
    fun `profile lookup from id resolves full avatar data`() {
        val profiles = listOf(
            testProfile("ana-1", "Ana", "https://photos/ana.jpg"),
            testProfile("carlos-2", "Carlos", null),
        )

        // Simulate profileNameResolver upgraded to profileResolver
        fun resolveProfile(id: String): ProfileItem? = profiles.find { it.id == id }

        val ana = resolveProfile("ana-1")
        assertNotNull(ana)
        assertEquals("Ana", ana.name)
        assertNotNull(ana.photoUrl)

        val carlos = resolveProfile("carlos-2")
        assertNotNull(carlos)
        assertEquals("Carlos", carlos.name)
        assertNull(carlos.photoUrl)

        val unknown = resolveProfile("nonexistent")
        assertNull(unknown, "Unknown profile id should return null")
    }

    // ── CalculatorSheet ParameterInputRow data contract ──────────────────────

    @Test
    fun `parameterInputRow profile has name and optional photoUrl`() {
        val maria = testProfile(
            name = "María",
            photoUrl = "https://photos/maria.jpg",
        )

        // ProfileAvatar(name=maria.name, photoUrl=maria.photoUrl, size=24.dp)
        assertEquals("María", maria.name)
        assertEquals("https://photos/maria.jpg", maria.photoUrl)
    }

    @Test
    fun `parameterInputRow without photoUrl falls back to initial`() {
        val luis = testProfile(
            name = "Luis",
            photoUrl = null,
        )

        assertEquals("Luis", luis.name)
        assertNull(luis.photoUrl)
        assertEquals("L", extractInitial(luis.name))
    }
}
