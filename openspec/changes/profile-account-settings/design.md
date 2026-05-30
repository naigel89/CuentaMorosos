# Design: Profile & Account Settings

**Change**: `profile-account-settings`
**Phase**: sdd-design
**Date**: 2026-05-30

## 1. Technical Approach

Extend `ProfileItem` with 4 nullable fields, add a SQLDelight migration (`2.sqm`) + Firestore schema, add 6 methods to `ProfileRepository`, and introduce a new `AccountViewModel` + `AccountScreen` with 3 sub-screens navigated via `AnimatedContent` overlay from `SettingsScreen`. Photo upload uses Firebase Storage (Android-specific via expect/actual). Username validation uses debounced Firestore queries. Display name resolution: `customName` > `displayName` > `name`.

## 2. Architecture Decisions

| Decision | Option | Tradeoff | Chosen |
|---|---|---|---|
| Image loading | Coil in shared vs expect/actual | Coil is Android-only; expect/actual keeps platform contract clean | **expect/actual**: `expect fun ProfileAvatarImage(...)` in commonMain, `actual` uses Coil `AsyncImage` on Android, emoji fallback on JVM/iOS |
| ViewModel | New `AccountViewModel` vs extend `ProfilesViewModel` | Extending breaks SRP, mixes profile list with account details | **New `AccountViewModel`** — owns sub-screen state, debounced username validation, photo upload state |
| Photo upload location | Repository vs ViewModel | Repository keeps clean arch but needs Android deps; ViewModel can use Android-specific code | **ViewModel** — Android-specific Bitmap compression + Firebase Storage call, repository stores URL only |
| Custom names caching | Local cache vs read-through Firestore | Local cache adds complexity for namespace collisions | **Read-through** — always fetch from Firestore subcollection `profiles/{id}/customNames/{viewerId}`, no SQLDelight cache |
| Username uniqueness | Client-side query vs security rules | Both needed for race safety | **Query first + security rules** — ViewModel queries Firestore with 500ms debounce, Firestore security rules enforce uniqueness on write |
| Navigation overlay | New state in CuentaMorososApp vs nested within SettingsScreen | Nested breaks the AnimatedContent exit transition | **State in CuentaMorososApp** — `showAccountScreen` boolean drives AnimatedContent overlay, identical to EventDetailScreen pattern |

## 3. Data Model Changes

### Before (ProfileItem)
```kotlin
data class ProfileItem(
    val id: String, val name: String, val icon: String,
    val totalPendingEuros: Double, val isGhost: Boolean,
    val linkedEmail: String?, val ownerId: String,
)
```

### After
```kotlin
data class ProfileItem(
    // existing 7 fields unchanged
    val id: String, val name: String, val icon: String,
    val totalPendingEuros: Double, val isGhost: Boolean,
    val linkedEmail: String?, val ownerId: String,
    // new fields — all nullable, default null
    val photoUrl: String? = null,
    val username: String? = null,
    val displayName: String? = null,
    val customNames: Map<String, String>? = null, // viewerId → customName
) {
    /** Display name resolution: customName > displayName > name.
     *  @param viewerId The current user's profile ID for custom name lookup. */
    fun displayNameFor(viewerId: String): String =
        customNames?.get(viewerId) ?: displayName ?: name
}
```

## 4. Database Migration Plan

### SQLDelight Migration (`2.sqm`)
```sql
ALTER TABLE CachedProfile ADD COLUMN photo_url TEXT;
ALTER TABLE CachedProfile ADD COLUMN username TEXT;
ALTER TABLE CachedProfile ADD COLUMN display_name TEXT;
ALTER TABLE CachedProfile ADD COLUMN custom_names TEXT;  -- JSON: {"viewerId":"customName",...}
```

### Updated `CachedProfile.sq` upsert (12 params)
```sql
upsert:
INSERT OR REPLACE INTO CachedProfile
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
-- order: id, name, icon, email, isGhost, totalPendingEuros, updatedAt, ownerId,
--        photo_url, username, display_name, custom_names
```

### Firestore Schema
Profile document adds fields: `photoUrl`, `username`, `displayName`. New subcollection `profiles/{id}/customNames/{viewerId}` with single field `customName`.

## 5. Repository Layer Changes

### ProfileRepository interface — 6 new methods
```kotlin
interface ProfileRepository {
    // existing 4 methods unchanged

    suspend fun updateProfilePhoto(profileId: String, photoUrl: String?): Result<Unit>
    suspend fun updateUsername(profileId: String, username: String): Result<Unit>
    suspend fun updateDisplayName(profileId: String, displayName: String?): Result<Unit>
    suspend fun setCustomName(viewedProfileId: String, viewerId: String, customName: String?): Result<Unit>
    suspend fun isUsernameAvailable(username: String): Result<Boolean>
    suspend fun updatePassword(currentPassword: String, newPassword: String): Result<Unit>
}
```

**OfflineFirstProfileRepository**: Each new method writes local cache immediately, delegates to remote, enqueues pending op on failure. `updatePassword` is remote-only (Firebase Auth), not cached locally. `isUsernameAvailable` is remote-only.

**FirestoreProfileRepository**: `toMap()` adds `photoUrl`, `username`, `displayName`. `toProfileItem()` reads new fields. Custom names read/write subcollection `customNames/{viewerId}`. Password change handled by Firebase Auth directly.

### Data flow for photo upload
```
User picks photo → AccountViewModel.compressAndUpload(bytes)
  → Bitmap.compress 256×256 (Android-specific)
  → FirebaseStorage ref "avatars/{uid}/profile.jpg".putBytes()
  → downloadUrl → updateProfilePhoto(profileId, url) → repository upserts local + Firestore
```

## 6. UI Component Tree

```
CuentaMorososApp
 └── AnimatedContent (selectedEvent / showAccountScreen)
      ├── EventDetailScreen (existing)
      ├── AccountScreen (NEW)
      │    └── AnimatedContent (subScreen index)
      │         ├── NamePhotoScreen (NEW)
      │         │    └── ProfileAvatar(photoUrl) — Coil AsyncImage (Android)
      │         ├── UsernameScreen (NEW) — debounced validation
      │         └── SecurityScreen (NEW) — password change
      └── HorizontalPager (existing)
           └── SettingsScreen (modified) — "Mi perfil" → showAccountScreen=true
```

### Component details

**ProfileAvatar** — Add `photoUrl` param. Android actual uses Coil `AsyncImage` in a `Box` masked to circle. Fallback: same initial/emoji as today. Ghost profiles: always emoji, ignore photoUrl.

**ProfileCard** — Add optional `displayName`, `username`, `customName` params. Title resolves via display resolution. Secondary label shows `@username` when present.

## 7. Navigation Flow

```
SettingsScreen → tap "Mi perfil"
  → CuentaMorososApp sets showAccountScreen = true
  → AnimatedContent slides in AccountScreen
    → AccountScreen has 3 tabs (sub-screens):
      [Foto y nombre] → NamePhotoScreen
      [Nombre de usuario] → UsernameScreen
      [Seguridad] → SecurityScreen
  → AccountScreen back button → showAccountScreen = false
```

Navigation state lives in `AccountViewModel`:
- `subScreenIndex: StateFlow<Int>` — which sub-screen is active
- `onBackRequested: () -> Unit` — callback set by CuentaMorososApp to clear overlay

## 8. Data Flow / State Management

**AccountViewModel** owns:
```kotlin
class AccountViewModel(
    private val profileRepository: ProfileRepository,
    private val currentUserId: String,
) : ViewModel() {
    val subScreenIndex: StateFlow<Int>           // 0=namePhoto, 1=username, 2=security
    val photoState: StateFlow<PhotoUiState>       // idle/loading/url/error
    val usernameState: StateFlow<UsernameUiState>  // idle/checking/available/taken/error
    val passwordState: StateFlow<PasswordUiState>  // idle/loading/requiresReauth/error
    val currentProfile: StateFlow<ProfileItem>     // the user's own profile
}
```

**Username debounce flow**:
```kotlin
searchFlow
  .debounce(500)
  .filter { it.length >= 3 }
  .flatMapLatest { repo.isUsernameAvailable(it) }
  .stateIn(viewModelScope, SharingStarted.WhileSubscribed, ...)
```

**State persistence**: Edits preserved across sub-screen switches — state stored in ViewModel, survives rotation. Only committed on explicit save button.

## 9. Key Implementation Decisions

### 9a. Coil for Android only
`ProfileAvatar` gets a `photoUrl: String?` parameter. Android `actual fun` wraps `AsyncImage(model = photoUrl, contentDescription = ...)`. Common code delegates to an `expect` composable or uses a `@Composable` expect function. Non-Android platforms keep the emoji/initial fallback. This avoids pulling Coil multiplatform artifacts while the project is Android-only in practice.

### 9b. CustomNames read-through pattern
No local caching for `customNames` — the data is viewer-specific and low-volume. AccountViewModel fetches on demand via `profileRepository.setCustomName()` and the subcollection listener. ProfileCard resolves display name via `ProfileItem.displayNameFor(viewerId)` computed at render time.

### 9c. Password change reauthentication
Flow: validate current password via `EmailAuthProvider.getCredential` → `user.reauthenticate(credential)` → `user.updatePassword(newPassword)`. On `FirebaseAuthInvalidUserException` with `ERROR_REQUIRES_RECENT_LOGIN`, show dialog "Por seguridad, necesitás volver a iniciar sesión para cambiar tu contraseña." then sign out.

### 9d. Ghost profile constraint
`isGhost == true` → `photoUrl`, `username`, `displayName` are always null. Enforced at repository layer: `FirestoreProfileRepository.toProfileItem()` ignores photoUrl/username for ghost documents. `OfflineFirstProfileRepository.toProfileItem()` sets them to null when `isGhost == 1`.

## 10. Migration Strategy for Existing Data

1. **SQLDelight**: `2.sqm` runs `ALTER TABLE ... ADD COLUMN` — backward compatible, existing rows get `NULL` for new columns.
2. **Firestore**: Existing profile documents lack new fields → `toProfileItem()` reads them as `null` (merge-safe with `as? String` cast).
3. **CuentaMorososLocalStore**: JSON deserialization uses `optString()` which returns `""` for missing keys → post-process to `null`.
4. **CuentaMorososApp**: `FirebaseUserSyncManager.ensureOwnProfile()` updated to write `photoUrl=""`, `displayName=FirebaseAuth.displayName`, `username=""` on next sync — but only via merge, so existing data isn't overwritten.

## 11. Rollback Plan

1. `git revert` the commit — all changes are additive (SQLDelight ALTER TABLE, Firestore merge writes)
2. Old clients without new fields continue to work: `ProfileItem` defaults handle nulls
3. Firebase Storage files remain orphaned but cost is negligible; cleanup cron can be added later
4. No migration downgrade needed — `ALTER TABLE` is forward-only, but old code ignores unknown columns in `Cursor`

---

## File Changes Summary

| File | Action | Description |
|------|--------|-------------|
| `shared/.../model/Models.kt` | Modify | ProfileItem +4 fields, displayNameFor() |
| `shared/.../db/CachedProfile.sq` | Modify | upsert 12 params, selectAll unchanged |
| `shared/.../db/2.sqm` | Create | ALTER TABLE for 4 new columns |
| `shared/.../data/repository/ProfileRepository.kt` | Modify | +6 methods |
| `shared/.../data/repository/FirestoreProfileRepository.kt` | Modify | toMap/toProfileItem new fields, customNames subcollection |
| `shared/.../data/repository/OfflineFirstProfileRepository.kt` | Modify | upsert 12 params, toProfileItem mapping, 6 new methods |
| `shared/.../ui/ProfileAvatar.kt` | Modify | expect fun with photoUrl, Android actual with Coil |
| `shared/.../ui/ProfileCard.kt` | Modify | display resolution, @username label |
| `shared/.../ui/AccountViewModel.kt` | Create | State management for account flows |
| `shared/.../ui/AccountScreen.kt` | Create | 3 sub-screens, AnimatedContent |
| `shared/.../ui/SettingsScreen.kt` | Modify | "Mi perfil" row in existing card layout |
| `shared/.../ui/CuentaMorososApp.kt` | Modify | showAccountScreen state, AnimatedContent overlay |
| `app/.../data/CuentaMorososLocalStore.kt` | Modify | loadProfiles/saveProfiles for 4 new fields |
| `app/.../data/FirebaseUserSyncManager.kt` | Modify | ensureOwnProfile writes photoUrl, displayName, username |
| `shared/.../RepositoryProvider.kt` | Modify | (no change needed — storage is ViewModel-scoped) |
| `shared/.../AppViewModelFactory.kt` | Modify | Register AccountViewModel |
| `app/build.gradle.kts` | Modify | Add firebase-storage-ktx dependency |

## Testing Strategy

| Layer | What | How |
|-------|------|-----|
| Unit | ProfileItem backward compat | Parse 7-field JSON → all new fields null |
| Unit | displayNameFor resolution | All 3 cases: customName set, displayName set, neither |
| Unit | OfflineFirstProfileRepository.upsert | Verify 12-param call matches migration order |
| Unit | AccountViewModel username debounce | Turbine test on flow with 500ms debounce |
| Unit | AccountViewModel password reauth error | Mock FirebaseAuth throws requiresRecentLogin → verify UI state |
| Unit | CuentaMorososLocalStore roundtrip | Save profile with new fields → load → assert match |
| Integration | FirestoreProfileRepository read/write | Emulator-based: write profile, read back, verify new fields |

## Open Questions

- [ ] **Coil version**: Which Coil multiplatform artifact to use for the shared module? `io.coil-kt.coil3:coil-compose:3.0.4` and `coil-network` — verify compatibility with Compose Multiplatform 1.6.x.
- [ ] **Firebase Storage emulator**: Do we need Storage emulator integration tests, or is manual testing sufficient for this feature?
- [ ] **Custom names listener**: Should we attach a Firestore snapshot listener for reactive updates, or fetch on profile open? Spec says "Sync reactively" (R-014), but this adds listener management complexity.
