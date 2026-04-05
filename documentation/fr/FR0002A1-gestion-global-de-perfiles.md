# FR0002A1: Gestión global de perfiles

> **Código:** FR0002A1
> **Versión:** A
> **Revisión:** 1
> **Fecha:** 2026-04-05

## Resumen
La aplicación debe permitir crear y administrar perfiles reutilizables para las personas que participan en distintos eventos.

## Historia de usuario
Como usuario de `CuentaMorosos`, quiero crear perfiles con nombre e icono para reutilizarlos en varios eventos y saber cuánto me debe cada persona en total.

## Descripción funcional
- El usuario puede crear un perfil antes de asociarlo a uno o varios eventos.
- Cada perfil debe incluir al menos un **nombre** y un **icono representativo**.
- Si es viable técnicamente, el perfil puede almacenar una **foto** o un icono personalizado elegido por el usuario.
- En la pantalla de perfiles se mostrará el conjunto de perfiles creados y el **importe total pendiente** de cada uno.
- Al pulsar sobre un perfil, se mostrará un desglose de eventos pendientes y el importe asociado a cada evento.

## Reglas relacionadas
- `RN0001A1`
- `RN0002A1`

## Criterios de aceptación
- [ ] se puede crear un perfil con nombre e icono
- [ ] un perfil creado puede añadirse posteriormente a distintos eventos
- [ ] la pantalla de perfiles muestra el total pendiente por persona
- [ ] el detalle de un perfil lista solo los eventos pendientes de pago
- [ ] el usuario puede editar la información del perfil

## Casos límite
- creación de perfiles con nombres similares
- perfil sin foto personalizada
- perfil sin deudas activas en ningún evento

## Dependencias
- `UI0003A1`
- `DD0001A1`

## Suposiciones
- La foto personalizada se documenta como capacidad deseada y dependerá de la implementación de permisos o selector de imágenes.

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-05 | A | 1 | Alta | Creación inicial del requisito de gestión global de perfiles. |
