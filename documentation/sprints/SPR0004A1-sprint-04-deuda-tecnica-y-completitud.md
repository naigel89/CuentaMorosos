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

## Tareas técnicas

### DT0001A1 — Eliminación de eventos y perfiles ✓
- Botón "Eliminar" añadido en la tarjeta de cada evento en `EventsScreen`.
- Diálogo `AlertDialog` de confirmación antes de eliminar el evento.
- Al confirmar, `removeEvent()` elimina el evento y todas sus `EventDebtItem` y `EventExpenseItem` asociadas, llamando a `persistData()`.
- Botón "Eliminar" añadido en `ProfileDetailDialog` (junto a "Editar" y "Cerrar").
- Funciones `removeEvent()` y `removeProfile()` añadidas al final de `CuentaMorososApp.kt`.

### DT0002A1 — Permiso POST_NOTIFICATIONS en runtime ✓
- `MainActivity` registra `ActivityResultContracts.RequestPermission` antes de `setContent`.
- En `onCreate`, `requestNotificationsPermissionIfNeeded()` verifica si `Build.VERSION.SDK_INT >= TIRAMISU`.

### DT0004A1 — Liquidación real de deudas ✓
- Función `buildSettlementTransfers(profileNames, amounts)` añadida en `CalculatorEngine.kt`.
- Algoritmo greedy O(n log n): calcula balances netos en céntimos, separa acreedores y deudores.
- `SettlementSuggestionCard` actualizada para usar `buildSettlementTransfers`.

### DT0005A1 — WorkManager para notificaciones en segundo plano ✓
- Dependencia `androidx.work:work-runtime-ktx:2.9.1` añadida.
- `ReminderWorker` en `data/`: `CoroutineWorker` que carga datos, ejecuta `ReminderService` y llama a `NotificationScheduler`.
- `MainActivity.scheduleReminderWorkerIfEnabled()` programa o cancela según preferencias.

### DT0003A1 — Ampliar selector de iconos de perfil ✓
- Lista `iconOptions` en `ProfileEditorDialog` ampliada de 6 a 30 emojis.
- Layout cambiado a `LazyVerticalGrid(GridCells.Fixed(6))` con scroll interno.

## Definition of Done
- [x] el usuario puede eliminar un evento y todos sus datos asociados desaparecen
- [x] el usuario puede eliminar un perfil y sus deudas en todos los eventos desaparecen
- [x] en Android 13+ la app solicita el permiso de notificaciones al arrancar si no está concedido
- [x] la liquidación usa el algoritmo de mínimo de transferencias real
- [x] el selector de iconos ofrece 30 opciones en grid con scroll
- [x] WorkManager programa recordatorios diarios en segundo plano

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-30 | A | A.1 | Alta | Creación del sprint 04 para cerrar deuda técnica acumulada. |
| 2026-04-30 | A | A.2 | Actualización | Sprint completado: eliminación de eventos/perfiles, permiso runtime, liquidación real, WorkManager y grid de iconos implementados. |
