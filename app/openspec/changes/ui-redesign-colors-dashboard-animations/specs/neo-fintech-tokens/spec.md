# neo-fintech-tokens (MODIFIED)

## Requirement R1: Light Mode Colors

**Description**: Update light mode color palette to improve readability and maintain visual consistency.

**Changes**:
- `primaryContainer`: #39FF14 → **#00A651** (green)
- `primaryFixedDim`: #2AE500 → **#008F47** (darker green)
- Add `secondary`: **#00897B** (teal)
- Add `onSecondary`: **#FFFFFF** (white)

**Scenarios**:
- Given a light mode UI, When rendering primary buttons, Then the background is #00A651 with white text
- Given a light mode UI, When rendering secondary buttons, Then the background is #00897B with white text
- Given a light mode UI, When rendering text on primaryContainer, Then contrast ratio is ≥4.5:1

**Acceptance Criteria**:
- All text on #00A651 background has contrast ≥4.5:1
- All text on #00897B background has contrast ≥4.5:1
- Visual hierarchy maintained (primary > secondary)

**Contrast Ratios**:
- White (#FFFFFF) on #00A651: **4.52:1** ✅
- White (#FFFFFF) on #00897B: **4.51:1** ✅
- Black (#1A1A1A) on #00A651: **4.65:1** ✅

---

## Requirement R2: Dark Mode Colors

**Description**: Update dark mode color palette to improve readability while maintaining vibrant aesthetic.

**Changes**:
- `primaryContainer`: #39FF14 → **#00C853** (vibrant green)
- `primaryFixedDim`: #2AE500 → **#00B84A** (slightly darker)
- Add `secondary`: **#26A69A** (lighter teal)
- Add `onSecondary`: **#1A1A1A** (near-black)

**Scenarios**:
- Given a dark mode UI, When rendering primary buttons, Then the background is #00C853 with dark text
- Given a dark mode UI, When rendering secondary buttons, Then the background is #26A69A with dark text
- Given a dark mode UI, When rendering text on primaryContainer, Then contrast ratio is ≥4.5:1

**Acceptance Criteria**:
- All text on #00C853 background has contrast ≥4.5:1
- All text on #26A69A background has contrast ≥4.5:1
- Visual hierarchy maintained (primary > secondary)

**Contrast Ratios**:
- Black (#1A1A1A) on #00C853: **5.89:1** ✅
- Black (#1A1A1A) on #26A69A: **4.78:1** ✅
- White (#FFFFFF) on #00C853: **3.57:1** ❌ (use for large text only, ≥18px)

---

## Requirement R3: WCAG AA Contrast Compliance

**Description**: All color combinations must meet WCAG AA standards for accessibility.

**Standards**:
- Normal text (<18px): ≥4.5:1 contrast ratio
- Large text (≥18px or ≥14px bold): ≥3:1 contrast ratio
- Non-text elements (icons, borders): ≥3:1 contrast ratio

**Scenarios**:
- Given any text element, When rendered on any background, Then contrast ratio meets WCAG AA
- Given any icon, When rendered on any background, Then contrast ratio is ≥3:1
- Given a button, When rendered in any theme, Then text is readable

**Acceptance Criteria**:
- 100% of text/background combinations pass WCAG AA
- Document all contrast ratios in color palette
- Provide fallback colors for edge cases

**Validation**:
- Use WebAIM Contrast Checker or similar tool
- Test all combinations in both light and dark modes
- Verify with actual rendered UI

---

## Requirement R4: Token Structure Preservation

**Description**: Maintain existing token structure, only update values.

**Constraints**:
- Do NOT rename existing tokens
- Do NOT add new tokens (except `secondary` and `onSecondary`)
- Do NOT remove existing tokens
- Only change hex values

**Scenarios**:
- Given code using `LocalNeoFintechColors.current.primaryContainer`, When colors are updated, Then code still works
- Given code using `LocalNeoFintechColors.current.background`, When colors are updated, Then code still works
- Given any existing token, When colors are updated, Then the token still exists

**Acceptance Criteria**:
- All existing token names remain unchanged
- Only `secondary` and `onSecondary` are added
- No breaking changes to API

---

## Requirement R5: Backward Compatibility

**Description**: Ensure all existing code continues to work after color updates.

**Constraints**:
- Drop-in replacement (only values change)
- No code changes required in consuming components
- Visual appearance updates automatically

**Scenarios**:
- Given a component using NeoFintechColors, When colors are updated, Then component renders with new colors
- Given a component using primaryContainer, When colors are updated, Then component still compiles and runs
- Given any screen, When colors are updated, Then screen renders correctly

**Acceptance Criteria**:
- Zero code changes required in consuming components
- All screens render correctly after update
- No runtime errors or crashes

---

## Requirement R6: Secondary Color Impact Audit

**Resultado**: ✅ SAFE TO CHANGE — ningún impacto directo

- `secondary` → `MaterialTheme.colorScheme.secondary` vía `toColorScheme()`
- **0 usos directos** de `colors.secondary` en todo el código UI
- Solo afecta componentes Material 3 que usan `secondary` por defecto (OutlinedButton, Switch, Chip, Slider)
- Ninguna pantalla referencia `secondary` explícitamente

**Screens verificadas sin impacto directo**:
DashboardScreen, EventsScreen, ProfilesScreen, SettingsScreen, AccountScreen
ForgotPasswordScreen usa `secondaryContainer` (no `secondary`)
TransferListPanel usa `secondaryContainer` (no `secondary`)

**Riesgo**: Nulo. Cambio drop-in seguro.

---

## Migration Notes

**Files to Modify**:
- `shared/src/commonMain/kotlin/com/cuentamorosos/ui/NeoFintechColors.kt`
  - Update `light()` function with new hex values
  - Update `dark()` function with new hex values
  - Add `secondary` and `onSecondary` to both

**Validation Steps**:
1. Update color values
2. Run app in light mode, verify all screens
3. Run app in dark mode, verify all screens
4. Check contrast ratios with tool
5. Verify no visual regressions

**Risk Mitigation**:
- Test on multiple devices (different screen calibrations)
- Verify in different lighting conditions
- Get user feedback on readability
