# Documentación del proyecto

Esta carpeta centraliza la documentación funcional, técnica, de planificación y de API del proyecto.

## Estructura

- `dd/`: documentos de diseño detallado
- `fr/`: requisitos funcionales
- `nfr/`: requisitos no funcionales
- `r-neg/`: reglas de negocio
- `api/`: documentación de endpoints y contratos
- `ui/`: documentación de pantallas, flujos visuales y comportamiento de interfaz
- `sprints/`: backlog y planificación iterativa del trabajo pendiente
- `CHANGELOG.md`: registro global de altas y revisiones documentales

## Convenciones de nomenclatura

- Formato: `<TIPO><XXXX><VERSIÓN><REVISIÓN>-<nombre>.md`
- Ejemplos:
  - `DD0001A1-arquitectura.md`
  - `FR0002A1-login.md`
  - `API0003B2-clientes.md`
  - `UI0004A1-pantalla-login.md`
- `XXXX`: correlativo de 4 dígitos
- `VERSIÓN`: letra (`A`, `B`, `C`...)
- `REVISIÓN`: número (`1`, `2`, `3`...)

## Convenciones generales

- Todos los archivos deben estar en formato Markdown (`.md`).
- Todo documento nuevo comienza por defecto en versión `A` y revisión `1`.
- Cuando aplique, incluir historias de usuario con el formato:
  - `Como <rol>, quiero <objetivo>, para <beneficio>.`
- Cada documento debe incluir su propio `## Changelog` con fecha, versión, revisión y descripción del cambio.
- El fichero `documentation/CHANGELOG.md` debe reflejar cada alta o actualización relevante.

## Índice actual

### Requisitos funcionales
- `FR0001A1-gestion-eventos-y-calendario.md`
- `FR0002A1-gestion-global-de-perfiles.md`
- `FR0003A1-control-deudas-pagos-y-notas.md`
- `FR0004A1-notificaciones-y-recordatorios.md`
- `FR0005A1-calculadora-automatica-cuentas-por-evento.md`
- `FR0006A1-perfiles-fantasma.md`
- `FR0008A1-divisiones-porcentuales-item.md`
- `FR0009A1-user-stories-online-ios.md`

### Requisitos no funcionales
- `NFR0001A1-experiencia-visual-y-personalizacion.md` — Versión A (TT Hoves Pro, gradients)
- `NFR0001B1-experiencia-visual-y-personalizacion.md` — Versión B (Neo-Fintech Precision, Geist, animaciones)
- `NFR0002A1-persistencia-y-usabilidad.md`

### Reglas de negocio
- `RN0001A1-estado-de-pago-y-visibilidad-por-evento.md`
- `RN0002A1-calculo-deuda-activa-y-recordatorios.md`
- `RN0003A1-reglas-de-reparto-y-redondeo-automatico.md`

### Diseño detallado
- `DD0001A1-modelo-funcional-base.md`
- `DD0002A1-diseno-calculadora-automatica-por-evento.md`
- `DD0003A1-especificaciones-tecnicas-online.md`
- `DD0004A1-guia-migracion-datos-firebase.md`
- `DD0005A1-configuracion-multiplatform-ios.md`

### UI — Versión A (vigente)
- `UI0001A1-pantalla-eventos.md`
- `UI0002A1-detalle-evento-y-pagos.md`
- `UI0003A1-pantalla-perfiles.md`
- `UI0004A1-ajustes-y-apariencia.md`
- `UI0005A1-calculadora-automatica-por-evento.md`
- `UI0006A1-simplificacion-calculadora.md`

### UI — Versión B (rediseño Neo-Fintech Precision)
- `UI0001B1-pantalla-eventos.md` — Eventos con LazyVerticalGrid y filter chips
- `UI0002B1-detalle-evento-y-pagos.md` — Detalle con panel de settlement multi-deuda, applyCalculation sync-safe
- `UI0003B1-pantalla-perfiles.md` — Perfiles con avatar, monto y color semántico
- `UI0004B1-ajustes-y-apariencia.md` — Ajustes con theme selector, reminders y sign-out
- `UI0005B1-calculadora-automatica-por-evento.md` — Calculadora con 6 modos SplitMode visibles
- `UI0007B1-panel-de-control.md` — Dashboard con Financial Summary, Net Balance y Unified Debts
- `UI0008B1-autenticacion-firebase.md` — **NUEVA**: Pantallas de autenticación Firebase (Login, Register, ForgotPassword)

### API
- `API0001A1-servicios-locales-y-notificaciones.md`

## Referencias de Diseño

Los conceptos visuales del rediseño Neo-Fintech Precision se encuentran en:

```
app/documentation/Concepto Diseño Pantallas/stitch_splitflow_expenses/
```

### Estructura de la carpeta de diseño

| Carpeta | Contenido | Modo |
|---------|-----------|------|
| `neo_fintech_precision_1/` | DESIGN.md + referencia visual | Claro |
| `neo_fintech_precision_2/` | DESIGN.md + referencia visual | Oscuro |
| `panel_de_control_1/` | Dashboard (code.html + screen.png) | Claro |
| `panel_de_control_2/` | Dashboard (code.html + screen.png) | Oscuro |
| `eventos_neon_edit_1/` | Eventos (code.html + screen.png) | Claro |
| `eventos_neon_edit_2/` | Eventos (code.html + screen.png) | Oscuro |
| `detalle_del_evento_neon_edit_1/` | Detalle evento (code.html + screen.png) | Claro |
| `detalle_del_evento_neon_edit_2/` | Detalle evento (code.html + screen.png) | Oscuro |
| `perfiles_neon_edit_1/` | Perfiles (code.html + screen.png) | Claro |
| `perfiles_neon_edit_2/` | Perfiles (code.html + screen.png) | Oscuro |
| `ajustes_neon_edit_1/` | Ajustes (code.html + screen.png) | Claro |
| `ajustes_neon_edit_2/` | Ajustes (code.html + screen.png) | Oscuro |

**Convención**: `_1` = modo claro, `_2` = modo oscuro.

Cada subcarpeta de pantalla contiene:
- `code.html` — prototipo HTML funcional con Tailwind CSS
- `screen.png` — captura visual del diseño

Los archivos `DESIGN.md` en `neo_fintech_precision_1/` y `neo_fintech_precision_2/` contienen la guía de estilo completa (colores, tipografía, spacing, componentes).

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-05 | A | 4 | Actualización | Se añade el índice documental completo de CuentaMorosos. |
| 2026-04-05 | A | 5 | Actualización | Se incorpora la funcionalidad de calculadora automática de cuentas por evento. |
| 2026-05-14 | B | 1 | Alta | Rediseño Neo-Fintech Precision: 6 documentos UI versión B + NFR0001B1 con sistema de diseño completo, paleta dual, Geist + JetBrains Mono y guía de animaciones. Nueva pantalla Panel de Control (UI0007B1). Referencias de diseño añadidas. |
| 2026-06-18 | B | 2 | Actualización | Índice completado: añadidos FR0006A1, FR0008A1, FR0009A1, DD0003A1, DD0004A1, DD0005A1. Eliminado FR0007A1. |
| 2026-06-21 | B | 3 | Actualización | UI0002B1 revisión 3: multi-deuda y applyCalculation sync-safe. RN0003A1 revisión 2: round-robin. FR0005A1 revisión 2: modos reales. |
