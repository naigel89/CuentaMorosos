# FR0001A1: Gestión de eventos y calendario

> **Código:** FR0001A1
> **Versión:** A
> **Revisión:** 1
> **Fecha:** 2026-04-05

## Resumen
La aplicación debe permitir al usuario crear y gestionar eventos en los que haya realizado gastos compartidos para controlar lo que otras personas le deben.

## Historia de usuario
Como usuario de `CuentaMorosos`, quiero crear eventos con nombre y fecha para organizar cada deuda según el contexto en el que se produjo.

## Descripción funcional
- El usuario puede crear un evento indicando al menos un **nombre** y una **fecha**.
- El usuario puede consultar el listado de eventos creados.
- Cada evento actúa como contenedor de perfiles, importes y notas asociadas.
- El usuario puede visualizar opcionalmente los eventos en un formato de **calendario** si lo considera útil.
- La aplicación debe permitir editar los datos básicos del evento cuando sea necesario.

## Reglas relacionadas
- `RN0001A1`
- `RN0002A1`

## Criterios de aceptación
- [ ] se puede crear un evento con nombre y fecha
- [ ] el evento aparece en la pantalla de eventos tras guardarlo
- [ ] cada evento puede abrirse para ver sus perfiles participantes
- [ ] existe una visualización alternativa en calendario o una reserva funcional para activarla más adelante
- [ ] el usuario puede editar los datos básicos del evento

## Casos límite
- intento de crear un evento sin nombre
- selección de una fecha pasada o futura
- existencia de varios eventos el mismo día

## Dependencias
- `UI0001A1`
- `UI0002A1`
- `DD0001A1`

## Suposiciones
- No se ha definido todavía la necesidad de borrar eventos; se deja como decisión de implementación posterior.

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-05 | A | 1 | Alta | Creación inicial del requisito de gestión de eventos y calendario. |
