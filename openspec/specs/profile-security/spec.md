# Profile Security Specification

## Purpose
Enable users to change their account password via Firebase Authentication with reauthentication. Handle the `requiresRecentLogin` error by prompting the user to re-enter credentials before proceeding.

## Requirements

### R-001: Password change with reauthentication
**Priority**: P0
**Description**: The system MUST allow an authenticated user to change their password. The flow MUST: (1) prompt for the current password, (2) reauthenticate the user with `EmailAuthProvider`, (3) call `user.updatePassword()` with the new password. The new password MUST meet Firebase's minimum strength requirements (6+ characters).
**Rationale**: Firebase requires recent authentication for sensitive operations. Reauthentication prevents unauthorized password changes from stale tokens.
**Scenarios**:
  - Given a user enters the correct current password and a valid new password (6+ chars) When they tap "Cambiar contraseña" Then the system reauthenticates with current credentials AND updates the password to the new value AND shows a success message.
  - Given a user enters the wrong current password When they attempt reauthentication Then the system returns an "invalid credentials" error AND the password is NOT changed.
  - Given a user enters a new password shorter than 6 characters When they tap "Cambiar contraseña" Then the system rejects with a "minimum 6 characters" error BEFORE calling Firebase.

### R-002: Reauthentication error handling
**Priority**: P0
**Description**: If Firebase throws `requiresRecentLogin` during password update despite reauthentication, the system MUST prompt the user to sign out and sign in again before retrying. The error MUST be surfaced in the UI with a clear explanation.
**Rationale**: Edge case where token expiry happens between reauth and update; user needs explicit guidance.
**Scenario**:
  - Given the user has reauthenticated but Firebase still returns `requiresRecentLogin` When the password update call fails Then the system displays "Tu sesión expiró. Por favor cerrá sesión y volvé a iniciarla." and offers a "Cerrar sesión" action.

### R-003: Password change confirmation
**Priority**: P1
**Description**: The system SHOULD require the user to type the new password twice (new + confirm) before enabling the change button. The button MUST be disabled until both fields match and pass minimum length validation.
**Rationale**: Double-entry prevents typos that would lock users out.
**Scenario**:
  - Given a user enters "newpass1" in the new password field and "newpass2" in the confirm field When both fields are filled Then the "Cambiar contraseña" button remains disabled and an inline error says "Las contraseñas no coinciden".
