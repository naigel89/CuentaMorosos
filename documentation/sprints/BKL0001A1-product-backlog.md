# BKL0001A1: Product Backlog

> **Código:** BKL0001A1
> **Versión:** A
> **Revisión:** 8
> **Fecha:** 2026-04-06

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

## Notas de priorización
- Priorizar primero la base de datos y el flujo mínimo de eventos y perfiles.
- Completar después el control de pagos y el recálculo de importes pendientes.
- Dejar recordatorios y personalización visual para una fase posterior ya estable.

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-05 | A | 1 | Alta | Creación inicial del backlog del proyecto. |
| 2026-04-05 | A | 2 | Actualización | Se priorizan requisitos y se reparte el trabajo por sprints reales. |
| 2026-04-05 | A | 3 | Actualización | Se añade la calculadora automática de cuentas al backlog priorizado. |
| 2026-04-05 | A | 4 | Actualización | Se marca como en progreso la base funcional del sprint 01 tras iniciar la implementación en la app. |
| 2026-04-05 | A | 5 | Actualización | Se marca en progreso el sprint 02 con control manual de pagos y reparto rápido inicial. |
| 2026-04-06 | A | 6 | Actualización | Se marcan como completados los sprints 01 y 02 tras cerrar la base funcional, pagos y calculadora avanzada. |
| 2026-04-06 | A | 7 | Actualización | Se arranca el sprint 03 con recordatorios locales y personalización visual persistente. |
| 2026-04-06 | A | 8 | Actualización | Se marca como completado el sprint 03 a nivel funcional tras cerrar ajustes, recordatorios y apariencia base. |
