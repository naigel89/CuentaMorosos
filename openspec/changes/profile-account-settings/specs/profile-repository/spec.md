# Delta Spec: profile-repository

## ADDED Requirements

### R-001: Update display name
**Priority**: P0
**Description**: The `ProfileRepository` interface MUST include `suspend fun updateDisplayName(profileId: String, displayName: String): Result<Unit>` that updates the `displayName` field in both Firestore (via merge) and the local SQLDelight cache.
**Rationale**: Users need to set their display name; the operation must be offline-consistent.

**Scenarios**:
  - Given an authenticated user with a profile When `updateDisplayName` is called with a non-blank name Then Firestore document `profiles/{profileId}` gets `displayName` merged AND the local SQLDelight cache is updated.
  - Given the Firestore write fails When `updateDisplayName` is called Then the local cache is still updated AND the operation is enqueued for retry.

### R-002: Upload avatar photo
**Priority**: P0
**Description**: The repository MUST include `suspend fun uploadPhoto(profileId: String, imageBytes: ByteArray): Result<String>` that compresses to 256×256, uploads to Firebase Storage at `avatars/{profileId}/profile.jpg`, persists the download URL to Firestore `profiles/{profileId}/photoUrl`, and caches locally. Returns the download URL on success.
**Rationale**: Photo upload crosses Storage, Firestore, and SQLDelight — repository is the right coordination boundary.

**Scenarios**:
  - Given valid `imageBytes` When `uploadPhoto` is called Then Firebase Storage receives the file at `avatars/{profileId}/profile.jpg`, the download URL is written to Firestore AND the local cache, and `Result.success(url)` is returned.
  - Given a Storage quota/network error When `uploadPhoto` is called Then `Result.failure` is returned with a descriptive error and no local changes are made.

### R-003: Delete photo
**Priority**: P1
**Description**: The repository MUST include `suspend fun deletePhoto(profileId: String): Result<Unit>` that removes the Storage file and sets `photoUrl` to null in Firestore and local cache.
**Rationale**: Users may revert to emoji avatar.

**Scenario**:
  - Given a user with an uploaded photo When `deletePhoto` is called Then the Storage file is deleted AND `profiles/{profileId}/photoUrl` is set to null AND the local cache is updated.

### R-004: Set username
**Priority**: P0
**Description**: The repository MUST include `suspend fun setUsername(profileId: String, username: String): Result<Unit>` that first queries Firestore for uniqueness, then writes the username to Firestore and local cache if available. It MUST also include `suspend fun checkUsernameAvailability(username: String): Result<Boolean>` for debounced pre-validation.
**Rationale**: Username uniqueness requires a query before write; splitting availability check from set prevents race conditions.

**Scenarios**:
  - Given `checkUsernameAvailability("maria")` returns `true` When `setUsername("maria")` is called Then `profiles/{profileId}/username` is set to "maria" via merge in Firestore AND the local cache is updated.
  - Given `checkUsernameAvailability("takenUser")` returns `false` When `setUsername("takenUser")` is called Then `Result.failure` is returned with "Username already taken" and NO write occurs.

### R-005: Set custom name
**Priority**: P0
**Description**: The repository MUST include `suspend fun setCustomName(viewedProfileId: String, customName: String): Result<Unit>` that writes to the Firestore subcollection `profiles/{viewedProfileId}/customNames/{currentUserId}`. No local caching is required (resolved at display time from Firestore).
**Rationale**: Custom names are viewer-private and stored in a subcollection; local caching is deferred.

**Scenario**:
  - Given the current user is viewer A When `setCustomName(B.id, "Mi compa")` is called Then a merge write occurs at `profiles/{B.id}/customNames/{A.id}` with `{ customName: "Mi compa" }`.

### R-006: Change password
**Priority**: P0
**Description**: The repository MUST include `suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit>` that reauthenticates with `EmailAuthProvider` then calls Firebase `updatePassword`. This is a shared KMP suspend function; the actual Firebase auth calls MUST be in the `app` module and injected via the repository factory.
**Rationale**: Password change is an auth operation, not a profile data operation; it lives in the repository for dependency consistency.

**Scenario**:
  - Given valid current and new passwords When `changePassword` is called Then Firebase reauthenticates the user, updates the password, and returns `Result.success`.
