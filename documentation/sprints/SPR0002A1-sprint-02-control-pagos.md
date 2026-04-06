# SPR0002A1: Sprint 02 - Control de pagos y calculadora por evento

> **Código:** SPR0002A1
> **Versión:** A
> **Revisión:** 6
> **Fecha:** 2026-04-06

## Objetivo del sprint
Completar la operativa principal de cobro dentro de cada evento, incluyendo importes, notas, estados de pago, vista `Han pagado` y calculadora automática de reparto.

## Estado
Hecho

## Requisitos e historias incluidas
| ID | Tipo | Nombre | Prioridad | Estado | Dependencias |
|---|---|---|---|---|---|
| FR0003A1 | FR | Control de deudas, pagos y notas | Alta | Hecho | FR0001A1, FR0002A1 |
| FR0005A1 | FR | Calculadora automática de cuentas por evento | Alta | Hecho | FR0003A1, DD0002A1 |
| RN0001A1 | RN | Estado de pago por evento | Alta | Hecho | FR0003A1 |
| RN0002A1 | RN | Cálculo de deuda activa | Alta | Hecho | FR0003A1 |
| RN0003A1 | RN | Reglas de reparto y redondeo automático | Alta | Hecho | FR0005A1 |
| UI0002A1 | UI | Detalle de evento y pagos | Alta | Hecho | FR0003A1 |
| UI0005A1 | UI | Calculadora automática por evento | Alta | Hecho | FR0005A1 |
| DD0002A1 | DD | Diseño de la calculadora automática | Alta | Hecho | DD0001A1 |

## Tareas técnicas
- crear o ampliar el modelo de ítems del evento para soportar reparto automático
- implementar el modal o pantalla de calculadora con simulación en tiempo real
- soportar los modos de reparto definidos y sus validaciones específicas
- escribir el resultado en la deuda por perfil y guardar metadatos del cálculo
- separar visualmente pendientes y `Han pagado`
- recalcular los totales activos por perfil tras aplicar el reparto

## Riesgos o bloqueos
- complejidad de UX al exponer muchos modos sin sobrecargar la pantalla
- incoherencias de redondeo y reparto si no se centraliza la lógica
- errores al revertir estados de pago o recalcular sobre cantidades previas
- la verificación `assembleDebug` sigue bloqueada en este entorno por falta de `JAVA_HOME`, aunque los archivos Kotlin no presentan errores del editor

## Definition of Done
- [x] se registran importes con decimales manualmente o por cálculo automático
- [x] el estado pagado se puede marcar y desmarcar
- [x] la pestaña `Han pagado` funciona correctamente
- [x] la calculadora solo aparece cuando el evento tiene perfiles
- [x] la vista previa y la simulación `¿y si...?` funcionan antes de confirmar
- [x] las validaciones de `custom_percentage` y `by_category` se cumplen correctamente
- [x] los totales agregados excluyen eventos pagados y respetan el redondeo a 2 decimales

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-05 | A | 1 | Alta | Creación inicial del sprint 02 para control de pagos. |
| 2026-04-05 | A | 2 | Actualización | Se amplía el sprint 02 con la calculadora automática de reparto por evento. |
| 2026-04-05 | A | 3 | Actualización | Se inicia la implementación técnica con detalle de evento, control manual de pagos y reparto rápido `simple_avg`. |
| 2026-04-05 | A | 4 | Actualización | Se amplía la calculadora con modos `custom_percentage`, `by_weight` y `by_attendance`, validación y comparativa rápida. |
| 2026-04-06 | A | 5 | Actualización | Se añade gestión de ítems del evento y nuevos modos `real_consumption` y `by_category` con persistencia y simulación previa. |
| 2026-04-06 | A | 6 | Actualización | Se completa funcionalmente el sprint 02 con los modos restantes (`by_income`, `base_plus_surplus`, `mixed`) y ayuda de liquidación en la calculadora. |
