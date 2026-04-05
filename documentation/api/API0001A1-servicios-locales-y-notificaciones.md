# API0001A1: Servicios locales y notificaciones

> **Código:** API0001A1
> **Versión:** A
> **Revisión:** 1
> **Fecha:** 2026-04-05

## Resumen
En el estado actual del proyecto no se identifica una API REST externa implementada. Este documento define los contratos funcionales mínimos que la app necesitará para almacenamiento local y gestión de recordatorios.

## Historia de usuario relacionada
Como usuario, quiero que la app guarde mis eventos y me avise de pagos pendientes sin depender de un servicio externo obligatorio.

## Endpoint
`LOCAL /servicios-internos`

## Autenticación
No aplica en la primera versión local de la app.

## Parámetros de entrada
| Campo | Tipo | Requerido | Descripción |
|---|---|---|---|
| `evento.nombre` | texto | Sí | nombre del evento |
| `evento.fecha` | fecha | Sí | fecha asociada al evento |
| `perfil.nombre` | texto | Sí | nombre visible del perfil |
| `perfil.icono` | texto/uri | Sí | icono base o personalizado |
| `deuda.importeEuro` | decimal | Sí | importe pendiente en euros |
| `deuda.notas` | texto | No | detalle aclaratorio del gasto |
| `recordatorio.dias` | entero | No | días transcurridos para avisar |

## Ejemplo de request
```json
{
  "evento": { "nombre": "Cena viernes", "fecha": "2026-04-05" },
  "perfil": { "nombre": "Luis", "icono": "avatar_01" },
  "deuda": { "importeEuro": 20.5, "notas": "Pizza y bebida" }
}
```

## Ejemplo de response
```json
{
  "resultado": "ok",
  "eventoId": "evt_001",
  "perfilId": "prf_001",
  "deudaId": "deb_001"
}
```

## Códigos de error
| Código | Significado |
|---|---|
| `VALIDATION_ERROR` | faltan campos obligatorios o el importe no es válido |
| `NOT_FOUND` | no existe el evento o perfil referenciado |
| `SCHEDULE_ERROR` | no se pudo programar la notificación |

## Observaciones
- Documento orientado a contratos internos de la app, no a un backend remoto confirmado.
- Si en el futuro se añade sincronización en nube, este documento deberá evolucionar a una API externa versionada.

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-05 | A | 1 | Alta | Creación inicial del documento de servicios locales y notificaciones. |
