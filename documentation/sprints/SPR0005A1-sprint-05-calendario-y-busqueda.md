# SPR0005A1: Sprint 05 - Calendario y búsqueda

> **Código:** SPR0005A1
> **Versión:** A
> **Revisión:** 2
> **Fecha:** 2026-04-30

## Objetivo del sprint
Mejorar la navegación y la localización de eventos: añadir una vista de calendario mensual que sitúe los eventos en su fecha y una barra de búsqueda con filtros de estado en `EventsScreen`.

## Estado
Hecho

## Requisitos e historias incluidas
| ID | Tipo | Nombre | Prioridad | Estado | Dependencias |
|---|---|---|---|---|---|
| FR0001B1 | FR | Vista de calendario de eventos | Baja | Hecho | FR0001A1 |
| UX0001A1 | UX | Búsqueda y filtrado de eventos | Media | Hecho | FR0001A1 |

## Tareas técnicas

### FR0001B1 — Vista de calendario de eventos ✓
- Nuevo valor `CALENDAR` ("Calendario", "🗓️") añadido al enum `MainSection`.
- Nuevo composable privado `CalendarScreen(modifier, events, pendingTotalsByEvent, onOpenEvent)`.
- Estado interno: `displayYear`, `displayMonth` (navegables con botones `←` / `→`), `selectedDay`.
- `calGrid` calculado con `derivedStateOf`: lista de `Int?` (null = celda vacía) con el primer día del mes ajustado a semana europea (lunes = columna 0).
- `eventsByDay` calculado con `derivedStateOf`: mapa `day → List<EventItem>` para el mes/año mostrado.
- Encabezados de semana "L M X J V S D" en `Row`.
- Cuadrícula renderizada con `calGrid.chunked(7).forEach { week -> Row { ... } }` y celdas de aspecto 1:1.
- Día hoy resaltado con `primaryContainer`; día seleccionado con `primary`; días con eventos muestran un punto de 5dp bajo el número.
- Al seleccionar un día con eventos, aparece debajo del calendario una lista con `ElevatedCard` por evento (nombre + deuda pendiente); al pulsar, navega a `EventDetailScreen`.
- Importes de deuda se toman de `pendingTotalsByEvent` pasado desde `CuentaMorososApp`.
- Imports añadidos: `background`, `size`, `width`, `CircleShape`, `Icons`, `Icons.AutoMirrored.Filled.ArrowBack/Forward`, `ElevatedCard`, `Icon`, `IconButton`, `DateFormatSymbols`, `Calendar`, `Locale`.

### UX0001A1 — Búsqueda y filtrado de eventos ✓
- `var searchQuery by remember { mutableStateOf("") }` añadido como estado local en `EventsScreen`.
- `var activeFilter by remember { mutableStateOf(0) }` para los chips (0=Todos, 1=Con deuda, 2=Sin deuda).
- `filteredEvents` calculado con `derivedStateOf` aplicando ambos filtros sobre `events`.
- `OutlinedTextField` de búsqueda (label "Buscar evento", `singleLine`, trailing icon "✕" para limpiar).
- `Row` de tres `FilterChip`: "Todos" / "Con deuda" / "Sin deuda"; el chip activo queda marcado como `selected`.
- `LazyColumn` ahora itera `filteredEvents` en lugar de `events`.
- Estado vacío diferenciado: si `events.isEmpty()` → mensaje original; si `filteredEvents.isEmpty()` → "Sin resultados para «query»" o "No hay eventos que cumplan el filtro".
- Botón "Calendario próximamente" (desactivado) eliminado de la cabecera, ya que la pestaña de calendario es real.

## Riesgos o bloqueos
- Ninguno. El calendario se implementó con composables propios sin librerías adicionales.

## Definition of Done
- [x] Existe una pestaña "Calendario" en la barra de navegación inferior
- [x] El calendario mensual muestra correctamente todos los eventos en su fecha
- [x] Se puede navegar entre meses
- [x] Al pulsar un día con eventos se ven los eventos de ese día y se puede acceder al detalle
- [x] `EventsScreen` tiene barra de búsqueda funcional por nombre
- [x] Los chips "Todos / Con deuda / Sin deuda" filtran la lista correctamente
- [x] El estado vacío muestra mensaje apropiado cuando la búsqueda no arroja resultados

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-30 | A | A.1 | Alta | Creación del sprint 05 con calendario y búsqueda de eventos. |
| 2026-04-30 | A | A.2 | Actualización | Sprint completado: CalendarScreen y búsqueda+filtros en EventsScreen implementados. |

> **Código:** SPR0005A1
> **Versión:** A
> **Revisión:** 1
> **Fecha:** 2026-04-30

## Objetivo del sprint
Mejorar la navegación y la localización de eventos: añadir una vista de calendario mensual que sitúe los eventos en su fecha y una barra de búsqueda con filtros de estado en `EventsScreen`.

## Estado
En progreso

## Requisitos e historias incluidas
| ID | Tipo | Nombre | Prioridad | Estado | Dependencias |
|---|---|---|---|---|---|
| FR0001B1 | FR | Vista de calendario de eventos | Baja | En progreso | FR0001A1 |
| UX0001A1 | UX | Búsqueda y filtrado de eventos | Media | Pendiente | FR0001A1 |

## Tareas técnicas

### FR0001B1 — Vista de calendario de eventos
- Nueva pestaña "Calendario" en el `NavigationBar` de `CuentaMorososApp`.
- `CalendarScreen` composable con una cuadrícula mensual (7 columnas, semanas).
- Cada día con eventos muestra un indicador visual (punto coloreado bajo el número de día).
- Al pulsar un día con eventos, se despliega una lista de los eventos de ese día debajo del calendario.
- Navegación anterior/siguiente mes con botones de flecha.
- Al pulsar un evento en la lista del día, navega a `EventDetailScreen`.

### UX0001A1 — Búsqueda y filtrado de eventos
- `OutlinedTextField` de búsqueda en la parte superior de `EventsScreen`.
- Filtro en tiempo real: la lista de eventos se reduce al escribir (por nombre, insensible a mayúsculas).
- Chips de filtro bajo la barra de búsqueda: "Todos" · "Con deuda pendiente" · "Sin deuda".
- El estado de búsqueda y filtro es local al composable (`remember`), no se persiste.
- Texto de estado vacío actualizado: si hay búsqueda activa y no hay resultados, mostrar "Sin resultados para «query»".

## Riesgos o bloqueos
- El calendario se implementa con composables propios (sin librería externa) para evitar dependencias adicionales.
- La pestaña de calendario requiere añadir un ítem más al `NavigationBar` sin romper la navegación existente.

## Definition of Done
- [ ] Existe una pestaña "Calendario" en la barra de navegación inferior
- [ ] El calendario mensual muestra correctamente todos los eventos en su fecha
- [ ] Se puede navegar entre meses
- [ ] Al pulsar un día con eventos se ven los eventos de ese día y se puede acceder al detalle
- [ ] `EventsScreen` tiene barra de búsqueda funcional por nombre
- [ ] Los chips "Todos / Con deuda / Sin deuda" filtran la lista correctamente
- [ ] El estado vacío muestra mensaje apropiado cuando la búsqueda no arroja resultados

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-30 | A | A.1 | Alta | Creación del sprint 05 con calendario y búsqueda de eventos. |
