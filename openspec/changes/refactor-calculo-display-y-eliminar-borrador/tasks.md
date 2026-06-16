# Tasks: Refactor Calculation Display & Remove DRAFT State

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 350–420 |
| 400-line budget risk | Medium |
| Chained PRs recommended | No |
| Suggested split | Single PR (borderline; monitor during apply) |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Medium

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Remove DRAFT state (model + data + UI + tests) | PR 1 | ~280 lines; core of the change |
| 2 | Refactor calculation display (remove PreviewBreakdown, polish TransferListPanel) | PR 2 | ~120 lines; independent of PR 1 |

## Phase 1: RED — Failing Tests

- [ ] 1.1 Update `StateMachineTest.kt` — remove DRAFT→OPEN, CLOSED→DRAFT, `isDraft`, `stateLabel(DRAFT)` tests; add: OPEN default on creation, 3-state transitions only
  - **Files**: `shared/src/commonTest/.../model/StateMachineTest.kt`
  - **Accept**: Tests compile-fail referencing `EventState.DRAFT`

- [ ] 1.2 Update `EventValidatorTest.kt` — remove EV-05 draft warning and EV-06 draft skip tests; assert EV-05 always errors for <2 members, EV-06 always active
  - **Files**: `shared/src/commonTest/.../model/validation/EventValidatorTest.kt`
  - **Accept**: Tests fail — isDraft logic still present

- [ ] 1.3 Update `IntegrityGuardTest.kt` — remove `canDeleteExpense blocks when DRAFT` test
  - **Files**: `shared/src/commonTest/.../model/IntegrityGuardTest.kt`
  - **Accept**: DRAFT reference removed from test

- [ ] 1.4 Add test: loading persisted `"DRAFT"` string falls back to `OPEN` in `EventPersistenceTest.kt`
  - **Files**: `shared/src/commonTest/.../data/repository/EventPersistenceTest.kt`
  - **Accept**: Test fails — fallback still DRAFT

## Phase 2: GREEN — Model & Data Layer

- [ ] 2.1 Remove `DRAFT` from `EventState` enum; remove `isDraft()`; update `stateLabel()`; change `EventItem` default `state = OPEN`
  - **Files**: `shared/src/commonMain/.../model/Models.kt`
  - **Accept**: Enum has 3 values; `EventItem()` defaults to OPEN

- [ ] 2.2 Remove `DRAFT→OPEN` guard, `CLOSED→DRAFT` case, and `guardDraftToOpen()` from StateMachine
  - **Files**: `shared/src/commonMain/.../model/StateMachine.kt`
  - **Accept**: 3-state machine: OPEN→CALCULATED→CLOSED

- [ ] 2.3 Remove `isDraft` flag from EventValidator; make EV-05 always error, EV-06 always active
  - **Files**: `shared/src/commonMain/.../model/validation/EventValidator.kt`
  - **Accept**: No DRAFT references remain

- [ ] 2.4 Change `getOrDefault(DRAFT)` → `getOrDefault(OPEN)` in all 3 persistence layers
  - **Files**: `app/.../data/CuentaMorososLocalStore.kt`, `shared/.../data/repository/OfflineFirstEventRepository.kt`, `shared/.../data/repository/FirestoreEventRepository.kt`
  - **Accept**: All fallbacks default to OPEN

- [ ] 2.5 Add `"state" to state.name` to `MigrationManager.toMigrationMap()` (pre-existing bug fix)
  - **Files**: `app/.../data/MigrationManager.kt`
  - **Accept**: Migration map includes state field

## Phase 3: GREEN — UI Wiring

- [ ] 3.1 Remove "Abrir evento" button and `onOpenEvent` param from `EventDetailScreen`
  - **Files**: `shared/src/commonMain/.../ui/EventDetailScreen.kt`
  - **Accept**: No DRAFT-conditional UI remains

- [ ] 3.2 Remove `openEvent()` from `EventDetailViewModel` and `openEvent()`/`openEventConfirmed()` from `EventsViewModel`
  - **Files**: `shared/src/commonMain/.../ui/EventDetailViewModel.kt`, `shared/src/commonMain/.../ui/EventsViewModel.kt`
  - **Accept**: No openEvent methods remain

- [ ] 3.3 Remove DRAFT-transition `onOpenEvent` wiring from `CuentaMorososApp` (keep navigation `onOpenEvent`)
  - **Files**: `shared/src/commonMain/.../ui/CuentaMorososApp.kt`
  - **Accept**: Compiles; navigation onOpenEvent preserved

- [ ] 3.4 Remove DRAFT entries from `EventStateColors.kt` and `StateBadge.kt`; change `SettlementPanel` default to OPEN; change `EventsScreen` creation default to OPEN
  - **Files**: `shared/.../ui/EventStateColors.kt`, `shared/.../ui/StateBadge.kt`, `shared/.../ui/SettlementPanel.kt`, `shared/.../ui/EventsScreen.kt`
  - **Accept**: No DRAFT references in UI layer

## Phase 4: GREEN — Display Refactor

- [ ] 4.1 Remove `PreviewBreakdown(...)` call from `CalculatorSheet.kt` (lines ~363-369)
  - **Files**: `shared/src/commonMain/.../ui/CalculatorSheet.kt`
  - **Accept**: Only TransferListPanel renders after calculation

- [ ] 4.2 Delete `PreviewBreakdown.kt` (single call site removed)
  - **Files**: `shared/src/commonMain/.../ui/PreviewBreakdown.kt`
  - **Accept**: File deleted; no compile errors

- [ ] 4.3 Add card shadow + border to `TransferListPanel` outer Card (NeoFintech theme)
  - **Files**: `shared/src/commonMain/.../ui/TransferListPanel.kt`
  - **Accept**: Card has `shadow()` + `BorderStroke` matching design spec

## Phase 5: REFACTOR — Cleanup & Verification

- [ ] 5.1 Update `EventsScreenTest.kt` and `DashboardAggregatesTest.kt` — replace any DRAFT references with OPEN
  - **Files**: `shared/src/commonTest/.../ui/EventsScreenTest.kt`, `shared/src/commonTest/.../ui/DashboardAggregatesTest.kt`
  - **Accept**: No DRAFT references in test suite

- [ ] 5.2 Run `./gradlew compileDebugKotlin` — verify zero `EventState.DRAFT` references across entire codebase
  - **Accept**: Clean compile; grep confirms no DRAFT remains

- [ ] 5.3 Run full test suite `./gradlew test` — all tests pass
   - **Accept**: 0 failures

---

## Summary

| Metric | Value |
|--------|-------|
| Total tasks | 19 |
| Total estimated changed lines | 350–420 |
| Phases | 5 (RED → GREEN Model/Data → GREEN UI → GREEN Display → REFACTOR) |

## Risk Assessment

**Risk: Medium**

| Risk | Impact | Mitigation |
|------|--------|------------|
| Stored `"DRAFT"` strings in Firestore/SQLite after enum removal | High (expected) | `valueOf` + `getOrDefault(OPEN)` handles implicitly — verified in all 3 persistence layers |
| Compile breakage from missed DRAFT reference | Medium | Phase 5.2 grep + compile check catches any stragglers |
| Visual regression removing PreviewBreakdown | Low | TransferListPanel polish (4.3) compensates with shadow + border |
| MigrationManager missing state field causes state loss on sync | Medium (pre-existing) | Fixed in task 2.5 |
