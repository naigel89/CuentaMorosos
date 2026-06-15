# Tasks: ui-redesign-colors-dashboard-animations

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~800–1000 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 (Colors + Animations) → PR 2 (Dashboard) → PR 3 (Other screens) |
| Delivery strategy | ask-on-risk |

---

## Phase 1: neo-fintech-tokens (Foundation)

**File**: `shared/src/commonMain/kotlin/com/cuentamorosos/ui/NeoFintechColors.kt`

| ID | Task | Lines | Deps |
|----|------|-------|------|
| 1.1 | Update `light()`: `primaryContainer` #39FF14→#00A651, `primaryFixedDim` #2AE500→#008F47 | 2 | — |
| 1.2 | Update `light()`: `secondary` #5E5E5E→#00897B | 1 | — |
| 1.3 | Add `onSecondary` = #FFFFFF to `light()` | 1 | — |
| 1.4 | Update `dark()`: `primaryContainer` #39FF14→#00C853, `primaryFixedDim` #2AE500→#00B84A | 2 | — |
| 1.5 | Update `dark()`: `secondary` #C8C5CB→#26A69A | 1 | — |
| 1.6 | Add `onSecondary` = #1A1A1A to `dark()` | 1 | — |
| 1.7 | Add `onSecondary: Color` to `NeoFintechColorSet` data class | 1 | — |
| 1.8 | Verify WCAG AA contrast (≥4.5:1) on all text/background pairs | — | 1.1–1.7 |
| 1.9 | Run full app in light + dark mode, verify no visual regressions | — | 1.8 |

---

## Phase 2: animation-system (New Components)

**File**: `shared/src/commonMain/kotlin/com/cuentamorosos/ui/NeoFintechAnimations.kt`

| ID | Task | Lines | Deps |
|----|------|-------|------|
| 2.1 | **R1**: Create `rememberAnimatedAmount(target, duration=800, prefix="", suffix="€", decimals=2): String` using `animateFloatAsState` | ~30 | — |
| 2.2 | Handle edge cases: negative values, zero value, mid-animation target change (restart from current, not 0) | ~10 | 2.1 |
| 2.3 | **R2**: Create `Modifier.fadeInStaggered(index, delayPerItem=100, fadeDuration=300, enabled=true)` using `animateFloatAsState` | ~20 | — |
| 2.4 | **R3**: Create `Modifier.slideUp(distanceDp=24, duration=400, delay=0, enabled=true)` using `animateDpAsState` | ~20 | — |
| 2.5 | **R4**: Create `rememberAnimationCoordinator(maxSimultaneous=4): AnimationCoordinator` with `requestSlot()/releaseSlot()` | ~25 | — |
| 2.6 | **R5**: Create `LocalAnimationsEnabled` CompositionLocal + `shouldAnimate()` checking system accessibility + app flag | ~15 | — |
| 2.7 | Write unit tests: AnimatedCounter interpolation, FadeInStaggered delay calc, SlideUp range, Coordinator capacity | ~60 | 2.1–2.6 |
| 2.8 | Verify animation <1s total, 60fps, no frame drops | — | 2.1–2.6 |

---

## Phase 3: dashboard-screen (Redesign)

**Files**: `DashboardScreen.kt`, `CuentaMorososApp.kt`, `AlertAccordionCard.kt`, `Models.kt`

| ID | Task | Lines | Deps |
|----|------|-------|------|
| 3.1 | **R1**: Remove "Alertas Inteligentes" section (lines 127–167) and "Todos Mis Eventos" section (lines 169–194) from DashboardScreen | -70 | — |
| 3.2 | **R7**: Remove `DashboardEventRow` composable | -55 | 3.1 |
| 3.3 | **R7**: Remove `AlertCard` composable (already dead code) | -60 | 3.1 |
| 3.4 | **R7**: Remove params: `smartAlerts`, `allEvents`, `onAlertTap`, `onEventTap` from DashboardScreen signature | -4 | 3.1 |
| 3.5 | **R7**: Delete `AlertAccordionCard.kt` (verify no other usages first) | -100 | 3.1 |
| 3.6 | **R7**: Remove `SmartAlert` + `AlertType` from Models.kt (verify no other usages) | -15 | 3.1 |
| 3.7 | **R7**: Clean up CuentaMorososApp.kt: remove `smartAlerts` computation, `allEvents` for dashboard, `onAlertTap`/`onEventTap` callbacks | -15 | 3.1–3.6 |
| 3.8 | **R2**: Create `FinancialSummaryRow(debes, teDeben, debesCount, teDebenCount)` composable — Row with 2 cards, weight(1f), AnimatedCounter for amounts, FadeInStaggered(0) and FadeInStaggered(1) | ~40 | 2.1, 2.3, 3.1 |
| 3.9 | **R2**: Create `NetBalanceCard(balance)` composable — full-width card, AnimatedCounter, green/red tint, FadeInStaggered(2) | ~30 | 2.1, 2.3, 3.8 |
| 3.10 | **R3**: Verify DebtAccordionCard renders correctly with new colors + SlideUp animation | ~5 | 2.4, 1.9 |
| 3.11 | **R4**: Wire FadeInStaggered for summary row (indices 0,1) + NetBalanceCard (index 2) + SlideUp for debt sections | ~10 | 3.8-3.10 |
| 3.12 | **R5**: Update all color references in DashboardScreen to new tokens (primaryContainer→new green, secondary→teal) | ~10 | 1.1–1.9 |
| 3.13 | **R6**: Profile render time (target <500ms), verify 60fps scroll, check memory with profiler | — | 3.1–3.12 |
| 3.14 | Build and run full app, verify dashboard layout, animations, and colors | — | 3.13 |

---

## Phase 4: Other Screens (Animation Polish)

**Files**: `EventsScreen.kt`, `ProfilesScreen.kt`, `SettingsScreen.kt`, `AccountScreen.kt`

| ID | Task | Lines | Deps |
|----|------|-------|------|
| 4.1 | Apply `slideUp()` to event cards in EventsScreen | ~5 | 2.4 |
| 4.2 | Apply `slideUp()` to profile cards in ProfilesScreen | ~5 | 2.4 |
| 4.3 | Apply `fadeInStaggered()` to settings sections (Apariencia, Recordatorios) | ~5 | 2.3 |
| 4.4 | Apply `slideUp()` to account sub-screens in AccountScreen | ~5 | 2.4 |
| 4.5 | Verify all screens: animations play once, no re-animation on scroll-back, 60fps maintained | — | 4.1–4.4 |

---

## Task Summary

| Phase | Tasks | Estimated Lines | Effort |
|-------|-------|----------------|--------|
| Phase 1 (Colors) | 9 | ~8 | 30 min |
| Phase 2 (Animations) | 8 | ~180 | 1.5h |
| Phase 3 (Dashboard) | 14 | ~-180 / +90 | 2.5h |
| Phase 4 (Polish) | 5 | ~20 | 45 min |
| **Total** | **36** | **~300 net new** | **~5h** |

---

## PR Split (Chained)

| PR | Phases | Lines | Focus |
|----|--------|-------|-------|
| PR 1 | Phase 1 + 2 | ~190 added | Foundation: colors + animation system |
| PR 2 | Phase 3 | ~90 added, ~180 deleted | Dashboard redesign + cleanup |
| PR 3 | Phase 4 | ~20 added | Animation polish on other screens |

---

## Rollback Plan

Per PR:
- **PR 1 Rollback**: Revert `NeoFintechColors.kt` hex values + remove new animation functions
- **PR 2 Rollback**: Restore DashboardScreen sections from git history
- **PR 3 Rollback**: Remove animation modifiers from other screens
