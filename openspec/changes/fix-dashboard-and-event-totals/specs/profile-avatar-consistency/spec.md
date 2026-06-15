# Profile Avatar Consistency

## Purpose
All calculation views MUST use the `ProfileAvatar` component for displaying profile photos and fallback initials, replacing manual initials-only rendering.

## Requirements

### R004 — ProfileAvatar in PreviewBreakdown
Each profile row in `PreviewBreakdown` MUST render a `ProfileAvatar(name, emoji, photoUrl, size=32.dp)` before the profile name. The current manual initials-on-circle Box MUST be removed.

#### Scenario: Profile with photo
- GIVEN profile "Pepe" has `photoUrl = "https://example.com/photo.jpg"`
- WHEN PreviewBreakdown renders that profile row
- THEN `ProfileAvatar` displays the photo via Coil AsyncImage

#### Scenario: Profile without photo
- GIVEN profile "Luis" has no `photoUrl` and name is "Luis"
- WHEN PreviewBreakdown renders that profile row
- THEN `ProfileAvatar` shows initial "L" on a colored background

### R005 — ProfileAvatar in TransferListPanel
`TransferRow` and `BalanceRow` in `TransferListPanel` MUST render a `ProfileAvatar` (size=24.dp) before the profile name. The current text-only name rendering MUST be replaced.

#### Scenario: Transfer between two profiles
- GIVEN transfer "Ana → Carlos: €25", both profiles have avatars
- WHEN TransferListPanel renders the transfer row
- THEN both "Ana" and "Carlos" show `ProfileAvatar` with their respective photo or initial

### R006 — ProfileAvatar in CalculatorSheet ParameterInputRow
`ParameterInputRow` in `CalculatorSheet` MUST render a `ProfileAvatar` (size=24.dp) before the profile name text. The current `"${profile.icon} ${profile.name}"` text pattern MUST include the avatar.

#### Scenario: EXACT mode parameter row
- GIVEN CalculatorSheet in EXACT mode with profile "María" (emoji "👩")
- WHEN ParameterInputRow renders
- THEN the row shows `ProfileAvatar` + "María" + amount input field

## Acceptance Criteria
- [ ] `PreviewBreakdown` uses `ProfileAvatar` (32.dp) with `photoUrl` support
- [ ] `TransferRow` shows `ProfileAvatar` (24.dp) for both from/to profiles
- [ ] `BalanceRow` shows `ProfileAvatar` (24.dp)
- [ ] `ParameterInputRow` shows `ProfileAvatar` (24.dp)
- [ ] Coil AsyncImage loads photos when `photoUrl` is non-null
- [ ] Fallback to initials works when `photoUrl` is null
- [ ] No manual Box+initials rendering remains in any of the 3 components
