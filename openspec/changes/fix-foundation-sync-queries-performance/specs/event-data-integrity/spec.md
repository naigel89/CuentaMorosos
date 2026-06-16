# Event Data Integrity Specification

## Purpose

Ensure `replaceMemberId()` updates ALL fields referencing a member ID so ghost profiles are fully linked to real users.

## Requirements

### Requirement: Complete Member ID Replacement

The system SHALL update every field referencing the old member ID in a single atomic batch: `memberIds`, `ownerId`, `participantIds`, and `profileId` within the `participants` array.

#### Bug Context

**Síntoma**: Tras vincular un perfil ghost (creado offline) con un usuario real, los eventos donde el ghost era participante NO aparecen en las queries del usuario real. El usuario ve "0 eventos" aunque debería ver varios.

**Causa raíz**: `FirestoreEventRepository.replaceMemberId()` líneas 136-153 solo actualiza `memberIds` y `ownerId`, pero NO actualiza `participantIds` ni el array `participants`. Las queries de eventos usan TRES campos: `ownerId`, `memberIds`, `participantIds`. Si `participantIds` aún contiene el ID ghost, la query `participantIds contains realUserId` no encuentra el evento.

**Por qué el fix lo resuelve**: Actualizar TODOS los campos en el mismo batch atómico: `memberIds`, `ownerId`, `participantIds`, y `profileId` dentro del array `participants`.

#### Scenario: Ghost profile linked via participantIds

- GIVEN an event where `participantIds` contains a ghost profile ID "ghost-123"
- WHEN `replaceMemberId("ghost-123", "real-456")` is called
- THEN `participantIds` no longer contains "ghost-123"
- AND `participantIds` contains "real-456"
- AND the event is discoverable via `participantIds` query for "real-456"

#### Scenario: Ghost profile was event owner

- GIVEN an event where `ownerId` equals "ghost-123"
- WHEN `replaceMemberId("ghost-123", "real-456")` is called
- THEN `ownerId` is updated to "real-456"

#### Scenario: Ghost profile in participants array

- GIVEN an event with `participants` array containing `{profileId: "ghost-123", ...}`
- WHEN `replaceMemberId("ghost-123", "real-456")` is called
- THEN the participant entry's `profileId` is updated to "real-456"

#### Scenario: Atomic batch update

- GIVEN 10 events reference the ghost ID across different fields
- WHEN `replaceMemberId()` completes
- THEN all 10 events are updated atomically — no partial state is visible to observers
