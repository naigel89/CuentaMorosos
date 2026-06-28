# Tasks: fix-auth-register-crud-issues

## Review Workload Forecast

| Field | Value |
|---|---|
| Estimated changed lines | 600–900 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 auth → PR 2 permissions/rules → PR 3 event UI/UX |
| Delivery strategy | ask-on-risk |
| Chain strategy | stacked-to-main |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|---|---|---|---|
| 1 | Auth retry, mapper, and responsive screens | PR 1 | Base: main |
| 2 | Permission engine, rules, DB migration, repository | PR 2 | Base: PR 1 branch |
| 3 | Event detail, settlement, ViewModel UX | PR 3 | Base: PR 2 branch |

## Phase 1: Foundation

- [x] 1.1 Create `AuthErrorMapper.kt` mapping auth exceptions.
- [x] 1.2 Create `SignInWithRetry.kt` with timeout, retry, and `SignInResult`.
- [x] 1.3 Add `createdByProfileId` column to `CachedExpense.sq`.
- [x] 1.4 Add SQLDelight migration in `DriverFactory.android.kt`.
- [x] 1.5 Update `PermissionEngine.kt` `EventAction` sealed class.
- [x] 1.6 Draft `firestore.rules` skeleton.

## Phase 2: Core Implementation

- [x] 2.1 Wire `signInWithRetry` into `MainActivity.kt`.
- [x] 2.2 Implement `PermissionEngine.canDo` matrix.
- [x] 2.3 Update `OfflineFirstExpenseRepository.kt` to persist creator.
- [x] 2.4 Update `RepositoryProvider.kt` to reconstruct `createdByProfileId`.
- [x] 2.5 Complete `firestore.rules` for OWNER/CONTRIBUTOR/VIEWER/unauth.
- [x] 2.6 Add `isLoading`/`errorMessage` to event ViewModels.

## Phase 3: UI/Compose

- [x] 3.1 Update `SplashAuthScreen.kt` with `Scaffold`/`imePadding`.
- [x] 3.2 Update `RegisterScreen.kt` with `Scaffold`/`imePadding`/48dp.
- [x] 3.3 Update `EventDetailScreen.kt` to set creator and gate edit/delete.
- [x] 3.4 Update `SettlementPanel.kt` to disable non-OWNER actions.
- [x] 3.5 Add role badges in participant list (SettlementPanel.kt).

## Phase 4: Testing (RED → GREEN → REFACTOR)

- [x] 4.1 RED: `AuthErrorMapperTest` for invalid email, wrong password, disabled, unknown.
- [x] 4.2 GREEN: Run `AuthErrorMapperTest`.
- [x] 4.3 REFACTOR: Shared exception builders in `AuthErrorMapperTest`.
- [x] 4.4 RED: `SignInWithRetryTest` for timeout, retry success, retry exhaust.
- [x] 4.5 GREEN: Run `SignInWithRetryTest`.
- [x] 4.6 REFACTOR: Test dispatcher usage in `SignInWithRetryTest`.
- [ ] 4.7 RED: `PermissionEngineTest` for own/other expense and OWNER-only calc.
- [ ] 4.8 GREEN: Run `PermissionEngineTest`.
- [ ] 4.9 REFACTOR: Parameterized role matrix in `PermissionEngineTest`.
- [ ] 4.10 RED: `OfflineFirstExpenseRepositoryTest` for creator persistence and denied-op drop.
- [ ] 4.11 GREEN: Run `OfflineFirstExpenseRepositoryTest`.
- [ ] 4.12 REFACTOR: Fake cache/remote helpers in repository tests.
- [x] 4.13 RED: Compose tests for `RegisterScreen` CTA/48dp and `SplashAuthScreen` loading.
- [x] 4.14 GREEN: Run Compose tests.
- [x] 4.15 REFACTOR: Shared test helpers (`BaseComposeTest`).
- [ ] 4.16 RED: Firestore rules emulator tests for OWNER/CONTRIBUTOR/VIEWER/unauth.
- [ ] 4.17 GREEN: Run rules emulator tests.
- [ ] 4.18 REFACTOR: Rule test fixtures.

## Phase 5: Cleanup/Polish

- [x] 5.1 Remove unused auth error logging (verified — none to remove).
- [x] 5.2 Run lint on modified Kotlin and rules files (`lintDebug` — clean).
- [x] 5.3 Update KDoc for new types (`SignInResult`, `AuthErrorMapper`, `SignInWithRetry`).
