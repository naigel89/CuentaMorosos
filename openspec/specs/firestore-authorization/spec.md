# Firestore Authorization Specification

## Purpose

Firestore data MUST be protected by server-enforced security rules. Unauthenticated access MUST be denied. Email verification MUST be enforced before granting access to authenticated features. Firestore rules MUST be committed to the repository for auditability.

## Requirements

### R001: Firestore Rules Checked In

A `firestore.rules` file MUST exist in the repository root and be deployable via `firebase deploy --only firestore:rules`. A `firebase.json` MUST reference it.

**Acceptance Criteria**: `firestore.rules` and `firebase.json` exist in repo. Rules deploy without errors. Rules are reviewed as part of code review.

#### Scenario: Rules are deployed and auditable

- **GIVEN** `firestore.rules` exists with valid syntax
- **WHEN** `firebase deploy --only firestore:rules` is executed
- **THEN** rules are deployed to the Firestore project
- **AND** the rules content is visible in the Firebase Console

### R002: Unauthenticated Writes Are Denied

The system MUST deny any write to events, expenses, or participants from an unauthenticated request. No collection or document may be world-writable.

**Acceptance Criteria**: Direct REST API write call to Firestore without auth token returns `PERMISSION_DENIED`.

#### Scenario: Anonymous user attempts to create an expense

- **GIVEN** the request is not authenticated
- **WHEN** it attempts to create an expense
- **THEN** Firestore rules MUST reject the write

### R003: User Document Self-Access Only

Authenticated users MUST only read/write their own document at `/users/{uid}` where `request.auth.uid == uid`. They MUST NOT read other users' documents containing FCM tokens or private fields.

**Acceptance Criteria**: User A cannot read `/users/{userB_uid}`. User A can write to `/users/{userA_uid}`.

#### Scenario: User reads own document

- **GIVEN** user A is authenticated with `uid = "abc123"`
- **WHEN** they query `/users/abc123`
- **THEN** the read is allowed

#### Scenario: User cannot read another user's document

- **GIVEN** user A is authenticated with `uid = "abc123"`
- **WHEN** they attempt to read `/users/xyz789`
- **THEN** Firestore returns `PERMISSION_DENIED`

### R004: Event Read Access Restricted to Participants

Event documents MUST only be readable by authenticated users who are listed as participants in the event's `participants` map.

**Acceptance Criteria**: User not in event's `participants` map cannot read event data.

#### Scenario: Participant reads their event

- **GIVEN** user A is a participant (OWNER role) in event X
- **WHEN** they query `/events/eventX`
- **THEN** the read is allowed

#### Scenario: Non-participant cannot read event

- **GIVEN** user B is NOT in event X's `participants` map
- **WHEN** they attempt to read `/events/eventX`
- **THEN** Firestore returns `PERMISSION_DENIED`

### R005: OWNER Has Full Write Access

The system MUST allow an event OWNER to create, update, and delete both the event document and its expense sub-documents.

**Acceptance Criteria**: OWNER can write any event or expense document in their event.

#### Scenario: OWNER updates event metadata

- **GIVEN** the authenticated user is the event OWNER
- **WHEN** they update the event title or date
- **THEN** Firestore rules MUST allow the write

#### Scenario: OWNER deletes an event

- **GIVEN** the authenticated user is the event OWNER
- **WHEN** they delete the event
- **THEN** Firestore rules MUST allow the deletion

### R006: CONTRIBUTOR Has Limited Write Access

The system MUST allow a CONTRIBUTOR to create expenses and update expenses they created. A CONTRIBUTOR MUST NOT add, edit, or delete participants.

**Acceptance Criteria**: CONTRIBUTOR can create and manage their own expenses but cannot mutate participants or run final settlement.

#### Scenario: CONTRIBUTOR creates an expense

- **GIVEN** the authenticated user is a CONTRIBUTOR on the event
- **WHEN** they create a new expense with their profile id as `createdByProfileId`
- **THEN** Firestore rules MUST allow the create

#### Scenario: CONTRIBUTOR updates their own expense

- **GIVEN** the authenticated user is a CONTRIBUTOR and the expense's `createdByProfileId` matches their profile id
- **WHEN** they update the expense
- **THEN** Firestore rules MUST allow the update

#### Scenario: CONTRIBUTOR deletes their own expense

- **GIVEN** the authenticated user is a CONTRIBUTOR and the expense's `createdByProfileId` matches their profile id
- **WHEN** they delete the expense
- **THEN** Firestore rules MUST allow the delete

#### Scenario: CONTRIBUTOR attempts to delete another user's expense

- **GIVEN** the authenticated user is a CONTRIBUTOR and the expense's `createdByProfileId` does NOT match their profile id
- **WHEN** they attempt to delete the expense
- **THEN** Firestore rules MUST reject the write

#### Scenario: CONTRIBUTOR attempts to delete a participant

- **GIVEN** the authenticated user is a CONTRIBUTOR
- **WHEN** they attempt to delete a participant document
- **THEN** Firestore rules MUST reject the write

#### Scenario: CONTRIBUTOR attempts to run final calculation

- **GIVEN** the authenticated user is a CONTRIBUTOR
- **WHEN** they attempt to write a calculation/settlement result
- **THEN** Firestore rules MUST reject the write

### R007: VIEWER Is Read-Only

The system MUST deny all write operations on events, expenses, and participants to VIEWERs.

**Acceptance Criteria**: VIEWER cannot create, update, or delete any document in an event.

#### Scenario: VIEWER attempts to create an expense

- **GIVEN** the authenticated user is a VIEWER on the event
- **WHEN** they attempt to create an expense
- **THEN** Firestore rules MUST reject the write

#### Scenario: VIEWER attempts to update event metadata

- **GIVEN** the authenticated user is a VIEWER
- **WHEN** they attempt to update the event
- **THEN** Firestore rules MUST reject the write

### R008: Email Verification Enforced

After Firebase Auth registration, the app MUST check `user.isEmailVerified`. If `false`, the app MUST present a verification screen and block access to main content.

**Acceptance Criteria**: Newly registered user with unverified email cannot access the main app. Verified users proceed normally.

#### Scenario: Unverified user is blocked

- **GIVEN** a user registers with email "test@example.com" but has not verified
- **WHEN** they attempt to access the main app after login
- **THEN** a verification prompt is displayed
- **AND** main navigation (event list, profile, settings) is inaccessible

#### Scenario: Verified user proceeds normally

- **GIVEN** a user has verified their email via Firebase email link
- **WHEN** they log in
- **THEN** `user.isEmailVerified` is `true`
- **AND** the main app screen loads immediately
