# Tasks: Fix Dashboard and Event Totals

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 340–400 |
| 400-line budget risk | Medium |
| Chained PRs recommended | No |
| Suggested split | Single PR (all 4 groups are small; estimate ≤400) |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: Yes
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Medium

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Groups 1+2+3+4 | PR 1 | All 4 groups combined; under 400 lines; tests included |

---

## Phase 1: Dashboard Debt Calculation Fix (R001–R003)

- [x] T-01 **RED**: Write `DashboardViewModelTest` — user's own debts excluded from `totalOwedToYou`
  - Files: `shared/src/commonTest/.../DashboardViewModelTest.kt`
  - Test: multi-person scenario: debts [B→Ana €30, Ana→Carlos €20] → `totalOwedToYou`=€30, `totalYouOwe`=€20

- [x] T-02 **GREEN**: Add `&& it.profileId != currentUserUid` to `totalOwedToYou` filter in `DashboardViewModel.kt:89`
  - Files: `shared/src/commonMain/.../DashboardViewModel.kt`
  - Verify: T-01 passes; `totalYouOwe` unchanged

- [x] T-03 **RED** → **GREEN**: Write `DashboardAggregatesTest` — `youAreOwedByEvent` excludes own debts (code already correct, test-only)
  - Files: `shared/src/commonTest/.../DashboardAggregatesTest.kt`
  - Test: event E1 debts [Ana→B €10, C→Ana €15] → `youAreOwedByEvent[E1]`=€15

- [x] T-04 **REFACTOR**: Verify `computeProfileBreakdown` unchanged; run full `DashboardViewModelTest` suite
  - Files: `shared/src/commonMain/.../DashboardViewModel.kt` (read-only)
  - Verify: all existing tests pass; no regression

## Phase 2: Events Screen Cleanup (R011–R013)

- [x] T-05 **RED** (*regression test*): Write test verifying EventsScreen renders without `BalanceSummaryCard` + filtering/search/CRUD unaffected
  - Files: new `shared/src/commonTest/.../ui/EventsScreenCleanupTest.kt` (or inline in existing EventsScreen tests)
  - Test: verify header row appears directly after Column padding (no card above)

- [x] T-06 **GREEN**: Remove `BalanceSummaryCard(...)` call (lines 163–168) + `totalPending`, `activeEventCount`, `owedEventCount` derived states (lines 113–121) from `EventsScreen.kt`
  - Files: `shared/src/commonMain/.../EventsScreen.kt`
  - Keep: `BalanceSummaryCard.kt` file (no deletion)
  - Verify: compilation succeeds; T-05 passes

- [x] T-07 **REFACTOR**: Remove unused imports; clean up `_owedEventCount` references; verify all dialogs/filters intact
  - Files: `shared/src/commonMain/.../EventsScreen.kt`
  - Verify: `./gradlew compileDebugKotlin` clean

## Phase 3: Profile Avatar Consistency (R004–R006)

- [x] T-08 **RED**: Write Compose UI tests for `ProfileAvatar` in `PreviewBreakdown`, `TransferListPanel`, `CalculatorSheet`
  - Files: new `shared/src/commonTest/.../ui/ProfileAvatarConsistencyTest.kt` (or instrumentation test)
  - Test: with/without `photoUrl`; `ProfileAvatar` renders photo vs initial fallback

- [x] T-09 **GREEN**: Replace initials Box with `ProfileAvatar(name, icon, photoUrl, 32.dp)` in `PreviewBreakdown.kt:84-105`
  - Files: `shared/src/commonMain/.../PreviewBreakdown.kt`
  - Verify: T-08 passes; both icon and photoUrl cases

- [x] T-10 **GREEN**: Add `profiles: List<ProfileItem> = emptyList()` param to `TransferListPanel`; render `ProfileAvatar(24dp)` in `TransferRow` + `BalanceRow`
  - Files: `shared/src/commonMain/.../TransferListPanel.kt`
  - Verify: avatar before name in each row

- [x] T-11 **GREEN**: Replace `"${profile.icon} ${profile.name}"` text with `Row(ProfileAvatar(...), name)` in `CalculatorSheet.kt ParameterInputRow:502`
  - Files: `shared/src/commonMain/.../CalculatorSheet.kt`
  - Verify: T-08 passes for CalculatorSheet

- [x] T-12 **REFACTOR**: Remove unused imports (Box, background, clip in PreviewBreakdown); verify no manual avatar rendering remains
  - Files: `PreviewBreakdown.kt`, `TransferListPanel.kt`, `CalculatorSheet.kt`
  - Verify: `./gradlew compileDebugKotlin` clean

## Phase 4: Calculation Snapshot Persistence (R007–R010)

- [x] T-13 **RED**: Write `CalculationSnapshot.toJson()` roundtrip test — serialize → deserialize → assert `participantBalances` and transfers match
  - Files: new `shared/src/commonTest/.../model/CalculationSnapshotPersistenceTest.kt`
  - Test: snapshot with transfers + participantBalances → roundtrip → fields equal

- [x] T-14 **RED**: Write `toCalculationSnapshot()` null safety tests — null, empty, malformed JSON → returns null
  - Files: same as T-13
  - Test: `"".toCalculationSnapshot()`, `"garbage".toCalculationSnapshot()` → null

- [x] T-15 **GREEN**: Extend `CalculationSnapshot.toJson()` (Models.kt:363) to include `"participantBalances"`; extend `String.toCalculationSnapshot()` (Models.kt:387) to parse it
  - Files: `shared/src/commonMain/.../model/Models.kt`
  - Verify: T-13 + T-14 pass

- [x] T-16 **GREEN**: Change `onApplyCalculation` in `CuentaMorososApp.kt:449` — store `result.snapshot?.toJson()` instead of `result.status?.message`
  - Files: `shared/src/commonMain/.../ui/CuentaMorososApp.kt`
  - Verify: T-13 roundtrip (real snapshot survives serialization)

- [x] T-17 **RED** (*SettlementPanel test*): Render `SettlementPanel` with mock `lastCalculationSummary` JSON → verify transfer rows + per-profile balances + read-only total cost
  - Files: new `shared/src/commonTest/.../ui/SettlementPanelPersistenceTest.kt`
  - Test: null summary → only balances; valid summary → transfers rendered in format "debtor debe X€ (Y a creditor)"

- [x] T-18 **GREEN**: Redesign `SettlementPanel` — accept `lastCalculationSummary: String?`, deserialize, render transfers below per-profile balances; make total event cost `Text` (read-only)
  - Files: `shared/src/commonMain/.../ui/SettlementPanel.kt`
  - Format: `"{debtor} debe {total}€ ({amount1} a {cred1}, {amount2} a {cred2})"`
  - Backward compat: null → show only net balances (current behavior)
  - Verify: T-17 passes

- [x] T-19 **VERIFY**: Run full test suite — `./gradlew test` — all existing + new tests pass; no regressions
  - Verify: R001–R013 acceptance criteria met
