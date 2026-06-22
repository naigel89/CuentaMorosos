# Ghost Profile Sync Specification

## Purpose

Reconcile ghost profiles into real profiles when a user registers with matching `linkedEmail`. Rewrite all UUID references, delete ghosts, fix migration, validate uniqueness, and clean up orphans.

## Requirements

| ID | Requirement | Rationale |
|----|-------------|-----------|
| GPS-REQ-001 | SHALL implement `linkGhostProfile(email, realUid)` to rewrite all debt/expense/participant UUIDs from ghost to real user, then delete ghost from Firestore and SQLDelight | Orphaned UUIDs corrupt aggregates |
| GPS-REQ-002 | SHALL call `linkGhostProfile` from `ensureOwnProfile()` after Firestore profile creation. Merge failure SHALL NOT block registration | New users must inherit ghost data |
| GPS-REQ-003 | SHALL call `linkGhostProfile` from `acceptInvitation()` before adding participant. Ghost references SHALL be rewritten before participant insertion | Prevents duplicate participation |
| GPS-REQ-004 | SHALL include `isGhost` and `linkedEmail` in `toMigrationMap()` output | Omission corrupts profile identity during migration |
| GPS-REQ-005 | SHALL validate `linkedEmail` uniqueness in `ProfileValidator`; reject duplicates with a validation error | Ambiguous merge targets cause data races |
| GPS-REQ-006 | SHALL execute idempotent orphan cleanup on first post-upgrade launch. Orphaned debts/expenses/participants referencing non-existent profile UUIDs SHALL be removed. Cleanup SHALL NOT run more than once | Existing corruption from pre-fix era must be repaired |
| GPS-REQ-007 | SHALL wrap `linkGhostProfile` in a Firestore transaction with up to 3 retries. On network failure, SHALL enqueue in `PendingOperationQueue`. Concurrent modification SHALL trigger transaction retry | Partial merges leave irrecoverable data corruption |

## Scenarios

### GPS-REQ-001: Ghost profile linking

**Single ghost merged**
- GIVEN ghost `g-1` with `linkedEmail="a@b.com"` owns 2 debts and 1 expense
- WHEN `linkGhostProfile("a@b.com", "real-123")` executes
- THEN all debts/expenses reference `real-123`; `g-1` deleted from Firestore and SQLDelight

**No ghost found (no-op)**
- GIVEN no ghost has `linkedEmail="x@y.com"`
- WHEN `linkGhostProfile("x@y.com", "real-456")` executes
- THEN no data modified; returns success

**Multiple ghosts**
- GIVEN ghosts `g-1` and `g-2` both share `linkedEmail="dup@b.com"`
- WHEN `linkGhostProfile("dup@b.com", "real-789")` executes
- THEN all references from both ghosts rewritten to `real-789`; both deleted

**Ghost participates in events**
- GIVEN ghost `g-1` is a participant in event `ev-A`
- WHEN merge rewrites `g-1` → `real-1`
- THEN participant entry in `ev-A` updated from `g-1` to `real-1`

### GPS-REQ-002: Merge on registration

- GIVEN ghost with `linkedEmail="user@mail.com"` exists
- WHEN new user registers with `"user@mail.com"` and `ensureOwnProfile()` creates their Firestore profile
- THEN `linkGhostProfile` called; ghost data transferred to new user; registration succeeds even if merge fails

### GPS-REQ-003: Merge on invitation acceptance

- GIVEN ghost `g-3` holds debts in event `ev-X`
- WHEN real user with email `"invited@mail.com"` accepts invitation to `ev-X`
- THEN ghost debts rewritten to real UID before participant added to `ev-X`; no duplicate participant entry

### GPS-REQ-004: Migration preserves ghost fields

- GIVEN SQLDelight row: `isGhost=true`, `linkedEmail="ghost@x.com"`
- WHEN `toMigrationMap()` serializes
- THEN output contains `"isGhost": true` and `"linkedEmail": "ghost@x.com"`

- GIVEN regular profile: `isGhost=false`, `linkedEmail=null`
- WHEN `toMigrationMap()` serializes
- THEN output contains `"isGhost": false` and `"linkedEmail": null`

### GPS-REQ-005: Linked email uniqueness

- GIVEN profile A has `linkedEmail="shared@mail.com"`
- WHEN profile B is created/updated with `linkedEmail="shared@mail.com"`
- THEN `ProfileValidator` returns duplicate linkedEmail error

- GIVEN no profile has `linkedEmail="unique@mail.com"`
- WHEN profile is created with `linkedEmail="unique@mail.com"`
- THEN validation passes

### GPS-REQ-006: One-time orphan cleanup

- GIVEN a debt references profile UUID `dead-1` that does not exist in the profiles collection
- AND orphan cleanup has not run before
- WHEN the app launches for the first time after upgrade
- THEN orphaned debt is removed; cleanup completion flag persisted

- GIVEN orphan cleanup has already completed
- WHEN the app launches again
- THEN no cleanup operations execute; no data is modified

### GPS-REQ-007: Transactional integrity and failure recovery

**Transaction retry on conflict**
- GIVEN a concurrent write conflicts with the merge transaction
- WHEN `linkGhostProfile` is executing
- THEN the transaction retries up to 3 times; if all fail, operation is queued in `PendingOperationQueue`

**Offline merge queued**
- GIVEN the device is offline
- WHEN `linkGhostProfile` is called
- THEN the merge operation is enqueued in `PendingOperationQueue`; executes when connectivity restores

**Queued merge replays on connectivity**
- GIVEN a merge is queued in `PendingOperationQueue`
- WHEN connectivity is restored
- THEN the queued operation executes successfully; ghost data correctly transferred

## Acceptance Criteria

- [ ] Ghost profile deleted after real user registers with matching `linkedEmail`
- [ ] All debt, expense, and participant UUID references rewritten to real user UID
- [ ] Dashboard aggregates reflect correct post-merge values
- [ ] `toMigrationMap()` preserves `isGhost` and `linkedEmail` fields
- [ ] `ProfileValidator` rejects duplicate `linkedEmail` with a validation error
- [ ] Orphan cleanup runs exactly once, is idempotent, and logs all actions
- [ ] Firestore transaction retries on conflict; queues on persistent failure
- [ ] Offline merges execute when connectivity restores without data loss
