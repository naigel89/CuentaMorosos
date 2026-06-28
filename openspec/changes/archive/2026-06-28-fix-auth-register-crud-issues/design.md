# Design: fix-auth-register-crud-issues

## Technical Approach

Fix three coupled regressions with a single, coherent slice:

1. **Auth errors**: keep the native Firebase call in `MainActivity`, but wrap it in a testable `signInWithRetry` helper that applies `withTimeoutOrNull`, exponential-backoff retry, and maps Firebase exceptions to user-facing strings.
2. **Register layout**: rewrite `SplashAuthScreen` and `RegisterScreen` around `Scaffold` + `WindowInsets.ime`/`navigationBars` so the register CTA is pinned above the keyboard and never requires scrolling.
3. **Permissions**: make `createdByProfileId` the single ownership key, align `PermissionEngine`, UI enable flags, and Firestore rules, and restrict participant mutations to OWNER.
4. **Error UX**: replace silent `LogSanitizer` calls in the event ViewModels with a `SharedFlow<String>` consumed as a Snackbar.

## Architecture Decisions

| Decision | Options | Trade-offs | Choice |
|---|---|---|---|
| Where to map login errors | a) Inline in `MainActivity` lambda b) Pure `AuthErrorMapper` object + `signInWithRetry` | (a) untestable, mixes UI + network; (b) keeps Firebase calls in `MainActivity` while the mapping is unit-testable | **b** |
| Login timeout/retry shape | a) `withTimeoutOrNull` around callback adapter b) Firebase `Task` timeout APIs | (a) works with Kotlin coroutines and gives a single `SignInResult`; (b) platform-specific and harder to retry | **a** |
| Layout strategy for auth screens | a) `Column` + `verticalScroll` b) `Scaffold` + `imePadding` + bottom CTA | (a) violates "no scroll" spec; (b) keeps CTA visible and respects insets | **b** |
| Ownership field | a) `paidByProfileId` b) `createdByProfileId` | (a) already used but wrong semantics (payer can change); (b) matches spec and Firestore rules | **b** |
| Enforce participant mutations | a) Only UI disable b) UI disable + Firestore rules + `PermissionEngine` | (a) drifts again; (b) three-layer alignment prevents regression | **b** |
| Error channel | a) `StateFlow` b) `SharedFlow`/`Channel` | (a) replays stale errors; (b) one-shot Snackbar-friendly events | **b** |
| Cache schema for `createdByProfileId` | a) Add SQLDelight column + migration b) Keep only in remote | (a) preserves ownership offline; (b) loses field on sync and breaks permissions | **a** |

## Data Flow

```
Auth:
  SplashAuthScreen ──onLogin──▶ MainActivity
                                      │
                                      ▼
                          ViewModel.setLoading(true)
                          signInWithRetry(auth, email, password)
                          ├── withTimeoutOrNull(10s)
                          ├── retry(transient, 3× exp. backoff)
                          └── AuthErrorMapper.map(e) ──▶ SignInResult.Error(msg)
                                      │
                                      ▼
                          ViewModel.setLoading(false)
                          onResult(error) ──▶ UI shows message + hides spinner

Expense / participant:
  EventDetailScreen ──onSaveExpense──▶ EventDetailViewModel
                          │
                          ├── setLoading(true)
                          ├── sets createdByProfileId = currentUid (new only)
                          ├── PermissionEngine.canDo(...) for edit/delete flags
                          ├── OfflineFirstExpenseRepository.saveExpense()
                          │       ├── SQLDelight upsert (createdByProfileId column)
                          │       └── remote save (permission denied → drop, don't enqueue)
                          └── setLoading(false) + error SharedFlow if needed

  SettlementPanel ──onRemoveMember──▶ EventsViewModel
                          ├── setLoading(true)
                          ├── permission pre-check
                          ├── eventRepository.removeMember() or SharedFlow error
                          └── setLoading(false)

Calculation:
  SettlementPanel ──onCalculate──▶ EventsViewModel
                          ├── PermissionEngine.canDo(RunCalculation)
                          ├── setLoading(true)
                          ├── run calculation / save result
                          └── setLoading(false)
```

## File Changes

| File | Action | Description |
|---|---|---|
| `app/src/main/java/com/cuentamorosos/MainActivity.kt` | Modify | Replace inline login callback with `signInWithRetry` + `AuthErrorMapper`. |
| `app/src/main/java/com/cuentamorosos/auth/AuthErrorMapper.kt` | Create | Pure mapper from `Throwable` to user-facing string. |
| `app/src/main/java/com/cuentamorosos/auth/SignInWithRetry.kt` | Create | `suspend` wrapper with timeout + retry + `SignInResult`. |
| `shared/.../ui/auth/SplashAuthScreen.kt` | Modify | `Scaffold` + insets, no-scroll, bottom-anchored register CTA. |
| `shared/.../ui/auth/RegisterScreen.kt` | Modify | Same inset-aware, no-scroll layout; 48dp button. |
| `shared/.../model/PermissionEngine.kt` | Modify | Use `createdByProfileId`, `ManageParticipants` OWNER-only. |
| `shared/.../ui/EventDetailScreen.kt` | Modify | Set `createdByProfileId` on new expenses; pass creator to card/sheet; hide sheet Remove when not allowed. |
| `shared/.../ui/SettlementPanel.kt` | Modify | Disable participant add/remove/invite for non-OWNER; show role badges; disable calculate/settle for non-OWNER; show loading. |
| `shared/.../ui/EventDetailViewModel.kt` | Modify | Add `SharedFlow<String>` errors; wrap save/delete expense. |
| `shared/.../ui/EventsViewModel.kt` | Modify | Add `SharedFlow<String>` errors; pre-check participant removal. |
| `shared/.../data/repository/OfflineFirstExpenseRepository.kt` | Modify | Persist `createdByProfileId`; drop permission-denied ops from queue. |
| `shared/.../data/repository/FirestoreExpenseRepository.kt` | Modify | Already serializes field; no change beyond rule alignment. |
| `shared/src/commonMain/sqldelight/.../CachedExpense.sq` | Modify | Add `createdByProfileId TEXT NOT NULL DEFAULT ''`. |
| `shared/src/androidMain/.../db/DriverFactory.android.kt` | Modify | Migrate existing DBs: `ALTER TABLE CachedExpense ADD COLUMN createdByProfileId ...`. |
| `shared/src/androidMain/.../RepositoryProvider.kt` | Modify | Reconstruct `createdByProfileId` in `drainAllBeforeLogout`. |
| `firestore.rules` | Modify | Role helpers; OWNER full access, CONTRIBUTOR own-expense update, VIEWER read-only. |
| `shared/src/commonTest/.../PermissionEngineTest.kt` | Modify | Update matrix expectations. |

## Interfaces / Contracts

```kotlin
sealed class SignInResult {
    data object Success : SignInResult()
    data class Error(val message: String) : SignInResult()
}

object AuthErrorMapper {
    fun map(throwable: Throwable): String
}

suspend fun signInWithRetry(
    auth: FirebaseAuth,
    email: String,
    password: String,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    timeoutMs: Long = 10_000,
    maxRetries: Int = 2
): SignInResult
```

`PermissionEngine` changes:

```kotlin
// Rename semantic owner parameter to creator
sealed class EventAction {
    data class EditExpense(val createdByProfileId: String) : EventAction()
    data class DeleteExpense(val createdByProfileId: String) : EventAction()
    object ManageParticipants : EventAction()
    // ...
}
```

- `EditExpense`: OWNER or (`CONTRIBUTOR` and `createdByProfileId == profileId` and non-blank).
- `DeleteExpense`: OWNER or (`CONTRIBUTOR` and `createdByProfileId == profileId` and non-blank).
- `ManageParticipants`: OWNER only.
- `RunCalculation` / `SettleDebts`: OWNER only.

ViewModel error contract:

```kotlin
private val _errorMessage = MutableSharedFlow<String>()
val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()
```

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit | `AuthErrorMapper` maps each Firebase exception to expected message | app module JUnit with fake exception subclasses |
| Unit | `signInWithRetry` timeout, retry success, retry exhaustion | `kotlinx-coroutines-test` + fake `FirebaseAuth` / `Task` adapter |
| Unit | `PermissionEngine` role matrix for expenses & participants | existing `PermissionEngineTest` updated |
| Unit | `OfflineFirstExpenseRepository` drops permission-denied ops | fake remote that throws `FirebaseFirestoreException` PERMISSION_DENIED |
| Compose | Register button visible with keyboard, 48dp touch target | Compose UI test / semantics measurement |
| Rules | Firestore rules allow OWNER/CONTRIBUTOR/VIEWER matrix | `firebase emulators:exec` if available, otherwise manual rules review |
| Integration | End-to-end expense create/edit/delete with CONTRIBUTOR | manual emulator flow against updated rules |

## Migration / Rollout

- SQLDelight: add `createdByProfileId` column with `ALTER TABLE ... ADD COLUMN` in `DriverFactory.android.kt` before `AndroidSqliteDriver` opens the DB.
- Firestore: deploy rules incrementally; no data backfill required because missing `createdByProfileId` is treated as "no edit rights for non-OWNER".
- Existing expenses with empty `createdByProfileId` remain editable only by the event OWNER until a future migration or manual repair.
- Rollback: revert commit and re-deploy previous Firestore rules.

## Loading States

Every async operation MUST expose an observable loading state:

- ViewModels expose `isLoading: StateFlow<Boolean>` (or per-action loading flags for parallel operations).
- Screens show `CircularProgressIndicator` (or a button-level `CircularProgressIndicator` inside the triggered button) while `isLoading` is true.
- Controls that trigger the active operation are disabled while loading.
- Initial data loads show a centered progress indicator; action loads show inline progress on the affected control.

Applies to: login, register, save expense, delete expense, add/remove participant, run calculation, load event list, load event detail.

## Open Questions

- [x] Should CONTRIBUTORs be allowed to delete their own expenses, or only edit them? → **Resolved**: CONTRIBUTOR may edit and delete their own expenses.
- [x] Do we add a dedicated calculation action to `PermissionEngine`, or reuse an existing action? → **Resolved**: Add `EventAction.RunCalculation` / `EventAction.SettleDebts` gated to OWNER only.
- [ ] Is a one-time Cloud Function backfill for empty `createdByProfileId` required, or is OWNER-only editing acceptable for legacy data?
