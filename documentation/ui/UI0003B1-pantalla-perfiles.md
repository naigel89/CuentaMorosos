# UI0003B1: Pantalla de perfiles — Rediseño Neo-Fintech

> **Código:** UI0003B1
> **Versión:** B
> **Revisión:** 2
> **Fecha:** 2026-06-18

## Resumen
Pantalla de perfiles con lista de perfiles (avatar, nombre, monto total pendiente), balance unificado por perfil y FAB para añadir nuevos perfiles. Implementada en `ProfilesScreen.kt` dentro de `shared/src/commonMain/kotlin/com/cuentamorosos/ui/`.

## Historia de usuario relacionada
Como usuario, quiero ver de un vistazo cuánto me debe cada persona y cuánto debo yo, con acceso rápido al detalle de sus eventos pendientes.

## Objetivo de la pantalla
Ofrecer una vista agregada por persona con indicación visual clara de la dirección de la deuda (te debe / debes) y el monto total pendiente.

## Componentes visibles
| Componente | Tipo | Descripción | Obligatorio |
|---|---|---|---|
| Section Header | texto | "Perfiles" como título de sección | Sí |
| Profile List | LazyColumn | Lista de perfiles con avatar, nombre y monto | Sí |
| Profile Row | fila | Avatar circular, nombre del perfil, monto con signo (+/−) y color semántico | Sí |
| Add Profile FAB | botón flotante | Botón circular con icono + para crear perfil | Sí |
| Add Profile Dialog | modal | Formulario con nombre para nuevo perfil | Sí |
| Bottom Navigation | barra | Events, Profiles, Settings | Sí |

## Estados de la interfaz
- Sin perfiles: lista vacía con mensaje y FAB visible para crear el primero
- Perfiles con deuda a favor: monto en verde (`primaryContainer`) con signo +
- Perfiles con deuda en contra: monto en rojo (`error`) con signo −
- Perfiles saldados: monto $0.00 en color neutro

## Reglas de interacción
- Pulsar un perfil abre el desglose de eventos pendientes de esa persona
- FAB abre el diálogo de creación de perfil
- El monto visible excluye eventos ya pagados
- Bottom nav: ítem activo con icono relleno

## Navegación
- Origen: Bottom nav (segunda pestaña)
- Destino secundario: detalle del perfil con desglose por evento

## Consideraciones UX/UI
- **Avatar**: iniciales del perfil sobre fondo `primaryContainer`
- **Color semántico**: verde (`primaryContainer`) para "te debe", rojo (`error`) para "debes"
- **Montos**: JetBrains Mono para alineación tabular de cifras
- **FAB**: color `primaryContainer`, posición bottom-end

## Referencias de diseño
- Sistema visual completo: `documentation/nfr/NFR0001B1-experiencia-visual-y-personalizacion.md`
- Código fuente: `shared/src/commonMain/kotlin/com/cuentamorosos/ui/ProfilesScreen.kt`

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-05-14 | B | 1 | Alta | Rediseño con dashboard Bento Grid de balance, badges de estado con tintes de color, avatares con iniciales fallback. Basado en concepto Neo-Fintech Precision. |
| 2026-06-18 | B | 2 | Actualización | Sincronizado con código real: eliminados Bento Grid dashboard, Filter button y badges de estado con tintes. Documentados LazyColumn de perfiles con avatar+nombre+monto y add-profile dialog. |
