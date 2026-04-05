# NFR0002A1: Persistencia y usabilidad operativa

> **Código:** NFR0002A1
> **Versión:** A
> **Revisión:** 1
> **Fecha:** 2026-04-05

## Objetivo
Garantizar que la información de eventos, perfiles, deudas y estados de pago se conserve correctamente y sea rápida de consultar y editar.

## Categoría
Persistencia / mantenibilidad / usabilidad.

## Métrica o criterio medible
- los datos creados por el usuario deben persistir entre sesiones de la app
- los importes deben almacenarse en euros con soporte de decimales
- las acciones habituales de consulta y edición deben requerir pocos pasos y ser comprensibles

## Justificación
La utilidad principal de la aplicación depende de no perder información y de poder actualizarla con rapidez durante o después de cada evento.

## Validación
- crear datos de prueba, cerrar y reabrir la app, y verificar la persistencia
- comprobar edición, eliminación y reversión del estado de pago
- revisión funcional del flujo de perfil y detalle de evento

## Impacto técnico
- almacenamiento local persistente
- validación de importes decimales
- estructura de datos preparada para agregación por perfil y por evento

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-05 | A | 1 | Alta | Creación inicial del requisito no funcional de persistencia y usabilidad. |
