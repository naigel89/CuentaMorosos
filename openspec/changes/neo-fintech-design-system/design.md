# Design: Neo-Fintech Precision Design System

## Technical Approach

Replace the M3 accent-color system (4 hardcoded colors via `AccentColorOption` enum) with an explicit token layer (colors, typography, spacing, shapes) backed by bundled fonts. The theme becomes a single dark-mode-first design system — no runtime theme/accent switching. User preferences lose `accentColorId`; existing values are silently ignored on read (backward-compatible deserialization).

Compose Multiplatform 1.6.11 / Kotlin 1.9.24 constraints:
- `BlurEffect` is **not** available in CMP 1.6 → neon glow uses a multi-layer `drawBehind` shadow approximation.
- Font bundling uses `Font()` with `compose.components.resources` (already a dependency).

## Architecture Decisions

| Decision | Option A | Option B | Choice | Rationale |
|----------|----------|----------|--------|-----------|
| Color token shape | Single object with `light`/`dark` nested objects | Two separate objects `NeoFintechLight` / `NeoFintechDark` | **Nested objects** | One entry point `NeoFintechColors.isDark(isDark)` returns a `NeoFintechColorSet` data class — easier to pass around and test |
| Font loading | Lazy (load on first use) | Eager (register at startup) | **Eager** | Fonts are small (~700KB total), CMP caches them, avoids jank on first text render |
| Neon glow | `Modifier.drawWithContent` + `RenderEffect` (Skia) | `Modifier.drawBehind` + layered alpha shadows | **drawBehind shadows** | `RenderEffect.createBlurEffect` requires CMP 1.7+; shadow layers work on 1.6.11 and are cross-platform |
| Theme mode | Keep light/dark toggle via `isSystemInDarkTheme()` | Dark-only (remove toggle entirely) | **Keep dark toggle** | Proposal says "dark-mode-first" but `isSystemInDarkTheme()` still determines which color set to use; no user-facing toggle |
| `accentColorId` migration | DB migration to drop column | Silent ignore (field stays in data class but unused) | **Silent ignore** | `UserPreferences` is a data class serialized to JSON/SharedPreferences; removing the field breaks backward compat. Keep it, mark `@Deprecated` |

## Data Flow

```
CuentaMorososApp
    │
    ├─ preferences (UserPreferences) ──────────────────────┐
    │                                                       │
    ▼                                                       ▼
CuentaMorososTheme ──→ NeoFintechColors.isDark(systemDark) ──→ NeoFintechColorSet
    │                                                           │
    ├─ NeoFintechTypography (eager font registration)           │
    ├─ NeoFintechShapes                                         │
    └─ MaterialTheme(colorScheme = mappedColorSet,              │
                     typography = neoTypography)                │
                                                                │
All screens ────────────────────────────────────────────────────┘
    │
    ├─ MaterialTheme.colorScheme.primary → NeoFintechColors.primary
    ├─ MaterialTheme.colorScheme.surface → NeoFintechColors.surface
    └─ MaterialTheme.typography.bodyLarge → NeoFintechTypography.body
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `shared/src/commonMain/kotlin/com/cuentamorosos/ui/NeoFintechColors.kt` | Create | Color tokens: `NeoFintechColorSet` data class + `NeoFintechColors` object with `light()` / `dark()` factories. 18 tokens each. |
| `shared/src/commonMain/kotlin/com/cuentamorosos/ui/NeoFintechTypography.kt` | Create | Custom `Typography` with Geist (sans) and JetBrains Mono (code/numbers). 6 styles. |
| `shared/src/commonMain/kotlin/com/cuentamorosos/ui/NeoFintechSpacing.kt` | Create | `object NeoFintechSpacing` with Dp constants: xs=4, sm=8, md=16, lg=24, xl=32, xxl=48 |
| `shared/src/commonMain/kotlin/com/cuentamorosos/ui/NeoFintechShapes.kt` | Create | `object NeoFintechShapes` with `CornerBasedShape` constants: sm=4, md=8, lg=12, xl=16, full=Circle |
| `shared/src/commonMain/kotlin/com/cuentamorosos/ui/NeonGlowModifier.kt` | Create | `Modifier.neonGlow(color, blurRadius)` using `drawBehind` with 3 layered alpha shadows |
| `shared/src/commonMain/composeResources/font/Geist-Regular.ttf` | Create | Bundled font (download from Google Fonts / GitHub) |
| `shared/src/commonMain/composeResources/font/Geist-Medium.ttf` | Create | Bundled font |
| `shared/src/commonMain/composeResources/font/Geist-SemiBold.ttf` | Create | Bundled font |
| `shared/src/commonMain/composeResources/font/Geist-Bold.ttf` | Create | Bundled font |
| `shared/src/commonMain/composeResources/font/JetBrainsMono-Regular.ttf` | Create | Bundled font |
| `shared/src/commonMain/kotlin/com/cuentamorosos/ui/CuentaMorososTheme.kt` | Modify | Remove `ThemeModeOption`, `AccentColorOption` enums. Rewrite to use `NeoFintechColors` + `NeoFintechTypography`. Map tokens to M3 `ColorScheme`. |
| `shared/src/commonMain/kotlin/com/cuentamorosos/ui/SettingsScreen.kt` | Modify | Remove theme mode buttons (lines 78-103), accent color grid (lines 105-141), `AppearancePreviewCard` usage. Keep reminders + sign out. |
| `shared/src/commonMain/kotlin/com/cuentamorosos/ui/ScreenComponents.kt` | Modify | Remove `AppearancePreviewCard` composable entirely. |
| `shared/src/commonMain/kotlin/com/cuentamorosos/model/Models.kt` | Modify | Add `@Deprecated("No longer used — design system is fixed", ReplaceWith(""))` to `accentColorId` parameter. Keep field for backward compat. |

## Interfaces / Contracts

### NeoFintechColors

```kotlin
data class NeoFintechColorSet(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val primary: Color,          // Neon green #39FF14 (dark) / #00C853 (light)
    val primaryVariant: Color,
    val secondary: Color,        // Cyan #00E5FF
    val tertiary: Color,         // Electric purple #B388FF
    val accent: Color,           // Hot pink #FF4081
    val warning: Color,          // Amber #FFB300
    val error: Color,            // Red #FF5252
    val success: Color,          // Green #69F0AE
    val onBackground: Color,
    val onSurface: Color,
    val onPrimary: Color,
    val onSecondary: Color,
    val border: Color,
    val borderSubtle: Color,
    val glow: Color,             // Semi-transparent primary for glow effects
)

object NeoFintechColors {
    fun light(): NeoFintechColorSet = NeoFintechColorSet(...)
    fun dark(): NeoFintechColorSet = NeoFintechColorSet(...)
}
```

### M3 ColorScheme Mapping

```kotlin
// Inside CuentaMorososTheme
fun NeoFintechColorSet.toColorScheme(): ColorScheme = lightColorScheme(
    background = background,
    surface = surface,
    surfaceVariant = surfaceVariant,
    primary = primary,
    secondary = secondary,
    tertiary = tertiary,
    error = error,
    onBackground = onBackground,
    onSurface = onSurface,
    onPrimary = onPrimary,
    onSecondary = onSecondary,
    // ... remaining mappings
)
```

### NeoFintechTypography

```kotlin
val NeoFintechTypography: Typography
    get() {
        val geist = FontFamily(
            Font(Res.font.geist_regular, FontWeight.Normal),
            Font(Res.font.geist_medium, FontWeight.Medium),
            Font(Res.font.geist_semi_bold, FontWeight.SemiBold),
            Font(Res.font.geist_bold, FontWeight.Bold),
        )
        val jetbrainsMono = FontFamily(
            Font(Res.font.jetbrains_mono_regular, FontWeight.Normal),
        )
        return Typography(
            displayLarge = TextStyle(fontFamily = geist, fontWeight = FontWeight.Bold, fontSize = 57.sp),
            headlineMedium = TextStyle(fontFamily = geist, fontWeight = FontWeight.SemiBold, fontSize = 28.sp),
            titleMedium = TextStyle(fontFamily = geist, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
            bodyLarge = TextStyle(fontFamily = geist, fontWeight = FontWeight.Normal, fontSize = 16.sp),
            bodyMedium = TextStyle(fontFamily = geist, fontWeight = FontWeight.Normal, fontSize = 14.sp),
            labelMono = TextStyle(fontFamily = jetbrainsMono, fontWeight = FontWeight.Normal, fontSize = 12.sp),
        )
    }
```

### Neon Glow Modifier

```kotlin
fun Modifier.neonGlow(
    color: Color = NeoFintechColors.dark().primary,
    blurRadius: Dp = 8.dp,
    intensity: Float = 0.6f,
): Modifier = this.drawBehind {
    // 3-layer shadow approximation (no BlurEffect in CMP 1.6)
    drawContent()
    drawCircle(color.copy(alpha = intensity * 0.3f), radius = size.minDimension / 2 + blurRadius.toPx())
    drawCircle(color.copy(alpha = intensity * 0.5f), radius = size.minDimension / 2 + blurRadius.toPx() * 0.6f)
    drawCircle(color.copy(alpha = intensity * 0.8f), radius = size.minDimension / 2 + blurRadius.toPx() * 0.3f)
}
```

### NeoFintechSpacing

```kotlin
object NeoFintechSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}
```

### NeoFintechShapes

```kotlin
object NeoFintechShapes {
    val sm: CornerBasedShape = RoundedCornerShape(4.dp)
    val md: CornerBasedShape = RoundedCornerShape(8.dp)
    val lg: CornerBasedShape = RoundedCornerShape(12.dp)
    val xl: CornerBasedShape = RoundedCornerShape(16.dp)
    val full: CornerBasedShape = CircleShape
}
```

### Rewritten CuentaMorososTheme

```kotlin
@Composable
fun CuentaMorososTheme(
    preferences: UserPreferences,  // kept for API compat, accentColorId ignored
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val colorSet = if (systemDark) NeoFintechColors.dark() else NeoFintechColors.light()

    MaterialTheme(
        colorScheme = colorSet.toColorScheme(),
        typography = NeoFintechTypography,
        shapes = MaterialTheme.shapes,  // or NeoFintechShapes when screens adopt it
        content = content,
    )
}
```

### Rewritten SettingsScreen (structure)

```kotlin
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    preferences: UserPreferences,
    reminders: List<ReminderMessage>,
    onSavePreferences: (UserPreferences) -> Unit,
    onPostReminders: (List<ReminderMessage>) -> Unit,
    onSignOut: (() -> Unit)? = null,
) {
    // Local state: reminderDays, remindersEnabled only
    // NO selectedThemeMode, NO selectedAccentColor

    LazyColumn(...) {
        // StatusCard: "Ajustes" (simplified description)
        // StatusCard: "Recordatorios"
        // Toggle reminders button
        // reminderDays text field
        // Save button
        // ReminderSummaryCard (if reminders exist)
        // Send reminders button
        // HorizontalDivider + Sign out button (if onSignOut != null)
    }
}
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | `NeoFintechColors.light()` / `dark()` return distinct color sets | Assert specific token values (e.g., `dark().primary == Color(0xFF39FF14)`) |
| Unit | `NeoFintechSpacing` constants match 4px scale | Assert each Dp value |
| Unit | `UserPreferences` backward compat: JSON with `accentColorId` still deserializes | Round-trip serialization test |
| Composable | `CuentaMorososTheme` applies correct color set based on `isSystemInDarkTheme()` | Compose testing rule with dark/light config |
| Manual | Font files render correctly (no fallback to system font) | Visual inspection in preview |

## Migration / Rollout

**No migration needed.** `accentColorId` stays in `UserPreferences` as a deprecated field — existing serialized preferences deserialize without error. The field is simply never read by the new theme. On next save, the field persists its old value (harmless). A future cleanup PR can remove it entirely once all users have saved preferences at least once.

**Breaking visual change:** Users lose their saved accent color preference. This is cosmetic only and acceptable per the proposal.

## Open Questions

- [ ] Font licensing: Geist (Open Font License via Vercel) and JetBrains Mono (OFL) are both open-source — confirm no attribution file needed in `composeResources`
- [ ] Should `NeoFintechShapes` be wired into `MaterialTheme.shapes` immediately, or leave for screen-level PRs? **Decision: wire it now** — shapes are a foundational token and don't require screen changes to be effective.
- [ ] CMP 1.6.11 `Font()` loading: confirm `Res.font.geist_regular` naming convention matches the actual resource path (`composeResources/font/Geist-Regular.ttf` → `Res.font.geist_regular`). **Decision: use lowercase with underscores** — this is the CMP convention.
