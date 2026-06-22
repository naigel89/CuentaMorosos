package com.cuentamorosos.model.validation

import com.cuentamorosos.model.ProfileItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileValidatorTest {

    private fun testProfile(
        id: String = "p1",
        name: String = "Juan Pérez",
        ownerId: String = "owner1",
    ) = ProfileItem(
        id = id,
        name = name,
        ownerId = ownerId,
    )

    // ── PV-01: Profile name required ────────────────────────────────────────

    @Test
    fun `PV-01 name provided passes`() {
        val profile = testProfile(name = "Juan Pérez")
        val result = ProfileValidator.validate(profile, emptyList())
        assertFalse(result.allErrors().any { it.field == "name" })
    }

    @Test
    fun `PV-01 empty name returns error`() {
        val profile = testProfile(name = "")
        val result = ProfileValidator.validate(profile, emptyList())
        assertTrue(result.hasErrors())
        assertEquals("El nombre del perfil es obligatorio", result.allErrors().first().message)
    }

    @Test
    fun `PV-01 blank name returns error`() {
        val profile = testProfile(name = "   ")
        val result = ProfileValidator.validate(profile, emptyList())
        assertTrue(result.hasErrors())
        assertEquals("El nombre del perfil es obligatorio", result.allErrors().first().message)
    }

    // ── PV-02: Profile name uniqueness per owner ────────────────────────────

    @Test
    fun `PV-02 unique name passes`() {
        val profile = testProfile(name = "Juan")
        val existing = listOf(testProfile(id = "p2", name = "Maria"))
        val result = ProfileValidator.validate(profile, existing)
        assertFalse(result.allErrors().any { it.message.contains("existe") })
    }

    @Test
    fun `PV-02 duplicate name exact case returns error`() {
        val profile = testProfile(name = "Juan")
        val existing = listOf(testProfile(id = "p2", name = "Juan", ownerId = "owner1"))
        val result = ProfileValidator.validate(profile, existing)
        assertTrue(result.hasErrors())
        assertEquals("Ya existe un perfil con ese nombre", result.allErrors().first().message)
    }

    @Test
    fun `PV-02 duplicate name different case returns error`() {
        val profile = testProfile(name = "juan")
        val existing = listOf(testProfile(id = "p2", name = "JUAN", ownerId = "owner1"))
        val result = ProfileValidator.validate(profile, existing)
        assertTrue(result.hasErrors())
        assertEquals("Ya existe un perfil con ese nombre", result.allErrors().first().message)
    }

    @Test
    fun `PV-02 same name different owner passes`() {
        val profile = testProfile(name = "Juan", ownerId = "ownerA")
        val existing = listOf(testProfile(id = "p2", name = "Juan", ownerId = "ownerB"))
        val result = ProfileValidator.validate(profile, existing)
        assertFalse(result.allErrors().any { it.message.contains("existe") })
    }

    @Test
    fun `PV-02 self-edit — same profile being edited passes`() {
        val profile = testProfile(id = "p1", name = "Juan")
        val existing = listOf(profile) // self only
        val result = ProfileValidator.validate(profile, existing)
        assertFalse(result.allErrors().any { it.message.contains("existe") })
    }

    // ── PV-03 (ghost-sync): linkedEmail uniqueness (GPS-REQ-005) ────────────

    @Test
    fun `PV-03 ghost-sync duplicate linkedEmail returns error`() {
        val profile = testProfile(name = "Ghost A").copy(linkedEmail = "dup@test.com")
        val existing = listOf(
            testProfile(id = "p2", name = "Ghost B").copy(linkedEmail = "dup@test.com")
        )
        val result = ProfileValidator.validate(profile, existing)
        assertTrue(result.hasErrors())
        assertEquals("linkedEmail duplicado", result.allErrors().first { it.field == "linkedEmail" }.message)
        assertEquals("linkedEmail", result.allErrors().first { it.field == "linkedEmail" }.field)
    }

    @Test
    fun `PV-03 ghost-sync duplicate linkedEmail case-insensitive`() {
        val profile = testProfile(name = "Ghost A").copy(linkedEmail = "Dup@Test.com")
        val existing = listOf(
            testProfile(id = "p2", name = "Ghost B").copy(linkedEmail = "dup@test.com")
        )
        val result = ProfileValidator.validate(profile, existing)
        assertTrue(result.hasErrors())
        assertEquals("linkedEmail duplicado", result.allErrors().first { it.field == "linkedEmail" }.message)
    }

    @Test
    fun `PV-03 ghost-sync unique linkedEmail passes`() {
        val profile = testProfile(name = "Ghost C").copy(linkedEmail = "unique@test.com")
        val existing = listOf(
            testProfile(id = "p2", name = "Ghost D").copy(linkedEmail = "other@test.com")
        )
        val result = ProfileValidator.validate(profile, existing)
        assertFalse(result.allErrors().any { it.field == "linkedEmail" })
    }

    @Test
    fun `PV-03 ghost-sync null linkedEmail ignored`() {
        val profile = testProfile(name = "Ghost E").copy(linkedEmail = null)
        val existing = listOf(
            testProfile(id = "p2", name = "Ghost F").copy(linkedEmail = null)
        )
        val result = ProfileValidator.validate(profile, existing)
        assertFalse(result.allErrors().any { it.field == "linkedEmail" })
    }

    @Test
    fun `PV-03 ghost-sync self-edit with same linkedEmail passes`() {
        val profile = testProfile(id = "p1", name = "Self").copy(linkedEmail = "same@test.com")
        val existing = listOf(profile) // self only
        val result = ProfileValidator.validate(profile, existing)
        assertFalse(result.allErrors().any { it.field == "linkedEmail" })
    }

    // ── PV-03: Delete warning for active events ─────────────────────────────

    @Test
    fun `PV-03 profile not in any active events — no warning`() {
        val profile = testProfile()
        val warnings = ProfileValidator.checkDeleteWarning(profile, emptySet())
        assertTrue(warnings.isEmpty())
    }

    @Test
    fun `PV-03 profile participates in active events — warning returned`() {
        val profile = testProfile()
        val activeEventIds = setOf("evt1", "evt2")
        val warnings = ProfileValidator.checkDeleteWarning(profile, activeEventIds)
        assertFalse(warnings.isEmpty())
        assertEquals("Este perfil participa en eventos activos. Se mantendrá como '[perfil eliminado]'", warnings.first().message)
    }

    @Test
    fun `PV-03 only one warning even with multiple active events`() {
        val profile = testProfile()
        val activeEventIds = setOf("evt1", "evt2", "evt3")
        val warnings = ProfileValidator.checkDeleteWarning(profile, activeEventIds)
        assertEquals(1, warnings.size)
    }

    // ── Combined scenarios ──────────────────────────────────────────────────

    @Test
    fun `fully valid profile returns Success`() {
        val profile = testProfile()
        val result = ProfileValidator.validate(profile, emptyList())
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `profile with blank name and duplicate returns Failure`() {
        val profile = testProfile(name = "")
        val existing = listOf(testProfile(id = "p2", name = "", ownerId = "owner1"))
        val result = ProfileValidator.validate(profile, existing)
        assertTrue(result is ValidationResult.Failure)
        // Only one error since blank name short-circuits duplicate check
        assertEquals(1, result.errors.size)
    }
}
