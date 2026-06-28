# Domain: calculation-permissions

## Purpose

Define who may run final calculations and settlement actions so that only event OWNERs can trigger these database-affecting operations.

## Requirements

### Requirement: Only OWNER may run final calculations

The system MUST allow only event OWNERs to execute final calculation or settlement actions. CONTRIBUTORs and VIEWERs MUST NOT trigger these operations.

#### Scenario: OWNER runs settlement

- GIVEN the current user is the event OWNER
- WHEN they tap the settle/calculate button
- THEN the system MUST execute the calculation and persist the result

#### Scenario: CONTRIBUTOR attempts to run settlement

- GIVEN the current user is a CONTRIBUTOR on the event
- WHEN they attempt to tap the settle/calculate button
- THEN the button MUST be disabled and the operation MUST NOT execute

#### Scenario: VIEWER attempts to run settlement

- GIVEN the current user is a VIEWER on the event
- WHEN they attempt to tap the settle/calculate button
- THEN the button MUST be disabled and the operation MUST NOT execute

### Requirement: Calculation actions show loading feedback

The system MUST display a loading indicator while a calculation is running and MUST disable the trigger control until it completes.

#### Scenario: Calculation starts

- GIVEN the user taps the settle/calculate button
- WHEN the calculation begins
- THEN the system MUST show a loading indicator and disable the button

#### Scenario: Calculation completes

- GIVEN a calculation is in progress
- WHEN it completes (success or failure)
- THEN the system MUST hide the loading indicator and re-enable the button

### Requirement: Calculation errors are surfaced

The system MUST display a user-facing message when a calculation fails and MUST NOT fail silently.

#### Scenario: Calculation fails due to permission

- GIVEN a non-OWNER somehow triggers a calculation
- WHEN the server rejects the write
- THEN the system MUST show a permission error and stop loading
