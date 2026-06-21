# invitation-flow Specification

## Purpose

Lifecycle of sending, receiving, accepting, and rejecting event invitations with inviter profile display, correct role assignment, and acceptance notification.

## Requirements

### R1: Invitation stores inviter identity

`EventInvitation` SHALL include `invitedByName: String` and `invitedByPhotoUrl: String?`.
The sender MUST pass `currentProfile.name` and `currentProfile.photoUrl` at creation.
`toInvitation()` deserialization MUST default `invitedByName` to `invitedByEmail` when absent.
`toMap()` serialization MUST include both new fields.

#### Scenario: Sender populates denormalized fields

- GIVEN a user with profile `name="Ana García"` and `photoUrl="https://..."` sends an invitation
- WHEN `CuentaMorososApp.onInviteMember` constructs `EventInvitation`
- THEN `invitedByName` = `"Ana García"` AND `invitedByPhotoUrl` = `"https://..."`

#### Scenario: Legacy document without new fields

- GIVEN a stored invitation document has no `invitedByName` or `invitedByPhotoUrl`
- WHEN `toInvitation()` deserializes it
- THEN `invitedByName` = `invitedByEmail` AND `invitedByPhotoUrl` = `null`

### R2: Accept assigns READER role

`acceptInvitation()` MUST assign `EventRole.READER` to the new participant.

#### Scenario: Accepted user becomes READER

- GIVEN a pending invitation for event E
- WHEN the invited user accepts
- THEN the participant added to E has `role = EventRole.READER`
- (Previously: `EventRole.CONTRIBUTOR`)

### R3: Invitations UI shows inviter avatar and display name

`InvitationsScreen` MUST render `ProfileAvatar` using `invitedByPhotoUrl` and display `"Invitado por: {invitedByName}"`.

#### Scenario: Invitation with photo

- GIVEN an invitation with non-null `invitedByPhotoUrl`
- WHEN the invitation card renders
- THEN `ProfileAvatar` loads the photo AND the label shows `invitedByName`

#### Scenario: Invitation without photo

- GIVEN an invitation with null `invitedByPhotoUrl`
- WHEN the invitation card renders
- THEN `ProfileAvatar` shows the initial of `invitedByName`

### R4: InvitationReceived notification uses display name

`InvitationsViewModel` MUST use `invitation.invitedByName` as `inviterName` in `NotificationEvent.InvitationReceived`.

#### Scenario: Correct inviterName

- GIVEN an invitation whose `invitedByName` = `"Ana García"`
- WHEN the ViewModel emits `InvitationReceived`
- THEN `inviterName` = `"Ana García"` (not the user's email)
- (Previously: used `invitedByEmail`)

### R5: Permission guard on sendInvitation

`sendInvitation` MUST check that the caller can perform `EventAction.ManageParticipants`. This is defense-in-depth; the UI gate at `InviteMemberDialog` is the primary guard.

#### Scenario: User has permission

- GIVEN the sender's role is OWNER or CONTRIBUTOR
- WHEN `sendInvitation` is called
- THEN the invitation is sent

#### Scenario: User lacks permission

- GIVEN the sender's role is READER
- WHEN `sendInvitation` is called
- THEN the invitation SHALL NOT be sent

### R6: Notify inviter on acceptance

`acceptInvitation()` MUST write a notification document to `notifications/{invitedByUid}/invitation-accepted/{invitationId}` with `eventId`, `eventName`, and `inviteeName`. The inviter's client MUST observe this collection and dispatch `NotificationEvent.InvitationAccepted` via `NotificationCallbacks.onInvitationAccepted`.

#### Scenario: Acceptance writes notification doc

- GIVEN invitation I from user U to user V
- WHEN V accepts I
- THEN a document exists at `notifications/{U.uid}/invitation-accepted/{I.id}` with `eventName` = I.eventName AND `inviteeName` = V's profile name

#### Scenario: Notification write fails

- GIVEN the notification Firestore write fails
- WHEN `acceptInvitation()` runs
- THEN the acceptance still succeeds (write is fire-and-forget) AND the error is logged
