# Photo Upload Specification

## Purpose
Handle avatar photo upload to Firebase Storage, compression, and URL persistence in Firestore profile documents. Enables users to set a profile photo that syncs across devices.

## Requirements

### R-001: Photo upload to Firebase Storage
**Priority**: P0
**Description**: The system MUST accept an image from the device gallery, compress it to 256×256 px, upload it to `avatars/{uid}/profile.jpg` in Firebase Storage, and persist the download URL in the Firestore profile document under `photoUrl`.
**Rationale**: Users expect standard avatar photo upload; the storage path is scoped per user to prevent overwrites. Compression ensures reasonable upload size.
**Scenarios**:
  - Given an authenticated user When they select a photo from the gallery Then the image is compressed to 256×256 px Then the file is uploaded to `avatars/{uid}/profile.jpg` Then the download URL is written to `profiles/{uid}/photoUrl` via merge.
  - Given an upload failure (network error) When the user attempts to upload a photo Then the system returns a descriptive error and the existing photo (if any) is unchanged.

### R-002: Photo display in ProfileAvatar
**Priority**: P0
**Description**: The ProfileAvatar component MUST display the photo from `photoUrl` using an `AsyncImage` (Android: Coil) when the field is non-null. When `photoUrl` is null or the image fails to load, it MUST fall back to the existing emoji/initial avatar.
**Rationale**: KMP shared code cannot use Coil directly; Android uses Coil via `expect`/`actual`, KMP fallback remains emoji. Ghost profiles must never show a photo.
**Scenarios**:
  - Given a ProfileItem with a non-null `photoUrl` When ProfileAvatar renders Then it displays `AsyncImage` with that URL as the source.
  - Given a ProfileItem where image loading fails When ProfileAvatar renders Then it falls back to the first-letter initial derived from the resolved display name.
  - Given a ghost profile (isGhost=true) with a non-null `photoUrl` When ProfileAvatar renders Then it MUST show the emoji fallback and ignore the photo.

### R-003: Photo deletion
**Priority**: P1
**Description**: The system SHOULD allow the user to delete their avatar photo, removing the Storage file and setting `photoUrl` to null in Firestore.
**Rationale**: Users may want to revert to emoji-only display.
**Scenario**:
  - Given a user with an uploaded photo When they tap "Remove photo" Then the Storage file at `avatars/{uid}/profile.jpg` is deleted AND `profiles/{uid}/photoUrl` is set to null AND ProfileAvatar falls back to emoji.
