# Domain: event/expense-management

## Purpose
Define expense ownership, creation, editing, and deletion rules so permissions are consistent across the UI, domain engine, and Firestore authorization.

## Requirements

### Requirement: Expenses record their creator
The system MUST set `createdByProfileId` to the profile id of the user who creates the expense.

#### Scenario: OWNER creates an expense
- GIVEN an OWNER creates a new expense
- WHEN the expense is saved
- THEN `createdByProfileId` MUST equal the OWNER's profile id

#### Scenario: CONTRIBUTOR creates an expense
- GIVEN a CONTRIBUTOR creates a new expense
- WHEN the expense is saved
- THEN `createdByProfileId` MUST equal the CONTRIBUTOR's profile id

### Requirement: Edit and delete permissions align across UI, engine, and rules
The system MUST allow editing or deleting an expense only when the user's role and the expense's `createdByProfileId` satisfy the same rule in the UI, `PermissionEngine`, and Firestore rules.

#### Scenario: OWNER edits an expense created by another user
- GIVEN an OWNER views an expense created by a CONTRIBUTOR
- WHEN the OWNER chooses to edit it
- THEN the edit MUST be permitted by the engine and accepted by Firestore rules

#### Scenario: CONTRIBUTOR edits their own expense
- GIVEN a CONTRIBUTOR views an expense they created
- WHEN they choose to edit it
- THEN the edit MUST be permitted by the engine and accepted by Firestore rules

#### Scenario: CONTRIBUTOR deletes their own expense
- GIVEN a CONTRIBUTOR views an expense they created
- WHEN they choose to delete it
- THEN the delete MUST be permitted by the engine and accepted by Firestore rules

#### Scenario: CONTRIBUTOR edits another user's expense
- GIVEN a CONTRIBUTOR views an expense created by another user
- WHEN they attempt to edit it
- THEN the edit control MUST be disabled and the operation MUST be rejected by the engine

#### Scenario: CONTRIBUTOR deletes another user's expense
- GIVEN a CONTRIBUTOR views an expense created by another user
- WHEN they attempt to delete it
- THEN the delete control MUST be disabled and the operation MUST be rejected by the engine

#### Scenario: VIEWER attempts to edit or delete
- GIVEN a VIEWER views any expense
- WHEN they attempt to edit or delete it
- THEN the controls MUST be disabled and the operation MUST be rejected

### Requirement: Missing ownership does not grant unrestricted edit or delete rights
The system MUST NOT allow a non-OWNER to edit or delete an expense solely because `createdByProfileId` is missing or empty.

#### Scenario: Expense lacks creator id
- GIVEN an expense has an empty `createdByProfileId`
- WHEN a non-OWNER attempts to edit it
- THEN the operation MUST be rejected
