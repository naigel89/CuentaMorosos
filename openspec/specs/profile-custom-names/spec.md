# Custom Names Specification

## Purpose
Allow users to assign a personal label to each contact. Custom names are stored per-viewer in a Firestore subcollection and are private to the user who set them.

## Requirements

### R-001: Custom name storage
**Priority**: P0
**Description**: The system MUST store custom names in a Firestore subcollection at `profiles/{viewedProfileId}/customNames/{viewerId}`. The document MUST contain a single field `customName` with the viewer-defined label. Writes MUST use merge to avoid overwriting other fields if added later.
**Rationale**: A subcollection per profile scoped by viewer ID ensures privacy — only the viewer sees their own label. This also avoids mutating the source profile document.
**Scenarios**:
  - Given user A views user B's profile When user A sets customName "Mi compa" for user B Then the system writes `{ customName: "Mi compa" }` to `profiles/{B.id}/customNames/{A.id}` with merge.
  - Given user A has set no custom name for user B When the system resolves the display name for user B in user A's context Then it falls back to `displayName` then `name`.

### R-002: Display name resolution from customName
**Priority**: P0
**Description**: The system MUST resolve the visible display name for a profile in a given viewer context using this priority: `customName` (from subcollection) > `displayName` (on the profile) > `name` (original name field). The custom name is viewer-specific and MUST NOT be persisted in the `ProfileItem` cache; it is resolved at display time from Firestore or a local viewer-names cache.
**Rationale**: Custom names are personal labels; caching them in the shared profile cache would leak viewer-specific data to other users.
**Scenarios**:
  - Given user A has customName "Mi compa" for user B, and B has displayName "Maria" and name "María García" When user A views the ProfileCard for user B Then the card title shows "Mi compa".
  - Given user A has no customName for user B, and B has displayName "Maria" When user A views the ProfileCard for user B Then the card title shows "Maria".
  - Given no customName or displayName are set When user A views the ProfileCard for user B Then the card title shows B's `name`.

### R-003: Custom names sync
**Priority**: P1
**Description**: Custom names SHOULD be synced via Firestore snapshot listeners on the `customNames` subcollection. When a custom name changes on another device, the local display SHOULD update reactively.
**Rationale**: Users expect their labels to be consistent across devices.
**Scenario**:
  - Given user A changed a custom name on device 1 When the Firestore snapshot listener fires on device 2 Then the displayed name updates without user action.
