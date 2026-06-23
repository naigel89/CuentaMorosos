# Exploration: invite-by-username-search

## Summary

The current invitation flow is **email-only**: users must know the invitee's exact email. `ProfileItem` already has a `username` field (`String?`), persisted in both Firestore and local SQLite (CachedProfile). However, there is **no search/filter method** in any repository to look up profiles by username prefix. The recommended approach is **Firestore remote prefix search** using `whereGreaterThanOrEqualTo("username", prefix)` + `whereLessThanOrEqualTo("username", prefix + "\uf8ff")` — searches ALL profiles globally, not just the local cache. This is the correct approach because the user must be able to find ANY user by @username, including people they've never shared an event with. A Firestore composite index on `username ASC` is required. No `EventInvitation` model changes needed — the selected profile's `linkedEmail` is used to send the invitation as before. After invitation, the profile is synced to the local cache via existing `observeProfiles()` flow.

---

## Current Architecture

### A. Username Data Layer (`username` in ProfileItem)

| Storage | Field | Nullability | Default | Source |
|---------|-------|-------------|---------|--------|
| `ProfileItem` (model) | `username` | `String?` | `null` | `Models.kt:137` |
| Firestore `profiles` collection | `"username"` | nullable | `null` | `FirestoreProfileRepository.kt:169` (toMap), `:184` (from doc) |
| SQLite `CachedProfile` | `username` | `TEXT NOT NULL` | `''` (empty string) | `CachedProfile.sq:10` |
| `OfflineFirstProfileRepository` mapping | `username` | nullable (null if ghost) | `null` / `""` | `OfflineFirstProfileRepository.kt:469` |

Key observations:
- **No existing search/filter method** for username anywhere in `ProfileRepository` interface or any implementation
- `isUsernameAvailable()` (`FirestoreProfileRepository.kt:99-108`) uses `where { "username" equalTo username }` — an **exact equality** query, not prefix. This already requires a Firestore index on `username`, so a composite index for prefix search is a minor extension.
- `FirestoreProfileRepository.observeProfiles()` (line 23-26) is **intentionally unfiltered by ownerId** — returns ALL profiles the security rules allow. The comment explicitly states: *"users need to see other participants' profiles in shared events."*

### B. Profile Sync — local cache coverage

`OfflineFirstProfileRepository` syncs all profiles from Firestore into local SQLite:
- `startSync()` (line 59-70) launches a coroutine that subscribes to `remoteRepository.observeProfiles()`
- `upsertProfiles()` (line 119-138) bulk-inserts all remote profiles into `CachedProfile` table
- `observeProfiles()` (line 72-79) returns a `Flow<List<ProfileItem>>` from SQLite via `asFlow().mapToList()`

**CachedProfile columns** (`CachedProfile.sq`): `id`, `name`, `email`, `isGhost`, `totalPendingEuros`, `updatedAt`, `ownerId`, `photo_url`, `username`, `display_name`, `custom_names`.

**Coverage**: `observeProfiles()` returns ALL profiles from Firestore (no ownerId filter). After syncing, any profile the user has access to is in the local cache. For the search feature, we query Firestore directly to find profiles NOT yet in cache. Once an invitation is sent and accepted, the profile syncs automatically via `observeProfiles()`.

**Missing**: No `searchByUsername` method exists. No Firestore prefix query exists.

### C. Invitation Flow — full trace

```
EventDetailScreen (line 376-384)
  └─ showInviteMemberDialog = true
       └─ InviteMemberDialog (line 591-639)
            └─ onInvite(email: String)
                 └─ EventDetailScreen.onInviteMember(email) — line 381
                      └─ CuentaMorososApp.onInviteMember = { email -> ... } — line 494-509
                           └─ EventInvitation(
                                invitedByUid  = currentUserUid,
                                invitedByEmail= currentProfile?.linkedEmail ?: "",
                                invitedEmail  = email,          // ← only email
                                invitedByName = currentProfile?.name ?: ...,
                                invitedByPhotoUrl = currentProfile?.photoUrl,
                              )
                           └─ invitationsViewModel.sendInvitation(invitation) — line 49-52
                                └─ InvitationRepository.sendInvitation(invitation)
                                     └─ FirestoreInvitationRepository.sendInvitation() — line 38-65
                                          └─ Invitations/invitation.id → toFirestoreMap()
```

**`EventInvitation` model** (`Models.kt:213-225`):
- `invitedByUid: String` — inviter's UID
- `invitedByEmail: String` — inviter's email
- `invitedEmail: String` — invitee's email **← email-only, no username field**
- `invitedByName: String` — inviter's display name
- `invitedByPhotoUrl: String?` — inviter's photo
- NO `invitedByUsername` field exists

**Resolution**: `sendInvitation()` REQUIRES `invitedEmail` (lowercased). To invite by username, we must resolve the selected profile's `linkedEmail` first. No model changes to `EventInvitation` needed.

### D. UI — InviteMemberDialog

Location: `EventDetailScreen.kt` lines 591-639 (`private fun InviteMemberDialog`)

Current implementation:
- `AlertDialog` with a `SingleLine OutlinedTextField` label "Email"
- Validates with `isValidEmail()` on confirm
- Calls `onInvite(email)` and dismisses
- No dropdown, no username input, no autocomplete

Triggered from `SettlementPanel` "Invitar" button (`SettlementPanel.kt:432-439`), wired through `EventDetailScreen` (lines 89, 182, 243, 376-384).

### E. Testing Patterns

| Aspect | Detail |
|--------|--------|
| Framework | `kotlin.test` (commonTest), JUnit 4 (app module) |
| Coroutines | `kotlinx.coroutines.test.runTest`, `UnconfinedTestDispatcher`, `advanceUntilIdle` |
| Fakes | Inline `FakeXxxRepository` classes using `MutableStateFlow` |
| Turbine | **Not used** — tests use `backgroundScope.launch { flow.collect {} }` pattern |
| Location | `shared/src/commonTest/kotlin/com/cuentamorosos/` |
| Existing VM tests | `InvitationsViewModelNotificationTest.kt`, `AccountViewModelTest.kt`, `DashboardViewModelNotificationTest.kt` |

### F. Debounce Pattern

`AccountViewModel` (`AccountViewModel.kt:109-129`) implements username validation debounce:

```kotlin
_usernameText
    .debounce(500)           // 500ms debounce
    .filter { it.length >= 3 }
    .distinctUntilChanged()
    .map { username -> ... } // remote check
    .collect { ... }
```

This pattern is directly reusable for remote username search. For Firestore queries (100-300ms latency), 500ms debounce prevents excessive reads.

---

## Gaps Identified

1. **No profile search method** — `ProfileRepository` interface (and all implementations) lacks `searchByUsername(prefix: String): List<ProfileItem>`. Severity: **blocking**. Files: `ProfileRepository.kt`, `FirestoreProfileRepository.kt`, `OfflineFirstProfileRepository.kt`.

2. **No Firestore prefix query for profiles** — No query using `whereGreaterThanOrEqualTo` / `whereLessThanOrEqualTo` on `username`. Severity: **blocking**. File: `FirestoreProfileRepository.kt`.

3. **No Firestore composite index on username** — Prefix queries require a composite index on `username ASC`. Severity: **blocking**. Must be created in Firebase Console or via `firestore.indexes.json`.

4. **InviteMemberDialog only accepts email** — No username input, no dropdown, no profile selection UI. Severity: **blocking**. File: `EventDetailScreen.kt:591-639`.

5. **No `linkedEmail` resolution** — If a profile has `linkedEmail = null`, the invitation cannot be sent. Severity: **edge-case risk**. Requires validation in the dialog.

6. **No way to distinguish "own profile" from search results** — The inviter shouldn't invite themselves. Severity: **minor**. Filter by `currentUserUid` in search results.

---

## Approaches

### Approach 1: Firestore Remote Prefix Search (RECOMMENDED)

Use Firestore composite query `whereGreaterThanOrEqualTo("username", prefix)` + `whereLessThanOrEqualTo("username", prefix + "\uf8ff")` for prefix matching across ALL profiles. Requires a composite index on `username ASC`.

| Pros | Cons |
|------|------|
| Searches ALL profiles globally — finds anyone by @username | Requires network — needs online handling |
| No SQL schema changes | Needs composite index on `username` in Firestore |
| Correct for the use case: find ANY user, not just cached ones | ~100-300ms latency (vs microsecond local) |
| Profile auto-syncs to local cache via existing `observeProfiles()` | |
| Reuses existing debounce pattern (500ms, like AccountViewModel) | |

### Approach 2: Local SQLite Prefix Search (REJECTED)

Add a `WHERE username LIKE ?` query to `CachedProfile.sq` and expose through `OfflineFirstProfileRepository`.

| Pros | Cons |
|------|------|
| Works offline, instant results | Only searches cached profiles (shared event participants) — can't find new people |
| No Firestore index changes | Defeats the purpose: invite people you DON'T already share events with |
| No network round-trips | Users with empty cache see no results |

### Approach 3: Hybrid (Local + Remote Fallback) — OVER-ENGINEERED

| Pros | Cons |
|------|------|
| Best theoretical coverage | Double maintenance burden, complex error handling, premature optimization |

---

## Recommendation

**Approach 1: Firestore Remote Prefix Search.** The core use case is finding ANY user by @username — including people the inviter hasn't shared events with yet. Local SQLite only covers cached profiles (people you already know), which defeats the purpose. Firestore's `whereGreaterThanOrEqualTo`/`whereLessThanOrEqualTo` pattern provides server-side prefix matching across the entire `profiles` collection. The 500ms debounce from `AccountViewModel` is directly reusable.

---

## Risks

1. **Profile without `linkedEmail`**: If a profile has no `linkedEmail` (ghost profile, or email not yet linked), the invitation cannot be sent. The dialog MUST validate this before enabling the invite button. Ghost profiles are already excluded from search (username forced null for ghost in Firestore mapping at `FirestoreProfileRepository.kt:184-185`).

2. **Empty username profiles**: Profiles without a set username won't appear in search results. This is correct behavior — the feature requires usernames.

3. **Network dependency**: Search requires connectivity. The dialog should show "Sin conexión" when offline. Debounce at 500ms prevents excessive reads on slow networks.

4. **Self-invitation**: Must filter out `profile.id == currentUserUid` from search results.

5. **Firestore composite index**: Required on `profiles` collection, field `username ASC`. Must be created before deployment. `isUsernameAvailable()` already uses an equality index on `username` — the composite index is a separate, additive resource.

6. **Firestore read costs**: Each keystroke after debounce triggers a query. With 500ms debounce and 2+ char minimum, typical invitation flow generates 1-5 queries. Acceptable.

---

## Testing Strategy

- **Unit tests for Firestore query**: Test `searchByUsername()` in `FirestoreProfileRepositoryTest.kt` using Firestore emulator or mock. Verify prefix matching, empty results, ghost exclusion.
- **Repository stub**: `OfflineFirstProfileRepository.searchByUsername()` throws `UnsupportedOperationException` (remote-only feature).
- **Dialog tests**: Compose test with mock repository. Verify debounce behavior, self-exclusion, no-linkedEmail exclusion, empty/no-match states, offline message.
- **Test frameworks**: `kotlin.test` + `kotlinx.coroutines.test.runTest` (no turbine — consistent with existing tests).

---

## Files Touched (forecast)

| File | Change Type | Est. Lines |
|------|-------------|------------|
| `shared/.../repository/ProfileRepository.kt` | **ADD** `searchByUsername()` interface method | +6 |
| `shared/.../repository/FirestoreProfileRepository.kt` | **ADD** `searchByUsername()` implementation (Firestore prefix query) | +25 |
| `shared/.../repository/OfflineFirstProfileRepository.kt` | **ADD** `searchByUsername()` stub (throw Unsupported) | +5 |
| `shared/.../ui/EventDetailScreen.kt` | **MODIFY** `InviteMemberDialog` — username search + dropdown + loading/offline states | +70 |
| `firestore.indexes.json` or Firebase Console | **CREATE** composite index on `profiles.username ASC` | +5 |
| `shared/.../commonTest/.../repository/FirestoreProfileRepositoryTest.kt` | **ADD** tests for remote search | +40 |
| `shared/.../commonTest/.../ui/InviteMemberDialogTest.kt` | **ADD** dialog integration tests | +50 |
