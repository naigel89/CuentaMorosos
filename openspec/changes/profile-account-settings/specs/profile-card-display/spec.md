# Delta Spec: profile-card-display

## MODIFIED Requirements

### R-001: ProfileAvatar with photo support
**Priority**: P0
**Description**: The ProfileAvatar composable MUST accept an optional `photoUrl: String?` parameter. When non-null, it MUST display an `AsyncImage` loading the photo from the URL (Android: Coil via `expect`/`actual`). When null or loading fails, it MUST fall back to the existing first-letter initial or emoji display. The composable signature MUST remain backward-compatible with existing callers.
(Previously: ProfileAvatar only accepted `name` and `emoji`, rendered as first-letter initial or emoji)

**Rationale**: Photo display is the primary visual enhancement; the fallback chain ensures every profile has a visible avatar regardless of photo availability.

**Scenarios**:
  - Given ProfileAvatar receives a valid `photoUrl` When it renders on Android Then an `AsyncImage` loads and displays the remote photo.
  - Given ProfileAvatar receives a valid `photoUrl` but the image fails to load (network error, 404) When it renders Then it falls back to the first-letter initial derived from the resolved display name.
  - Given ProfileAvatar receives `photoUrl = null` When it renders Then it behaves identically to the current implementation (first-letter or emoji).

### R-002: ProfileCard display name resolution
**Priority**: P0
**Description**: The ProfileCard composable MUST resolve the displayed title using the priority: `customName` (passed as optional parameter) > `displayName` (from profile) > `name` (from profile). The card MUST also show `@username` as a secondary label when present. The composable signature MUST add optional `customName: String?` and `showUsername: Boolean` parameters that default to null/false for backward compatibility.
(Previously: ProfileCard always used `profile.name` as the title with no secondary label)

**Rationale**: The card is the primary profile display surface; resolving the best available name and showing @username improves identity clarity.

**Scenarios**:
  - Given ProfileCard receives `customName = "Mi compa"`, profile with `displayName = "Maria"`, `name = "María García"`, and `username = "maria"` When the card renders Then the title shows "Mi compa" and a secondary label shows "@maria".
  - Given ProfileCard receives `customName = null`, profile with `displayName = null`, `name = "María"`, and `username = null` When the card renders Then the title shows "María" with no secondary label.
  - Given ProfileCard receives no customName parameter (backward-compatible call) When the card renders Then it resolves from `displayName` then `name` as before.

### R-003: Ghost profile enforcement
**Priority**: P0
**Description**: The system MUST enforce that ghost profiles (isGhost=true) NEVER display `photoUrl`, `displayName`, or `username`. They MUST always use the existing emoji avatar and no display name resolution — only `name` is shown.
(Previously: ghost profiles used emoji; no other fields existed to enforce)

**Rationale**: Ghost profiles represent unregistered users; they must not show any profile settings to avoid confusion.

**Scenarios**:
  - Given a ghost profile with `photoUrl`, `displayName`, and `username` all populated (malformed data) When ProfileAvatar or ProfileCard renders Then it shows only the emoji and the `name` field; `photoUrl`, `displayName`, and `username` are ignored.
