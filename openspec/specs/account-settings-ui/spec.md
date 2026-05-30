# Account Settings UI Specification

## Purpose
Provide the user interface for managing account/profile settings: avatar photo, display name, unique username, and security (password). Three sub-screens accessible from the SettingsScreen.

## Requirements

### R-001: "Mi perfil" entry point in SettingsScreen
**Priority**: P0
**Description**: The SettingsScreen MUST include a "Mi perfil" section that navigates to AccountScreen. This section MUST show the user's current profile (avatar, display name, @username if set) and use the same state-based overlay navigation pattern as EventDetailScreen.
**Rationale**: Users need a clear entry point to profile settings; consistent navigation pattern reduces cognitive load.
**Scenario**:
  - Given the user is on SettingsScreen When they tap "Mi perfil" Then the AccountScreen overlay opens with an animated transition showing the user's current avatar, display name, and @username.

### R-002: AccountScreen with three sub-screens
**Priority**: P0
**Description**: The AccountScreen MUST contain three sub-screens navigated via `AnimatedContent`: NamePhotoScreen (avatar + display name editing), UsernameScreen (unique @username with validation), SecurityScreen (password change). Each sub-screen MUST have its own state management scoped to AccountViewModel.
**Rationale**: Three distinct concerns in account management; sub-navigation keeps each flow focused and testable.
**Scenarios**:
  - Given the user opens AccountScreen and taps "Cambiar foto o nombre" When the NamePhotoScreen renders Then it shows the current avatar (photo or emoji), a display name text field, and a "Guardar" button.
  - Given the user opens AccountScreen and taps "Nombre de usuario" When the UsernameScreen renders Then it shows the current @username (or empty), a text field with debounced validation, an availability indicator, and a "Guardar" button.
  - Given the user opens AccountScreen and taps "Seguridad" When the SecurityScreen renders Then it shows a "Cambiar contraseña" option that requires current password entry.

### R-003: AccountViewModel state management
**Priority**: P0
**Description**: The system MUST provide an AccountViewModel that manages state for all three sub-screens. The ViewModel MUST use `viewModelScope` coroutines and expose `StateFlow` for each sub-screen's state. It MUST handle photo upload, debounced username validation, display name save, and password change.
**Rationale**: Centralized state prevents duplication; ViewModel lifecycle survives configuration changes.
**Scenario**:
  - Given the user navigates between sub-screens When they edit a field on one sub-screen and switch to another Then their edits on the first sub-screen are preserved in the ViewModel state.
