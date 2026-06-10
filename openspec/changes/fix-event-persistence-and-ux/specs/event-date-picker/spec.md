# Event Date Picker Specification

## Purpose

Replace manual text-based date input with a visual calendar date picker for event creation and editing, reducing input errors and improving usability.

## Requirements

### Requirement: Visual Date Selection

The system MUST provide a `DatePicker` dialog for selecting event dates. The date picker SHALL replace the manual `dd/MM/yyyy` text input field in the event editor.

#### Scenario: User selects a single date

- GIVEN the user is creating or editing an event
- WHEN the user taps the date field
- THEN a calendar date picker dialog MUST appear
- AND selecting a date MUST populate the field in `dd/MM/yyyy` format

#### Scenario: User selects a date range

- GIVEN the user has enabled the "Rango de fechas" checkbox
- WHEN the user taps the start date field
- THEN a date range picker MUST appear allowing selection of start and end dates
- AND both fields MUST be populated in `dd/MM/yyyy` format

### Requirement: Date Validation

The system MUST validate that selected dates are logically consistent. The end date MUST NOT be before the start date.

#### Scenario: Valid date range

- GIVEN the user selected a start date of 15/03/2026
- WHEN the user selects an end date of 20/03/2026
- THEN the event MUST be saved with the valid date range

#### Scenario: End date before start date

- GIVEN the user selected a start date of 15/03/2026
- WHEN the user attempts to select an end date of 10/03/2026
- THEN the date picker MUST prevent selection of dates before the start date

### Requirement: Default Date Initialization

The system SHALL initialize the date field with the current date when creating a new event. When editing, the field SHALL show the event's existing date.

#### Scenario: New event defaults to today

- GIVEN the user opens the "Crear nuevo evento" dialog
- WHEN the date field is displayed
- THEN it MUST show today's date in `dd/MM/yyyy` format

#### Scenario: Editing event shows existing date

- GIVEN the user is editing an event with date 25/12/2025
- WHEN the edit dialog opens
- THEN the date field MUST display 25/12/2025
