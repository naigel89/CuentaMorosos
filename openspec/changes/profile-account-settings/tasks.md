# Tasks: Profile & Account Settings

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~800 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 (Foundation) → PR 2 (Repository) → PR 3 (UI) → PR 4 (Tests) |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Model + DB + build deps + local store | PR 1 | base=main; ~130 lines |
| 2 | Repository interface + implementations | PR 2 | base=PR 1; ~180 lines |
| 3 | ProfileAvatar, ProfileCard, SettingsScreen, CuentaMorososApp wiring, AppViewModelFactory | PR 3 | base=PR 2; ~120 lines |
| 4 | AccountViewModel + AccountScreen (3 sub-screens, AnimatedContent, photo upload) | PR 4 | base=PR 3; ~320 lines |
| 5 | All tests | PR 5 | base=PR 4; ~120 lines |

---

## Phase 1: Foundation (Model + DB + Build)

- [x] 1.1 Extend `ProfileItem` in `Models.kt`: add `photoUrl`, `username`, `displayName`, `customNames` (all nullable, default null) + `displayNameFor(viewerId)` resolution method
- [x] 1.2 Create `shared/.../db/2.sqm` with 4 `ALTER TABLE ADD COLUMN` statements for new CachedProfile columns
- [x] 1.3 Update `CachedProfile.sq` upsert to 12 params (id, name, icon, email, isGhost, totalPendingEuros, updatedAt, ownerId, photo_url, username, display_name, custom_names)
- [x] 1.4 Update `CuentaMorososLocalStore.kt`: `loadProfiles()` reads 4 new fields via `optString()`/`optJSONObject()`, `saveProfiles()` writes them
- [x] 1.5 Update `FirebaseUserSyncManager.kt`: `ensureOwnProfile()` writes `photoUrl=""`, `displayName=FirebaseAuth.displayName`, `username=""` via merge
- [x] 1.6 Add `firebase-storage-ktx` to `app/build.gradle.kts`; add Coil deps (`io.coil-kt.coil3:coil-compose`, `coil-network`) to `shared/build.gradle.kts`

## Phase 2: Repository Layer

- [x] 2.1 Add 6 methods to `ProfileRepository` interface: `updateProfilePhoto`, `updateUsername`, `updateDisplayName`, `setCustomName`, `isUsernameAvailable`, `updatePassword`
- [x] 2.2 Update `FirestoreProfileRepository.toMap()` — add `photoUrl`, `username`, `displayName`; `toProfileItem()` — read new fields as nullable
- [x] 2.3 Add `FirestoreProfileRepository` customNames subcollection: `profiles/{id}/customNames/{viewerId}` read/write
- [x] 2.4 Update `FirestoreProfileRepository.toProfileItem()` — ghost profiles force null for photoUrl/username/displayName
- [x] 2.5 Update `OfflineFirstProfileRepository.upsertProfiles()` — pass 12 params, `toProfileItem()` maps new columns
- [x] 2.6 Implement 6 new methods in `OfflineFirstProfileRepository`: local cache first, remote delegate, pending queue on failure (password is remote-only)

## Phase 3: UI Components + Wiring

- [x] 3.1 Update `ProfileAvatar.kt`: add `photoUrl` param with "📷" placeholder; future Coil integration via Android-specific composable
- [x] 3.2 Update `ProfileCard.kt`: title resolves `displayNameFor(currentUid)` = `customName > displayName > name`; add optional `@username` secondary label; pass `photoUrl` to ProfileAvatar
- [x] 3.3 Update `SettingsScreen.kt`: add `currentProfile` + `onOpenAccountSettings` params; "Mi perfil" section with avatar, name, and tap handler
- [x] 3.4 Create `AccountViewModel.kt`: `subScreenIndex`, `selectedPhotoUri`, `displayNameText`, `usernameText`, `usernameAvailability`, `currentPassword`, `newPassword`, `passwordState`, `state`, `currentProfile` StateFlows; debounced username validation (500ms); `saveDisplayName()`, `saveUsername()`, `changePassword()` with repository calls
- [x] 3.5 Create `AccountScreen.kt`: 3 sub-screens via `AnimatedContent` (NamePhotoScreen, UsernameScreen, SecurityScreen); AccountMainMenu with avatar, name, navigation rows
- [x] 3.6 Wire `showAccountScreen` in `CuentaMorososApp.kt`: add mutable state + `currentProfile` derivation + `AccountViewModel` + overlay Box (same pattern as CalendarScreen)
- [x] 3.7 Register `AccountViewModel` in `AppViewModelFactory.kt`

## Phase 4: Testing

- [x] 4.1 Write unit tests: ProfileItem backward compat (7-field JSON → all new fields null) + `displayNameFor` resolution (3 cases)
- [x] 4.2 Write unit test: OfflineFirstProfileRepository upsert calls with 12 params matching migration order
- [x] 4.3 Write unit test: AccountViewModel username debounce flow (Turbine, 500ms debounce, filter < 3 chars)
- [x] 4.4 Write unit test: CuentaMorososLocalStore roundtrip — save profile with new fields → load → assert all fields match
- [x] 4.5 Write unit test: AccountViewModel password reauth error — mock `requiresRecentLogin` → verify UI state transitions
