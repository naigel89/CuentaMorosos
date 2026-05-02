# SPR0004A1: Sprint 04 - Deuda técnica y completitud

> **Código:** SPR0004A1
> **Versión:** A
> **Revisión:** 2
> **Fecha:** 2026-04-30

## Objetivo del sprint
Cerrar la deuda técnica acumulada en los sprints anteriores: permitir eliminar eventos y perfiles, solicitar el permiso de notificaciones en runtime, mejorar la liquidación de deudas y preparar las notificaciones en segundo plano.

## Estado
Hecho

## Requisitos e historias incluidas
| ID | Tipo | Nombre | Prioridad | Estado | Dependencias |
|---|---|---|---|---|---|
| DT0001A1 | DT | Eliminación de eventos y perfiles | Alta | Hecho | FR0001A1, FR0002A1 |
| DT0002A1 | DT | Solicitud de permiso POST_NOTIFICATIONS en runtime (Android 13+) | Alta | Hecho | FR0004A1 |
| DT0004A1 | DT | Liquidación real de deudas (mínimo de transferencias entre perfiles) | Media | Hecho | FR0005A1, RN0003A1 |
| DT0005A1 | DT | Notificaciones en segundo plano con WorkManager | Media | Hecho | FR0004A1, API0001A1 |
| DT0003A1 | DT | Ampliar selector de iconos de perfil (más de 6 opciones) | Baja | Hecho | FR0002A1 |
| FR0001B1 | FR | Vista de calendario de eventos | Baja | Pendiente | FR0001A1 |

## Tareas técnicas

### DT0001A1 — Eliminación de eventos y perfiles ✓
- Botón "Eliminar" añadido en la tarjeta de cada evento en `EventsScreen`.
- Diálogo `AlertDialog` de confirmación antes de eliminar el evento.
- Al confirmar, `removeEvent()` elimina el evento y todas sus `EventDebtItem` y `EventExpenseItem` asociadas, llamando a `persistData()`.
- Botón "Eliminar" añadido en `ProfileDetailDialog` (junto a "Editar" y "Cerrar").
- Diálogo `AlertDialog` de confirmación antes de eliminar el perfil.
- Al confirmar, `removeProfile()` elimina el perfil y todas sus deudas en todos los eventos.
- Funciones `removeEvent()` y `removeProfile()` añadidas al final de `CuentaMorososApp.kt` usando `SnapshotStateList.removeAll`.

### DT0002A1 — Permiso POST_NOTIFICATIONS en runtime ✓
- `MainActivity` registra `ActivityResultContracts.RequestPermission` antes de `setContent`.
- En `onCreate`, `requestNotificationsPermissionIfNeeded()` verifica si `Build.VERSION.SDK_INT >= TIRAMISU` y si el permiso no está concedido antes de lanzar la solicitud.
- El flujo no se bloquea si el usuario rechaza el permiso.

### DT0004A1 — Liquidación real de deudas ✓
- Función `buildSettlementTransfers(profileNames, amounts)` añadida en `CalculatorEngine.kt`.
- Algoritmo greedy O(n log n): calcula balances netos en céntimos, separa acreedores y deudores, genera transferencias mínimas emparejando el mayor deudor con el mayor acreedor iterativamente.
- Data class `SettlementTransfer(fromName, toName, amount)` añadida como tipo de retorno.
- `SettlementSuggestionCard` en `CuentaMorososApp.kt` actualizada para usar `buildSettlementTransfers` con `remember`, mostrando "X → Y: importe" en lugar de la presentación simplificada anterior.

### DT0005A1 — WorkManager para notificaciones en segundo plano ✓
- Dependencia `androidx.work:work-runtime-ktx:2.9.1` añadida en `app/build.gradle.kts`.
- Nuevo archivo `data/ReminderWorker.kt`: `CoroutineWorker` que carga datos del store, ejecuta `ReminderService.buildReminderMessages` y llama a `NotificationScheduler.postReminders`.
- `ReminderWorker.schedule(context)` programa trabajo periódico de 24h con `ExistingPeriodicWorkPolicy.UPDATE`.
- `ReminderWorker.cancel(context)` cancela el trabajo por nombre único.
- `MainActivity.scheduleReminderWorkerIfEnabled()` programa o cancela según las preferencias al arrancar.
- En `CuentaMorososApp`, el callback `onSavePreferences` reprograma o cancela el worker cuando el usuario cambia `remindersEnabled`.

### DT0003A1 — Ampliar selector de iconos de perfil ✓
- Lista `iconOptions` en `ProfileEditorDialog` ampliada de 6 a 30 emojis variados (personas, profesiones, hobbies, objetos).
- Layout cambiado de `Row` a `LazyVerticalGrid(GridCells.Fixed(6))` con 160dp de alto y scroll interno.
- Los iconos seleccionados se muestran con `Button` relleno; los no seleccionados con `OutlinedButton`.
- Imports añadidos: `LazyVerticalGrid`, `GridCells`, `aspectRatio`.

## Riesgos o bloqueos
- WorkManager requiere la dependencia `work-runtime-ktx`; ya añadida en `build.gradle.kts`.
- La vista de calendario (`FR0001B1`) queda pendiente para el sprint 05 por su mayor complejidad.
- El permiso `POST_NOTIFICATIONS` solo aplica a Android 13+; implementación condicional con `Build.VERSION_CODES.TIRAMISU`.

## Definition of Done
- [x] el usuario puede eliminar un evento y todos sus datos asociados desaparecen
- [x] el usuario puede eliminar un perfil y sus deudas en todos los eventos desaparecen
- [x] en Android 13+ la app solicita el permiso de notificaciones al arrancar si no está concedido
- [x] la liquidación en `SettlementSuggestionCard` usa el algoritmo de mínimo de transferencias real
- [x] el selector de iconos de perfil ofrece 30 opciones en un grid con scroll
- [x] WorkManager programa recordatorios diarios en segundo plano

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-30 | A | A.1 | Alta | Creación del sprint 04 para cerrar deuda técnica acumulada. |
| 2026-04-30 | A | A.2 | Actualización | Sprint completado: eliminación de eventos/perfiles, permiso runtime, liquidación real, WorkManager y grid de iconos implementados. |

## Objetivo del sprint
Cerrar la deuda técnica acumulada en los sprints anteriores: permitir eliminar eventos y perfiles, solicitar el permiso de notificaciones en runtime, mejorar la liquidación de deudas y preparar las notificaciones en segundo plano.

## Estado
Pendiente

## Requisitos e historias incluidas
| ID | Tipo | Nombre | Prioridad | Estado | Dependencias |
|---|---|---|---|---|---|
| DT0001A1 | DT | Eliminación de eventos y perfiles | Alta | Pendiente | FR0001A1, FR0002A1 |
| DT0002A1 | DT | Solicitud de permiso POST_NOTIFICATIONS en runtime (Android 13+) | Alta | Pendiente | FR0004A1 |
| DT0004A1 | DT | Liquidación real de deudas (mínimo de transferencias entre perfiles) | Media | Pendiente | FR0005A1, RN0003A1 |
| DT0005A1 | DT | Notificaciones en segundo plano con WorkManager | Media | Pendiente | FR0004A1, API0001A1 |
| DT0003A1 | DT | Ampliar selector de iconos de perfil (más de 6 opciones) | Baja | Pendiente | FR0002A1 |
| FR0001B1 | FR | Vista de calendario de eventos | Baja | Pendiente | FR0001A1 |

## Tareas técnicas

### DT0001A1 — Eliminación de eventos y perfiles
- Añadir botón "Eliminar evento" en `EventDetailScreen` o en la tarjeta de evento en `EventsScreen`.
- Al eliminar un evento, eliminar también todas sus deudas (`EventDebtItem`) y gastos (`EventExpenseItem`) asociados y persistir.
- Añadir opción "Eliminar perfil" en `ProfileDetailDialog`.
- Al eliminar un perfil, eliminar también todas sus deudas en todos los eventos y persistir.
- Mostrar diálogo de confirmación antes de eliminar en ambos casos.
- Añadir funciones `removeEvent(eventId)` y `removeProfile(profileId)` en `CuentaMorososApp`.

### DT0002A1 — Permiso POST_NOTIFICATIONS en runtime
- En `MainActivity.onCreate`, detectar si `Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU` (API 33).
- Si el permiso `POST_NOTIFICATIONS` no ha sido concedido, solicitarlo con `ActivityResultContracts.RequestPermission`.
- Registrar el launcher antes de `setContent` con `registerForActivityResult`.
- Solo mostrar la solicitud si el permiso no está ya concedido (`ContextCompat.checkSelfPermission`).

### DT0004A1 — Liquidación real de deudas
- Implementar algoritmo de liquidación mínima (problema de "simplificación de deudas") en `CalculatorEngine.kt` o en una nueva función en `model/`.
- La lógica: calcular balance neto de cada perfil (lo que debe menos lo que le deben), ordenar acreedores y deudores, y generar la lista mínima de transferencias.
- Actualizar `SettlementSuggestionCard` en `CuentaMorososApp.kt` para usar esta lógica real en lugar de la presentación simplificada actual.

### DT0005A1 — Notificaciones en segundo plano con WorkManager
- Añadir dependencia `androidx.work:work-runtime-ktx` en `build.gradle`.
- Crear `ReminderWorker` en `data/` que extienda `CoroutineWorker`:
  - Carga datos desde `CuentaMorososLocalStore`.
  - Ejecuta `ReminderService.buildReminderMessages(...)`.
  - Llama a `NotificationScheduler.postReminders(context, messages)`.
- Programar el worker en `MainActivity` con `PeriodicWorkRequestBuilder` (periodicidad: 1 vez al día).
- Cancelar y reprogramar el worker cuando el usuario desactive los recordatorios en ajustes.

### DT0003A1 — Ampliar selector de iconos de perfil
- Reemplazar la lista `iconOptions` de 6 elementos en `ProfileEditorDialog` por una lista ampliada (mínimo 20 emojis variados: personas, profesiones, hobbies, etc.).
- Presentar los iconos en un grid con scroll en lugar de una fila simple.

### FR0001B1 — Vista de calendario (baja prioridad)
- Implementar la navegación al presionar el botón "Calendario" en `EventsScreen`.
- Mostrar una vista mensual básica con los eventos marcados en su fecha correspondiente.
- Al pulsar un día con evento, navegar al detalle del evento.

## Orden de implementación recomendado
1. DT0002A1 — permiso runtime (prerequisito para que las notificaciones funcionen en Android 13+)
2. DT0001A1 — eliminación de eventos y perfiles (alta prioridad de negocio)
3. DT0004A1 — liquidación real (mejora de calidad en la calculadora)
4. DT0005A1 — WorkManager en background (depende de DT0002A1)
5. DT0003A1 — más iconos (baja complejidad, se puede hacer en cualquier momento)
6. FR0001B1 — calendario (baja prioridad, puede pasarse al sprint 05 si hay bloqueos)

## Riesgos o bloqueos
- WorkManager requiere añadir dependencia en `build.gradle`; no se puede validar el build en el entorno actual por ausencia de `JAVA_HOME`.
- La vista de calendario puede requerir una librería externa o implementación manual costosa.
- El permiso `POST_NOTIFICATIONS` solo aplica a Android 13+; en versiones anteriores no es necesario y no debe solicitarse.
- Al eliminar perfiles con deudas activas en varios eventos, hay que garantizar la integridad de los datos (no dejar deudas huérfanas).

## Definition of Done
- [ ] el usuario puede eliminar un evento y todos sus datos asociados desaparecen
- [ ] el usuario puede eliminar un perfil y sus deudas en todos los eventos desaparecen
- [ ] en Android 13+ la app solicita el permiso de notificaciones al arrancar si no está concedido
- [ ] la liquidación en `SettlementSuggestionCard` usa el algoritmo de mínimo de transferencias real
- [ ] el selector de iconos de perfil ofrece al menos 20 opciones en un grid con scroll
- [ ] (opcional) WorkManager programa recordatorios diarios en segundo plano

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-30 | A | A.1 | Alta | Creación del sprint 04 para cerrar deuda técnica acumulada: eliminación, permisos runtime, liquidación real y notificaciones en background. |
