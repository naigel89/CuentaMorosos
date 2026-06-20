# UI0007B1: Panel de Control (Dashboard) — Nueva pantalla principal

> **Código:** UI0007B1
> **Versión:** B
> **Revisión:** 2
> **Fecha:** 2026-06-18

## Resumen
Pantalla principal de la aplicación que ofrece una vista de alto nivel con resumen financiero, balance neto y deudas unificadas por perfil. Implementada en `DashboardScreen.kt` dentro de `shared/src/commonMain/kotlin/com/cuentamorosos/ui/`.

## Historia de usuario relacionada
Como usuario, quiero al abrir la app ver inmediatamente cuánto me deben, cuánto debo yo, y qué deudas tengo pendientes con cada persona.

## Objetivo de la pantalla
Ser el centro de comando financiero: resumir el estado de deudas del usuario, mostrar el balance neto codificado por color, y permitir navegación rápida a cualquier sección.

## Componentes visibles
| Componente | Tipo | Descripción | Obligatorio |
|---|---|---|---|
| Financial Summary Row | fila 2 cards | "DEBES" (rojo) + "TE DEBEN" (verde) con montos grandes | Sí |
| Card DEBES | card | Monto total que el usuario debe a otros, borde/color rojo | Sí |
| Card TE DEBEN | card | Monto total que otros deben al usuario, borde/color verde | Sí |
| Net Balance Card | card | Balance neto (TE DEBEN − DEBES), fondo verde si positivo, rojo si negativo | Sí |
| Unified Debts Card | lista | Lista unificada de deudas por perfil: avatar, nombre, monto y dirección (te debe/debes) | Sí |
| Bottom Navigation | barra | Events, Profiles, Settings | Sí (móvil) |

## Estados de la interfaz
- Dashboard con datos: todos los indicadores con montos y lista de deudas
- **Sin deudas**: las cards financieras (DEBES, TE DEBEN, Balance Neto) se muestran siempre, incluso con montos en `0,00 €`. La Unified Debts Card muestra el mensaje "No tenés deudas pendientes"
- Balance positivo: Net Balance Card con fondo `primaryContainer` (verde)
- Balance negativo: Net Balance Card con fondo `error` (rojo)
- Balance cero: Net Balance Card con fondo `surfaceContainerHigh` (neutro)

## Reglas de interacción
- Pulsar un perfil en Unified Debts Card: navega al detalle de ese perfil
- Bottom nav: navegación entre Events, Profiles, Settings
- El dashboard es accesible desde la pestaña Events como vista principal

## Navegación
- Origen: pantalla inicial de la app (primera pestaña del Bottom Nav)
- Destinos: `UI0001B1` (eventos), `UI0002B1` (detalle), `UI0003B1` (perfiles), `UI0004B1` (settings)

## Planned Features
Los siguientes componentes fueron parte del diseño conceptual Neo-Fintech pero **no están implementados** en el código actual:
- **Smart Alerts**: Alertas proactivas (eventos sin participantes, cálculos pendientes). Planeado para futura iteración.
- **Recent Activity**: Feed de actividad reciente con timestamps y badges de estado. Planeado para futura iteración.
- **Top App Bar**: Avatar del usuario, nombre de la app, toggle de tema rápido. Actualmente el toggle de tema está en SettingsScreen (`UI0004B1`).

## Consideraciones UX/UI
- **Montos grandes**: Display Large en Geist 700 para los totales en Financial Summary Row
- **Codificación por color**: verde (`primaryContainer`) para montos a favor, rojo (`error`) para deudas
- **Net Balance Card**: fondo dinámico — `primaryContainer` si el balance es ≥ 0, `error` si es negativo
- **Unified Debts Card**: cada fila muestra avatar circular, nombre del perfil, monto con signo (+/−) y color semántico

## Referencias de diseño
- Sistema visual completo: `documentation/nfr/NFR0001B1-experiencia-visual-y-personalizacion.md`
- Código fuente: `shared/src/commonMain/kotlin/com/cuentamorosos/ui/DashboardScreen.kt`

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-05-14 | B | 1 | Alta | Nueva pantalla principal (Dashboard/Panel de Control) con indicadores financieros, Smart Alerts y actividad reciente. Basada en concepto Neo-Fintech Precision. |
| 2026-06-18 | B | 2 | Actualización | Sincronizado con código real: removidos Smart Alerts, Recent Activity, Top App Bar (no implementados). Documentados Financial Summary Row, Net Balance Card, Unified Debts Card. Añadida sección Planned Features. |
| 2026-06-20 | B | 3 | Actualización | Documentado que las cards financieras (DEBES, TE DEBEN, Balance Neto) son siempre visibles incluso con montos en cero. Agregado estado "Balance cero". |
