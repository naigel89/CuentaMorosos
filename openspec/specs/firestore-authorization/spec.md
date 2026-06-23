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

### R002: Unauthenticated Access Denied

Firestore rules MUST deny all reads and writes when `request.auth == null`. No collection or document may be world-readable or world-writable.

**Acceptance Criteria**: Direct REST API call to Firestore without auth token returns `PERMISSION_DENIED`.

#### Scenario: Anonymous access to events is denied

- **GIVEN** an unauthenticated client (no Firebase Auth token)
- **WHEN** it attempts to read `/events/{eventId}`
- **THEN** Firestore returns `PERMISSION_DENIED`
- **AND** no data is returned

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

### R004: Event Access Restricted to Participants

Event documents MUST only be readable by authenticated users who are listed as participants in the event's `participants` map. Writes MUST be restricted by participant role (OWNER can write, CONTRIBUTOR can add expenses, READER can only read).

**Acceptance Criteria**: User not in event's `participants` map cannot read event data.

#### Scenario: Participant reads their event

- **GIVEN** user A is a participant (OWNER role) in event X
- **WHEN** they query `/events/eventX`
- **THEN** the read is allowed

#### Scenario: Non-participant cannot read event

- **GIVEN** user B is NOT in event X's `participants` map
- **WHEN** they attempt to read `/events/eventX`
- **THEN** Firestore returns `PERMISSION_DENIED`

### R005: Email Verification Enforced

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
