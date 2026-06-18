# UI0004B1: Ajustes y apariencia — Rediseño Neo-Fintech

> **Código:** UI0004B1
> **Versión:** B
> **Revisión:** 2
> **Fecha:** 2026-06-18

## Resumen
Pantalla de ajustes con selector de tema de 3 opciones (System/Light/Dark), toggle de recordatorios con campo de días, tarjeta de perfil, botón de cierre de sesión y botón global de guardar. Implementada en `SettingsScreen.kt` dentro de `shared/src/commonMain/kotlin/com/cuentamorosos/ui/`.

## Historia de usuario relacionada
Como usuario, quiero personalizar el tema, configurar recordatorios de pago y gestionar mi sesión desde una interfaz clara y organizada.

## Objetivo de la pantalla
Centralizar las preferencias del usuario en una sola pantalla con diseño de columna única y controles intuitivos.

## Componentes visibles
| Componente | Tipo | Descripción | Obligatorio |
|---|---|---|---|
| Theme Selector | fila de 3 botones | "Sistema", "Claro", "Oscuro" — selección exclusiva con highlight en `primaryContainer` | Sí |
| Reminders Toggle | Switch + campo | Activa/desactiva recordatorios + campo numérico para días de antelación | Sí |
| Profile Card | card | Muestra email del usuario autenticado, acceso a AccountScreen | Sí |
| Sign Out Button | botón | Cierra la sesión del usuario (requiere confirmación) | Sí |
| Save Button | botón global | Guarda todos los cambios de configuración | Sí |
| Bottom Navigation | barra | Events, Profiles, Settings | Sí |

## Estados de la interfaz
- Preferencias por defecto: tema "Sistema", recordatorios activados, 3 días
- Tema seleccionado: botón correspondiente con fondo `primaryContainer`
- Toggle activo: Switch en `primaryContainer`, campo de días editable
- Toggle inactivo: campo de días deshabilitado
- Sin guardar: botón "Guardar" visible cuando hay cambios pendientes
- Guardado: botón "Guardar" se oculta o deshabilita tras persistir

## Reglas de interacción
- Theme selector: al pulsar un botón, los otros se deseleccionan. El cambio se aplica al guardar
- Reminders toggle: al activar, el campo de días se habilita (1–30)
- Sign Out: muestra diálogo de confirmación antes de cerrar sesión
- Save: persiste todas las preferencias mediante `CuentaMorososLocalStore.savePreferences()`
- Profile card: navega a `AccountScreen` para gestionar datos de cuenta

## Navegación
- Origen: Bottom nav (tercera pestaña)
- Destino: `AccountScreen` (perfil de usuario), `LoginScreen` (tras cerrar sesión)

## Consideraciones UX/UI
- **Layout LazyColumn**: diseño de columna única, todas las secciones apiladas verticalmente
- **Theme selector**: 3 botones en fila con `primaryContainer` como color de selección
- **Reminders**: Switch Material 3 + OutlinedTextField para días
- **Save button**: botón prominente al final de la pantalla, visible solo con cambios pendientes
- **Sign Out**: botón de texto con color `error` para acción destructiva

## Referencias de diseño
- Sistema visual completo: `documentation/nfr/NFR0001B1-experiencia-visual-y-personalizacion.md`
- Código fuente: `shared/src/commonMain/kotlin/com/cuentamorosos/ui/SettingsScreen.kt`

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-05-14 | B | 1 | Alta | Rediseño con layout dos columnas, segmented control para tema, swatches de color, toggles neón, sección de seguridad. Basado en concepto Neo-Fintech Precision. |
| 2026-06-18 | B | 2 | Actualización | Sincronizado con código real: eliminados sidebar, SegmentedControl, color swatches, View Devices (no implementados). Documentado LazyColumn con theme selector de 3 botones, reminders toggle+days, profile card, sign-out y save button. |
