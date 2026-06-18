# Delta for Smart Alerts

## ADDED Requirements

### Requirement: AlertAccordionCard Component (R001)

The system MUST provide an `AlertAccordionCard` composable that renders a collapsible alert with individual expand/collapse state per instance.

#### Scenario: Collapsed alert shows header only

- GIVEN an AlertAccordionCard is rendered with `initiallyExpanded = false`
- WHEN the card is displayed
- THEN the header row shows: icon circle (40dp), alert message, and chevron icon (▶)
- AND the AlertCard body is NOT visible

#### Scenario: Expanded alert shows full details

- GIVEN an AlertAccordionCard is rendered and expanded
- WHEN the card is displayed
- THEN the header row shows: icon circle, alert message, and chevron icon (▼)
- AND the full AlertCard body is visible below the header
- AND the body includes "Tocar para ver detalles" text

#### Scenario: Tapping header toggles expand/collapse

- GIVEN an AlertAccordionCard is in collapsed state
- WHEN user taps the header row
- THEN the card expands with `animateContentSize` animation
- AND the chevron rotates from ▶ to ▼
- AND tapping again collapses it back

### Requirement: Section Header Toggles All Alerts (R002)

The system MUST allow the "ALERTAS INTELIGENTES (N)" section header to toggle expand/collapse of ALL alert cards simultaneously.

#### Scenario: Section header expands all alerts

- GIVEN no alerts are expanded (expandedAlertIds is empty)
- WHEN user taps the section header
- THEN all alert cards become expanded
- AND `expandedAlertIds` contains all alert eventIds

#### Scenario: Section header collapses all alerts

- GIVEN all alerts are expanded
- WHEN user taps the section header
- THEN all alert cards become collapsed
- AND `expandedAlertIds` becomes empty

## MODIFIED Requirements

### Requirement: Alert Rendering in Dashboard

The system MUST replace the `AnimatedVisibility(visible = isExpanded)` + `items` pattern with per-alert `AlertAccordionCard` instances. Each alert card owns its own expand state. The section header "ALERTAS INTELIGENTES (N)" is clickable and toggles all alerts. Empty state shows "✅ Todo en orden" when no alerts exist.

(Previously: Alerts used `AnimatedVisibility` wrapping entire `AlertCard` inside `LazyColumn items {}`, which removed items from composition on collapse)

#### Scenario: Empty alerts state

- GIVEN `state.smartAlerts` is empty
- WHEN DashboardScreen renders the alerts section
- THEN it shows "ALERTAS INTELIGENTES" title
- AND shows "✅ Todo en orden" message
- AND no alert cards are rendered

#### Scenario: Multiple alerts with independent state

- GIVEN 3 smart alerts exist
- WHEN user expands only the second alert
- THEN alert 1 remains collapsed
- AND alert 2 shows its full AlertCard body
- AND alert 3 remains collapsed

#### Scenario: Alert tap navigates to event detail

- GIVEN an alert card is visible (collapsed or expanded)
- WHEN user taps the AlertCard body area
- THEN `onAlertTap(alert)` is invoked
- AND the app navigates to EventDetailScreen for that alert's eventId
