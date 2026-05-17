# Responsive Layout Specification

## Purpose

EventDetailScreen transitions from single-column scrollable layout to a responsive two-column layout that adapts to screen width, with expense list and settlement sidebar as distinct regions.

## Requirements

### Requirement: Responsive Breakpoint

The screen MUST select layout mode based on available width at 600dp threshold.

| Width | Layout |
|-------|--------|
| < 600dp | Single column: expenses stacked above settlement |
| >= 600dp | Two columns: expenses 2/3 width (left), settlement 1/3 width (right) |

#### Scenario: Mobile viewport renders single column

- GIVEN device width is 599dp
- WHEN EventDetailScreen is rendered
- THEN expenses list appears above settlement panel in a single vertical column

#### Scenario: Desktop viewport renders two columns

- GIVEN device width is 600dp or greater
- WHEN EventDetailScreen is rendered
- THEN expenses occupy left 2/3 of width and settlement occupies right 1/3

#### Scenario: Layout adapts on orientation change

- GIVEN screen is rendered at 700dp width (two columns)
- WHEN device rotates and width becomes 400dp
- THEN layout switches to single column stacked layout

### Requirement: Full-Width Header

The header row (back button + event title + date) and the Total Event Cost card MUST span full width regardless of layout mode.

#### Scenario: Header spans full width on desktop

- GIVEN device width is 800dp
- WHEN EventDetailScreen is rendered
- THEN header row and Total Event Cost card span the full width above the two-column content area

#### Scenario: Header spans full width on mobile

- GIVEN device width is 400dp
- WHEN EventDetailScreen is rendered
- THEN header row and Total Event Cost card span the full width

### Requirement: Expense Column Content

The expense column (or full-width on mobile) MUST contain: action buttons (Add Expense, Add Profile, Open Calculator), expense list, and the "Pendientes" / "Han pagado" debt sections.

#### Scenario: Expense column shows all expense-related content

- GIVEN an event with 3 expenses and 4 participants
- WHEN rendered in desktop mode
- THEN left column contains action buttons, expense cards, pending debts, and paid debts

### Requirement: Settlement Column Content

The settlement column (or bottom section on mobile) MUST contain: Total Event Cost summary, Calculate Totals button, and participant debt list with checkboxes.

#### Scenario: Settlement column shows settlement-related content

- GIVEN an event with 4 participants with debts
- WHEN rendered in desktop mode
- THEN right column contains Total Event Cost card, Calculate Totals button, and participant checkboxes
