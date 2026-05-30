# Username Specification

## Purpose
Enable users to set a unique @username that identifies them across the app. Username validation runs against Firestore with debounced queries to prevent collisions.

## Requirements

### R-001: Unique username assignment
**Priority**: P0
**Description**: The system MUST allow an authenticated user to set a `username` field on their profile. The username MUST be unique across all Firestore profile documents. Validation MUST query the `profiles` collection for an existing document with the same `username` field.
**Rationale**: Username uniqueness is a core identity feature; without it, @username references would be ambiguous.
**Scenarios**:
  - Given no other profile has username "maria" When a user sets username to "maria" Then the system writes `username = "maria"` to `profiles/{uid}` via merge and returns success.
  - Given another profile already has username "maria" When a user attempts to set username to "maria" Then the system returns a "Username already taken" error and does NOT write to Firestore.
  - Given the Firestore query fails (network error) When a user attempts to set a username Then the system returns a descriptive error and the username field is unchanged.

### R-002: Debounced validation
**Priority**: P0
**Description**: The system MUST debounce username uniqueness checks by 500ms from the last keystroke before performing the Firestore query. During validation, the UI MUST show a loading indicator and disable the save action.
**Rationale**: Unthrottled queries would spike Firestore read costs and create a poor UX with constant validation churn.
**Scenarios**:
  - Given a user types "mar" then waits 300ms then types "ia" When the debounce timer fires 500ms after the last keystroke Then a single Firestore query executes for the full string "maria" (not intermediate substrings).
  - Given the debounced query returns "available" When the user taps Save Then the username is written to Firestore.

### R-003: Username display
**Priority**: P1
**Description**: When a profile has a `username` set, the system SHOULD display it as `@username` in profile cards and detail dialogs. The display resolution MUST be: `customName` > `displayName` > `name` for the visible name; `@username` is shown as a secondary label when set.
**Rationale**: The @username is a handle, not a display name; both are shown together.
**Scenario**:
  - Given a ProfileItem with `name = "María"` and `username = "maria"` When ProfileCard renders Then it shows "María" as the primary name and "@maria" as a secondary label.
