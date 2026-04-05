# UI0001A1: Pantalla de eventos

> **Código:** UI0001A1
> **Versión:** A
> **Revisión:** 1
> **Fecha:** 2026-04-05

## Resumen
Pantalla principal para listar, crear y localizar eventos registrados por el usuario.

## Historia de usuario relacionada
Como usuario, quiero ver rápidamente mis eventos y crear nuevos para organizar mis cuentas pendientes.

## Objetivo de la pantalla
Actuar como punto de entrada principal de la app y ofrecer acceso rápido al listado de eventos, su fecha y una posible vista de calendario.

## Componentes visibles
| Componente | Tipo | Descripción | Obligatorio |
|---|---|---|---|
| encabezado `Eventos` | texto/título | identifica la sección principal | Sí |
| botón `Nuevo evento` | acción/FAB | abre el formulario de creación | Sí |
| listado de eventos | lista/tarjetas | muestra nombre y fecha de cada evento | Sí |
| conmutador o acceso a calendario | acción secundaria | cambia de lista a vista calendario si se implementa | No |
| buscador o filtros | control opcional | facilita localizar eventos | No |

## Estados de la interfaz
- estado inicial vacío sin eventos
- lista con eventos creados
- vista calendario opcional
- error de validación al crear un evento sin nombre

## Reglas de interacción
- pulsar un evento abre su detalle
- pulsar `Nuevo evento` abre formulario con nombre y fecha
- la fecha debe poder elegirse de forma cómoda

## Navegación
- origen: pantalla inicial de la app
- destino: `UI0002A1` al abrir un evento

## Consideraciones UX/UI
Interfaz minimalista, clara y con jerarquía visual basada en color secundario personalizable.

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-05 | A | 1 | Alta | Creación inicial del documento UI de la pantalla de eventos. |
