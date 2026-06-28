# Delta for firestore-authorization

Define Firestore security rules for events and expenses so write access matches the OWNER / CONTRIBUTOR / VIEWER role matrix.

## MODIFIED Requirements

### Requirement: OWNER has full write access
The system MUST allow an event OWNER to create, update, and delete both the event document and its expense sub-documents.

#### Scenario: OWNER updates event metadata
- GIVEN the authenticated user is the event OWNER
- WHEN they update the event title or date
- THEN Firestore rules MUST allow the write

#### Scenario: OWNER deletes an event
- GIVEN the authenticated user is the event OWNER
- WHEN they delete the event
- THEN Firestore rules MUST allow the deletion

### Requirement: CONTRIBUTOR has limited write access
The system MUST allow a CONTRIBUTOR to create expenses and update expenses they created. A CONTRIBUTOR MUST NOT add, edit, or delete participants.

#### Scenario: CONTRIBUTOR creates an expense
- GIVEN the authenticated user is a CONTRIBUTOR on the event
- WHEN they create a new expense with their profile id as `createdByProfileId`
- THEN Firestore rules MUST allow the create

#### Scenario: CONTRIBUTOR updates their own expense
- GIVEN the authenticated user is a CONTRIBUTOR and the expense's `createdByProfileId` matches their profile id
- WHEN they update the expense
- THEN Firestore rules MUST allow the update

#### Scenario: CONTRIBUTOR deletes their own expense
- GIVEN the authenticated user is a CONTRIBUTOR and the expense's `createdByProfileId` matches their profile id
- WHEN they delete the expense
- THEN Firestore rules MUST allow the delete

#### Scenario: CONTRIBUTOR attempts to delete another user's expense
- GIVEN the authenticated user is a CONTRIBUTOR and the expense's `createdByProfileId` does NOT match their profile id
- WHEN they attempt to delete the expense
- THEN Firestore rules MUST reject the write

#### Scenario: CONTRIBUTOR attempts to delete a participant
- GIVEN the authenticated user is a CONTRIBUTOR
- WHEN they attempt to delete a participant document
- THEN Firestore rules MUST reject the write

#### Scenario: CONTRIBUTOR attempts to run final calculation
- GIVEN the authenticated user is a CONTRIBUTOR
- WHEN they attempt to write a calculation/settlement result
- THEN Firestore rules MUST reject the write

### Requirement: VIEWER is read-only
The system MUST deny all write operations on events, expenses, and participants to VIEWERs.

#### Scenario: VIEWER attempts to create an expense
- GIVEN the authenticated user is a VIEWER on the event
- WHEN they attempt to create an expense
- THEN Firestore rules MUST reject the write

#### Scenario: VIEWER attempts to update event metadata
- GIVEN the authenticated user is a VIEWER
- WHEN they attempt to update the event
- THEN Firestore rules MUST reject the write

### Requirement: Unauthenticated writes are denied
The system MUST deny any write to events, expenses, or participants from an unauthenticated request.

#### Scenario: Anonymous user attempts to create an expense
- GIVEN the request is not authenticated
- WHEN it attempts to create an expense
- THEN Firestore rules MUST reject the write
