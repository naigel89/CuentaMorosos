# Tasks: Fix Invitations Flow

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~145 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | ask-on-risk |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

## Phase 1: Model + Serialization (Foundation)

- [ ] 1.1 Add `invitedByName: String = ""`, `invitedByPhotoUrl: String? = null` to `EventInvitation`  
  **Files**: `shared/.../model/Models.kt`  
  **Acceptance**: Compile — new fields exist with defaults; existing constructors unchanged.

- [ ] 1.2 Serialize new fields in `toMap()` and deserialize with legacy fallback in `toInvitation()`  
  **Files**: `shared/.../data/repository/FirestoreInvitationRepository.kt`  
  **Acceptance**: `invitedByName` defaults to `invitedByEmail` when absent; `invitedByPhotoUrl` defaults to `null`.

## Phase 2: Role Fix + Permission Guard (P0)

- [ ] 2.1 Change `acceptInvitation()` participant role from `CONTRIBUTOR` to `READER`  
  **Files**: `shared/.../data/repository/FirestoreInvitationRepository.kt`  
  **Acceptance**: New participant added with `"role" to EventRole.READER.name`.

- [ ] 2.2 Add permission guard to `sendInvitation()` — check `ManageParticipants` via `PermissionEngine`  
  **Files**: `shared/.../data/repository/FirestoreInvitationRepository.kt`  
  **Acceptance**: OWNER/CONTRIBUTOR can send; READER call returns early (defense-in-depth).

## Phase 3: Inviter Notification on Acceptance (P2)

- [ ] 3.1 Update `InvitationRepository` interface — `acceptInvitation` gets `inviteeName: String` param; add `observeInvitationAccepted(): Flow<NotificationEvent.InvitationAccepted>`  
  **Files**: `shared/.../data/repository/InvitationRepository.kt`  
  **Acceptance**: Interface compiles; impl stubs updated.

- [ ] 3.2 Write fire-and-forget notification doc in `acceptInvitation()`  
  **Files**: `shared/.../data/repository/FirestoreInvitationRepository.kt`  
  **Acceptance**: Doc written to `notifications/{invitedByUid}/invitation-accepted/{id}`; acceptance succeeds even if write fails.

- [ ] 3.3 Implement `observeInvitationAccepted()` — Firestore snapshot listener on `notifications/{uid}/invitation-accepted`  
  **Files**: `shared/.../data/repository/FirestoreInvitationRepository.kt`  
  **Acceptance**: Flow emits `InvitationAccepted(eventId, inviteeName, eventName)` when new doc appears.

- [ ] 3.4 Wire `observeInvitationAccepted()` into `InvitationsViewModel` with `onInvitationAccepted` callback  
  **Files**: `shared/.../ui/InvitationsViewModel.kt`, `shared/.../ui/CuentaMorososApp.kt`  
  **Acceptance**: VM collects flow and dispatches via callback; `CuentaMorososApp.onAccept` passes `inviteeName = currentProfile?.name ?: ""`.

- [ ] 3.5 Wire `onInvitationAccepted` callback in `AppViewModelFactory`  
  **Files**: `shared/.../AppViewModelFactory.kt`  
  **Acceptance**: `InvitationsViewModel` receives `onInvitationAccepted = notificationCallbacks.onInvitationAccepted`.

## Phase 4: UI + Profile Denormalization (P1)

- [ ] 4.1 Pass `currentProfile.name` and `currentProfile.photoUrl` to `EventInvitation` in `onInviteMember`  
  **Files**: `shared/.../ui/CuentaMorososApp.kt`  
  **Acceptance**: `invitedByName` and `invitedByPhotoUrl` populated from sender profile.

- [ ] 4.2 Render `ProfileAvatar` + `"Invitado por: {invitedByName}"` in invitation card  
  **Files**: `shared/.../ui/InvitationsScreen.kt`  
  **Acceptance**: Card shows avatar (photo or initial) and display name instead of email.

- [ ] 4.3 Fix `InvitationsViewModel` to use `invitation.invitedByName` (not `invitedByEmail`) as `inviterName`  
  **Files**: `shared/.../ui/InvitationsViewModel.kt`  
  **Acceptance**: `InvitationReceived.inviterName` = `invitedByName`, not email.

## Phase 5: Tests

- [ ] 5.1 Update `InvitationsViewModelNotificationTest` — change `inviterName` assertion from email to `invitedByName`; update `FakeInvitationRepository` for new interface  
  **Files**: `shared/.../ui/InvitationsViewModelNotificationTest.kt`  
  **Acceptance**: `assertEquals("Bob", event.inviterName)` after helper updated; compiles with new method stub.

- [ ] 5.3 Add test: `acceptInvitation` assigns READER role  
  **Files**: new or existing test file  
  **Acceptance**: Verifies `"role"` → `"READER"`, not `"CONTRIBUTOR"`.

- [ ] 5.4 Add test: `sendInvitation` permission guard blocks READER  
  **Files**: new test file or `RoleUiGatingTest.kt`  
  **Acceptance**: Mock Firestore; `set()` called for OWNER/CONTRIBUTOR, not for READER.

- [ ] 5.5 Add test: legacy invitation without new fields falls back gracefully  
  **Files**: new or existing test file  
  **Acceptance**: `invitedByName` defaults to `invitedByEmail`; `invitedByPhotoUrl` stays `null`.
