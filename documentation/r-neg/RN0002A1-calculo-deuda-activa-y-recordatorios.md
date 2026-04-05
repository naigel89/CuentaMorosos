# RN0002A1: Cálculo de deuda activa y recordatorios

> **Código:** RN0002A1
> **Versión:** A
> **Revisión:** 2
> **Fecha:** 2026-04-05

## Descripción
El total mostrado por perfil debe reflejar únicamente la **deuda pendiente activa**, excluyendo los importes ya marcados como pagados, independientemente de si la cantidad se introdujo manualmente o mediante la calculadora automática del evento.

## Motivación
El usuario necesita una visión real de lo que todavía le deben y no del histórico completo ya cobrado.

## Condiciones
- la suma total por perfil incluye solo eventos no pagados
- el desglose interno del perfil no muestra eventos ya saldados
- las notificaciones se generan solo sobre deudas pendientes o eventos incompletos
- los importes deben expresarse en euros y admitir decimales
- las simulaciones de la calculadora no alteran el total pendiente hasta que el usuario confirme `Aplicar cálculo`

## Impacto funcional
- la pantalla de perfiles muestra una cifra agregada útil para reclamar pagos
- el sistema de recordatorios evita avisos sobre eventos ya cerrados
- el cálculo automático se integra sin romper la lógica de deuda activa

## Ejemplos
- **Válido:** si Luis debía `12,50 €` en un evento ya pagado y `8,00 €` en otro pendiente, el total visible será `8,00 €`
- **Válido:** simular un nuevo reparto en la calculadora sin confirmar no cambia el total mostrado en la pantalla de perfiles
- **No válido:** seguir mostrando el evento ya pagado en el total pendiente del perfil

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-05 | A | 1 | Alta | Creación inicial de la regla de negocio sobre deuda activa y recordatorios. |
| 2026-04-05 | A | 2 | Actualización | Se aclara la integración de la deuda activa con la calculadora automática del evento. |
