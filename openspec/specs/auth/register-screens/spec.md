# Domain: auth/register-screens

## Purpose
Define the responsive, keyboard-aware layout for authentication screens so the register call-to-action is always reachable without scrolling.

## Requirements

### Requirement: Register CTA remains visible above the keyboard
The system MUST keep the register action visible and tappable whenever the on-screen keyboard is open.

#### Scenario: Keyboard opens on email field
- GIVEN the user focuses the email input on a small-screen device
- WHEN the keyboard appears
- THEN the register button MUST remain fully visible and tappable

#### Scenario: Keyboard opens on password field
- GIVEN the user focuses the password input
- WHEN the keyboard appears
- THEN the register button MUST remain fully visible and tappable

### Requirement: Layout does not require scrolling
The system MUST present all essential auth controls within the available viewport without vertical scrolling.

#### Scenario: Small-screen portrait
- GIVEN the device is a small-screen phone in portrait orientation
- WHEN the auth screen renders
- THEN all inputs, labels, and the register button MUST be visible at once

#### Scenario: Landscape orientation
- GIVEN the device is rotated to landscape
- WHEN the auth screen renders
- THEN the register button MUST remain visible without scrolling

### Requirement: Touch targets meet minimum size
The system MUST ensure every tappable element on the register screen is at least 48 dp in height.

#### Scenario: Register button tap target
- GIVEN the register button is rendered
- WHEN its bounds are measured
- THEN its height MUST be at least 48 dp

### Requirement: Layout adapts to system insets
The system MUST apply window insets so controls are not hidden by the keyboard, navigation bar, or status bar.

#### Scenario: Navigation bar present
- GIVEN a device with a software navigation bar
- WHEN the auth screen renders
- THEN the register button MUST not be obscured by the navigation bar
