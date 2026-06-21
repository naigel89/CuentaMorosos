## Exploration: fix-invitations-flow

### Current State

The invitation flow in CuentaMorosos works via Firestore real-time snapshots on the `invitations` collection. Here's how each piece currently works:

**Sending an invitation:**
1. `EventDetailScreen` has `InviteMemberDialog` that collects email, calls `onInviteMember(email)`
2. In `CuentaMorososApp` (line 494-506), an `EventInvitation` is created with `invitedByEmail = currentProfile?.linkedEmail ?: ""` and sent via `invitationsViewModel.sendInvitation()`
3. `FirestoreInvitationRepository.sendInvitation()` writes to Firestore `invitations/{id}` collection

**Receiving an invitation (the invited user):**
1. `FirestoreInvitationRepository.observePendingInvitations()` queries Firestore `invitations` where `invitedEmail == currentUser.email && status == PENDING`
2. `InvitationsViewModel.pendingInvitations` Flow processes each invitation, calling `onNewInvitation` callback with `inviterName = invitation.invitedByEmail`
3. `InvitationsScreen` shows `invitation.eventName` and `"Invitado por: ${invitation.invitedByEmail}"`

**Accepting an invitation:**
1. `InvitationsScreen` "Aceptar" button → `acceptInvitation(invitation)`
2. `FirestoreInvitationRepository.acceptInvitation()` adds the UID to the event's `memberIds` and `participants` with **`EventRole.CONTRIBUTOR`** role, then updates invitation status to ACCEPTED

**Rejecting an invitation:**
1. Updates invitation status to REJECTED; Firestore snapshot filter automatically removes it from the UI

### Key Architectural Constraints

- **Profiles are per-user-scoped**: `FirestoreProfileRepository.observeProfiles()` filters by `ownerId == currentUser.uid`. The current user CANNOT look up other users' profiles (like the inviter's name/photo) via the standard profile flow.
- **FCM tokens are stored** in `users/{uid}/fcmToken` via `FirebaseUserSyncManager`, but there's NO server-side Cloud Function to send FCM messages. Notifications are currently local-only, triggered client-side by Firestore snapshot side-effects.
- **`users/{uid}` collection** stores `uid`, `email`, `displayName`, `fcmToken`, `updatedAt` — but NOT `photoUrl`.

### Affected Areas

- `shared/src/commonMain/kotlin/com/cuentamorosos/model/Models.kt` — `EventInvitation` data class (lines 213-223)
- `shared/src/commonMain/kotlin/com/cuentamorosos/data/repository/FirestoreInvitationRepository.kt` — `toMap()`, `toInvitation()`, `acceptInvitation()` (lines 37-101)
- `shared/src/commonMain/kotlin/com/cuentamorosos/data/repository/InvitationRepository.kt` — interface (lines 1-15)
- `shared/src/commonMain/kotlin/com/cuentamorosos/ui/InvitationsViewModel.kt` — notification callback `inviterName` (line 30)
- `shared/src/commonMain/kotlin/com/cuentamorosos/ui/InvitationsScreen.kt` — UI showing only email, no avatar (lines 64-66)
- `shared/src/commonMain/kotlin/com/cuentamorosos/ui/CuentaMorososApp.kt` — `onInviteMember` wiring (lines 494-507)
- `shared/src/commonMain/kotlin/com/cuentamorosos/notifications/NotificationEvent.kt` — `InvitationAccepted` event (lines 13-17)
- `shared/src/commonMain/kotlin/com/cuentamorosos/NotificationCallbacks.kt` — callbacks (lines 1-9)
- `shared/src/androidMain/kotlin/com/cuentamorosos/AppViewModelFactory.kt` — `InvitationsViewModel` creation (lines 45-49)
- `app/src/main/java/com/cuentamorosos/data/FirebaseUserSyncManager.kt` — for FCM token lookup (lines 75-91)
- `app/src/main/java/com/cuentamorosos/data/CuentaMorososFirebaseMessagingService.kt` — FCM parsing (lines 82-91)
- `shared/src/commonTest/kotlin/com/cuentamorosos/ui/InvitationsViewModelNotificationTest.kt` — test updates
- `shared/src/commonTest/kotlin/com/cuentamorosos/ui/RoleUiGatingTest.kt` — verify READER role gates

### Approaches

#### 1. Add inviter profile info (name + photoUrl) to invitations

**Problem**: `EventInvitation` model has only `invitedByUid` and `invitedByEmail`. Since profiles are per-user-scoped, the invited user cannot look up the inviter's profile.

**Approach**: Denormalize inviter info onto the invitation document at creation time. Add `invitedByName: String` and `invitedByPhotoUrl: String?` to `EventInvitation`.

- **Pros**: No extra Firestore reads; works offline; simple data model change
- **Cons**: Stale data if inviter updates their profile (acceptable for historical invitations)
- **Effort**: Low (3 files: Models.kt, FirestoreInvitationRepository.kt, CuentaMorososApp.kt)

**Alternative — Runtime lookup**: Add a `users/{uid}` document lookup at invitation display time. Query the `users` collection for `displayName` and the `profiles` collection for `photoUrl`.

- **Pros**: Always current data
- **Cons**: N+1 queries per invitation; offline complexity; much higher effort
- **Effort**: High

**Recommendation**: Denormalize. The inviter info is captured at invite-time, which is "good enough" for a historical invitation log.

#### 2. Change accepted role from CONTRIBUTOR to READER

**Problem**: `FirestoreInvitationRepository.acceptInvitation()` (line 50) hardcodes `EventRole.CONTRIBUTOR.name`.

**Change**: Replace `EventRole.CONTRIBUTOR.name` with `EventRole.READER.name`.

- **Effort**: Trivial (1 line)
- **Risks**: None — the PermissionEngine already correctly gates all actions for READER role. Existing `RoleUiGatingTest` already verifies READER has no write permissions.

#### 3. Implement FCM notification to inviter on acceptance

**Problem**: `FirestoreInvitationRepository.acceptInvitation()` has no side-effect to notify the inviter. The FCM infrastructure exists (tokens stored at `users/{uid}/fcmToken`) but there's no sending mechanism.

**Three sub-approaches:**

**3a. Firebase Cloud Function (recommended)**
- Create a Firebase Cloud Function that triggers on Firestore `invitations/{invitationId}` document updates
- When `status` changes to `ACCEPTED`, look up the inviter's FCM token from `users/{invitedByUid}` and send via Admin SDK
- **Pros**: Secure (server-side), no client changes to the repository, proper FCM delivery
- **Cons**: Requires Firebase Blaze plan for Cloud Functions; new deployment pipeline; initial setup overhead
- **Effort**: Medium-High

**3b. Client-side HTTP FCM send**
- In `FirestoreInvitationRepository.acceptInvitation()`, after updating Firestore, read the inviter's FCM token from `users/{invitedByUid}` and POST to FCM HTTP API
- **Pros**: No server-side code; stays in existing codebase
- **Cons**: Exposes FCM server key (security risk); the FCM HTTP v1 API requires OAuth2 which is complex from a client; client-side FCM sending is not recommended by Google
- **Effort**: Medium

**3c. Client-side Firestore-based notification (pragmatic MVP)**
- In `acceptInvitation()`, after the Firestore update succeeds, write a notification document to Firestore (e.g., `notifications/{invitedByUid}/...`) that the inviter's client will pick up via a snapshot listener
- The inviter's `EventsViewModel` or a new notification observer picks up this document and triggers a local notification
- **Pros**: No server-side code; leverages existing Firestore snapshot pattern; offline-compatible
- **Cons**: Not a true push notification (requires app to be running or syncing); higher latency
- **Effort**: Medium

#### 4. Fix InvitationsScreen UI to show profile icon + name

**Problem**: `InvitationsScreen` (lines 64-66) shows only `"Invitado por: ${invitation.invitedByEmail}"`. No profile avatar, no display name.

**Approach**: Once `invitedByName` and `invitedByPhotoUrl` are available on the invitation model (Approach 1), update `InvitationsScreen` to show:
- A `ProfileAvatar(name = invitation.invitedByName, photoUrl = invitation.invitedByPhotoUrl, size = 32.dp)` next to "Invitado por:"
- The display name (`invitation.invitedByName`) instead of email

- **Effort**: Low

#### 5. Fix notification callback inviterName

**Problem**: `InvitationsViewModel` (line 30) passes `invitation.invitedByEmail` as `inviterName` to `NotificationEvent.InvitationReceived`.

**Change**: Replace with `invitation.invitedByName` (once available from Approach 1).

- **Effort**: Trivial (1 line)

#### 6. Permission guard before sending invitation

**Problem**: `onInviteMember` in `CuentaMorososApp` (line 494) doesn't verify permissions at the ViewModel level.

**Assessment**: The UI already gates the InviteMemberDialog with `canDo(EventAction.ManageParticipants)` (line 376 in EventDetailScreen), and `ManageParticipants` is correctly gated to OWNER/CONTRIBUTOR in PermissionEngine. This is a defense-in-depth concern — add a `PermissionEngine.canDo()` check in the ViewModel or before the `sendInvitation()` call.

**Approach**: Add a permission check in `CuentaMorososApp.onInviteMember` before creating the invitation, or move the logic to a new ViewModel method that verifies `canDo`.

- **Effort**: Low

#### 7. Flow refresh after accept/reject

**Problem**: Listed as an issue in the prompt — "observer flow doesn't refresh properly when accepted/rejected".

**Assessment**: `observePendingInvitations()` uses Firestore `.snapshots` (real-time listener) filtered by `status == PENDING`. When `acceptInvitation()` updates the status to `ACCEPTED` via `.update("status"...)`, the snapshot should re-emit, and the Flow will automatically filter out the non-PENDING item. **This should work correctly.** If there's a bug, it would be in the Firestore snapshot listener not re-emitting, which is a Firestore SDK issue, not code.

**Recommendation**: Investigate if this is a real issue during testing. If confirmed, it may be a Firestore rule or security issue preventing the snapshot from firing after the status update.

### Recommendation

| Priority | Change | Effort | Dependencies |
|----------|--------|--------|-------------|
| P0 | Add `invitedByName` and `invitedByPhotoUrl` to `EventInvitation` model | Low | None |
| P0 | Change role from `CONTRIBUTOR` to `READER` in `acceptInvitation()` | Trivial | None |
| P1 | Fix `InvitationsScreen` UI to show profile avatar + name | Low | P0 (model) |
| P1 | Fix `InvitationsViewModel` notification callback to use `invitedByName` | Trivial | P0 (model) |
| P1 | Add permission guard in `onInviteMember` (defense-in-depth) | Low | None |
| P2 | FCM notification to inviter on acceptance — Option 3c (Firestore-based) | Medium | None |
| P3 | FCM notification to inviter — Option 3a (Cloud Function) | High | Infrastructure |

### Risks

- **Firestore security rules**: The `invitations` collection must allow creation and reading by email. If security rules are too restrictive, the snapshots won't fire. Verify rules before deployment.
- **READER role impact**: Once a user accepts as READER, they cannot create expenses, manage participants, or calculate. This is by design but must be clearly communicated in the UI (e.g., disable buttons, show "view-only" badges).
- **Denormalized inviter info staleness**: If the inviter changes their name/photo after sending the invitation, the invitation still shows the old info. Acceptable for MVP; document as known limitation.
- **Firestore-based notification (3c)**: Requires the inviter to have the app open or syncing recently. Not a true push notification. May need upgrading to Cloud Functions later.

### Ready for Proposal

Yes — the scope is well-understood. All changes are isolated to the invitation flow. Recommend the orchestrator proceed with proposal phase, prioritizing model changes (P0) and UI fixes (P1), while deferring FCM notification to inviter as a follow-up (P2+).
