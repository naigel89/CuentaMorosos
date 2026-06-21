# Design: fix-invitations-flow

## Technical Approach

Six coordinated changes across model, repository, UI, and notification layers. P0 (model+role) enables P1 (UI+display name) and P2 (inviter notification). No data migration — legacy docs degrade gracefully via deserialization defaults.

## Architecture Decisions

### Decision: Denormalize inviter profile onto EventInvitation

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Look up inviter profile at read time | + No storage duplication; − N+1 reads per invitation card, offline-unfriendly | Rejected |
| Store `invitedByName`/`invitedByPhotoUrl` on the doc | + Single read, offline-safe; − Stale photo URL if inviter changes avatar later | **Chosen** — acceptable staleness, photo URL updates are rare |

### Decision: Permission check inside repository, not only UI

| Option | Tradeoff | Decision |
|--------|----------|----------|
| UI gate only | + Simple; − No defense-in-depth, bypassable via direct API calls | Rejected |
| Check in `FirestoreInvitationRepository.sendInvitation()` | + Defense-in-depth; − Slight latency from event fetch | **Chosen** — matches existing pattern in `acceptInvitation()` |

### Decision: New observer method for acceptance notifications

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Put observer in `InvitationsViewModel` | + Self-contained; − ViewModel already handles pending flow, mixing concerns | Rejected |
| Add `observeInvitationAccepted(uid)` to `InvitationRepository` | + Clean separation; − Repository returns `NotificationEvent` type | **Chosen** — cross-module types already used in `InvitationsViewModel` |

### Decision: Fire-and-forget notification write on accept

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Gated (fail accept if notification write fails) | + Consistency; − Blocks user experience for non-critical side effect | Rejected |
| Fire-and-forget with try/catch | + Acceptance always succeeds; − Notification may be lost silently | **Chosen** — per spec R6, acceptance must not fail due to notification write |

## Data Flow

```
CuentaMorososApp.onInviteMember
  │  passes currentProfile.name/photoUrl
  ▼
FirestoreInvitationRepository.sendInvitation()
  │  checks PermissionEngine.hasPermission(role, ManageParticipants) ← NEW
  │  writes EventInvitation with invitedByName + invitedByPhotoUrl
  ▼
Firestore (invitations/{id})

InvitationsViewModel.observePendingInvitations()
  │  deserializes with fallback to invitedByEmail for invitedByName
  │  uses invitedByName as inviterName in NotificationEvent  ← FIXED
  ▼
InvitationsScreen
  │  renders ProfileAvatar + "Invitado por: {invitedByName}"   ← CHANGED
  ▼
CuentaMorososApp — onAccept callback
  │  passes inviteeName = currentProfile?.name ?: ""
  ▼
acceptInvitation(invitation, inviteeName)
  │  writes EventRole.READER ← FIXED
  │  writes notifications/{invitedByUid}/invitation-accepted/{id}
  │    with {eventId, eventName, inviteeName}                  ← NEW
  ▼
InvitationsViewModel
  │  collects repository.observeInvitationAccepted()           ← NEW
  │  dispatches via onInvitationAccepted callback              ← NEW
  ▼
NotificationCallbacks.onInvitationAccepted
  │  → NotificationDispatcher.dispatch()
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `shared/.../model/Models.kt` | Modify | Add `invitedByName: String`, `invitedByPhotoUrl: String?` to `EventInvitation` (default `""` / `null`) |
| `shared/.../data/repository/InvitationRepository.kt` | Modify | Add `observeInvitationAccepted()`. Change `acceptInvitation` signature to include `inviteeName: String` |
| `shared/.../data/repository/FirestoreInvitationRepository.kt` | Modify | `toMap()` — serialize new fields; `toInvitation()` — deserialize with fallback; `acceptInvitation()` — use `EventRole.READER` + write notification doc; `sendInvitation()` — add permission guard; new `observeInvitationAccepted()` |
| `shared/.../ui/InvitationsViewModel.kt` | Modify | Use `invitation.invitedByName` (not `invitedByEmail`) for `inviterName`. Add `onInvitationAccepted` callback parameter + collect `observeInvitationAccepted()` flow |
| `shared/.../ui/InvitationsScreen.kt` | Modify | Render `ProfileAvatar(invitedByPhotoUrl, invitedByName)` + label `"Invitado por: ${invitation.invitedByName}"` |
| `shared/.../ui/CuentaMorososApp.kt` | Modify | `onInviteMember` (line ~494-506): pass `currentProfile?.name` and `currentProfile?.photoUrl` to `EventInvitation` |
| `shared/.../ui/ProfileAvatar.kt` | **Use existing** | Already exists — `fun ProfileAvatar(name: String, photoUrl: String?, size: Dp, modifier: Modifier)`. Only needs to be called from InvitationsScreen |
| `shared/.../AppViewModelFactory.kt` | Modify | Wire `onInvitationAccepted` callback into `InvitationsViewModel` constructor |

## Interfaces / Contracts

### EventInvitation (Models.kt)

```kotlin
data class EventInvitation(
    // ... existing fields unchanged ...
    val invitedByName: String = "",
    val invitedByPhotoUrl: String? = null,
)
```

### FirestoreInvitationRepository.toInvitation() — deserialization

```kotlin
EventInvitation(
    // ... existing fields ...
    invitedByName = data["invitedByName"] as? String
        ?: (data["invitedByEmail"] as? String ?: ""),  // legacy fallback
    invitedByPhotoUrl = data["invitedByPhotoUrl"] as? String,
)
```

### FirestoreInvitationRepository.toMap() — serialization

```kotlin
private fun EventInvitation.toMap(): Map<String, Any?> = mapOf(
    // ... existing fields ...
    "invitedByName" to invitedByName,
    "invitedByPhotoUrl" to invitedByPhotoUrl,
)
```

### ProfileAvatar (new file)

```kotlin
@Composable
fun ProfileAvatar(
    photoUrl: String?,
    name: String,
    modifier: Modifier = Modifier.size(32.dp),
)
```

Uses `AsyncImage` (Coil 3) for non-null `photoUrl`, otherwise renders first letter of `name` in a circle.

### InvitationRepository — new method + signature change

```kotlin
interface InvitationRepository {
    // ... existing methods ...
    suspend fun acceptInvitation(invitation: EventInvitation, inviteeName: String)
    /** Listens on notifications/{currentUser.uid}/invitation-accepted/* for accepted invitations. */
    fun observeInvitationAccepted(): Flow<NotificationEvent.InvitationAccepted>
}
```

`inviteeName` is the display name of the user accepting the invitation (passed from `CuentaMorososApp` where `currentProfile?.name` is available).

The repository reads the current user UID from `Firebase.auth` internally — no need to pass it from the ViewModel.

### Permission guard in sendInvitation (inside FirestoreInvitationRepository)

```kotlin
override suspend fun sendInvitation(invitation: EventInvitation) {
    // NEW: fetch event and check permission
    val eventDoc = db.collection("events").document(invitation.eventId).get()
    val event = eventDoc.toEvent() // local deserialization
    val role = PermissionEngine.getRole(invitation.invitedByUid, event)
    if (!PermissionEngine.hasPermission(role, EventAction.ManageParticipants)) {
        Log.w(TAG, "sendInvitation denied: user ${invitation.invitedByUid} lacks ManageParticipants permission")
        return  // defense-in-depth — UI gate is primary
    }
    // ... existing write logic ...
}
```

### Acceptance notification write (inside acceptInvitation)

```kotlin
override suspend fun acceptInvitation(invitation: EventInvitation, inviteeName: String) {
    // ... existing role/participant update logic ...
    
    // Fire-and-forget: acceptance succeeds even if this fails
    runCatching {
        db.collection("notifications")
            .document(invitation.invitedByUid)
            .collection("invitation-accepted")
            .document(invitation.id)
            .set(mapOf(
                "eventId" to invitation.eventId,
                "eventName" to invitation.eventName,
                "inviteeName" to inviteeName,
            ))
    }.onFailure { e ->
        Log.w(TAG, "Failed to write acceptance notification", e)
    }
}
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit (model) | `EventInvitation` new fields, legacy fallback | Update helper factory, assert `invitedByName` defaults to `invitedByEmail` |
| Unit (repo) | `sendInvitation` permission guard — OWNER/CONTRIBUTOR allowed, READER blocked | Mock Firestore, verify call vs no-op |
| Unit (repo) | `acceptInvitation` assigns `READER` role | Assert `"READER"` in participant map, not `"CONTRIBUTOR"` |
| Unit (repo) | Notification doc written on accept | Verify Firestore `set()` call on notification path |
| Unit (VM) | `inviterName` uses `invitedByName` not `invitedByEmail` | Update `InvitationsViewModelNotificationTest` — change assertion from `email` to `name` |
| Integration | `InvitationsScreen` renders avatar + display name | Compose UI test — assert label contains `invitedByName` |
| Manual | Legacy invitation without new fields | Load existing doc, verify UI shows email fallback |

## Migration / Rollout

No migration required. All new fields are optional in Firestore. `toInvitation()` defaults `invitedByName` to `invitedByEmail` when absent, preserving backward compatibility. Existing invitations display with "Invitado por: {email}" — which matches current behavior. New invitations carry structured display name.

Rollback: revert each file in reverse order. All steps are data-safe.

## Implementation Order

1. **P0 — Model + Serialization**: `EventInvitation` fields, `toMap()`, `toInvitation()` — no behavior change yet
2. **P0 — Role fix**: `acceptInvitation()` → `EventRole.READER`
3. **P1 — Profile denormalization**: `CuentaMorososApp.onInviteMember` — pass `currentProfile.name` + `currentProfile.photoUrl`
4. **P1 — UI**: `InvitationsScreen` layout (ProfileAvatar + invitedByName), `InvitationsViewModel` inviterName
5. **P2 — Notification observer**: `observeInvitationAccepted()` in repo, notification write in `acceptInvitation()`, ViewModel collection + callback
6. **P2 — Wiring**: `AppViewModelFactory` — pass `onInvitationAccepted`, `CuentaMorososApp` — pass `inviteeName` to `acceptInvitation()`
7. **P3 — Permission guard**: `sendInvitation()` check in repo
8. **Tests**: Update existing + add new

Steps 1-2 are independent of 3-4. Steps 5-6 depend on 1-3. Step 7 depends on none.

## Open Questions

- [x] `inviteeName` for notification doc → **Resolved**: `acceptInvitation()` gets a new `inviteeName: String` parameter. `CuentaMorososApp` passes `currentProfile?.name ?: ""` when calling `invitationsViewModel.acceptInvitation(invitation)`.
- [x] `ProfileAvatar` → **Resolved**: Already exists in `shared/.../ui/ProfileAvatar.kt`, no new file needed.
