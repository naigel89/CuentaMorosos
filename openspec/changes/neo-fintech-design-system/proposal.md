# Proposal: Neo-Fintech Precision Design System

## Intent

Replace the current generic Material3 theming (4 arbitrary accent colors: Rose/Blue/Green/Amber) with the Neo-Fintech Precision design system (NFR0001B1). This establishes a consistent, branded visual foundation with explicit color tokens, custom typography (Geist + JetBrains Mono), 4px spacing scale, and neon glow effects — required before any screen-level redesigns (PR2+) can proceed.

## Scope

### In Scope
- `NeoFintechColors.kt`: 15+ explicit color tokens for light/dark (neon green #39FF14 primary, semantic surface/outline/error tokens)
- Font resources: Bundle Geist (Regular, Medium, Semibold, Bold) + JetBrains Mono (Regular, Medium) in `composeResources/font/`
- `NeoFintechTypography.kt`: 6 custom typography styles (display, headline, title, body, label, mono)
- `NeoFintechSpacing.kt`: 4px-based spacing scale object (xs=4, sm=8, md=16, lg=24, xl=32, xxl=48)
- `NeoFintechShapes.kt`: Border radius token object (sm=4, md=8, lg=12, xl=16, full=9999)
- `NeonGlowModifier.kt`: Glow effect modifier for dark mode (drawWithContent + Shadow/Blur; stub if Compose Multiplatform limitations block full implementation)
- Rewrite `CuentaMorososTheme.kt`: Use new tokens, remove `ThemeModeOption` and `AccentColorOption` enums, dual light/dark scheme with explicit Neo-Fintech palette
- Update `SettingsScreen.kt`: Remove theme mode and accent color selection controls temporarily (preserves reminder controls)
- Update `CuentaMorososApp.kt`: Adjust theme invocation (no longer passes accent color from preferences)
- Update `UserPreferences`: Remove `accentColorId` field (breaking change — migrated to default)
- Update `AppearancePreviewCard`: Remove accent color preview, simplify to theme mode only

### Out of Scope
- Screen-level redesigns (Dashboard, Events, Detail, Calendar, Profiles) — PR2+
- Animation system (count-up, staggered lists, SharedAxis transitions) — future PR
- SettingsScreen full redesign — deferred to when new theme controls are designed
- iOS-specific font bundling adjustments — handled when iOS target is active

## Capabilities

### New Capabilities
- `neo-fintech-tokens`: Color, typography, spacing, and shape token system for Neo-Fintech Precision design language
- `neon-glow-effect`: Decorative glow modifier for dark mode accent elements

### Modified Capabilities
- `theme-management`: Replaces generic M3 accent color system with fixed Neo-Fintech dual theme; removes user-selectable accent colors

## Approach

1. **Token layer first** — Create `NeoFintechColors.kt` with explicit light/dark token objects. Each token is a named `Color` constant, not derived from M3. This ensures tokens are portable and testable.
2. **Typography** — Bundle font files in KMP `composeResources/font/` (standard Compose Multiplatform resource location). Define `NeoFintechTypography` using `Typography()` constructor with custom `FontFamily` for Geist (sans) and JetBrains Mono (code/numbers).
3. **Spacing & Shapes** — Simple Kotlin objects with `Dp` constants. No Compose integration needed — these are consumed by call sites.
4. **Neon glow** — Attempt `Modifier.drawWithContent { drawContent(); drawRect(..., blur = ...) }` pattern. If `BlurEffect` is unavailable in the Compose Multiplatform version, ship a stub that applies a colored shadow via `GraphicsLayer` with `shadow` — documented as `TODO` for future enhancement.
5. **Theme rewrite** — `CuentaMorososTheme.kt` becomes the integration point: constructs `MaterialTheme` with Neo-Fintech `colorScheme`, `typography`, and `shapes`. Removes old enums entirely.
6. **Settings cleanup** — Remove theme/accent UI blocks from `SettingsScreen`. Reminder controls remain functional. `AppearancePreviewCard` simplified.
7. **Model update** — `UserPreferences.accentColorId` removed. Existing persisted data with old accent IDs is safe (field simply ignored during migration, then removed).

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `shared/.../ui/NeoFintechColors.kt` | New | 15+ color tokens for light/dark themes |
| `shared/.../ui/NeoFintechTypography.kt` | New | Custom typography scale with Geist + JB Mono |
| `shared/.../ui/NeoFintechSpacing.kt` | New | 4px-based spacing scale constants |
| `shared/.../ui/NeoFintechShapes.kt` | New | Border radius token constants |
| `shared/.../ui/NeonGlowModifier.kt` | New | Glow effect modifier (or stub) |
| `shared/.../composeResources/font/` | New | Geist + JetBrains Mono .ttf/.otf files |
| `shared/.../ui/CuentaMorososTheme.kt` | Modified | Full rewrite — integrates new tokens, removes old enums |
| `shared/.../ui/SettingsScreen.kt` | Modified | Remove theme/accent controls, keep reminders |
| `shared/.../ui/CuentaMorososApp.kt` | Modified | Update theme invocation |
| `shared/.../ui/ScreenComponents.kt` | Modified | Simplify `AppearancePreviewCard` |
| `app/.../model/Models.kt` | Modified | Remove `accentColorId` from `UserPreferences` |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Font files increase APK size (~700KB) | Medium | Acceptable for brand identity; can subset fonts later if needed |
| Neon glow via `drawWithContent + Blur` not available in CMP | Medium | Ship stub with documented TODO; use `shadow` fallback |
| Breaking change: users lose accent color preference | Low | Preference was cosmetic; migration is silent field removal |
| M3 auto-derived colors (surfaceVariant, etc.) clash with Neo-Fintech palette | Medium | Explicitly override all semantic tokens in `colorScheme` |

## Rollback Plan

1. `git revert` the entire change — all new files are self-contained, modified files retain M3 compatibility
2. Restore `ThemeModeOption` and `AccentColorOption` enums from git history
3. Restore `UserPreferences.accentColorId` field — persisted preferences will still contain old accent IDs, which `AccentColorOption.fromId()` will resolve correctly
4. No database migration needed — only a model field removal, which is forward-compatible

## Dependencies

- None. This is a foundational change with no external prerequisites.
- Font files (Geist, JetBrains Mono) must be downloaded and placed in `composeResources/font/` before implementation.

## Success Criteria

- [ ] App compiles and runs with Neo-Fintech theme on Android
- [ ] Light and dark modes render with correct Neo-Fintech tokens (not M3 defaults)
- [ ] Typography uses Geist for body/headlines, JetBrains Mono for code/numbers
- [ ] SettingsScreen no longer shows theme/accent selectors
- [ ] No compilation warnings about unused imports or deprecated enums
- [ ] `AppearancePreviewCard` compiles without referencing removed enums
- [ ] Neon glow modifier exists (functional or documented stub)
