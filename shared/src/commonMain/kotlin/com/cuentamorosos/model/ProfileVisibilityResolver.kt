package com.cuentamorosos.model

/**
 * Decides which profiles a user is allowed to see — rules VIS-001..004.
 *
 * - VIS-001: your own real profile is always visible.
 * - VIS-002: ghost profiles you created are always visible.
 * - VIS-003: people you share an event with are visible.
 * - VIS-004: everything else is hidden.
 *
 * These rules used to be inlined in a `derivedStateOf` inside `CuentaMorososApp.kt`,
 * which meant they could only run after the entire `profiles` collection had already
 * been downloaded — one full-collection read per session, so global reads grew with
 * the square of the user count. Keeping the rule here lets
 * `FirestoreProfileRepository` push it into the query instead.
 */
object ProfileVisibilityResolver {

    /**
     * The profile ids [uid] can reach *by id*: itself plus everyone taking part in
     * [events] (owners included).
     *
     * Own ghosts are deliberately **not** here. Ghost ids are random UUIDs that
     * appear in no event the ghost hasn't joined, so no id-based query can find
     * them; the repository resolves those separately with an `ownerId == uid`
     * query. Adding them here would let a caller believe an `inArray` chunk over
     * this set is enough to satisfy VIS-002.
     *
     * Returns an empty set for a blank [uid] — signed out means nothing is visible.
     */
    fun visibleProfileIds(uid: String, events: List<EventItem>): Set<String> {
        if (uid.isBlank()) return emptySet()
        val ids = mutableSetOf(uid)
        events.forEach { event ->
            if (event.ownerId.isNotBlank()) ids.add(event.ownerId)
            // effectiveMemberIds already handles the participants → memberIds fallback.
            event.effectiveMemberIds.forEach { memberId ->
                if (memberId.isNotBlank()) ids.add(memberId)
            }
        }
        return ids
    }

    /**
     * Whether [profile] is visible to [uid] given their [events].
     *
     * Recomputes the id set on every call, so prefer [filterVisible] for lists.
     */
    fun isVisible(profile: ProfileItem, uid: String, events: List<EventItem>): Boolean {
        if (uid.isBlank()) return false
        return isVisibleAgainst(profile, uid, visibleProfileIds(uid, events))
    }

    /**
     * Keeps only the profiles visible to [uid], preserving the order of [profiles].
     *
     * This is the UI entry point; it replaces the inline filter that used to live in
     * `CuentaMorososApp.kt`. It stays useful even now that the query is scoped,
     * because the local SQLDelight cache can still hold profiles from an event the
     * user has since left.
     */
    fun filterVisible(
        profiles: List<ProfileItem>,
        uid: String,
        events: List<EventItem>,
    ): List<ProfileItem> {
        if (uid.isBlank()) return emptyList()
        val addressable = visibleProfileIds(uid, events)
        return profiles.filter { isVisibleAgainst(it, uid, addressable) }
    }

    /**
     * VIS-001..004 against an already-computed id set.
     *
     * [uid] must be non-blank: `ProfileItem.ownerId` defaults to `""`, so a blank uid
     * would make every ownerless ghost look like one of ours.
     */
    private fun isVisibleAgainst(
        profile: ProfileItem,
        uid: String,
        addressableIds: Set<String>,
    ): Boolean = when {
        profile.id == uid -> true                              // VIS-001
        profile.isGhost && profile.ownerId == uid -> true       // VIS-002
        else -> profile.id in addressableIds                    // VIS-003 / VIS-004
    }
}
