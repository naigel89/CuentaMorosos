package com.cuentamorosos.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [ProfileVisibilityResolver] — rules VIS-001..004.
 *
 * These rules used to live inline in a `derivedStateOf` inside CuentaMorososApp.kt,
 * where they could only ever run *after* the whole profiles collection had already
 * been downloaded. Moving them here lets the repository apply them to the query
 * itself, which is what removes the quadratic read growth.
 */
class ProfileVisibilityResolverTest {

    private val uid = "user-me"

    // ── Helper factories ──────────────────────────────────────────────────

    private fun profile(
        id: String,
        isGhost: Boolean = false,
        ownerId: String = "",
    ) = ProfileItem(id = id, name = "Profile $id", isGhost = isGhost, ownerId = ownerId)

    private fun event(
        ownerId: String,
        participantIds: List<String> = emptyList(),
        legacyMemberIds: List<String> = emptyList(),
    ) = EventItem(
        name = "Event",
        dateMillis = 0L,
        ownerId = ownerId,
        memberIds = legacyMemberIds,
        participants = participantIds.map { EventParticipant(profileId = it) },
    )

    // ── VIS-001: own real profile ─────────────────────────────────────────

    @Test
    fun `VIS-001 own profile is visible with no events at all`() {
        val own = profile(uid)
        assertTrue(ProfileVisibilityResolver.isVisible(own, uid, emptyList()))
    }

    @Test
    fun `VIS-001 own id is always part of the addressable id set`() {
        assertTrue(uid in ProfileVisibilityResolver.visibleProfileIds(uid, emptyList()))
    }

    // ── VIS-002: own ghosts ───────────────────────────────────────────────

    @Test
    fun `VIS-002 own ghost is visible even when it takes part in no event`() {
        val ghost = profile("ghost-1", isGhost = true, ownerId = uid)
        assertTrue(ProfileVisibilityResolver.isVisible(ghost, uid, emptyList()))
    }

    @Test
    fun `VIS-002 own ghosts are not id-addressable so they stay out of the id set`() {
        // Ghost ids are random UUIDs that appear nowhere in the events, so the
        // repository cannot fetch them by id — it resolves them with an
        // `ownerId == uid` query instead. Leaking them into this set would make
        // the caller believe an `inArray` chunk is enough.
        val ids = ProfileVisibilityResolver.visibleProfileIds(uid, emptyList())
        assertEquals(setOf(uid), ids)
    }

    // ── VIS-003: co-participants ──────────────────────────────────────────

    @Test
    fun `VIS-003 co-participant of a shared event is visible`() {
        val friend = profile("user-friend")
        val events = listOf(event(ownerId = uid, participantIds = listOf(uid, "user-friend")))
        assertTrue(ProfileVisibilityResolver.isVisible(friend, uid, events))
    }

    @Test
    fun `VIS-003 owner of a shared event is visible even when not listed as participant`() {
        val host = profile("user-host")
        val events = listOf(event(ownerId = "user-host", participantIds = listOf(uid)))
        assertTrue(ProfileVisibilityResolver.isVisible(host, uid, events))
    }

    @Test
    fun `VIS-003 falls back to legacy memberIds when participants is empty`() {
        val friend = profile("user-legacy")
        val events = listOf(event(ownerId = uid, legacyMemberIds = listOf(uid, "user-legacy")))
        assertTrue(ProfileVisibilityResolver.isVisible(friend, uid, events))
    }

    @Test
    fun `VIS-003 ids from every event are collected and deduplicated`() {
        val events = listOf(
            event(ownerId = uid, participantIds = listOf(uid, "a", "b")),
            event(ownerId = "c", participantIds = listOf(uid, "a")),
        )
        assertEquals(
            setOf(uid, "a", "b", "c"),
            ProfileVisibilityResolver.visibleProfileIds(uid, events),
        )
    }

    // ── VIS-004: everything else hidden ───────────────────────────────────

    @Test
    fun `VIS-004 stranger sharing no event is hidden`() {
        val stranger = profile("user-stranger")
        val events = listOf(event(ownerId = uid, participantIds = listOf(uid)))
        assertFalse(ProfileVisibilityResolver.isVisible(stranger, uid, events))
    }

    @Test
    fun `VIS-004 someone else's ghost is hidden`() {
        val theirGhost = profile("ghost-2", isGhost = true, ownerId = "user-other")
        assertFalse(ProfileVisibilityResolver.isVisible(theirGhost, uid, emptyList()))
    }

    // ── Edge cases ────────────────────────────────────────────────────────

    @Test
    fun `blank uid never matches own profile or own ghosts`() {
        // ProfileItem.ownerId defaults to "", so a blank uid used to make every
        // ownerless ghost look like one of ours — a leak while signed out.
        val ownerlessGhost = profile("ghost-3", isGhost = true, ownerId = "")
        val blankIdProfile = profile("")
        assertFalse(ProfileVisibilityResolver.isVisible(ownerlessGhost, "", emptyList()))
        assertFalse(ProfileVisibilityResolver.isVisible(blankIdProfile, "", emptyList()))
    }

    @Test
    fun `blank uid yields an empty id set`() {
        val events = listOf(event(ownerId = "someone", participantIds = listOf("a")))
        assertEquals(emptySet<String>(), ProfileVisibilityResolver.visibleProfileIds("", events))
    }

    @Test
    fun `event with neither participants nor memberIds contributes only its owner`() {
        val events = listOf(event(ownerId = "user-host"))
        assertEquals(
            setOf(uid, "user-host"),
            ProfileVisibilityResolver.visibleProfileIds(uid, events),
        )
    }

    // ── filterVisible: the UI entry point ─────────────────────────────────

    @Test
    fun `filterVisible keeps own profile own ghosts and co-participants only`() {
        val events = listOf(event(ownerId = uid, participantIds = listOf(uid, "user-friend")))
        val all = listOf(
            profile(uid),
            profile("ghost-1", isGhost = true, ownerId = uid),
            profile("user-friend"),
            profile("user-stranger"),
            profile("ghost-2", isGhost = true, ownerId = "user-other"),
        )

        val visible = ProfileVisibilityResolver.filterVisible(all, uid, events).map { it.id }

        assertEquals(listOf(uid, "ghost-1", "user-friend"), visible)
    }

    @Test
    fun `filterVisible preserves the order of the incoming list`() {
        val events = listOf(event(ownerId = uid, participantIds = listOf(uid, "a", "b")))
        val all = listOf(profile("b"), profile(uid), profile("a"))

        assertEquals(
            listOf("b", uid, "a"),
            ProfileVisibilityResolver.filterVisible(all, uid, events).map { it.id },
        )
    }

    @Test
    fun `filterVisible on an empty list returns an empty list`() {
        assertEquals(emptyList<ProfileItem>(), ProfileVisibilityResolver.filterVisible(emptyList(), uid, emptyList()))
    }
}
