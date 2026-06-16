# Spec: profile-avatar-consistency

## Purpose
Ensure all UI components that display user profiles use the `ProfileAvatar` composable with proper `photoUrl` support, replacing manual avatar rendering (initials-only boxes, text-only names).

## Requirements

### Requirement: ProfileAvatar in PreviewBreakdown (32dp)
`PreviewBreakdown` MUST render profile avatars using `ProfileAvatar(name, icon, photoUrl, 32.dp)` instead of the previous initials-only colored Box.

#### Scenario: ProfileItem with photoUrl provides all avatar data
- **GIVEN** a `ProfileItem` with `name`, `icon`, and `photoUrl`
- **WHEN** `PreviewBreakdown` renders a profile row
- **THEN** the profile MUST be rendered via `ProfileAvatar` with 32dp size, receiving `name`, `icon`, and `photoUrl`

### Requirement: ProfileAvatar in TransferListPanel TransferRow + BalanceRow (24dp)
`TransferListPanel` MUST accept a `profiles: List<ProfileItem>` parameter and render `ProfileAvatar`(24dp) in both `TransferRow` and `BalanceRow` components.

#### Scenario: Profile lookup from id resolves full avatar data
- **GIVEN** a list of `ProfileItem` passed to `TransferListPanel`
- **WHEN** a `TransferRow` or `BalanceRow` needs to display a profile identified by `profileId`
- **THEN** it MUST resolve the full `ProfileItem` from the list and render `ProfileAvatar` with 24dp size

### Requirement: ProfileAvatar in CalculatorSheet ParameterInputRow (24dp)
`CalculatorSheet`'s `ParameterInputRow` MUST render the profile as `Row(ProfileAvatar(name, icon, photoUrl, 24.dp), name)` instead of the previous text-only `"${profile.icon} ${profile.name}"`.

#### Scenario: ParameterInputRow profile has name, icon and optional photoUrl
- **GIVEN** a profile with `name`, `icon`, and optional `photoUrl`
- **WHEN** `ParameterInputRow` renders the profile selector
- **THEN** it MUST display a `ProfileAvatar`(24dp) followed by the profile name text
