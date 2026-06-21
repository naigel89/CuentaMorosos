# Proposal: fix-invitations-flow

## Intent

Fix 7 gaps in the invitation flow identified during exploration: incomplete model, wrong default role, missing avatar in UI, display name used instead of email, no inviter notification on acceptance, missing permission guard at the ViewModel/repository layer, and poor refresh after accept/reject. The core driver: invited users are assigned `CONTRIBUTOR` instead of `READER`, and the UI cannot show who invited them because `invitedByName`/`invitedByPhotoUrl` don't exist on `EventInvitation`.

## Scope

### In Scope

1. **P0 — Model + Role**: Add `invitedByName` / `invitedByPhotoUrl` to `EventInvitation`. Fix `acceptInvitation()` to assign `EventRole.READER`.
2. **P1 — UI & Display Name**: Wire `invitedByName` into `InvitationsScreen` alongside `ProfileAvatar`. Fix notification to pass `inviterName` from display name, not email.
3. **P2 — Notify Inviter**: On `acceptInvitation()`, write a Firestore notification document that the inviter's listener picks up. Use existing `NotificationEvent.InvitationAccepted` path.

### Out of Scope

- Server-side permission enforcement (Firestore Security Rules).
- FCM direct push — uses Firestore notification document approach (app listens to its own notifications collection).
- iOS push notifications.
- Email invitation outside the app ecosystem.
- Ghost profile creation for invited emails.

## Capabilities

### New Capabilities

- `invitation-flow`: Lifecycle of sending, receiving, accepting, and rejecting event invitations with proper role assignment and inviter display.

### Modified Capabilities

- `profile-avatar-consistency`: InvitationsScreen now uses `ProfileAvatar` — this is an implementation detail; no new spec-level requirements beyond the existing avatar consistency contract.

## Approach

**P0 changes are data-only** — no UI impact until P1 wires them. This allows parallel work on P0 (model+repo) and P1 (UI). P2 depends on P0 (needs acceptance path).

Denormalize inviter profile fields (`invitedByName`, `invitedByPhotoUrl`) onto `EventInvitation` at creation time in `CuentaMorososApp.onInviteMember`. Read from `currentProfile` which already has `name` and `photoUrl`.

Acceptance notification uses Firestore observer pattern: `acceptInvitation()` writes a document to `notifications/{uid}` subcollection; the inviter's `FirestoreInvitationRepository` (or a dedicated `NotificationRepository`) listens and dispatches via existing `onInvitationAccepted` callback.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `shared/.../model/Models.kt` | Modified | Add `invitedByName`, `invitedByPhotoUrl` to `EventInvitation` |
| `shared/.../data/repository/FirestoreInvitationRepository.kt` | Modified | Fix role to `READER`; write notification document on accept |
| `shared/.../data/repository/InvitationRepository.kt` | Modified | Optional: add `acceptInvitation` return type change |
| `shared/.../ui/InvitationsScreen.kt` | Modified | Add `ProfileAvatar` + `invitedByName` to card |
| `shared/.../ui/InvitationsViewModel.kt` | Modified | Fix `inviterName` to use `invitedByName` not email |
| `shared/.../ui/CuentaMorososApp.kt` | Modified | Denormalize inviter profile fields on `onInviteMember` |
| `shared/.../notifications/NotificationEvent.kt` | Unchanged | `InvitationAccepted` already has correct shape |
| `shared/.../NotificationCallbacks.kt` | Unchanged | Already has `onInvitationAccepted` |
| `app/.../notifications/NotificationDispatcher.kt` | Unchanged | Already handles `InvitationAccepted` |
| `app/.../data/CuentaMorososFirebaseMessagingService.kt` | Unchanged | Already parses `invitation_accepted` |
| `shared/.../ui/EventDetailScreen.kt` | Unchanged | Dialog guard is sufficient |
| `shared/.../data/repository/FirestoreProfileRepository.kt` | Unchanged | Architectural constraint confirmed |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Existing Firestore invitation docs lack `invitedByName`/`invitedByPhotoUrl` | High | Default to empty string in deserialization; UI falls back to `invitedByEmail` |
| READER role breaks existing accepted events | Low | Only applies to NEW acceptances; existing participants keep CONTRIBUTOR |
| Notification document write might fail silently | Med | Wrap in try/catch, log error, don't block acceptance |

## Rollback Plan

1. **Revert model**: Remove `invitedByName`/`invitedByPhotoUrl` from `EventInvitation`.
2. **Revert role**: Change `EventRole.READER` back to `EventRole.CONTRIBUTOR` in `acceptInvitation()`.
3. **Revert UI**: Restore `InvitationsScreen` to email-only display.
4. Disable notification document write in `acceptInvitation()`.

All P0/P1 changes are backward-compatible with existing Firestore data. No data migration needed.

## Dependencies

None. All changes are within the app codebase.

## Success Criteria

- [ ] `acceptInvitation()` assigns `EventRole.READER` (verified by test & manual check)
- [ ] `InvitationsScreen` shows inviter name + avatar for each invitation
- [ ] `InvitationReceived` notification uses display name, not email
- [ ] Inviter receives local notification when invitation is accepted
- [ ] Existing invitations with missing denormalized fields degrade gracefully (fallback to email)
- [ ] All existing invitation tests still pass (or updated for new fields)
