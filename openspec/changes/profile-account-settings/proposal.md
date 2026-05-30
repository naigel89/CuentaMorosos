# Proposal: Profile & Account Settings

## Intent

Users need a proper account/profile settings system to manage their identity: display name, avatar photo, unique @username, custom names for contacts, and security (password change). Currently `ProfileItem` only has `name` + `icon` (emoji) and `SettingsScreen` has no profile section. This adds full account management with Firebase Storage for avatars, unique username validation, and per-contact custom names.

## Scope

### In Scope
- `ProfileItem` model migration: add `photoUrl`, `username`, `displayName`, `customNames`
- SQLDelight schema migration: new columns `photo_url`, `username`, `display_name`, `custom_names` (JSON)
- Firestore schema: new fields + `customNames` subcollection `profiles/{id}/customNames/{viewerId}`
- Firebase Storage: avatar uploads to `avatars/{uid}/profile.jpg` (256x256)
- `ProfileRepository`: 6 new methods (photo, username, displayName, customName, password, availability)
- `AccountScreen` with avatar picker, display name, username validation, security section
- `ProfileCard`/`ProfileAvatar`: Coil `AsyncImage` for `photoUrl`, fallback to emoji
- `ProfileDetailDialog`: "Cómo le llamo yo" custom name option
- Sync: `updatedAt` propagates photo/name changes to contacts

### Out of Scope
- Email change flow (requires reauth rework) — deferred
- Delete account — deferred (needs data cascade cleanup)
- iOS photo picker (future iOS target)
- Username recovery or reset flow
- Multi-image avatar gallery

## Capabilities

### New Capabilities
- `profile-photo-upload`: Firebase Storage avatar upload with 256x256 compression
- `profile-username`: Unique @username with async debounced validation
- `profile-custom-names`: Per-contact display names stored in Firestore subcollection
- `account-settings-ui`: AccountScreen with 3 sub-screens (name+photo, username, security)
- `profile-security`: Password change with Firebase reauthentication

### Modified Capabilities
- `profile-item-model`: Extended with `photoUrl`, `username`, `displayName`, `customNames`
- `profile-repository`: New methods for photo, username, displayName, customName, password
- `profile-card-display`: Coil AsyncImage for photoUrl, display resolution (customName > displayName > name)

## Approach

1. **Model first** — Extend `ProfileItem` with new nullable fields. Backward-compatible: all new fields default to null/empty.
2. **Schema migration** — SQLDelight: `ALTER TABLE CachedProfile ADD COLUMN` for each new field. Firestore: add fields with merge-safe writes. `CuentaMorososLocalStore` JSON: extend serialization.
3. **Firebase Storage** — New storage bucket access via `FirebaseStorage.getInstance()`. Compress bitmap to 256x256 before upload. Store URL in Firestore profile doc.
4. **Repository layer** — Add methods to `ProfileRepository` interface and `ProfileRepositoryImpl`. Each method is a suspend function returning `Result<T>`.
5. **AccountScreen** — New composable navigated from `SettingsScreen` via state-based overlay (same pattern as EventDetailScreen). Internal sub-navigation with `AnimatedContent`.
6. **Username validation** — Firestore query for uniqueness, debounced 500ms via `Flow.debounce` in ViewModel.
7. **Custom names** — Store in Firestore subcollection `profiles/{viewedProfileId}/customNames/{viewerId}`. Default merge with `displayName` then `name`.
8. **Sync** — `updatedAt` timestamp triggers Firestore snapshot listeners to propagate changes.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `shared/.../model/Models.kt` | Modified | `ProfileItem` — 4 new fields + display resolution logic |
| `shared/.../sqldelight/.../CachedProfile.sq` | Modified | New columns + migration |
| `shared/.../data/repository/ProfileRepository.kt` | Modified | 6 new methods |
| `app/.../data/CuentaMorososLocalStore.kt` | Modified | JSON serialization for new fields |
| `app/.../data/FirebaseUserSyncManager.kt` | Modified | `ensureOwnProfile` includes photo/name |
| `shared/.../ui/ProfileAvatar.kt` | Modified | Coil `AsyncImage` for `photoUrl` |
| `shared/.../ui/ProfileCard.kt` | Modified | Display resolution logic |
| `shared/.../ui/AccountScreen.kt` | New | Full account settings screen |
| `shared/.../ui/AccountViewModel.kt` | New | State management for account flows |
| `shared/.../ui/SettingsScreen.kt` | Modified | "Mi perfil" navigation entry |
| `shared/.../ui/CuentaMorososApp.kt` | Modified | Wire AccountScreen overlay |
| `app/.../di/AppViewModelFactory.kt` | Modified | Register new ViewModel |
| `app/.../di/RepositoryProvider.kt` | Modified | Register Firebase Storage |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Coil/AsyncImage not available in Compose Multiplatform | Medium | Guard with `expect`/`actual`; Android uses Coil, KMP fallback to emoji/initial |
| SQLDelight ALTER TABLE not supported in-migration | Low | Use `CREATE TABLE IF NOT EXISTS` with new schema, migrate data with INSERT OR REPLACE |
| Username uniqueness race condition | Medium | Firestore query + security rules; handle conflict with retry or error feedback |
| Ghost profiles with photoUrl/username | Low | Always null; emoji stays as avatar — enforced in model and repository |
| Firebase reauthentication for password change | Medium | Existing user must reauth with credentials first; handle `requiresRecentLogin` error |

## Rollback Plan

1. `git revert` the change — all new columns are additive (ALTER TABLE), existing data unaffected
2. Old Firestore docs without new fields remain valid (merge-safe reads)
3. If Storage costs spike, disable avatar upload at the repository level
4. No data loss: old `ProfileItem` deserialization ignores unknown JSON fields

## Dependencies

- Coil (image loading library) must be added to Gradle dependencies (`io.coil-kt.coil3:coil-compose` + `coil-network`)
- Firebase Storage must be enabled in Firebase Console
- Firebase Storage security rules for `avatars/{uid}/{filename}` (authenticated, own UID)

## Success Criteria

- [ ] `ProfileItem` parses correctly with and without new fields (backward compat)
- [ ] Avatar photo uploads to Firebase Storage, URL persisted in Firestore
- [ ] `ProfileAvatar` shows `AsyncImage` when `photoUrl` is set, emoji otherwise
- [ ] Username validation: debounced 500ms, shows available/unavailable
- [ ] Custom name stored per-contact, displayed in profile cards
- [ ] Ghost profiles always show emoji, never photo/username
- [ ] Password change flow handles reauthentication
- [ ] `updatedAt` triggers sync propagation
