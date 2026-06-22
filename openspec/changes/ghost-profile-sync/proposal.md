# Proposal: Ghost Profile Sync

## Intent

When a user registers with an email matching a ghost profile's `linkedEmail`, merge the ghost's debts, expenses, and event participation into the real profile. Three critical gaps (dead `linkGhostProfile()` stub, `ensureOwnProfile()` never searches for ghosts, orphaned UUIDs corrupting aggregates) leave permanent data inconsistency.

## Scope

**In**: Implement `linkGhostProfile()` (GAP-2), call from `ensureOwnProfile()` (GAP-3) and `acceptInvitation()` (GAP-4), fix `toMigrationMap()` to preserve ghost fields (GAP-5), add `linkedEmail` uniqueness in `ProfileValidator` (GAP-8), one-time orphan cleanup (GAP-6).

**Out**: Auto-ghost-creation on invitation (GAP-1), visual ghost indicators (GAP-9), push notifications.

## Capabilities

### New Capabilities
- `ghost-profile-sync`: Ghost-to-real profile reconciliation — reference rewrite, metadata transfer, ghost deletion, orphan cleanup.

### Modified Capabilities
None.

## Approach

Implement `linkGhostProfile(email, realProfileId)` in FirestoreProfileRepository: query ghosts by linkedEmail, batch-rewrite all debt/expense/participant UUID references, transfer metadata, delete ghost from Firestore and SQLDelight. Call from `ensureOwnProfile()` post-registration and `acceptInvitation()` before adding participant. Supporting: fix `toMigrationMap()` to include `isGhost`/`linkedEmail`; add PV-03 uniqueness in `ProfileValidator`; run one-time orphan scan on first sync.

All write operations use Firestore transactions to prevent concurrent reconciliation races. Offline failures queue via existing `PendingOperationQueue`.

## Affected Areas

| File | Change |
|------|--------|
| `FirestoreProfileRepository.kt` | Implement `linkGhostProfile()` |
| `OfflineFirstProfileRepository.kt` | Delegate |
| `ProfileRepository.kt` | Keep interface method |
| `FirebaseUserSyncManager.kt` | Merge in `ensureOwnProfile()` |
| `FirestoreInvitationRepository.kt` | Merge in `acceptInvitation()` |
| `MigrationManager.kt` | Preserve ghost fields |
| `ProfileValidator.kt` | linkedEmail uniqueness |

## Risks

| Risk | Like | Mitigation |
|------|------|------------|
| Batch write limits (500 ops) | Low | Ghost-linked entries per profile are few |
| Concurrent reconciliation | Med | Firestore transaction wraps query+rewrite+delete |
| Offline merge failure | Med | Queue via PendingOperationQueue |
| Existing orphan corruption | Low | One-time idempotent cleanup |

## Rollback Plan

Remove merge calls from `ensureOwnProfile()` and `acceptInvitation()`. Firestore writes are irreversible — export ghost profiles via Firebase console before deploy for recovery.

## Dependencies

- Firestore security rules for `profiles`, `events`, `debts`, `expenses`
- SQLDelight schema already stores `isGhost`/`linkedEmail` (confirmed)

## Success Criteria

- [ ] Ghost deleted after real user registers with matching `linkedEmail`
- [ ] All debt/expense/participant references now point to real UUID
- [ ] Dashboard aggregates correct post-merge
- [ ] `toMigrationMap()` preserves ghost fields through migration
- [ ] `ProfileValidator` rejects duplicate `linkedEmail`
- [ ] Tests cover query, rewrite, delete, and both callers
- [ ] One-time cleanup fixes existing orphans on first launch
