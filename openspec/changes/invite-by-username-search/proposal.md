# Proposal: invite-by-username-search

## Intent

Add real-time username search to the invitation dialog via Firestore remote prefix query. Currently invites require knowing the invitee's exact email — users can now type a username prefix and select from matching profiles across the entire `profiles` collection.

## Scope

### In Scope
- Firestore prefix search: `where >= "prefix" AND <= "prefix\uf8ff"` on `profiles.username`
- `searchByUsername(prefix)` suspend method in `ProfileRepository` interface + `FirestoreProfileRepository` impl
- `OfflineFirstProfileRepository` stub (throws `UnsupportedOperationException`)
- Redesigned `InviteMemberDialog`: debounced text input (500ms) + dropdown with loading/offline/empty states
- Resolve selected profile's `linkedEmail` → send through existing `sendInvitation()` (no model changes)
- Exclude own profile, ghost profiles, and profiles without `linkedEmail` from results
- Firestore composite index on `profiles.username ASC`
- Unit tests with Firestore emulator mock + Compose dialog tests

### Out of Scope
- Local SQLite search (only searches cached profiles — defeats purpose of finding new people)
- Hybrid local+remote fallback
- Mandatory username enforcement / changes to signup

## Capabilities

### New Capabilities
- `profile-username-search`: Search ALL profiles in Firestore by username prefix for autocomplete in invitation dialogs

### Modified Capabilities
- None (additive feature; existing specs unchanged)

## Approach

**Firestore remote prefix search** — chosen over local SQLite because the core use case is finding ANY user by @username, including people the inviter has never shared an event with. Local cache only covers known contacts.

Key design points:
- Firestore query: `whereGreaterThanOrEqualTo("username", prefix)` + `whereLessThanOrEqualTo("username", prefix + "\uf8ff")`
- Debounce 500ms, `filter { length >= 2 }`, `distinctUntilChanged` — reuse `AccountViewModel` pattern
- Composite index on `profiles.username ASC` required (created via Firebase Console or `firestore.indexes.json`)
- `OfflineFirstProfileRepository.searchByUsername()` → throws `UnsupportedOperationException` (remote-only)
- TDD: mock Firestore query in tests, Compose tests for dialog behavior

## Affected Areas

| File | Change |
|------|--------|
| `ProfileRepository.kt` | New interface method `searchByUsername()` |
| `FirestoreProfileRepository.kt` | Real implementation with Firestore prefix query |
| `OfflineFirstProfileRepository.kt` | Stub (throws) |
| `EventDetailScreen.kt` | Redesigned `InviteMemberDialog` with Firestore-backed search |
| `firestore.indexes.json` | New composite index on `profiles.username ASC` |
| `commonTest/.../repository/` | `searchByUsername()` tests with mock Firestore |
| `commonTest/.../ui/` | Dialog integration tests |

## Risks

| Risk | Mitigation |
|------|------------|
| Profile without `linkedEmail` (ghost) | Already excluded (username=null for ghosts); validate before enabling invite |
| Self-invitation | Filter `profile.id != currentUserUid` |
| Network failure | Dialog shows "Sin conexión" message when offline |
| Firestore composite index missing | Document in deployment checklist; fails gracefully (Firestore returns error) |
| Firestore read costs | 500ms debounce + 2-char minimum limits queries per invitation to 1-5 |

## Rollback Plan

Entirely additive. Revert: restore email-only `InviteMemberDialog`, remove new repository method. No data migration — only a new Firestore query method, no schema changes.

## Dependencies

- Existing `username` field in `ProfileItem` (profile-username spec)
- `isUsernameAvailable()` already uses a Firestore equality index on `username` — the composite index for prefix search is additive
- Existing `sendInvitation()` flow (receives resolved email, unchanged)
- Existing `observeProfiles()` syncs all profiles to local cache after invitation

## Success Criteria

- [ ] Typing 2+ characters queries Firestore and shows matching profiles in ≤500ms
- [ ] Tapping profile resolves `linkedEmail` and sends invitation via existing flow
- [ ] Self and ghost profiles excluded from results
- [ ] No-match, empty, and offline states handled gracefully
- [ ] Firestore composite index on `profiles.username ASC` created
- [ ] All new code has passing unit tests (`./gradlew test`)
