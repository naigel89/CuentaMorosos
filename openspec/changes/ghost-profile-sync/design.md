# Design: Ghost Profile Sync

## Technical Approach

Implement `linkGhostProfile()` in `FirestoreProfileRepository` as a Firestore transaction (read-then-write atomicity per GPS-REQ-007). Wire two callers: `FirebaseUserSyncManager.ensureOwnProfile()` (app module, Android Firebase) post-registration, and `FirestoreInvitationRepository.acceptInvitation()` (shared module, KMP Firebase) pre-participant-insert. Offline resilience via `PendingOperationQueue` with new `"linkGhost"` operation type. Supporting fixes: migration map, validator, one-time orphan cleanup.

## Architecture Decisions

| Decision | Option A | Option B | Choice | Rationale |
|----------|----------|----------|--------|-----------|
| Firestore write consistency | `runTransaction` (reads+atomic writes) | `batch().commit()` (blind writes) | **Transaction** | Ghost merge is read-then-write: query ghosts, rewrite references, delete ghosts. A concurrent merge on same email could produce dangling writes with batches. Transaction retries on conflict. |
| Retry mechanism | Firestore built-in retry (up to 5) | Kotlin `repeat(3)` wrapper | **Kotlin `repeat(3)`** | Explicit control. Firestore retry count not configurable in KMP library. Wrap transaction in `repeat(3)` catching `FirebaseFirestoreException` with exponential backoff (1s, 2s, 4s). |
| Call from `ensureOwnProfile()` | Inject KMP `ProfileRepository` into `FirebaseUserSyncManager` | Move profile creation from Android to KMP Firebase | **Inject ProfileRepository** | `FirebaseUserSyncManager` is app-module code using Android Firebase SDK for `users` collection writes. Adding optional `ProfileRepository?` parameter is minimal. Migration to KMP Firebase is out of scope. |
| Call from `acceptInvitation()` | Inject `ProfileRepository` via constructor | Pass as method parameter | **Constructor injection** | `FirestoreInvitationRepository` already has no dependencies beyond Firebase singletons. Constructor injection follows existing `RepositoryProvider` wiring pattern. |
| Offline queue operation | New `"linkGhost"` op + `RemoteOperations.linkGhostProfile()` | Reuse `"save"` operation | **New operation** | Semantically distinct: linkGhost merges then deletes, save only upserts. Existing `"save"` dispatches to `saveProfile()` which is wrong for this. |
| Orphan cleanup trigger | `CuentaMorososApp.onCreate()` | After first sync in `OfflineFirstProfileRepository.startSyncLoop()` | **After first sync** | Cleanup needs Firestore access (available after login) AND complete profile list (available after first sync drain). Trigger from `MainActivity` after `startSyncStaggered` completes. Idempotency flag in SharedPreferences (`CUENTA_MOROSOS_STORE`). |
| Migration `toMigrationMap` | Add `isGhost` + `linkedEmail` fields | Full rewrite | **Add 2 fields** | Minimal fix per GPS-REQ-004. Existing map already serializes 4 fields; add 2 more following same pattern. |

## Data Flow

### Registration → Ghost Merge

```
MainActivity (login)
  └─ repositoryProvider.startSyncStaggered()
  └─ FirebaseUserSyncManager.syncCurrentUser(profileRepository = ...)
       └─ ensureOwnProfile(profileRepository)          // Android Firestore
            ├─ db.collection("profiles").set()         // creates real profile
            └─ fireAndForget { profileRepository.linkGhostProfile(email, uid) }
                 └─ KMP FirestoreProfileRepository
                      ├─ query ghosts: where linkedEmail==email, isGhost==true
                      ├─ tx: rewrite debts, expenses, participants → real uid
                      ├─ tx: delete ghost docs from profiles collection
                      └─ on fail → enqueue PendingOperationQueue
```

### Invitation Acceptance → Ghost Merge

```
FirestoreInvitationRepository.acceptInvitation(invitation)
  ├─ runCatching {
  │    val uid = auth.currentUser.uid
  │    val email = auth.currentUser.email
  │    // PRE-INSERT: merge ghosts first (GPS-REQ-003)
  │    profileRepository.linkGhostProfile(email, uid)
  │    // THEN add participant
  │    db.collection("events").document(eventId).update(participants: +uid)
  │  }
```

### Offline Merge → Queued → Replay

```
OfflineFirstProfileRepository.linkGhostProfile(email, uid)
  ├─ try: remoteRepository.linkGhostProfile(email, uid)  // KMP Firestore
  └─ catch: pendingQueue.enqueue(
       id = "linkghost_${uid}_${ts}",
       entityType = "profile",
       entityId = uid,
       operation = "linkGhost",
       payload = "$email|$uid"
     )

// On connectivity restore (sync loop):
OfflineFirstProfileRepository.startSyncLoop()
  └─ pendingQueue.drainAll(profileRemoteOps)
       └─ "linkGhost" → remoteOps.linkGhostProfile(email, realUid)  // from payload
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `shared/.../FirestoreProfileRepository.kt` | Modify | Implement `linkGhostProfile()` — query ghosts, run transaction to rewrite all references, delete ghosts. Add `cleanupOrphans()` method. |
| `shared/.../OfflineFirstProfileRepository.kt` | Modify | `linkGhostProfile()` delegates to remote, enqueues with `"linkGhost"` operation on failure. Add `linkGhostProfile` to `profileRemoteOps`. |
| `shared/.../ProfileRepository.kt` | No change | Interface already declares `linkGhostProfile()`. |
| `app/.../FirebaseUserSyncManager.kt` | Modify | `syncCurrentUser()` and `ensureOwnProfile()` accept optional `ProfileRepository?`. Call `linkGhostProfile()` after profile creation (non-blocking). |
| `shared/.../FirestoreInvitationRepository.kt` | Modify | Constructor receives `ProfileRepository`. In `acceptInvitation()`, call `linkGhostProfile()` before participant insert. |
| `app/.../MigrationManager.kt` | Modify | `ProfileItem.toMigrationMap()`: add `"isGhost"` and `"linkedEmail"` fields. |
| `shared/.../ProfileValidator.kt` | Modify | Add PV-03: `linkedEmail` uniqueness check against existing profiles, new `ValidationError`. |
| `shared/.../PendingOperationQueue.kt` | Modify | `drain()`: add `"linkGhost"` → `remoteOps.linkGhostProfile(...)` dispatch. |
| `shared/.../RemoteOperations` (in PendingOperationQueue.kt) | Modify | Add `suspend fun linkGhostProfile(email: String, realUid: String)`. |
| `app/.../CuentaMorososLocalStore.kt` | Modify | Add `isOrphanCleanupDone()` / `markOrphanCleanupDone()` flag methods. |
| `shared/androidMain/.../RepositoryProvider.kt` | Modify | Pass `remoteProfileRepository` into `FirestoreInvitationRepository` constructor. Pass into `FirebaseUserSyncManager` call. |
| `app/.../MainActivity.kt` | Modify | Trigger orphan cleanup after sync start. Pass `repositoryProvider.remoteProfileRepository` to `syncCurrentUser()`. |

## Transaction Scope (linkGhostProfile)

For each ghost profile found (query: `linkedEmail == email AND isGhost == true`), within a single Firestore transaction:

1. **Debts**: Query `events/*/debts where profileId == ghostId OR creditorId == ghostId` → update `profileId`/`creditorId` fields to real UID
2. **Expenses**: Query `events/*/expenses` → check `assignedProfileIds`, `profileWeights`, `paidByProfileId`, `debtorIds`, `payerContributions` for ghostId → rewrite to real UID
3. **Events (participants)**: Query `events where participantIds contains ghostId` → rewrite `participants[].profileId`, `participantIds`, `memberIds`, and `ownerId` fields
4. **Delete ghost**: `db.collection("profiles").document(ghostId).delete()`

If `linkGhostProfile` runs online (no ghosts, all ops succeed), also delete ghost from SQLDelight via local cache update.

## Orphan Cleanup (GPS-REQ-006)

**Trigger**: `MainActivity`, after `startSyncStaggered` + 2-second delay for initial sync to complete.

**Idempotency**: `CuentaMorososLocalStore` flag `orphan_cleanup_done` in SharedPreferences. Read before, write after success. Never marks done on failure (retry next launch).

**Algorithm**:
1. Fetch all known profile IDs from `profiles` collection
2. For each event the user can see (ownerId/memberIds/participantIds queries):
   - Debts: if `profileId` or `creditorId` not in known IDs → delete debt
   - Expenses: if any `assignedProfileIds`, `paidByProfileId`, `debtorIds`, or `profileWeights` keys reference unknown IDs → update to remove orphaned references
   - Events: if `participantIds` or `memberIds` contain unknown IDs → update participants array and IDs

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | `linkGhostProfile` query, rewrite, delete logic | Mock `Firebase.firestore`, verify transaction operations |
| Unit | `ProfileValidator` PV-03 uniqueness | Pass lists with duplicate `linkedEmail`, assert `ValidationResult.Failure` |
| Unit | `toMigrationMap` preserves isGhost/linkedEmail | Create ProfileItem with ghost=true, assert serialized map contains fields |
| Integration | Offline → enqueue → replay | Test `OfflineFirstProfileRepository.linkGhostProfile` with mock failing remote, verify enqueue, then drain |
| Integration | `acceptInvitation` calls merge before insert | Mock Firestore, verify `linkGhostProfile` called before document update |

## Open Questions

None.
