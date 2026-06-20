# RN0001A1: Estado de pago y visibilidad por evento

> **Código:** RN0001A1
> **Versión:** A
> **Revisión:** 2
> **Fecha:** 2026-06-20

## Descripción
El estado de pago de un perfil es **específico de cada evento** y no debe afectar automáticamente a otros eventos en los que participe la misma persona. Además, los controles de pago solo están disponibles cuando el evento ya tiene un cálculo aplicado.

## Motivación
Una misma persona puede deber importes distintos en eventos distintos y haber pagado solo algunos de ellos. En eventos en estado `OPEN` (sin cálculo aplicado), el concepto de «pago» no es relevante porque las deudas aún no se han calculado.

## Condiciones
- al marcar un perfil como pagado dentro de un evento, ese registro pasa a la sección `Han pagado`
- al desmarcarlo, vuelve a la sección principal de pendientes de ese mismo evento
- el cambio no modifica otros eventos del mismo perfil
- **los checkboxes de pago solo se muestran cuando `eventState != OPEN`** (es decir, en estados `CALCULATED` o `CLOSED`)
- en estado `OPEN`, los participantes se muestran sin checkbox de pago

## Impacto funcional
- la lista principal del evento prioriza a quienes aún no han pagado
- la sección `Han pagado` actúa como histórico visible pero secundario
- en un evento recién creado (OPEN), la interfaz no muestra opciones de pago hasta que se aplique un cálculo

## Ejemplos
- **Válido:** Ana paga en el evento `Cena viernes` y sigue apareciendo como pendiente en `Cumpleaños de Marta`
- **Válido:** en un evento en estado `OPEN`, los checkboxes de pago no aparecen — solo se ven los nombres de los participantes
- **No válido:** marcar a Ana como pagada en un evento y ocultarla en todos los demás
- **No válido:** mostrar checkboxes de pago en un evento sin cálculo aplicado

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-05 | A | 1 | Alta | Creación inicial de la regla de negocio sobre estado de pago por evento. |
| 2026-06-20 | A | 2 | Actualización | Agregada regla de visibilidad de checkboxes de pago: solo se muestran cuando `eventState != OPEN`. |
