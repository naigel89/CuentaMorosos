# UI0001B1: Pantalla de eventos — Rediseño Neo-Fintech

> **Código:** UI0001B1
> **Versión:** B
> **Revisión:** 2
> **Fecha:** 2026-06-18

## Resumen
Pantalla de eventos con layout `LazyVerticalGrid`, tarjetas de evento con información enriquecida (estado, participantes, saldo), chips de filtro por estado y FAB para crear nuevos eventos. Implementada en `EventsScreen.kt` dentro de `shared/src/commonMain/kotlin/com/cuentamorosos/ui/`.

## Historia de usuario relacionada
Como usuario, quiero ver mis eventos activos con toda la información relevante y poder filtrarlos rápidamente por estado.

## Objetivo de la pantalla
Listar eventos en un grid responsive con filtrado por estado (todos, activos, saldados) y acceso rápido a la creación de eventos.

## Componentes visibles
| Componente | Tipo | Descripción | Obligatorio |
|---|---|---|---|
| Filter Chips | fila de chips | "Todos", "Activos", "Saldados" — filtran la lista de eventos | Sí |
| Event Grid | LazyVerticalGrid | Grid de 2 columnas con tarjetas de evento | Sí |
| Event Card | card | Nombre del evento, fecha, badge de estado, monto total, tu parte, avatares de participantes | Sí |
| Create Event FAB | botón flotante | Botón circular con icono + para crear evento | Sí |
| Create Event Dialog | modal | Formulario con nombre y fecha para nuevo evento | Sí |
| Bottom Navigation | barra | Events, Profiles, Settings | Sí |

## Estados de la interfaz
- Sin eventos: mensaje "No hay eventos" con FAB visible para crear el primero
- Con eventos activos: grid de tarjetas con badge "Active"
- Con eventos saldados: tarjetas con opacidad reducida y badge "Saldado"
- Filtro activo: solo se muestran eventos del estado seleccionado

## Reglas de interacción
- Pulsar una tarjeta de evento abre su detalle (`UI0002B1`)
- FAB abre el diálogo de creación de evento
- Filter chips: al seleccionar un chip, se filtra la lista inmediatamente
- El Balance Summary **no pertenece a esta pantalla** — está en `DashboardScreen` (`UI0007B1`)
- Bottom nav: el ítem activo usa icono relleno y texto en negrita

## Navegación
- Origen: Bottom nav (primera pestaña) o Dashboard
- Destino: `UI0002B1` al abrir un evento
- Bottom nav: Events, Profiles, Settings

## Consideraciones UX/UI
- **Layout LazyVerticalGrid**: grid de 2 columnas con espaciado uniforme entre tarjetas
- **Tipografía**: Geist para títulos y cuerpo, JetBrains Mono para montos en las tarjetas
- **Filter chips**: Material 3 FilterChip con color `primaryContainer` para el estado seleccionado
- **FAB**: color `primaryContainer`, animación de escala al pulsar

## Referencias de diseño
- Sistema visual completo: `documentation/nfr/NFR0001B1-experiencia-visual-y-personalizacion.md`
- Código fuente: `shared/src/commonMain/kotlin/com/cuentamorosos/ui/EventsScreen.kt`

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-05-14 | B | 1 | Alta | Rediseño completo con layout Bento Grid, balance summary y tarjetas enriquecidas. Basado en concepto Neo-Fintech Precision. |
| 2026-06-18 | B | 2 | Actualización | Sincronizado con código real: reemplazado "Bento Grid" por LazyVerticalGrid, eliminado Balance Summary (pertenece a DashboardScreen). Documentados filter chips y create-event dialog. |
