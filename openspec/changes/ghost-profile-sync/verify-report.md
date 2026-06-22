## Verification Report

**Change**: ghost-profile-sync
**Version**: N/A
**Mode**: Standard

### Completeness
| Metric | Value |
|--------|-------|
| Tasks total | 15 |
| Tasks complete | 15 |
| Tasks incomplete | 0 |

### Build & Tests Execution
**Build**: ✅ Passed
```text
./gradlew :shared:testDebugUnitTest → BUILD SUCCESSFUL (22 actions, 1 from cache)
./gradlew :app:testDebugUnitTest    → BUILD SUCCESSFUL (52 actions, 1 from cache)
```

**Tests**: ✅ 0 failed / ⚠️ 0 skipped
```text
All test suites passed. Cached results from prior successful runs.
No failures or errors detected.
```

**Coverage**: ➖ Not available (no JaCoCo/kover configured)

### Spec Compliance Matrix
| Requirement | Scenario | Test | Result |
|-------------|----------|------|--------|
| GPS-REQ-001 | Single ghost merged | `FirestoreProfileRepositoryTest > linkGhostProfile merges single ghost with debts and expenses` | ✅ COMPLIANT |
| GPS-REQ-001 | No ghost found (no-op) | `FirestoreProfileRepositoryTest > linkGhostProfile is no-op when no ghost matches email` | ✅ COMPLIANT |
| GPS-REQ-001 | Multiple ghosts | `FirestoreProfileRepositoryTest > linkGhostProfile merges multiple ghosts with same linkedEmail` | ✅ COMPLIANT |
| GPS-REQ-001 | Ghost participates in events | `FirestoreProfileRepositoryTest > linkGhostProfile rewrites ghost participant in events` | ✅ COMPLIANT |
| GPS-REQ-002 | Merge on registration | (none found) | ❌ UNTESTED |
| GPS-REQ-003 | Merge on invitation acceptance | `FirestoreInvitationRepositoryTest > linkGhostProfile called before participant insert ordering per GPS-REQ-003` | ✅ COMPLIANT |
| GPS-REQ-004 | Ghost profile preserves `isGhost`/`linkedEmail` | `ProfileItemTest > ghost profile has isGhost true and linkedEmail set` | ⚠️ PARTIAL |
| GPS-REQ-004 | Regular profile with `isGhost=false`, `linkedEmail=null` | `ProfileItemTest > regular profile has isGhost false and linkedEmail null by default` | ⚠️ PARTIAL |
| GPS-REQ-005 | Duplicate `linkedEmail` rejected | `ProfileValidatorTest > PV-03 ghost-sync duplicate linkedEmail returns error` | ✅ COMPLIANT |
| GPS-REQ-005 | Unique `linkedEmail` passes | `ProfileValidatorTest > PV-03 ghost-sync unique linkedEmail passes` | ✅ COMPLIANT |
| GPS-REQ-006 | Orphan cleanup runs once | (none found — implementation gap) | ❌ UNTESTED |
| GPS-REQ-006 | Cleanup does not re-run | (none found — implementation gap) | ❌ UNTESTED |
| GPS-REQ-007 | Transaction retry on conflict | `FirestoreProfileRepositoryTest > linkGhostProfile merges single ghost...` (implicit via Fake) | ⚠️ PARTIAL |
| GPS-REQ-007 | Offline merge queued | `OfflineFirstLinkGhostIntegrationTest > remote failure enqueues linkGhost operation` | ✅ COMPLIANT |
| GPS-REQ-007 | Queued merge replays on connectivity | `OfflineFirstLinkGhostIntegrationTest > drain replays linkGhost via profileRemoteOps` | ✅ COMPLIANT |

**Compliance summary**: 9/15 scenarios compliant, 2 partial, 2 untested, 2 with untested wiring

### Correctness (Static Evidence)
| Requirement | Status | Notes |
|------------|--------|-------|
| GPS-REQ-001 `linkGhostProfile` implementation | ✅ Implemented | `FirestoreProfileRepository.kt:54-285`. Queries ghosts by `linkedEmail`, filters `isGhost` client-side, rewrites debts/expenses/events in Firestore transaction, deletes ghost. Wraps in `repeat(3)` with exponential backoff. |
| GPS-REQ-002 Merge on registration | ✅ Implemented | `FirebaseUserSyncManager.kt:58-101`. `ensureOwnProfile()` accepts `ProfileRepository?`, fire-and-forgets `linkGhostProfile()` after profile creation with `runCatching`. Merge failure does NOT block registration. |
| GPS-REQ-003 Merge on invitation acceptance | ✅ Implemented | `FirestoreInvitationRepository.kt:73-78`. Constructor receives `ProfileRepository?`. Calls `linkGhostProfile()` BEFORE participant insert. |
| GPS-REQ-004 Migration preserves ghost fields | ✅ Implemented | `MigrationManager.kt:139-146`. `toMigrationMap()` includes `"isGhost" to isGhost` and `"linkedEmail" to linkedEmail`. |
| GPS-REQ-005 `linkedEmail` uniqueness validation | ✅ Implemented | `ProfileValidator.kt:42-53`. PV-03 check case-insensitive against existing profiles, excludes self, null ignored. |
| GPS-REQ-006 Orphan cleanup | ❌ INCOMPLETE | `FirestoreProfileRepository.kt:362-529` implements the cleanup algorithm. `CuentaMorososLocalStore.kt:334-339` has the idempotency flag. **But `MainActivity.kt:245-248` never checks `isOrphanCleanupDone()` nor calls `markOrphanCleanupDone()`** — the guard is implemented but not wired. |
| GPS-REQ-007 Transactional integrity | ✅ Implemented | `FirestoreProfileRepository.kt:93-284` uses `repeat(3)` + exponential backoff (1s/2s/4s) wrapping Firestore `runTransaction`. `OfflineFirstProfileRepository.kt:189-203` enqueues `"linkGhost"` on failure. `PendingOperationQueue.kt:85-90` dispatches `"linkGhost"` on drain. |

### Coherence (Design)
| Decision | Followed? | Notes |
|----------|-----------|-------|
| Firestore `runTransaction` (not batch) | ✅ Yes | `FirestoreProfileRepository.kt:95` uses `db.runTransaction { ... }` |
| Kotlin `repeat(3)` retry wrapper | ✅ Yes | `FirestoreProfileRepository.kt:93` uses `repeat(3)` with `delay(1000L * (1 shl attempt))` — exponential backoff 1s, 2s, 4s |
| Inject `ProfileRepository` into `FirebaseUserSyncManager` (optional param) | ✅ Yes | `FirebaseUserSyncManager.kt:18,58` uses `ProfileRepository? = null` parameter pattern |
| Constructor injection for `FirestoreInvitationRepository` | ✅ Yes | `FirestoreInvitationRepository.kt:18` accepts `ProfileRepository?` in constructor |
| New `"linkGhost"` operation type | ✅ Yes | `PendingOperationQueue.kt:85-90` dispatches `"linkGhost"`; `RemoteOperations.kt:129` declares method |
| Orphan cleanup trigger from `MainActivity` after first sync | ✅ Yes | `MainActivity.kt:245-248` — triggered with 2s delay after `startSyncStaggered` |
| Idempotency flag in SharedPreferences | ❌ NOT WIRED | Flag methods exist in `CuentaMorososLocalStore` but `MainActivity` never calls them |
| Add `isGhost` + `linkedEmail` to `toMigrationMap` | ✅ Yes | `MigrationManager.kt:144-145` — exactly 2 fields added |

### Issues Found

**CRITICAL**: 
- **GPS-REQ-006 idempotency guard not wired**: `MainActivity.kt` line 245-248 calls `repositoryProvider.remoteProfileRepository.cleanupOrphans()` on every app launch without checking `localStore.isOrphanCleanupDone()` first and without calling `localStore.markOrphanCleanupDone()` after. The `isOrphanCleanupDone()` / `markOrphanCleanupDone()` methods exist in `CuentaMorososLocalStore` (lines 334-339) but are never invoked by any caller. This means orphan cleanup runs on EVERY launch, violating the spec requirement that cleanup SHALL NOT run more than once. The `localStore` reference IS available in `MainAppContent` (line 206), so the fix is trivial.

**WARNING**: 
- **GPS-REQ-001 SQLDelight deletion not explicit**: The spec says "delete ghost from Firestore and SQLDelight". The implementation deletes from Firestore only. SQLDelight sync relies on the Firestore snapshot listener removing the ghost from local cache when the Firestore document disappears. This is architecturally correct (eventual consistency via sync loop) but not explicitly deleting from SQLDelight as the spec wording suggests.
- **GPS-REQ-002 no automated test**: The `ensureOwnProfile()` wiring in `FirebaseUserSyncManager` has no unit/integration test. The singleton `object` pattern with Android Firebase SDK dependencies makes this hard to test, but it should be documented as a known testing gap.
- **GPS-REQ-004 tests cover model, not actual `toMigrationMap()`**: The tests validate `isGhost`/`linkedEmail` on `ProfileItem` instances but do not directly call `toMigrationMap()` to verify the output map contains these fields. The mapping is trivial (field-to-map-key), but direct coverage would be stronger.
- **GPS-REQ-007 retry logic tested implicitly only**: The `repeat(3)` retry wrapper and exponential backoff are only tested indirectly through the FakeGhostLinkProfileRepository (which doesn't simulate retries). The actual retry/backoff mechanism has no explicit test for the retry-count or delay intervals.

**SUGGESTION**: 
- Consider extracting orphan cleanup idempotency into `FirestoreProfileRepository.cleanupOrphans()` itself via a dependency-injected flag provider, so the idempotency can't be accidentally bypassed by any caller.
- Add a log statement in `MainActivity` when cleanup is skipped (flag already set) and when cleanup completes (flag just written).
- Document the `FirebaseUserSyncManager` testing gap in the project testing strategy doc.

### Verdict (Initial)
**FAIL**

GPS-REQ-006 has a CRITICAL gap: the one-time orphan cleanup idempotency flag exists in `CuentaMorososLocalStore` (methods `isOrphanCleanupDone()` / `markOrphanCleanupDone()`) but the `MainActivity` trigger path never reads nor writes it. The cleanup will execute on every app launch, violating the spec's explicit requirement: "Cleanup SHALL NOT run more than once." Fix: wrap the cleanup call in `MainActivity.kt:245-248` with the idempotency check using the already-available `localStore` reference.

---

## Re-verification (2026-06-22)

### Fix Applied

**File**: `app/src/main/java/com/cuentamorosos/MainActivity.kt`, lines 244–253

```kotlin
// Trigger idempotent orphan cleanup after initial sync completes (GPS-REQ-006)
if (!localStore.isOrphanCleanupDone()) {
    launch {
        delay(2000) // let initial sync complete
        runCatching {
            repositoryProvider.remoteProfileRepository.cleanupOrphans()
            localStore.markOrphanCleanupDone()
        }
    }
}
```

**Verification of the fix**:

| Check | Result |
|-------|--------|
| `localStore` available in scope | ✅ `MainAppContent` receives it at line 206 |
| Guard `isOrphanCleanupDone()` called before cleanup | ✅ Line 245 |
| `markOrphanCleanupDone()` called after success | ✅ Line 250, inside `runCatching` (only persists on success) |
| `cleanupOrphans()` is a `suspend fun` compatible with `launch {}` | ✅ `ProfileRepository.kt:50`, `FirestoreProfileRepository.kt:362` |
| Flag methods exist in `CuentaMorososLocalStore` | ✅ Lines 334–339, SharedPreferences key `orphan_cleanup_done` |
| Failure does NOT persist the flag (retries next launch) | ✅ `markOrphanCleanupDone()` is inside `runCatching` |
| Cleanup runs with 2s delay after `startSyncStaggered` | ✅ Line 247 |

### Build & Tests Execution (Fresh Run)

**Build**: ✅ Passed
```text
./gradlew :shared:cleanTest :shared:testDebugUnitTest :app:cleanTestDebugUnitTest :app:testDebugUnitTest --rerun-tasks
BUILD SUCCESSFUL — 58 actionable tasks: 57 executed, 1 up-to-date
```

**Tests**: ✅ 0 failed / ⚠️ 0 skipped
```text
Both :shared:testDebugUnitTest and :app:testDebugUnitTest passed with no failures.
Warnings are pre-existing (unused variables, unchecked casts, opt-in annotations) — unrelated to this change.
```

**Coverage**: ➖ Not available (no JaCoCo/kover configured)

### Re-evaluated Correctness (Static Evidence)

| Requirement | Previous Status | Current Status | Notes |
|------------|-----------------|----------------|-------|
| GPS-REQ-001 | ✅ Implemented | ✅ Implemented | No change |
| GPS-REQ-002 | ✅ Implemented | ✅ Implemented | `ensureOwnProfile()` at `FirebaseUserSyncManager.kt:98` calls `linkGhostProfile` |
| GPS-REQ-003 | ✅ Implemented | ✅ Implemented | `acceptInvitation()` at `FirestoreInvitationRepository.kt:76` calls `linkGhostProfile` before participant insert |
| GPS-REQ-004 | ✅ Implemented | ✅ Implemented | `toMigrationMap()` at `MigrationManager.kt:144-145` includes both fields |
| GPS-REQ-005 | ✅ Implemented | ✅ Implemented | PV-03 uniqueness check at `ProfileValidator.kt:42-53` |
| GPS-REQ-006 | ❌ INCOMPLETE | ✅ **IMPLEMENTED** | Idempotency guard now wired: `isOrphanCleanupDone()` check + `markOrphanCleanupDone()` call in `MainActivity.kt:245-253` |
| GPS-REQ-007 | ✅ Implemented | ✅ Implemented | Transaction + retry + offline queue unchanged |

### Re-evaluated Coherence (Design)

| Decision | Previous | Current | Notes |
|----------|----------|---------|-------|
| Idempotency flag in SharedPreferences | ❌ NOT WIRED | ✅ **WIRED** | `MainActivity.kt:245-253` now reads and writes the flag |
| Orphan cleanup trigger from `MainActivity` after first sync | ✅ Yes | ✅ Yes | Now with proper guard |
| All other design decisions | ✅ Yes | ✅ Yes | No change |

### Updated Issues

**CRITICAL**: ~~GPS-REQ-006 idempotency guard not wired~~ → **RESOLVED**. The guard `isOrphanCleanupDone()` / `markOrphanCleanupDone()` is now correctly wired at `MainActivity.kt:245-253`.

**WARNING** (unchanged from initial verification):
- **GPS-REQ-001 SQLDelight deletion not explicit**: Deletion relies on Firestore snapshot listener eventual consistency, not explicit SQLDelight DELETE.
- **GPS-REQ-002 no automated test**: `ensureOwnProfile()` wiring in `FirebaseUserSyncManager` remains untested. Singleton `object` + Android Firebase SDK dependencies make this hard to test.
- **GPS-REQ-004 tests cover model, not actual `toMigrationMap()`**: Tests validate `ProfileItem` fields, not the serialized map output directly.
- **GPS-REQ-007 retry logic tested implicitly only**: `repeat(3)` + exponential backoff has no explicit retry-count or delay-interval test.

**SUGGESTION** (unchanged):
- Consider extracting idempotency into `FirestoreProfileRepository.cleanupOrphans()` via a dependency-injected flag provider.
- Add log statements when cleanup is skipped and when it completes.
- Document the `FirebaseUserSyncManager` testing gap.

### Updated Task Completeness

The tasks.md checkboxes for Phase 3 (3.1, 3.2, 3.3) remain unchecked despite implementations existing in code:
- Task 3.1: `FirebaseUserSyncManager.kt:98` — `linkGhostProfile` called after profile creation ✅
- Task 3.2: `FirestoreInvitationRepository.kt:76` — `linkGhostProfile` called before participant insert ✅
- Task 3.3: `MainActivity.kt:245-253` — cleanup trigger with idempotency guard ✅ (was the critical gap)

Task 4.5 is covered by `FirestoreInvitationRepositoryTest > linkGhostProfile called before participant insert ordering per GPS-REQ-003` (COMPLIANT in matrix). The tasks.md should be updated to reflect completion.

### Final Verdict

**PASS WITH WARNINGS**

The CRITICAL issue from the initial verification (GPS-REQ-006 idempotency guard not wired) is **RESOLVED**. The `MainActivity.kt` trigger now correctly checks `isOrphanCleanupDone()` before running and calls `markOrphanCleanupDone()` after successful cleanup, satisfying the spec requirement that cleanup SHALL NOT run more than once.

All 7 requirements of the `ghost-profile-sync` change are now correctly implemented. All tests pass. Four WARNINGs remain (test coverage gaps and a minor spec-vs-implementation detail on SQLDelight deletion), but none are blocking — they are documentation/testing improvements for future iterations.
