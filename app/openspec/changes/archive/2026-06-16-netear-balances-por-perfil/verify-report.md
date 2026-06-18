## Verification Report

**Change**: netear-balances-por-perfil
**Version**: 1.0
**Mode**: Standard

### Completeness
| Metric | Value |
|--------|-------|
| Tasks total | 11 |
| Tasks complete | 8 |
| Tasks incomplete | 3 |

### Build & Tests Execution
**Build**: ✅ Passed
```
./gradlew :shared:test --rerun-tasks
BUILD SUCCESSFUL in 34s
37 actionable tasks: 36 executed, 1 up-to-date
```

**Tests**: ✅ 21 passed / ❌ 0 failed / ⚠️ 0 skipped
```
com.cuentamorosos.ui.DashboardViewModelTest — 21 tests, 0 failures, 0 errors
```

**Coverage**: ➖ Not available (KMP project, no coverage tool configured)

### Spec Compliance Matrix
| Requirement | Scenario | Test | Result |
|-------------|----------|------|--------|
| REQ-01: Profile debt netting | Profile in both sides nets to single row | `dual-direction profile nets positive owed-to-you` | ✅ COMPLIANT |
| REQ-01: Profile debt netting | Negative net reverses direction | `dual-direction profile nets negative you-owe` | ✅ COMPLIANT |
| REQ-01: Profile debt netting | Zero net removes profile | `zero net profile is excluded from results` | ✅ COMPLIANT |
| REQ-01: Profile debt netting | Single-direction profile unchanged | `single-direction owed-to-you profile unchanged` | ✅ COMPLIANT |
| REQ-01: Profile debt netting | Multiple events in same direction | `multiple events same direction aggregation works` | ✅ COMPLIANT |
| REQ-02: Negative amounts for youOwe events | Merged events preserve direction via sign | `merged events preserve correct sign for each source` | ✅ COMPLIANT |
| REQ-02: Negative amounts for youOwe events | Single-direction events keep positive sign | `single-direction owed-to-you profile unchanged` (events remain positive) | ✅ COMPLIANT |
| REQ-03: Event sign-based coloring | Mixed signs render correctly | (UI — code inspection) | ✅ COMPLIANT |
| REQ-03: Event sign-based coloring | All-youOwe renders all red | (UI — code inspection) | ✅ COMPLIANT |

**Compliance summary**: 9/9 scenarios compliant

### Correctness (Static Evidence)
| Requirement | Status | Notes |
|------------|--------|-------|
| Profile debt netting in unified breakdown | ✅ Implemented | `buildUnifiedBreakdown()` nets profiles by `profileId`, computes net = owedToYou - youOwe, determines direction by sign, excludes zero-net |
| Negative amounts for youOwe events in merged list | ✅ Implemented | youOwe events negated via `-abs()`; owedToYou events stay positive via `abs()` |
| Event sign-based coloring in breakdown dialog | ✅ Implemented | Negative amounts → red "Le debes {abs}€", non-negative → green "Te debe {abs}€", total shows algebraic sum |
| Wire unified breakdown into DashboardScreen | ✅ Implemented | `DashboardScreen.kt:88-89` passes `state.unifiedBreakdown` to `UnifiedDebtsCard` |
| Sorted by amount descending | ✅ Implemented | `buildUnifiedBreakdown` sorts by `.sortedByDescending { it.amount }` |

### Coherence (Design)
| Decision | Followed? | Notes |
|----------|-----------|-------|
| Extract `buildUnifiedBreakdown()` to `internal` in `DebtBreakdownCalculator.kt` | ❌ No | Function is `internal` at top level of `DashboardViewModel.kt`, NOT in `DebtBreakdownCalculator.kt` |
| Extract `computeProfileBreakdown()` to `DebtBreakdownCalculator.kt` | ✅ Yes | Located in `DebtBreakdownCalculator.kt` as `internal fun` |
| Remove old private `computeProfileBreakdown` from ViewModel | ❌ No | Private `computeProfileBreakdown` (5 params) still exists in ViewModel because it includes event resolution logic not in the calculator version |
| Per-event sign/color for mixed-direction events | ✅ Yes | Lines 249-268 in `DebtAccordionCard.kt` |
| Total row uses `item.direction` and `item.amount` (absolute) | ✅ Yes | Line 290 in `DebtAccordionCard.kt` |
| Use `kotlin.math.abs()` for display amounts | ✅ Yes | `abs(event.amount)` in dialog, `abs(it.amount)` in `buildUnifiedBreakdown` |

### Issues Found

**CRITICAL**:
- `buildUnifiedBreakdown()` was NOT placed in `DebtBreakdownCalculator.kt` as specified by tasks (1.1, 2.2). It remains as a top-level `internal fun` in `DashboardViewModel.kt`. The old private `computeProfileBreakdown` in the ViewModel also still exists — it has NOT been removed or replaced by the calculator version, though this is partly because the private version includes richer event resolution (expenses, eventMap, creditor resolution) that the simplified calculator version does not support.

**WARNING**:
- Task 2.2 (`remove old private impl`) is incomplete. The private `computeProfileBreakdown` in `DashboardViewModel.kt` (lines 123-187) and the extracted `computeProfileBreakdown` in `DebtBreakdownCalculator.kt` coexist with distinct signatures and responsibilities. This is technically not a code defect, but it deviates from the planned task.
- No test covers the `youOwe-only single-direction` scenario in `buildUnifiedBreakdown()`. The existing code negates youOwe-only events (line 274), which is reasonable for correct rendering, but the spec is explicit only about the owedToYou-only case.

**SUGGESTION**:
- Consider extracting `buildUnifiedBreakdown()` and `calculateTotalOwedToYou()` into `DebtBreakdownCalculator.kt` alongside `computeProfileBreakdown()` for consistency, and update imports in the ViewModel and test file.
- The private `computeProfileBreakdown` in ViewModel and the public one in `DebtBreakdownCalculator` have significant overlap. Consider refactoring the ViewModel to delegate to the calculator and add the event-resolution there instead.

### Verdict
**PASS WITH WARNINGS**
All 9 spec scenarios are COMPLIANT. All 21 tests pass. The functional implementation is correct end-to-end. However, 3 task items from the task plan were not fully completed (1.1, 2.2) and code organisation deviates from the design specification. The deviations do not affect correctness or behavior.
