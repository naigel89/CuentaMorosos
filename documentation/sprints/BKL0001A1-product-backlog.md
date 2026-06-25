# BKL0001A1: Product Backlog

> **Código:** BKL0001A1
> **Versión:** A
> **Revisión:** A.19
> **Fecha:** 2026-05-14

## Resumen
Backlog general del proyecto. Debe recoger todos los requisitos, tareas técnicas y pendientes relevantes para planificar los próximos sprints.

| ID | Tipo | Descripción | Prioridad | Sprint objetivo | Estado | Dependencias |
|---|---|---|---|---|---|---|
| DD0001A1 | DD | Modelo funcional base y entidades | Alta | Sprint 01 | Hecho | - |
| FR0001A1 | FR | Gestión de eventos y calendario | Alta | Sprint 01 | Hecho | DD0001A1 |
| FR0002A1 | FR | Gestión global de perfiles | Alta | Sprint 01 | Hecho | DD0001A1 |
| NFR0002A1 | NFR | Persistencia y usabilidad operativa | Alta | Sprint 01 | Hecho | DD0001A1 |
| FR0003A1 | FR | Control de deudas, pagos y notas | Alta | Sprint 02 | Hecho | FR0001A1, FR0002A1 |
| FR0005A1 | FR | Calculadora automática de cuentas por evento | Alta | Sprint 02 | Hecho | FR0003A1, DD0002A1 |
| RN0001A1 | RN | Estado de pago y visibilidad por evento | Alta | Sprint 02 | Hecho | FR0003A1 |
| RN0002A1 | RN | Cálculo de deuda activa y recordatorios | Alta | Sprint 02 | Hecho | FR0003A1 |
| RN0003A1 | RN | Reglas de reparto y redondeo automático | Alta | Sprint 02 | Hecho | FR0005A1 |
| DD0002A1 | DD | Diseño de la calculadora automática por evento | Alta | Sprint 02 | Hecho | DD0001A1 |
| UI0005A1 | UI | Calculadora automática por evento | Alta | Sprint 02 | Hecho | FR0005A1 |
| FR0004A1 | FR | Notificaciones y recordatorios | Media | Sprint 03 | Hecho | FR0003A1, API0001A1 |
| NFR0001A1 | NFR | Experiencia visual y personalización | Media | Sprint 03 | Hecho | UI0004A1 |
| API0001A1 | API | Servicios locales y notificaciones | Media | Sprint 03 | Hecho | DD0001A1 |
| DT0001A1 | DT | Eliminación de eventos y perfiles | Alta | Sprint 04 | Hecho | FR0001A1, FR0002A1 |
| DT0002A1 | DT | Solicitud de permiso POST_NOTIFICATIONS en runtime (Android 13+) | Alta | Sprint 04 | Hecho | FR0004A1 |
| DT0003A1 | DT | Ampliar selector de iconos de perfil (más de 6 opciones) | Baja | Sprint 04 | Hecho | FR0002A1 |
| DT0004A1 | DT | Liquidación real de deudas (mínimo de transferencias entre perfiles) | Media | Sprint 04 | Hecho | FR0005A1, RN0003A1 |
| DT0005A1 | DT | Notificaciones en segundo plano con WorkManager | Media | Sprint 04 | Hecho | FR0004A1, API0001A1 |
| FR0001B1 | FR | Vista de calendario de eventos | Baja | Sprint 05 | Hecho | FR0001A1 |
| UX0001A1 | UX | Búsqueda y filtrado de eventos | Media | Sprint 05 | Hecho | FR0001A1 |
| US-01 | US | Registro de nuevo usuario | Alta | Sprint 06 | Hecho | — |
| US-02 | US | Inicio de sesión | Alta | Sprint 06 | Hecho | US-01 |
| US-03 | US | Recuperación de contraseña | Media | Sprint 06 | Hecho | US-01 |
| US-04 | US | Cerrar sesión | Media | Sprint 06 | Hecho | US-02 |
| US-05 | US | Crear un evento online | Alta | Sprint 07 | Hecho | SPR0006 |
| US-06 | US | Ver eventos en tiempo real | Alta | Sprint 07 | Hecho | US-05 |
| US-07 | US | Invitar a alguien a un evento por email | Alta | Sprint 08 | Hecho | SPR0007 |
| US-08 | US | Aceptar o rechazar una invitación | Alta | Sprint 08 | Hecho | US-07 |
| US-09 | US | Ver miembros de un evento | Media | Sprint 08 | Hecho | US-07 |
| US-10 | US | Expulsar a un miembro | Media | Sprint 08 | Hecho | US-09 |
| FR0006A1 | FR | Perfiles fantasma y vinculación manual | Alta | Sprint 08B.2 | Hecho | SPR0008A1 |
| FR0007A1 | FR | Creador de evento opcional en el reparto | Media | Sprint 08B.1 | Hecho | SPR0008A1 |
| FR0008A1 | FR | Divisiones porcentuales por ítem individual | Alta | Sprint 08B.2 | Hecho | FR0005A1 |
| UI0006A1 | UI | Simplificación de la interfaz de modos de cálculo | Media | Sprint 08B.1 | Hecho | UI0005A1 |


## Notas de priorización
- Priorizar primero la base de datos y el flujo mínimo de eventos y perfiles.
- Completar después el control de pagos y el recálculo de importes pendientes.
- Dejar recordatorios y personalización visual para una fase posterior ya estable.
- Sprint 04 cierra deuda técnica acumulada: eliminación de registros, permisos runtime, liquidación real y notificaciones en background.
- **NOTA (2026-06-25)**: Sprints 06-08 completados — todo el código de shared/ está integrado en MainActivity vía RepositoryProvider. Sprints 09-11 eliminados del plan por no ser prioritarios.

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-05 | A | A.1 | Alta | Creación inicial del backlog del proyecto. |
| 2026-04-05 | A | A.2 | Actualización | Se priorizan requisitos y se reparte el trabajo por sprints reales. |
| 2026-04-05 | A | A.3 | Actualización | Se añade la calculadora automática de cuentas al backlog priorizado. |
| 2026-04-05 | A | A.4 | Actualización | Se marca como en progreso la base funcional del sprint 01 tras iniciar la implementación en la app. |
| 2026-04-05 | A | A.5 | Actualización | Se marca en progreso el sprint 02 con control manual de pagos y reparto rápido inicial. |
| 2026-04-06 | A | A.6 | Actualización | Se marcan como completados los sprints 01 y 02 tras cerrar la base funcional, pagos y calculadora avanzada. |
| 2026-04-06 | A | A.7 | Actualización | Se arranca el sprint 03 con recordatorios locales y personalización visual persistente. |
| 2026-04-06 | A | A.8 | Actualización | Se marca como completado el sprint 03 a nivel funcional tras cerrar ajustes, recordatorios y apariencia base. |
| 2026-04-30 | A | A.10 | Actualización | Sprint 04 completado; todos los ítems de deuda técnica marcados como Hecho. |
| 2026-04-30 | A | A.11 | Actualización | Sprint 05 arrancado: FR0001B1 (calendario) y UX0001A1 (búsqueda y filtrado) en progreso. |
| 2026-04-30 | A | A.12 | Actualización | Sprint 05 completado: calendario mensual y búsqueda+filtros en EventsScreen implementados. |
| 2026-04-30 | A | A.13 | Actualización | Sprint 06 completado: autenticación Firebase (registro, login, recuperación de contraseña, perfil, cierre de sesión) implementada. |
| 2026-05-01 | A | A.14 | Actualización | Sprint 07 completado: ProfileRepository + FirestoreProfileRepository, deleteDebtsForProfile en cascada, ProfilesViewModel refactorizado con Firestore, CuentaMorososApp migrado para perfiles. |
| 2026-05-01 | A | A.15 | Actualización | Se añaden requisitos de usabilidad (perfiles fantasma, creador opcional, % por ítem y UI simplificada) asignados al bloque Sprint 08B antes de la migración a iOS. |
| 2026-05-01 | A | A.16 | Actualización | Sprint 08B.1 completado: creador opcional en reparto y simplificación de modos de cálculo marcados como Hecho; quedan pendientes perfiles fantasma y porcentajes por ítem (08B.2). |
| 2026-05-02 | A | A.17 | Actualización | Sprint 08B.2 completado: perfiles fantasma con vinculación automática por email y reparto porcentual por ítem marcados como Hecho. |
| 2026-05-14 | A | A.19 | Corrección | Auditoría completa: sprints 06-09 cambiados de "Hecho" a "Parcial" (código en shared/ pero no integrado en MainActivity). Añadidos US-07 a US-13 al backlog. Nota de priorización actualizada con orden corregido: Integración KMP → Offline → Rediseño. |
| 2026-06-25 | A | A.20 | Actualización | Sprints 06, 07, 08, 08B.1, 08B.2 marcados como Hecho: todo el código shared/ integrado en MainActivity vía RepositoryProvider (OfflineFirst repos, PendingOperationQueue, invitaciones, ghost profiles). |
| 2026-06-25 | A | A.21 | Actualización | Eliminados sprints 09-11 y sus referencias del backlog por no ser prioritarios. |
