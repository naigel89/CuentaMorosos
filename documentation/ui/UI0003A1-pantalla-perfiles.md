# UI0003A1: Pantalla de perfiles

> **Código:** UI0003A1
> **Versión:** A
> **Revisión:** 1
> **Fecha:** 2026-04-05

## Resumen
Pantalla principal para consultar todos los perfiles creados y el total pendiente de cada uno.

## Historia de usuario relacionada
Como usuario, quiero ver de un vistazo cuánto me debe cada persona y acceder al detalle de sus eventos pendientes.

## Objetivo de la pantalla
Ofrecer una vista agregada por persona en lugar de por evento.

## Componentes visibles
| Componente | Tipo | Descripción | Obligatorio |
|---|---|---|---|
| encabezado `Perfiles` | texto/título | identifica la sección | Sí |
| lista de perfiles | lista/tarjetas | muestra nombre, icono y total pendiente | Sí |
| botón `Nuevo perfil` | acción | crea un nuevo perfil global | Sí |
| detalle expandido o pantalla secundaria | vista de detalle | muestra eventos pendientes del perfil | Sí |

## Estados de la interfaz
- sin perfiles creados
- perfiles con deuda pendiente
- perfiles sin deuda activa
- detalle de perfil con desglose por evento

## Reglas de interacción
- pulsar un perfil abre o expande el desglose de eventos pendientes
- el total visible debe excluir eventos ya pagados
- el usuario puede editar datos del perfil desde esta vista o desde su detalle

## Navegación
- origen: navegación principal de la app
- destino secundario: detalle del perfil con desglose por evento

## Consideraciones UX/UI
La cifra total debe destacarse visualmente para facilitar priorización y seguimiento.

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-05 | A | 1 | Alta | Creación inicial del documento UI de la pantalla de perfiles. |
