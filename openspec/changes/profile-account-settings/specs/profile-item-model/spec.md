# Delta Spec: profile-item-model

## MODIFIED Requirements

### R-001: ProfileItem fields
**Priority**: P0
**Description**: The ProfileItem data class MUST include four new nullable fields: `photoUrl: String?`, `username: String?`, `displayName: String?`, `customNames: Map<String, String>?`. All new fields MUST default to `null` to maintain backward compatibility with existing serialized data. The existing `name`, `icon`, `totalPendingEuros`, `isGhost`, `linkedEmail`, and `ownerId` fields remain unchanged.
(Previously: ProfileItem had 7 fields — id, name, icon, totalPendingEuros, isGhost, linkedEmail, ownerId)

**Rationale**: New account features need storage for photo, username, display name, and per-viewer custom names. Nullable defaults ensure existing profile documents (Firestore, SQLDelight, JSON) deserialize without error.

**Scenarios**:
  - Given a Firestore document with only the original 7 fields When deserialized into ProfileItem Then `photoUrl`, `username`, `displayName`, and `customNames` are all null and no deserialization error occurs.
  - Given a Firestore document with all 11 fields (7 original + 4 new) When deserialized into ProfileItem Then all fields populate correctly.
  - Given SQLDelight migration has run When loading a cached profile from before migration Then new columns are null/empty and the ProfileItem has null for the new fields.

### R-002: Display name resolution
**Priority**: P0
**Description**: The system MUST resolve the visible display name with this priority: `customName` (viewer-specific, resolved at display time) > `displayName` (on the profile) > `name` (original field). This resolution MUST NOT be stored in the model; it is computed at the UI layer.
(Previously: only `name` was displayed directly)

**Rationale**: Custom names and display names give users flexible identity management; resolution priority ensures a sensible fallback chain.

**Scenarios**:
  - Given a ProfileItem with `displayName = "Maria"` and `name = "María García"` When the UI resolves the display name without a customName context Then it shows "Maria".
  - Given a ProfileItem with `displayName = null` and `name = "María García"` When the UI resolves the display name Then it shows "María García".
