# Design: ui-redesign-colors-dashboard-animations

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    NeoFintechColors.kt                       │
│  (primaryContainer, secondary, onSecondary, etc.)           │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                  NeoFintechAnimations.kt                     │
│  AnimatedCounter, FadeInStaggered, SlideUp                  │
│  AnimationCoordinator (max 4 simultaneous)                  │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                      DashboardScreen.kt                      │
│  FinancialSummaryRow, NetBalanceCard, DebtAccordionCard     │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│              Other Screens (Events, Profiles, etc.)          │
│  Apply animations where appropriate                         │
└─────────────────────────────────────────────────────────────┘
```

## Implementation Order

### Phase 1: neo-fintech-tokens (Foundation)
**Why first**: No dependencies, all other phases use these colors.

**Changes**:
- `NeoFintechColors.kt`: Update hex values for light/dark modes
- Add `secondary` and `onSecondary` tokens
- Verify WCAG AA compliance

**Testing**: Visual inspection on multiple screens

---

### Phase 2: animation-system (Infrastructure)
**Why second**: Dashboard depends on animations, but animations don't depend on colors.

**New Components**:

#### AnimatedCounter
```kotlin
@Composable
fun AnimatedCounter(
    targetValue: Double,
    durationMillis: Int = 800,
    formatter: (Double) -> String = { "%.2f".format(it) }
): String
```
- Uses `animateFloatAsState` for smooth interpolation
- Returns formatted string (e.g., "150.00€")
- Handles target changes mid-animation (restarts from current value)

#### FadeInStaggered
```kotlin
fun Modifier.fadeInStaggered(
    index: Int,
    delayPerItem: Int = 100,
    fadeDuration: Int = 300
): Modifier
```
- Applies fade-in with calculated delay based on index
- Total sequence: index * delayPerItem + fadeDuration
- For 5 items: 0ms, 100ms, 200ms, 300ms, 400ms delays

#### SlideUp
```kotlin
fun Modifier.slideUp(
    distance: Dp = 24.dp,
    duration: Int = 400,
    delay: Int = 0
): Modifier
```
- Translates from +distance to 0 with fade-in
- Uses ease-out easing for natural feel

#### AnimationCoordinator
```kotlin
class AnimationCoordinator {
    fun requestAnimation(): Boolean  // Returns false if at capacity
    fun releaseAnimation()
}
```
- Tracks active animations (max 4)
- Graceful degradation: items beyond cap render instantly

#### Animation Disable
```kotlin
val LocalAnimationsEnabled = staticCompositionLocalOf { true }

@Composable
fun shouldAnimate(): Boolean {
    val systemEnabled = !LocalConfiguration.current.isReduceMotionEnabled
    val appEnabled = LocalAnimationsEnabled.current
    return systemEnabled && appEnabled
}
```

**Testing**: Unit tests for each component, visual verification

---

### Phase 3: dashboard-screen (Main Feature)
**Why third**: Depends on both colors and animations.

**New Components**:

#### FinancialSummaryRow
```kotlin
@Composable
fun FinancialSummaryRow(
    debes: Double,
    teDeben: Double,
    debesCount: Int,
    teDebenCount: Int
)
```
- Two cards side-by-side (Row with weight 1f each)
- Each card shows:
  - Label ("DEBES" / "TE DEBEN")
  - AnimatedCounter for amount
  - Count of people
- Uses FadeInStaggered (index 0 and 1)

#### NetBalanceCard
```kotlin
@Composable
fun NetBalanceCard(balance: Double)
```
- Full-width card below summary row
- Shows "BALANCE NETO" label
- AnimatedCounter for balance (green if positive, red if negative)
- Uses FadeInStaggered (index 2)

#### Layout Structure
```kotlin
LazyColumn {
    item { FinancialSummaryRow(...) }  // FadeInStaggered 0,1
    item { NetBalanceCard(...) }       // FadeInStaggered 2
    item { SectionHeader("TE DEBEN") }
    items(debtBreakdown) { DebtAccordionCard(...) }  // SlideUp
    item { SectionHeader("DEBES") }
    items(oweBreakdown) { DebtAccordionCard(...) }   // SlideUp
}
```

**Removed Sections**:
- AlertasInteligentes (entire section + AlertAccordionCard)
- TodosMisEventos (entire section + event list)

**Performance Optimizations**:
- LazyColumn for virtualization
- `remember` for animated values
- Avoid recomputing totals on every recomposition
- Use `derivedStateOf` for calculated values

**Testing**: Visual verification, performance profiling

---

### Phase 4: Other Screens (Polish)
**Why last**: Nice-to-have, can be deferred if needed.

**Screens to Update**:
- EventsScreen: Add SlideUp to event cards
- ProfilesScreen: Add SlideUp to profile cards
- SettingsScreen: Add FadeInStaggered to settings sections
- AccountScreen: Add SlideUp to account sections

**Approach**:
- Apply animations conservatively (don't over-animate)
- Respect AnimationCoordinator limits
- Test performance on each screen

**Testing**: Visual verification, performance profiling

---

## Performance Strategy

### Targets
- Initial render: <500ms
- Scroll: 60fps
- Animations: 60fps
- Memory: no leaks

### Techniques

1. **Lazy Loading**
   - LazyColumn for lists
   - Only render visible items
   - Use `key` parameter for stable identity

2. **Memoization**
   - `remember` for expensive calculations
   - `derivedStateOf` for computed values
   - Avoid recomputation on every recomposition

3. **Animation Optimization**
   - Use `animateFloatAsState` (hardware-accelerated)
   - Limit to 4 simultaneous animations
   - Disable on low-end devices

4. **Memory Management**
   - No circular references
   - Clean up animation listeners
   - Use `remember` with proper keys

### Measurement
- Use Android Profiler for render time
- Use FPS meter for scroll performance
- Use Memory Profiler for leaks
- Test on mid-range device (e.g., Pixel 4a)

---

## Testing Strategy

### Unit Tests
- AnimatedCounter: test value interpolation, formatting
- FadeInStaggered: test delay calculation
- SlideUp: test distance and duration
- AnimationCoordinator: test capacity limits

### Integration Tests
- DashboardScreen: verify layout structure
- FinancialSummaryRow: verify two cards render
- NetBalanceCard: verify balance calculation

### Visual Tests
- Light mode: verify colors and contrast
- Dark mode: verify colors and contrast
- Animations: verify smooth transitions
- Accessibility: verify with reduce motion enabled

### Performance Tests
- Initial render time <500ms
- Scroll FPS = 60
- Animation FPS = 60
- No memory leaks after 10 minutes

---

## Risk Mitigation

### Risk: Animations cause jank on low-end devices
**Mitigation**:
- AnimationCoordinator limits to 4 simultaneous
- Detect low-end devices (RAM < 4GB, old CPU)
- Provide manual toggle in settings
- Respect system accessibility settings

### Risk: Color changes break existing UI
**Mitigation**:
- Only change values, not token names
- Test all screens after color update
- Verify contrast ratios with tool
- Rollback plan: revert hex values

### Risk: Dashboard performance degrades
**Mitigation**:
- Profile before and after changes
- Use LazyColumn for virtualization
- Memoize expensive calculations
- Limit simultaneous animations

### Risk: Animations feel inconsistent across screens
**Mitigation**:
- Centralize animation definitions
- Use consistent timing (800ms, 400ms, 300ms)
- Use consistent easing (ease-out)
- Document animation guidelines

---

## Summary

**3 phases, 4 capabilities, 7 files modified**

1. **neo-fintech-tokens**: Update colors (1 file, 30 min)
2. **animation-system**: Create animation infrastructure (1 file, 1.5h)
3. **dashboard-screen**: Redesign layout (1 file, 2h)
4. **Other screens**: Apply animations (4 files, 1.5h)

**Total: ~5.5 hours**

**Success criteria**:
- WCAG AA compliance
- Dashboard loads <500ms
- 60fps during animations
- No visual regressions
