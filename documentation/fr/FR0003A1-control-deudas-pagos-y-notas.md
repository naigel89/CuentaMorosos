# FR0003A1: Control de deudas, pagos y notas

> **Código:** FR0003A1
> **Versión:** A
> **Revisión:** 3
> **Fecha:** 2026-06-20

## Resumen
La aplicación debe permitir registrar el dinero que cada perfil debe dentro de un evento, ya sea manualmente o mediante cálculo automático, así como marcar el pago realizado o pendiente y añadir notas explicativas.

## Historia de usuario
Como usuario de `CuentaMorosos`, quiero registrar cuánto me debe cada persona, añadir notas aclaratorias y marcar cuándo ya me ha pagado para mantener el control real de los importes pendientes.

## Descripción funcional
- Dentro de un evento, el usuario puede asignar a cada perfil un importe en **euros**, admitiendo **decimales**.
- El usuario puede añadir **notas aclaratorias** que expliquen el motivo o detalle de la deuda.
- El importe y las notas pueden ser **editados** o **eliminados** cuando el usuario lo necesite.
- El usuario puede usar una **calculadora automática** para escribir o recalcular las cantidades del evento según un modo de reparto definido en `FR0005A1`.
- El usuario puede marcar un perfil como **pagado** mediante un control tipo check. **El checkbox solo se muestra cuando el evento ya tiene un cálculo aplicado (`eventState != OPEN`)**. Ver regla `RN0001A1`.
- Al marcarlo como pagado, el perfil pasa a una pestaña o sección oculta llamada **`Han pagado`**.
- Si el usuario desmarca el check, el perfil vuelve a la lista principal de pendientes del evento.

## Reglas relacionadas
- `RN0001A1`
- `RN0002A1`
- `RN0003A1`

## Criterios de aceptación
- [ ] se puede registrar una deuda con importes decimales
- [ ] se pueden guardar notas aclaratorias junto al importe
- [ ] importe y notas se pueden editar y eliminar
- [ ] el usuario puede rellenar importes manualmente o aplicar un cálculo automático
- [ ] el estado `pagado` mueve el perfil a la sección `Han pagado`
- [ ] al revertir el estado `pagado`, el perfil vuelve a la vista de pendientes

## Casos límite
- importes con céntimos
- notas vacías o muy breves
- cambio de estado por error y posterior reversión
- perfil sin deuda pero ya añadido al evento
- recalcular importes existentes con la calculadora automática

## Dependencias
- `UI0002A1`
- `UI0005A1`
- `RN0001A1`
- `RN0002A1`
- `RN0003A1`

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-05 | A | 1 | Alta | Creación inicial del requisito de control de deudas, pagos y notas. |
| 2026-04-05 | A | 2 | Actualización | Se añade la opción de cálculo automático de cantidades dentro del evento. |
| 2026-06-20 | A | 3 | Actualización | Actualizada regla de visibilidad del checkbox de pago: solo se muestra cuando `eventState != OPEN`. Referencia a `RN0001A1`. |
