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

### Requisitos no funcionales
- `NFR0001A1-experiencia-visual-y-personalizacion.md`
- `NFR0002A1-persistencia-y-usabilidad.md`

### Reglas de negocio
- `RN0001A1-estado-de-pago-y-visibilidad-por-evento.md`
- `RN0002A1-calculo-deuda-activa-y-recordatorios.md`
- `RN0003A1-reglas-de-reparto-y-redondeo-automatico.md`

### Diseño detallado
- `DD0001A1-modelo-funcional-base.md`
- `DD0002A1-diseno-calculadora-automatica-por-evento.md`

### UI
- `UI0001A1-pantalla-eventos.md`
- `UI0002A1-detalle-evento-y-pagos.md`
- `UI0003A1-pantalla-perfiles.md`
- `UI0004A1-ajustes-y-apariencia.md`
- `UI0005A1-calculadora-automatica-por-evento.md`

### API
- `API0001A1-servicios-locales-y-notificaciones.md`

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-05 | A | 4 | Actualización | Se añade el índice documental completo de CuentaMorosos. |
| 2026-04-05 | A | 5 | Actualización | Se incorpora la funcionalidad de calculadora automática de cuentas por evento. |
