> ⚠️ DEPRECATED — Replaced by [UI0002B1](UI0002B1-detalle-evento-y-pagos.md) (Neo-Fintech rediseño). This document is kept for historical reference only.

# UI0002A1: Detalle de evento y pagos

> **Código:** UI0002A1
> **Versión:** A
> **Revisión:** 2
> **Fecha:** 2026-04-05

## Resumen
Pantalla donde se gestionan los participantes de un evento, el dinero que debe cada uno, sus notas, el cambio de estado entre pendiente y pagado y el acceso a la calculadora automática del evento.

## Historia de usuario relacionada
Como usuario, quiero controlar dentro de un evento quién me debe dinero, cuánto me debe y quién ya ha pagado.

## Objetivo de la pantalla
Centralizar el seguimiento operativo de la deuda de cada perfil dentro de un evento concreto.

## Componentes visibles
| Componente | Tipo | Descripción | Obligatorio |
|---|---|---|---|
| título del evento | encabezado | muestra el nombre y la fecha del evento | Sí |
| botón `Añadir perfil` | acción | permite incorporar participantes existentes | Sí |
| banner de sugerencia | card informativa | invita a usar la calculadora si hay perfiles pero aún no hay cantidades | No |
| botón `Calculadora` | acción | abre el flujo automático de reparto | No |
| lista de pendientes | lista/tarjetas | muestra perfiles con importe y notas | Sí |
| check `Pagado` | control | cambia el estado del registro | Sí |
| pestaña `Han pagado` | pestaña secundaria | agrupa perfiles ya saldados | Sí |
| edición de importe/notas | acción contextual | modifica o elimina la deuda | Sí |

## Estados de la interfaz
- evento sin perfiles
- evento con perfiles pendientes
- evento con perfiles pagados en pestaña secundaria
- evento con sugerencia de calculadora al detectar importes vacíos
- error de validación en importe o en el flujo de cálculo

## Reglas de interacción
- el botón `Calculadora` solo aparece si el evento tiene perfiles asociados
- si ningún perfil tiene cantidad asignada, puede mostrarse una sugerencia no intrusiva para usar la calculadora
- marcar `Pagado` mueve el perfil a `Han pagado`
- desmarcarlo lo devuelve a pendientes
- el importe admite decimales en euros
- las notas se pueden añadir, editar o eliminar
- al aplicar el cálculo automático, el banner desaparece y se actualizan los importes del evento

## Navegación
- origen: `UI0001A1`
- destino secundario: selector de perfil, formulario de edición o `UI0005A1`

## Consideraciones UX/UI
Debe priorizar visualmente a los perfiles pendientes, dejar los pagados accesibles pero menos predominantes y presentar la calculadora como ayuda contextual sin resultar intrusiva.

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-05 | A | 1 | Alta | Creación inicial del documento UI del detalle de evento y pagos. |
| 2026-04-05 | A | 2 | Actualización | Se añade el acceso contextual a la calculadora automática y su banner de sugerencia. |
