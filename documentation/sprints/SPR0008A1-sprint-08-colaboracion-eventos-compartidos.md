# SPR0008A1: Sprint 08 - Colaboración y eventos compartidos

> **Código:** SPR0008A1
> **Versión:** A
> **Revisión:** 1
> **Fecha:** 2026-04-30

## Objetivo del sprint
Permitir que varios usuarios accedan a un mismo evento, implementando el sistema de invitaciones, la gestión de participantes y las notificaciones push mediante Firebase Cloud Messaging.

## Estado
Pendiente

## Requisitos e historias incluidas
| ID | Tipo | Nombre | Prioridad | Estado | Dependencias |
|---|---|---|---|---|---|
| US-07 | US | Invitar a alguien a un evento por email | Alta | Pendiente | SPR0007 |
| US-08 | US | Aceptar o rechazar una invitación | Alta | Pendiente | US-07 |
| US-09 | US | Ver miembros de un evento | Media | Pendiente | US-07 |
| US-10 | US | Expulsar a un miembro | Media | Pendiente | US-09 |

## Tareas técnicas

### T3-01 — Modelo y repositorio de membresías
- Añadir campos `ownerId: String` y `memberIds: List<String>` al modelo `EventItem`.
- Actualizar `FirestoreEventRepository` para gestionar `memberIds`.

### T3-02 — Pantalla de participantes
- Nuevo composable `EventMembersScreen` dentro del detalle del evento.
- Lista de miembros con nombre, icono y etiqueta "Propietario" para el creador.
- Botón "Añadir participante" visible solo para el propietario.

### T3-03 — Flujo de envío de invitación
- Nuevo composable `InviteMemberDialog` con campo de email.
- Al confirmar, crear documento en la colección `invitations` con estado `pending`.
- Validar que el email invitado existe como usuario registrado.

### T3-04 — Pantalla de invitaciones pendientes
- Nueva sección "Invitaciones" accesible desde la pantalla principal o ajustes.
- Lista de invitaciones con nombre del evento, quien invita y botones "Aceptar" / "Rechazar".

### T3-05 — Aceptar y rechazar invitaciones
- Al aceptar: añadir `uid` del usuario a `memberIds` del evento y actualizar estado de la invitación a `accepted`.
- Al rechazar: actualizar estado a `rejected`.

### T3-06 — Configuración de Firebase Cloud Messaging
- Habilitar FCM en Firebase Console.
- Implementar `FirebaseMessagingService` para recibir notificaciones.
- Guardar el token FCM del dispositivo en `users/{uid}/fcmToken`.

### T3-07 — Notificación push al invitado
- Al crear una invitación, disparar una Cloud Function (o llamada al servidor FCM) que envíe una notificación push al token del usuario invitado.

### T3-08 — Expulsión de miembros
- Botón "Expulsar" en `EventMembersScreen`, visible solo para el propietario y no aplicable a sí mismo.
- Al confirmar, eliminar el `uid` del miembro de `memberIds`.

### T3-09 — Actualización de reglas de seguridad
- Verificar que las reglas de Firestore aplican correctamente el control de acceso por `memberIds` tras todos los cambios.

### T3-10 — Pruebas de colaboración
- Prueba manual con dos cuentas distintas: invitación, aceptación y edición simultánea de un evento.

## Riesgos o bloqueos
- Las Cloud Functions requieren activar el plan Blaze de Firebase si se usan para enviar notificaciones; evaluar si se puede gestionar desde el cliente.
- La invitación a usuarios no registrados requiere un flujo adicional de deep link.

## Definition of Done
- [ ] El creador puede invitar a otro usuario por email
- [ ] El invitado recibe una notificación push
- [ ] El invitado puede aceptar o rechazar la invitación
- [ ] Dos usuarios ven los cambios del mismo evento en tiempo real
- [ ] El creador puede ver la lista de miembros del evento
- [ ] El creador puede expulsar a un miembro

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-30 | A | 1 | Alta | Creación del sprint 08 con colaboración e invitaciones. |
