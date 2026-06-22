# Tasks: Ghost Profile Sync

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~500 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 → PR 2 → PR 3 |
| Delivery strategy | ask-on-risk |
| Chain strategy | stacked-to-main |

Decision needed before apply: Yes — resolved to stacked-to-main
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Foundation: interface, migration, validation, local store flags | PR 1 | Base: `main` (stacked-to-main); includes tests |
| 2 | Core: linkGhostProfile transaction + cleanupOrphans + offline delegation | PR 2 | Base: `main` (after PR 1 merges); largest unit (~340 lines) |
| 3 | Wiring: callers + DI + MainActivity trigger | PR 3 | Base: `main` (after PR 2 merges); includes integration tests |

## Phase 1: Foundation

- [x] 1.1 Add `suspend fun linkGhostProfile(email: String, realUid: String)` to `RemoteOperations` interface in `PendingOperationQueue.kt`; add `"linkGhost"` → `remoteOps.linkGhostProfile(...)` dispatch in `drain()`. (GPS-REQ-007)
- [x] 1.2 Add `"isGhost"` and `"linkedEmail"` fields to `ProfileItem.toMigrationMap()` in `MigrationManager.kt`. (GPS-REQ-004)
- [x] 1.3 Add `isOrphanCleanupDone(): Boolean` and `markOrphanCleanupDone()` flag methods to `CuentaMorososLocalStore.kt` using SharedPreferences key `orphan_cleanup_done`. (GPS-REQ-006)
- [x] 1.4 Add PV-03 `linkedEmail` uniqueness check to `ProfileValidator.validate()`: reject duplicates with `ValidationError("linkedEmail duplicado", "linkedEmail")`. (GPS-REQ-005)

## Phase 2: Core Implementation

- [x] 2.1 Implement `linkGhostProfile(email, realUid)` in `FirestoreProfileRepository.kt`: query ghosts by `linkedEmail==email && isGhost==true`; wrap in `repeat(3)` + exponential backoff (1s,2s,4s) + Firestore transaction; within transaction rewrite debts/expenses/participant references to `realUid` and delete ghost docs. (GPS-REQ-001, GPS-REQ-007)
- [x] 2.2 Implement `cleanupOrphans()` in `FirestoreProfileRepository.kt`: fetch all known profile IDs, scan events/debts/expenses/participants for unknown profile references, remove orphaned entries. (GPS-REQ-006)
- [x] 2.3 Update `OfflineFirstProfileRepository.linkGhostProfile()` to try `remoteRepository.linkGhostProfile()`, enqueue `"linkGhost"` with payload `"$email|$realUid"` on failure; add `linkGhostProfile` impl to `profileRemoteOps` anonymous object. (GPS-REQ-001, GPS-REQ-007)

## Phase 3: Wiring

- [x] 3.1 Accept optional `ProfileRepository?` parameter in `FirebaseUserSyncManager.syncCurrentUser()` and `ensureOwnProfile()`; fire-and-forget `profileRepository?.linkGhostProfile(email, uid)` after profile creation. (GPS-REQ-002)
- [x] 3.2 Inject `ProfileRepository` via constructor into `FirestoreInvitationRepository`; call `linkGhostProfile(email, uid)` before participant-insert in `acceptInvitation()`. (GPS-REQ-003)
- [x] 3.3 Update `RepositoryProvider.kt`: pass `remoteProfileRepository` to `FirestoreInvitationRepository` constructor. Update `MainActivity.kt`: pass `repositoryProvider.remoteProfileRepository` to `syncCurrentUser()`, trigger `cleanupOrphans()` after `startSyncStaggered` + 2s delay. (GPS-REQ-002, GPS-REQ-003, GPS-REQ-006)

## Phase 4: Testing

- [x] 4.1 Unit test `linkGhostProfile` in `FirestoreProfileRepositoryTest.kt`: single ghost merged, no ghost (no-op), multiple ghosts, ghost participates in events — per GPS-REQ-001 scenarios.
- [x] 4.2 Unit test PV-03 in `ProfileValidatorTest.kt`: duplicate `linkedEmail` returns `ValidationResult.Failure`; unique `linkedEmail` passes; null `linkedEmail` ignored — per GPS-REQ-005.
- [x] 4.3 Unit test `toMigrationMap` in `ProfileItemTest.kt`: ghost profile serializes with `isGhost=true`/`linkedEmail`; regular profile with `isGhost=false`/`linkedEmail=null` — per GPS-REQ-004.
- [x] 4.4 Integration test in `OfflineFirstProfileRepositoryTest.kt`: mock remote failure, verify `"linkGhost"` enqueued; then drain replays via `profileRemoteOps.linkGhostProfile` — per GPS-REQ-007.
- [ ] 4.5 Integration test in existing invitation test: verify `linkGhostProfile` called before `participants` update in `acceptInvitation()` — per GPS-REQ-003.
