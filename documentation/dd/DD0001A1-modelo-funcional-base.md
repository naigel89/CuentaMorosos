# DD0001A1: Modelo funcional base de CuentaMorosos

> **Código:** DD0001A1
> **Versión:** A
> **Revisión:** 1
> **Fecha:** 2026-04-05

## Resumen técnico
Se define una propuesta de diseño funcional para una app Android `local-first` que gestione eventos, perfiles, deudas, notas y estados de pago.

## Historia de usuario relacionada
Como usuario, quiero registrar eventos y perfiles con sus importes pendientes para controlar qué personas me deben dinero y cuándo ya me han pagado.

## Componentes involucrados
- `MainActivity` como punto de entrada actual
- pantalla de eventos
- detalle de evento
- pantalla de perfiles
- módulo de ajustes y apariencia
- sistema local de persistencia y recordatorios

## Flujo de solución
1. el usuario crea perfiles reutilizables con nombre e icono
2. crea un evento con nombre y fecha
3. añade perfiles al evento y registra importe y notas
4. la app calcula los pendientes por evento y por perfil
5. cuando alguien paga, el estado cambia a `pagado` y el registro se oculta en la lista principal del evento
6. el módulo de recordatorios revisa deudas activas y eventos incompletos

## Modelo de datos
### Entidades principales
- **Evento**: `id`, `nombre`, `fecha`, `estadoVisual/opcional`
- **Perfil**: `id`, `nombre`, `icono`, `fotoUri/opcional`
- **DeudaEventoPerfil**: `id`, `eventoId`, `perfilId`, `importeEuro`, `notas`, `pagado`, `fechaPago/opcional`
- **PreferenciasUsuario**: `tema`, `colorSecundario`, `diasRecordatorio`

## Dependencias y riesgos
- selección de imágenes para fotos de perfil
- licencia de la tipografía `TT Hoves Pro`
- implementación fiable de recordatorios en Android

## Decisiones técnicas
- interfaz construida con **Jetpack Compose**
- persistencia local recomendada para eventos y perfiles
- notificaciones programadas para los recordatorios de deuda
- agregación de deuda activa filtrando solo registros no pagados

## Suposiciones
- A fecha actual, el código fuente solo contiene la base de la app; este documento describe el diseño objetivo a implementar.

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-05 | A | 1 | Alta | Creación inicial del diseño detallado funcional base del proyecto. |
