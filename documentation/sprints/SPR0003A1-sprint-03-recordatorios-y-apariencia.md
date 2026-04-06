# SPR0003A1: Sprint 03 - Recordatorios y apariencia

> **Código:** SPR0003A1
> **Versión:** A
> **Revisión:** 4
> **Fecha:** 2026-04-06

## Objetivo del sprint
Añadir recordatorios automáticos, personalización visual y acabados de experiencia de usuario.

## Estado
Hecho

## Requisitos e historias incluidas
| ID | Tipo | Nombre | Prioridad | Estado | Dependencias |
|---|---|---|---|---|---|
| FR0004A1 | FR | Notificaciones y recordatorios | Media | Hecho | FR0003A1, API0001A1 |
| NFR0001A1 | NFR | Experiencia visual y personalización | Media | Hecho | UI0004A1 |
| API0001A1 | API | Servicios locales y notificaciones | Media | Hecho | DD0001A1 |
| UI0004A1 | UI | Ajustes y apariencia | Media | Hecho | NFR0001A1 |

## Tareas técnicas
- programar recordatorios por días transcurridos
- detectar eventos antiguos con saldos abiertos
- permitir cambio de tema claro/oscuro y color secundario
- integrar logo y revisar identidad visual

## Riesgos o bloqueos
- limitaciones del sistema de notificaciones en segundo plano
- disponibilidad o licencia de la tipografía objetivo
- la validación `assembleDebug` sigue sin poder ejecutarse en este entorno por ausencia de `JAVA_HOME`, aunque no hay errores de editor en el código del sprint

## Definition of Done
- [x] se envían recordatorios relevantes
- [x] el usuario puede cambiar tema y color secundario
- [x] la identidad visual se aplica de forma consistente
- [x] el flujo mantiene claridad y minimalismo

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-05 | A | 1 | Alta | Creación inicial del sprint 03 para recordatorios y apariencia. |
| 2026-04-06 | A | 2 | Actualización | Se inicia la implementación con ajustes persistentes de tema, color secundario y motor local de recordatorios visibles en la app. |
| 2026-04-06 | A | 3 | Actualización | Se añade preparación del canal local de notificaciones, permiso Android y vista previa visual en ajustes. |
| 2026-04-06 | A | 4 | Actualización | Se completa funcionalmente el sprint 03 con personalización persistente, lógica de avisos locales y pulido visual mínimo consistente. |
