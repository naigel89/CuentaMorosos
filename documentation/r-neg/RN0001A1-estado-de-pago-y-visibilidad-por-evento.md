# RN0001A1: Estado de pago y visibilidad por evento

> **Código:** RN0001A1
> **Versión:** A
> **Revisión:** 1
> **Fecha:** 2026-04-05

## Descripción
El estado de pago de un perfil es **específico de cada evento** y no debe afectar automáticamente a otros eventos en los que participe la misma persona.

## Motivación
Una misma persona puede deber importes distintos en eventos distintos y haber pagado solo algunos de ellos.

## Condiciones
- al marcar un perfil como pagado dentro de un evento, ese registro pasa a la sección `Han pagado`
- al desmarcarlo, vuelve a la sección principal de pendientes de ese mismo evento
- el cambio no modifica otros eventos del mismo perfil

## Impacto funcional
- la lista principal del evento prioriza a quienes aún no han pagado
- la sección `Han pagado` actúa como histórico visible pero secundario

## Ejemplos
- **Válido:** Ana paga en el evento `Cena viernes` y sigue apareciendo como pendiente en `Cumpleaños de Marta`
- **No válido:** marcar a Ana como pagada en un evento y ocultarla en todos los demás

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-05 | A | 1 | Alta | Creación inicial de la regla de negocio sobre estado de pago por evento. |
