package com.cuentamorosos.model

import com.cuentamorosos.data.repository.toEventInvitation
import com.cuentamorosos.data.repository.toFirestoreMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InvitationSerializationTest {

    // ── Task 1.1: Model fields ─────────────────────────────────────────────

    @Test
    fun `EventInvitation has invitedByName with empty default`() {
        val invitation = EventInvitation(
            id = "inv-1",
            eventId = "evt-1",
            eventName = "Test",
            invitedByUid = "uid-1",
            invitedByEmail = "ana@test.com",
            invitedEmail = "me@test.com",
        )
        assertEquals("", invitation.invitedByName)
    }

    @Test
    fun `EventInvitation has invitedByPhotoUrl with null default`() {
        val invitation = EventInvitation(
            id = "inv-1",
            eventId = "evt-1",
            eventName = "Test",
            invitedByUid = "uid-1",
            invitedByEmail = "ana@test.com",
            invitedEmail = "me@test.com",
        )
        assertNull(invitation.invitedByPhotoUrl)
    }

    @Test
    fun `EventInvitation accepts invitedByName`() {
        val invitation = EventInvitation(
            id = "inv-1",
            eventId = "evt-1",
            eventName = "Test",
            invitedByUid = "uid-1",
            invitedByEmail = "ana@test.com",
            invitedEmail = "me@test.com",
            invitedByName = "Ana García",
        )
        assertEquals("Ana García", invitation.invitedByName)
    }

    @Test
    fun `EventInvitation accepts invitedByPhotoUrl`() {
        val invitation = EventInvitation(
            id = "inv-1",
            eventId = "evt-1",
            eventName = "Test",
            invitedByUid = "uid-1",
            invitedByEmail = "ana@test.com",
            invitedEmail = "me@test.com",
            invitedByPhotoUrl = "https://example.com/photo.jpg",
        )
        assertEquals("https://example.com/photo.jpg", invitation.invitedByPhotoUrl)
    }

    // ── Task 1.2: Serialization ────────────────────────────────────────────

    @Test
    fun `toFirestoreMap includes invitedByName`() {
        val invitation = EventInvitation(
            id = "inv-1",
            eventId = "evt-1",
            eventName = "Test",
            invitedByUid = "uid-1",
            invitedByEmail = "ana@test.com",
            invitedEmail = "me@test.com",
            invitedByName = "Ana García",
        )
        val map = invitation.toFirestoreMap()
        assertEquals("Ana García", map["invitedByName"])
    }

    @Test
    fun `toFirestoreMap includes invitedByPhotoUrl`() {
        val invitation = EventInvitation(
            id = "inv-1",
            eventId = "evt-1",
            eventName = "Test",
            invitedByUid = "uid-1",
            invitedByEmail = "ana@test.com",
            invitedEmail = "me@test.com",
            invitedByPhotoUrl = "https://example.com/photo.jpg",
        )
        val map = invitation.toFirestoreMap()
        assertEquals("https://example.com/photo.jpg", map["invitedByPhotoUrl"])
    }

    @Test
    fun `toFirestoreMap serializes null invitedByPhotoUrl`() {
        val invitation = EventInvitation(
            id = "inv-1",
            eventId = "evt-1",
            eventName = "Test",
            invitedByUid = "uid-1",
            invitedByEmail = "ana@test.com",
            invitedEmail = "me@test.com",
        )
        val map = invitation.toFirestoreMap()
        assertNull(map["invitedByPhotoUrl"])
    }

    // ── Task 5.5: Legacy fallback (deserialization) ────────────────────────

    @Test
    fun `toEventInvitation falls back to invitedByEmail for invitedByName`() {
        val data = mapOf(
            "id" to "inv-1",
            "eventId" to "evt-1",
            "eventName" to "Test",
            "invitedByUid" to "uid-1",
            "invitedByEmail" to "ana@test.com",
            "invitedEmail" to "me@test.com",
            "status" to InvitationStatus.PENDING,
            "createdAtMillis" to 1000L,
            "expiresAtMillis" to 2000L,
        )
        val invitation = data.toEventInvitation()
        assertNotNull(invitation)
        assertEquals("ana@test.com", invitation.invitedByName)
    }

    @Test
    fun `toEventInvitation returns null for missing invitedByPhotoUrl`() {
        val data = mapOf(
            "id" to "inv-1",
            "eventId" to "evt-1",
            "eventName" to "Test",
            "invitedByUid" to "uid-1",
            "invitedByEmail" to "ana@test.com",
            "invitedEmail" to "me@test.com",
            "status" to InvitationStatus.PENDING,
            "createdAtMillis" to 1000L,
            "expiresAtMillis" to 2000L,
        )
        val invitation = data.toEventInvitation()
        assertNotNull(invitation)
        assertNull(invitation.invitedByPhotoUrl)
    }

    @Test
    fun `toEventInvitation uses invitedByName when present`() {
        val data = mapOf(
            "id" to "inv-1",
            "eventId" to "evt-1",
            "eventName" to "Test",
            "invitedByUid" to "uid-1",
            "invitedByEmail" to "ana@test.com",
            "invitedEmail" to "me@test.com",
            "status" to InvitationStatus.PENDING,
            "createdAtMillis" to 1000L,
            "expiresAtMillis" to 2000L,
            "invitedByName" to "Ana García",
            "invitedByPhotoUrl" to "https://example.com/photo.jpg",
        )
        val invitation = data.toEventInvitation()
        assertNotNull(invitation)
        assertEquals("Ana García", invitation.invitedByName)
        assertEquals("https://example.com/photo.jpg", invitation.invitedByPhotoUrl)
    }

    @Test
    fun `existing constructors unchanged — backward compatibility`() {
        // Verify that the same constructor pattern used in tests still works
        val invitation = EventInvitation(
            id = "inv-1",
            eventId = "evt-1",
            eventName = "Asado",
            invitedByUid = "user-other",
            invitedByEmail = "ana@test.com",
            invitedEmail = "me@test.com",
            status = InvitationStatus.PENDING,
        )
        assertEquals("inv-1", invitation.id)
        assertEquals("Asado", invitation.eventName)
        assertEquals("", invitation.invitedByName) // default
        assertNull(invitation.invitedByPhotoUrl) // default
    }

    // ── Task 5.3: acceptInvitation assigns READER role ───────────────────────

    @Test
    fun `acceptInvitation new participant has READER role not CONTRIBUTOR`() {
        val participant = com.cuentamorosos.data.repository.createAcceptanceParticipant("uid-1")
        assertEquals("READER", participant["role"])
        assertEquals("uid-1", participant["profileId"])
    }

    // ── Task 5.4: sendInvitation permission guard ────────────────────────────

    @Test
    fun `sendInvitation guard — OWNER can manage participants`() {
        assertTrue(com.cuentamorosos.data.repository.canSendInvitation(EventRole.OWNER))
    }

    @Test
    fun `sendInvitation guard — CONTRIBUTOR cannot manage participants`() {
        assertFalse(com.cuentamorosos.data.repository.canSendInvitation(EventRole.CONTRIBUTOR))
    }

    @Test
    fun `sendInvitation guard — READER blocked`() {
        assertFalse(com.cuentamorosos.data.repository.canSendInvitation(EventRole.READER))
    }
}
