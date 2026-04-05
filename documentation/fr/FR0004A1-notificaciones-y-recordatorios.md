# FR0004A1: Notificaciones y recordatorios

> **Código:** FR0004A1
> **Versión:** A
> **Revisión:** 1
> **Fecha:** 2026-04-05

## Resumen
La aplicación debe recordar al usuario qué deudas y eventos siguen pendientes mediante notificaciones configurables.

## Historia de usuario
Como usuario de `CuentaMorosos`, quiero recibir recordatorios sobre pagos pendientes y eventos incompletos para no olvidar reclamar ni cerrar cuentas.

## Descripción funcional
La aplicación debe poder enviar notificaciones para:
- recordar que hay usuarios que no han pagado después de **X días**
- recordar que existen **eventos antiguos** con cuentas aún sin saldar
- avisar de que hay **eventos incompletos** donde todavía no se han añadido perfiles o importes

## Reglas relacionadas
- `RN0002A1`

## Criterios de aceptación
- [ ] el usuario puede recibir recordatorios por deudas pendientes tras un periodo configurable
- [ ] los eventos antiguos con saldos abiertos generan aviso
- [ ] los eventos creados pero incompletos generan recordatorio
- [ ] las notificaciones no dependen de que el evento siga abierto en pantalla

## Casos límite
- múltiples eventos pendientes a la vez
- varios perfiles deudores dentro de un mismo evento
- cambio reciente de estado a `pagado` antes del recordatorio

## Dependencias
- `API0001A1`
- `DD0001A1`

## Suposiciones
- El valor exacto de `X días` se deja configurable en ajustes o en una preferencia futura.

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-05 | A | 1 | Alta | Creación inicial del requisito de notificaciones y recordatorios. |
