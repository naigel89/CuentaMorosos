# Animation System Specification

## Purpose

Reusable animation primitives for the Neo-Fintech design system. Provides count-up counters, staggered fade-in, and slide-up modifiers that integrate with `NeoFintechAnimations` tokens. All animations MUST be non-blocking, GPU-accelerated, and respect accessibility preferences.

## Requirements

### R001: AnimatedCounter

The system SHALL provide an `AnimatedCounter` composable that animates a numeric value from 0 to the target value with currency formatting.

- Duration: configurable, default 800ms (MUST NOT exceed 1200ms).
- Easing: `FastOutSlowInEasing` for natural deceleration.
- Format: MUST display euros with 2 decimal places (e.g., `150.50 €`).
- MUST NOT cause recomposition of parent composables during animation.
- When target value changes, animation SHALL restart from current displayed value (not from 0).

#### Scenario: Initial render with positive amount

- GIVEN `AnimatedCounter` receives `targetValue = 250.75`
- WHEN the composable enters composition
- THEN the displayed value animates from `0.00 €` to `250.75 €` over 800ms
- AND the final displayed text is `250.75 €`

#### Scenario: Target value updates

- GIVEN `AnimatedCounter` is displaying `100.00 €`
- WHEN `targetValue` changes to `350.50`
- THEN the display animates from `100.00 €` to `350.50 €` over 800ms

#### Scenario: Zero value

- GIVEN `AnimatedCounter` receives `targetValue = 0.0`
- WHEN the composable enters composition
- THEN the displayed text is `0.00 €` with no animation triggered

#### Scenario: Negative value (debt)

- GIVEN `AnimatedCounter` receives `targetValue = -120.30`
- WHEN the composable enters composition
- THEN the displayed text animates to `-120.30 €`

### R002: FadeInStaggered

The system SHALL provide a `Modifier.fadeInStaggered(index)` extension that applies a fade-in with incremental delay based on element index.

- Fade duration: 300ms.
- Stagger delay: `index * 100ms` (configurable step, default 100ms).
- Total sequence for 5 elements: 300ms + (4 × 100ms) = 700ms < 1s.
- MUST use `graphicsLayer { alpha }` to avoid layout recomposition.

#### Scenario: List of 4 items fades in

- GIVEN 4 composables with `fadeInStaggered(index)` where index = 0,1,2,3
- WHEN they enter composition
- THEN item 0 starts immediately, item 1 at 100ms, item 2 at 200ms, item 3 at 300ms
- AND each fades from alpha 0→1 over 300ms
- AND the last item reaches full opacity at 600ms

#### Scenario: Single item (index 0)

- GIVEN one composable with `fadeInStaggered(0)`
- WHEN it enters composition
- THEN it fades in immediately with 0ms delay, completing at 300ms

#### Scenario: Item removed mid-animation

- GIVEN items are mid-fade-in
- WHEN an item leaves composition before completing
- THEN no crash or orphaned animation occurs

### R003: SlideUp

The system SHALL provide a `Modifier.slideUp()` extension that translates the composable upward while fading in.

- Distance: 24dp (within 20–30dp range).
- Duration: 400ms.
- Easing: `FastOutSlowInEasing` (ease-out equivalent).
- MUST use `graphicsLayer { translationY; alpha }` — no layout invalidation.

#### Scenario: Element slides into view

- GIVEN a composable with `slideUp()` modifier
- WHEN it enters composition
- THEN it starts 24dp below final position at alpha 0
- AND arrives at final position at alpha 1 after 400ms with ease-out curve

#### Scenario: Element already visible (recomposition)

- GIVEN a composable with `slideUp()` that has already completed animation
- WHEN a parent recomposition occurs
- THEN the element stays at final position — no re-animation

### R004: Performance Guarantees

All animation primitives SHALL meet strict performance constraints.

- Every individual animation MUST complete in ≤1000ms.
- Frame rate MUST NOT drop below 60fps during animation sequences.
- MUST use `animateFloatAsState` / `animateDpAsState` (Compose-native, GPU-accelerated).
- MUST NOT trigger layout passes — all visual changes via `graphicsLayer`.
- Concurrent animations MUST be capped at 4 simultaneous active animations per screen.

#### Scenario: Dashboard with multiple animations

- GIVEN DashboardScreen with AnimatedCounter + 3 fadeInStaggered cards + slideUp header
- WHEN all animations trigger simultaneously
- THEN frame rate stays ≥60fps (no jank/stutter)
- AND all animations complete within 1s

#### Scenario: Animation count exceeds cap

- GIVEN 6 items with `fadeInStaggered` enter composition
- WHEN the 4-item cap is reached
- THEN items 5–6 render immediately at full opacity (no animation)

### R005: Animation Disable Mechanism

The system SHALL provide a mechanism to disable all animations globally.

- MUST respect Android `Settings.Global.ANIMATOR_DURATION_SCALE == 0` (system accessibility setting).
- MUST expose a `LocalAnimationsEnabled` CompositionLocal (default: `true`).
- When disabled, all animated values MUST render at their final state immediately.
- SHOULD detect low-end devices and auto-disable (heuristic: available RAM < 2GB or API level < 26).

#### Scenario: System animations disabled

- GIVEN device has "Remove animations" enabled in accessibility settings
- WHEN `AnimatedCounter`, `fadeInStaggered`, or `slideUp` are used
- THEN all values render at final state immediately — no animation

#### Scenario: Manual disable via CompositionLocal

- GIVEN `CompositionLocalProvider(LocalAnimationsEnabled provides false)`
- WHEN animated composables render within this scope
- THEN all animations are skipped, final values displayed instantly

#### Scenario: Low-end device auto-disable

- GIVEN a device with < 2GB available RAM
- WHEN the app starts
- THEN `LocalAnimationsEnabled` resolves to `false` automatically
