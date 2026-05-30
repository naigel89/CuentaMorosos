# Delta Specs: Profile & Account Settings

**Change**: profile-account-settings
**Phase**: sdd-spec
**Date**: 2026-05-30
**Author**: SDD Spec Agent

---

## 1. profile-item-model [MODIFIED]

### R-001: ProfileItem extended fields
**Priority**: P0
**Description**: ProfileItem MUST include four new nullable fields: `photoUrl: String?`, `username: String?`, `displayName: String?`, `customNames: Map<String, String>?`. All default to null for backward compatibility. Existing fields (id, name, icon, totalPendingEuros, isGhost, linkedEmail, ownerId) remain unchanged.
**Rationale**: New account features need storage for photo, username, display name, and per-viewer custom names. Nullable defaults ensure existing documents deserialize without error.
**Scenarios**:
  - Given a Firestore document with only the original 7 fields When deserialized into ProfileItem Then `photoUrl`, `username`, `displayName`, `customNames` are all null.
  - Given a Firestore document with all 11 fields When deserialized Then all fields populate correctly.

### R-002: Display name resolution
**Priority**: P0
**Description**: The system MUST resolve visible display name as: `customName` (viewer-specific) > `displayName` > `name`. Resolution is computed at the UI layer, not stored in the model.
**Rationale**: Custom names and display names give flexible identity with a sensible fallback chain.
**Scenarios**:
  - Given a ProfileItem with `displayName = "Maria"` and `name = "María García"` When resolving without customName Then "Maria" is shown.
  - Given a ProfileItem with `displayName = null` and `name = "María García"` When resolving Then "María García" is shown.

---

## 2. profile-repository [MODIFIED]

### R-003: Update display name
**Priority**: P0
**Description**: Repository MUST include `suspend fun updateDisplayName(profileId: String, displayName: String): Result<Unit>`, updating Firestore (merge) and local SQLDelight cache.
**Rationale**: Display name must be persisted remotely and cached locally.
**Scenarios**:
  - Given a valid display name When called Then Firestore `displayName` is merged AND local cache is updated.
  - Given Firestore write fails When called Then local cache is updated and operation is enqueued for retry.

### R-004: Upload and delete avatar photo
**Priority**: P0
**Description**: Repository MUST include `suspend fun uploadPhoto(profileId: String, imageBytes: ByteArray): Result<String>` (compress 256×256, upload to Firebase Storage, persist URL) and `suspend fun deletePhoto(profileId: String): Result<Unit>`.
**Rationale**: Photo upload crosses Storage, Firestore, and SQLDelight — repository is the right boundary.
**Scenarios**:
  - Given valid imageBytes When uploadPhoto is called Then Firebase Storage receives the file AND the download URL is written to Firestore AND local cache.
  - Given a network error When uploadPhoto is called Then Result.failure is returned with descriptive error, no local changes.

### R-005: Unique username
**Priority**: P0
**Description**: Repository MUST include `suspend fun checkUsernameAvailability(username: String): Result<Boolean>` (for debounced validation) and `suspend fun setUsername(profileId: String, username: String): Result<Unit>` (uniqueness-checked write).
**Rationale**: Username uniqueness requires a query before write; splitting availability from set prevents race conditions.
**Scenarios**:
  - Given checkUsernameAvailability returns true When setUsername is called Then Firestore `username` is set via merge AND local cache is updated.
  - Given checkUsernameAvailability returns false When setUsername is called Then Result.failure("Username already taken") is returned, no write occurs.

### R-006: Custom name and password
**Priority**: P0
**Description**: Repository MUST include `suspend fun setCustomName(viewedProfileId: String, customName: String): Result<Unit>` (writes to `profiles/{id}/customNames/{viewerId}`) and `suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit>` (Firebase reauth + updatePassword).
**Rationale**: Custom names are viewer-private; password change is an auth operation within the repository boundary.
**Scenarios**:
  - Given setCustomName is called Then merge write occurs at the subcollection path.
  - Given valid credentials When changePassword is called Then Firebase reauthenticates and updates password.

---

## 3. profile-card-display [MODIFIED]

### R-007: Photo-aware ProfileAvatar
**Priority**: P0
**Description**: ProfileAvatar MUST accept optional `photoUrl: String?`. When non-null, display `AsyncImage` (Android: Coil). When null or load fails, fall back to first-letter initial or emoji. Ghost profiles MUST always use the emoji fallback.
**Rationale**: Photo is the primary visual enhancement; fallback ensures every profile has a visible avatar.
**Scenarios**:
  - Given valid photoUrl When rendering on Android Then AsyncImage loads the remote photo.
  - Given photo load failure (network/404) When rendering Then first-letter initial is shown.
  - Given ghost profile with photoUrl populated When rendering Then emoji is shown (photoUrl ignored).

### R-008: ProfileCard display resolution
**Priority**: P0
**Description**: ProfileCard MUST resolve title as: `customName` (optional param) > `displayName` > `name`. MUST show `@username` as secondary label when present. Optional params `customName: String?` and `showUsername: Boolean` default to null/false for backward compat.
**Rationale**: The card is the primary profile display surface; resolving the best available name improves clarity.
**Scenarios**:
  - Given customName="Mi compa", displayName="Maria", username="maria" When rendering Then title="Mi compa", secondary="@maria".
  - Given no customName or displayName, name="María", no username When rendering Then title="María", no secondary label.

---

## 4. profile-photo-upload [NEW]

### R-009: Photo upload flow
**Priority**: P0
**Description**: The system MUST compress selected images to 256×256 px, upload to `avatars/{uid}/profile.jpg` in Firebase Storage, and persist the download URL in `profiles/{uid}/photoUrl` via merge write.
**Rationale**: Standard avatar upload scoped per user with compression for upload efficiency.
**Scenarios**:
  - Given a photo is selected When upload completes Then file at `avatars/{uid}/profile.jpg` exists AND `profiles/{uid}/photoUrl` contains the download URL.
  - Given a network failure during upload Then error is returned and existing photoUrl is unchanged.

### R-010: Photo deletion
**Priority**: P1
**Description**: The system SHOULD allow photo deletion: remove Storage file, set `photoUrl` to null.
**Rationale**: Users may revert to emoji-only avatar.
**Scenario**:
  - Given user has an uploaded photo When "Remove photo" is tapped Then Storage file is deleted AND `photoUrl` set to null AND avatar falls back to emoji.

---

## 5. profile-username [NEW]

### R-011: Unique username with debounced validation
**Priority**: P0
**Description**: The system MUST validate username uniqueness with a 500ms debounced Firestore query. UI MUST show loading during validation and disable save while validating.
**Rationale**: Unthrottled queries spike Firestore read costs; debounce creates smooth UX.
**Scenarios**:
  - Given user types "maria" When 500ms passes after last keystroke Then a single Firestore query fires for "maria".
  - Given username is taken When user attempts to save Then "Username already taken" error is returned, no write occurs.

### R-012: Username display
**Priority**: P1
**Description**: When `username` is set, the system SHOULD display `@username` as a secondary label in ProfileCard.
**Rationale**: @username is a handle shown alongside the display name.
**Scenario**:
  - Given a profile with username="maria" When ProfileCard renders Then "@maria" appears as a secondary label.

---

## 6. profile-custom-names [NEW]

### R-013: Custom name storage and resolution
**Priority**: P0
**Description**: Custom names MUST be stored at `profiles/{viewedProfileId}/customNames/{viewerId}` with merge writes. Resolution priority: `customName` > `displayName` > `name`. Custom names are viewer-private and NOT cached in the shared ProfileItem.
**Rationale**: Subcollection per profile per viewer ensures privacy and avoids mutating source documents.
**Scenarios**:
  - Given viewer A sets customName for user B When stored Then document exists at `profiles/{B.id}/customNames/{A.id}` with `{ customName: "Mi compa" }`.
  - Given no customName for a contact When resolving Then `displayName` then `name` are used as fallback.

### R-014: Custom names sync
**Priority**: P1
**Description**: Custom names SHOULD sync reactively via Firestore snapshot listeners on the `customNames` subcollection.
**Rationale**: Users expect labels to be consistent across devices.
**Scenario**:
  - Given a custom name changes on device 1 When the Firestore listener fires on device 2 Then the displayed name updates reactively.

---

## 7. account-settings-ui [NEW]

### R-015: "Mi perfil" entry in SettingsScreen
**Priority**: P0
**Description**: SettingsScreen MUST include a "Mi perfil" navigation entry showing the user's avatar, name, and @username. Navigation MUST use the same state-based overlay pattern as EventDetailScreen.
**Rationale**: Clear entry point; consistent navigation reduces cognitive load.
**Scenario**:
  - Given user is on SettingsScreen When tapping "Mi perfil" Then the AccountScreen overlay opens with animated transition showing current profile data.

### R-016: AccountScreen with three sub-screens
**Priority**: P0
**Description**: AccountScreen MUST contain three sub-screens via `AnimatedContent`: NamePhotoScreen (avatar + display name), UsernameScreen (username with validation), SecurityScreen (password change). Each sub-screen has state managed by AccountViewModel.
**Rationale**: Three distinct concerns; sub-navigation keeps each flow focused.
**Scenarios**:
  - Given NamePhotoScreen renders Then it shows current avatar, display name field, and "Guardar" button.
  - Given UsernameScreen renders Then it shows current @username, text field with debounced validation, availability indicator, and "Guardar" button.
  - Given SecurityScreen renders Then it shows "Cambiar contraseña" requiring current password entry.

### R-017: AccountViewModel
**Priority**: P0
**Description**: AccountViewModel MUST use `viewModelScope` coroutines and expose `StateFlow` per sub-screen. Must handle photo upload, debounced validation, display name save, and password change.
**Rationale**: Centralized state prevents duplication; ViewModel survives configuration changes.
**Scenario**:
  - Given user edits on one sub-screen and navigates to another Then edits on the first sub-screen are preserved in ViewModel state.

---

## 8. profile-security [NEW]

### R-018: Password change with reauthentication
**Priority**: P0
**Description**: The system MUST reauthenticate with `EmailAuthProvider` using the current password before calling `updatePassword`. New password MUST be 6+ characters. Double-entry confirm (new + confirm) SHOULD be required.
**Rationale**: Firebase requires recent auth for sensitive operations; double-entry prevents typos.
**Scenarios**:
  - Given correct current password and matching 6+ char new password When submitted Then reauth succeeds AND password is updated AND success message shown.
  - Given wrong current password When reauth is attempted Then "invalid credentials" error AND password NOT changed.
  - Given mismatched new/confirm fields When user tries to save Then button remains disabled with "Las contraseñas no coinciden" error.

### R-019: requiresRecentLogin handling
**Priority**: P0
**Description**: If Firebase throws `requiresRecentLogin` despite reauthentication, the system MUST prompt the user to sign out and sign in again.
**Rationale**: Edge case where token expires between reauth and update; user needs explicit guidance.
**Scenario**:
  - Given `requiresRecentLogin` error after reauth When password update fails Then display "Tu sesión expiró. Por favor cerrá sesión y volvé a iniciarla." with a "Cerrar sesión" action.

---

## Summary

| Domain | Type | Requirements |
|--------|------|-------------|
| profile-item-model | MODIFIED | 2 |
| profile-repository | MODIFIED | 4 |
| profile-card-display | MODIFIED | 2 |
| profile-photo-upload | NEW | 2 |
| profile-username | NEW | 2 |
| profile-custom-names | NEW | 2 |
| account-settings-ui | NEW | 3 |
| profile-security | NEW | 2 |
| **Total** | | **19** |

## Scenarios Coverage

- Happy paths: covered (19/19)
- Edge cases: covered — backward compat (R-001), ghost profiles (R-007), load failure (R-007), mismatched passwords (R-018), requiresRecentLogin (R-019)
- Error states: covered — network errors (R-004, R-009), username taken (R-005, R-011), invalid credentials (R-018)
