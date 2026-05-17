# SPR0008A1: Sprint 08 - Colaboración y eventos compartidos

> **Código:** SPR0008A1
> **Versión:** A
> **Revisión:** 4
> **Fecha:** 2026-05-14

## Objetivo del sprint
Permitir que varios usuarios accedan a un mismo evento, implementando el sistema de invitaciones, la gestión de participantes y las notificaciones push mediante Firebase Cloud Messaging.

## Estado
Parcial — código implementado en `shared/src/commonMain/` pero **NO integrado** en `MainActivity`. Cloud Functions y pruebas manuales pendientes.

## Requisitos e historias incluidas
| ID | Tipo | Nombre | Prioridad | Estado | Dependencias |
|---|---|---|---|---|---|
| US-07 | US | Invitar a alguien a un evento por email | Alta | Parcial | SPR0007 |
| US-08 | US | Aceptar o rechazar una invitación | Alta | Parcial | US-07 |
| US-09 | US | Ver miembros de un evento | Media | Parcial | US-07 |
| US-10 | US | Expulsar a un miembro | Media | Parcial | US-09 |

## Tareas técnicas

### T3-01 — Modelo y repositorio de membresías ✓ (en shared/)
- Campos `ownerId` y `memberIds` en `EventItem`.
- `FirestoreEventRepository` gestiona `memberIds`.

### T3-02 a T3-05 — Invitaciones ✓ (en shared/)
- `EventInvitation` model, `InvitationRepository`, `FirestoreInvitationRepository`.
- `InvitationsViewModel`, `InviteMemberDialog`, `InvitationsScreen`.
- Aceptar: añade uid a `memberIds`; Rechazar: actualiza estado a `rejected`.

### T3-06 a T3-07 — FCM y notificaciones push ⏳
- `CuentaMorososFirebaseMessagingService` implementado en shared/.
- Guardado de token FCM en `users/{uid}/fcmToken`.
- **Pendiente**: Cloud Functions para enviar push (requiere plan Blaze).

### T3-08 — Expulsión de miembros ✓ (en shared/)
- `removeMember` en `FirestoreEventRepository`.
- Botón "Expulsar" en `EventMembersScreen` (solo propietario).

### T3-09 — Reglas de seguridad ⏳
- Pendientes de aplicar en Firebase Console.

### T3-10 — Pruebas de colaboración ⏳
- Pendientes de integración con MainActivity.

### T3-11 — Hardening Firebase/Auth/FCM ✓ (en shared/)
- `FirebaseUserSyncManager` centraliza sync de usuario y token.
- Normalización de emails (trim + lowercase).
- `kotlinx-coroutines-play-services` para `await()`.

## Definition of Done
- [x] Código de invitaciones, membresías y FCM implementado en shared/
- [x] Hardening transversal Firebase/Auth/FCM completado
- [ ] MainActivity wireada para usar versiones de shared/
- [ ] Cloud Functions para push notifications (plan Blaze)
- [ ] Pruebas manuales con dos cuentas distintas

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-30 | A | A.1 | Alta | Creación del sprint 08 con colaboración e invitaciones. |
| 2026-05-01 | A | A.2 | Alta | Implementación completa en shared/: invitaciones, miembros, FCM. |
| 2026-05-02 | A | A.3 | Actualización | Hardening transversal Firebase/Auth/FCM. |
| 2026-05-14 | A | A.4 | Corrección | Estado cambiado a Parcial: código en shared/ pero MainActivity no integrada. |
