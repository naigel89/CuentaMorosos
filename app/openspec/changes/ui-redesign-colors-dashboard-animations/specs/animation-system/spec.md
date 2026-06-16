# animation-system (NEW)

## Requirement R1: AnimatedCounter

**Description**: Composable composable that animates a numeric value from 0 (or current value) to a target value, returning a formatted string. 

**API**:
```kotlin
@Composable
fun rememberAnimatedAmount(
    targetValue: Double,
    durationMillis: Int = 800,
    prefix: String = "",
    suffix: String = "€",
    decimals: Int = 2,
): String
```

**Behavior**:
- Uses `animateFloatAsState` for smooth interpolation on the composable thread (no blocking)
- Interpolates from `currentTarget` to `targetValue` over `durationMillis`
- Returns a formatted string like `"150.00€"` or `"-$45.50€"`
- On target change: restarts from CURRENT animated value (not from 0), avoids jarring visual reset
- Initial render: starts from 0 and animates to target

**Examples**:
- `rememberAnimatedAmount(150.0)` → animates 0→150, returns `"150.00€"`
- `rememberAnimatedAmount(-45.50)` → animates 0→-45.50, returns `"-45.50€"`
- `rememberAnimatedAmount(1000000.0, prefix="$")` → animates 0→1000000, returns `"$1000000.00"`

**Scenarios**:
- Given the dashboard loads, When data arrives (150.00€), Then AnimatedCounter displays `"0.00€"` and animates to `"150.00€"` in 800ms
- Given the counter is at 100.00€, When target changes to 200.00€, Then it animates from 100 to 200 (not from 0)
- Given target is 0.00, When counter renders, Then it shows `"0.00€"` without animation
- Given target is negative (-50.00), When counter renders, Then it shows `"-50.00€"` after animation

**Performance**:
- Uses `animateFloatAsState` (hardware-accelerated, non-blocking)
- Recomposition cost: O(1) — only the text recomposes
- No allocations during animation (reuses formatter)

---

## Requirement R2: FadeInStaggered

**Description**: Modifier that applies fade-in animation with staggered delay based on element index.

**API**:
```kotlin
fun Modifier.fadeInStaggered(
    index: Int,
    delayPerItemMs: Int = 100,
    fadeDurationMs: Int = 300,
    enabled: Boolean = true,
): Modifier
```

**Behavior**:
- Each element starts invisible (alpha = 0)
- After `index * delayPerItemMs` delay, element fades in over `fadeDurationMs`
- Total sequence time for N items: `(N-1) * delayPerItemMs + fadeDurationMs`
- For 5 items with default: `4*100 + 300 = 700ms` (under 1s budget)
- If `enabled = false`, renders immediately at full opacity
- Uses `animateFloatAsState` internally

**Examples**:
- 3 cards with indices 0,1,2 → card 0 fades at 0ms, card 1 at 100ms, card 2 at 200ms
- Disabled mode → all cards render instantly at alpha=1

**Scenarios**:
- Given 3 cards in a list, When rendered, Then card 0 fades in at 0ms, card 1 at 100ms, card 2 at 200ms
- Given animations disabled, When list renders, Then all cards appear immediately at full opacity
- Given a 5th card is added mid-list, When it appears, Then it fades in at index 4 (400ms delay)
- Given the user scrolls past cards, When they scroll back, Then cards do NOT re-animate (stable state)

**Performance**:
- Total animation time < 1s for up to 7 items (6*100+300 = 900ms)
- Only animates items that enter composition (LazyColumn optimization)
- No per-frame allocations

---

## Requirement R3: SlideUp

**Description**: Modifier that slides content up from below with simultaneous fade-in.

**API**:
```kotlin
fun Modifier.slideUp(
    distanceDp: Float = 24f,
    durationMs: Int = 400,
    delayMs: Int = 0,
    enabled: Boolean = true,
): Modifier
```

**Behavior**:
- Content starts translated down by `distanceDp` with alpha=0
- After `delayMs`, animates to original position with alpha=1 over `durationMs`
- Uses ease-out easing for natural feel
- If `enabled = false`, renders at final position instantly
- Uses `animateFloatAsState` and `animateDpAsState` internally

**Examples**:
- A card slides up 24dp over 400ms
- With 200ms delay: card appears 200ms after entering composition, then slides up

**Scenarios**:
- Given a section enters viewport, When first rendered, Then it slides up 24dp over 400ms
- Given animations disabled, When section enters viewport, Then it appears instantly at final position
- Given a LazyColumn with 20 items, When user scrolls, Then only newly visible items animate
- Given user scrolls past and back, When items re-enter viewport, Then they do NOT re-animate

**Performance**:
- Uses Compose native `animateDpAsState` (optimized)
- No layout passes triggered by translation animation
- Compatible with LazyColumn virtualization

---

## Requirement R4: AnimationCoordinator

**Description**: Mechanism to limit simultaneous animations to 4, preventing performance degradation.

**API**:
```kotlin
@Composable
fun rememberAnimationCoordinator(maxSimultaneous: Int = 4): AnimationCoordinator

class AnimationCoordinator {
    fun requestSlot(): Int  // Returns slot number (0-3) or -1 if full
    fun releaseSlot(slot: Int)
    fun hasAvailableSlot(): Boolean
}
```

**Behavior**:
- Tracks up to `maxSimultaneous` (default 4) active animation slots
- `requestSlot()`: returns a slot number 0-3 if available, -1 if all slots taken
- `releaseSlot(slot)`: frees up the slot for reuse
- Items that fail to get a slot render immediately (no animation) — graceful degradation
- First-come-first-served: items in the viewport get priority

**Scenarios**:
- Given 3 cards animating simultaneously, When a 4th card enters viewport, Then it gets slot 3 and animates
- Given 4 cards animating, When a 5th card enters viewport, Then `requestSlot()` returns -1 and card renders instantly
- Given a card's animation completes, When `releaseSlot()` is called, Then the slot is available for the next card
- Given a slow device, When `maxSimultaneous = 2`, Then only 2 cards animate at once

**Performance**:
- Coordinator overhead: O(1) per slot request (simple counter + array)
- No allocations during animation
- Thread-safe (runs on main thread only — Compose constraint)

---

## Requirement R5: Animation Disable Mechanism

**Description**: Respect system accessibility settings and provide manual override for animation control.

**API**:
```kotlin
// CompositionLocal — provides animations enabled flag
val LocalAnimationsEnabled = staticCompositionLocalOf { true }

// Utility function to check if animations should run
@Composable
fun shouldAnimate(): Boolean {
    val systemPrefersReducedMotion = // check system accessibility
        androidx.compose.ui.platform.LocalConfiguration.current
            .isReduceMotionEnabled
    val appEnabled = LocalAnimationsEnabled.current
    return appEnabled && !systemPrefersReducedMotion
}
```

**Behavior**:
- **Layer 1 — System**: If Android accessibility "Remove animations" is ON, all animations disabled
- **Layer 2 — App override**: `LocalAnimationsEnabled` can be set to `false` per subtree
- **Layer 3 — Device detection**: (future) detect low-end devices and auto-disable
- When disabled: `fadeInStaggered(enabled=false)` renders at full opacity, `slideUp(enabled=false)` renders at final position, counters show final value instantly

**Scenarios**:
- Given user has "Remove animations" enabled in Android settings, When any screen renders, Then no animations play
- Given app sets `LocalAnimationsEnabled = false` for a subtree, When that subtree renders, Then no animations play
- Given a slow device (RAM < 4GB), When animations are auto-disabled, Then content appears instantly (future enhancement)

**Performance**:
- `shouldAnimate()` is O(1) — reads two booleans
- No runtime cost when animations are disabled
