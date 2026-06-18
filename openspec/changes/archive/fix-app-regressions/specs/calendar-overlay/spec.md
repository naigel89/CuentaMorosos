# Delta for Calendar Overlay

## ADDED Requirements

### Requirement: Calendar Modal Overlay (R001)

The system MUST render CalendarScreen as a full-screen modal overlay triggered exclusively from the Dashboard header button, NOT as a bottom navigation tab.

#### Scenario: Calendar opened from Dashboard header

- GIVEN user is viewing the Dashboard screen
- WHEN user taps the calendar button in the Dashboard header
- THEN CalendarScreen appears as a full-screen modal overlay
- AND the bottom navigation bar remains visible with 4 items only

#### Scenario: Calendar closed via close button

- GIVEN the calendar modal overlay is visible
- WHEN user taps the close button (back arrow) in the calendar header
- THEN the modal is dismissed
- AND the user returns to the Dashboard screen

#### Scenario: Calendar receives parent state

- GIVEN the calendar modal is opened
- WHEN CalendarScreen renders
- THEN it receives `events` and `pendingTotalsByEvent` from the parent state
- AND tapping an event in the calendar opens EventDetailScreen via `onOpenEvent`

### Requirement: Bottom Navigation Has 4 Items (R002)

The system MUST display exactly 4 items in the bottom navigation bar: Panel, Eventos, Perfiles, Ajustes. The CALENDAR entry MUST NOT exist in the navigation enum.

#### Scenario: Navigation bar shows 4 items

- GIVEN the app is on any navigation page
- WHEN the bottom navigation bar is rendered
- THEN it shows exactly: Panel, Eventos, Perfiles, Ajustes
- AND no Calendar tab is present

#### Scenario: Pager has 4 pages

- GIVEN the HorizontalPager is initialized
- WHEN the pager state is queried
- THEN `pageCount` returns 4
- AND page indices 0-3 map to DASHBOARD, EVENTS, PROFILES, SETTINGS respectively

## MODIFIED Requirements

### Requirement: Main Navigation Structure

The MainSection enum MUST contain exactly 4 entries (DASHBOARD, EVENTS, PROFILES, SETTINGS). The CALENDAR entry MUST be removed. The pager `pageCount` MUST be `{ 4 }`. Calendar access is via a `showCalendar` boolean state at the CuentaMorososApp level, rendered conditionally as a Dialog overlay.

(Previously: MainSection had 5 entries including CALENDAR; pager had 5 pages; calendar was a nav destination)

#### Scenario: Calendar button triggers modal instead of navigation

- GIVEN user is on Dashboard page (page 0)
- WHEN user taps the calendar button
- THEN `showCalendar` state is set to true
- AND the pager does NOT scroll to a calendar page
- AND a Dialog containing CalendarScreen is rendered

#### Scenario: Calendar close resets modal state

- GIVEN `showCalendar` is true and the Dialog is visible
- WHEN the close callback is invoked
- THEN `showCalendar` is set to false
- AND the Dialog is removed from composition
