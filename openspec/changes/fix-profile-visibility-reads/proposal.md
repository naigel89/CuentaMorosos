# Proposal: Fix Profile Visibility Reads

## Intent

`FirestoreProfileRepository.observeProfiles()` escucha la colección `profiles` entera sin
filtro. Cada usuario lee todos los perfiles del sistema en cada sesión, así que las lecturas
globales crecen con el **cuadrado** del número de usuarios. Esto impone un techo duro de unos
pocos miles de usuarios — no por coste, sino porque el volumen deja de ser servible — y filtra
los datos de todos los usuarios a cualquier autenticado.

Las reglas de visibilidad VIS-001..004 ya existen, pero se aplican **en el cliente**, dentro de
un Composable (`CuentaMorososApp.kt:357`), después de que los datos ya han viajado.

## Scope

### In Scope
- Extraer VIS-001..004 de `CuentaMorososApp.kt` a un motor puro en `model/`
- Acotar la consulta remota de perfiles a: perfil propio + fantasmas propios + coparticipantes
- Inyectar el conjunto de IDs visibles en `OfflineFirstProfileRepository` como `Flow<Set<String>>`
- Eliminar la segunda lectura de colección completa en `profileRemoteOps.saveProfile`
- Podar del caché local los perfiles que dejan de ser visibles (con guarda anti-borrado)

### Out of Scope
- Cerrar `firestore.rules` para perfiles — requiere un campo `visibleTo` denormalizado y su
  backfill; se aborda en un cambio aparte (ver Riesgos)
- Denormalizar `displayName`/`avatarUrl` en `EventParticipant` — optimización posterior que
  llevaría estas lecturas a cero
- Las invitaciones sin camino offline (`InvitationRepository` no tiene `OfflineFirst*`)

## Capabilities

### New Capabilities
- `profile-visibility`: resolución pura y testeable de qué perfiles puede ver un usuario,
  aplicada tanto en la consulta remota como en el filtrado de UI

### Modified Capabilities
- `offline-first-sync`: la suscripción remota de perfiles pasa de una consulta fija a una
  suscripción reactiva al conjunto de IDs visibles

## Approach

`ProfileVisibilityResolver` (puro, en `model/`) calcula el conjunto visible a partir de
`uid + List<EventItem>`. `RepositoryProvider` construye un `Flow<Set<String>>` desde
`eventRepository.observeEvents()` y se lo pasa a `OfflineFirstProfileRepository`, cuyo bucle de
sync hace `flatMapLatest` sobre él y pide a `FirestoreProfileRepository` solo esos perfiles.

La consulta remota se descompone en dos piezas que Firestore sí sabe indexar:
`where { "ownerId" equalTo uid }` cubre VIS-001 y VIS-002 de una vez; VIS-003 se resuelve con
`where { "id" inArray chunk }` en trozos de 30. Los documentos de perfil ya llevan un campo `id`
espejo del ID de documento (`FirestoreProfileRepository.kt:611`), así que no hace falta recurrir
a `FieldPath.documentId`, cuya API en gitlive 1.13.0 es incómoda.

## Affected Areas

| Area | Impact |
|------|--------|
| `shared/.../model/ProfileVisibilityResolver.kt` | Nuevo — motor puro VIS-001..004 |
| `shared/.../repository/ProfileRepository.kt` | Modificado — nuevo `observeVisibleProfiles(ids)` |
| `shared/.../repository/FirestoreProfileRepository.kt` | Modificado — consulta acotada, sin `collection.snapshots` |
| `shared/.../repository/OfflineFirstProfileRepository.kt` | Modificado — `flatMapLatest` sobre IDs visibles, poda de caché |
| `shared/.../RepositoryProvider.kt` | Modificado — cablea el `Flow<Set<String>>` |
| `shared/.../ui/CuentaMorososApp.kt` | Modificado — delega el filtro al resolver |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Perfiles que desaparecen de la UI por un conjunto visible vacío en el arranque | Alta | La poda solo actúa tras una emisión real de eventos; nunca sobre el perfil propio |
| Churn de re-suscripción al cambiar los eventos | Media | `distinctUntilChanged` + trozos ordenados para que la pertenencia sea estable |
| Usuario con cientos de coparticipantes genera muchos listeners | Baja | Trozos de 30; se registra el número de trozos para vigilarlo |
| Las reglas siguen permitiendo leer cualquier perfil | **Aceptado** | El cliente deja de hacerlo, pero no es una garantía. Cerrarlo exige `visibleTo`; queda anotado como cambio siguiente |

## Rollback Plan

Revertir los commits. No hay migración de esquema ni de datos: el caché local se repuebla solo
en el siguiente sync, y los documentos de Firestore no se tocan.

## Success Criteria

- [ ] Las lecturas de perfil por sesión pasan de O(usuarios totales) a O(coparticipantes)
- [ ] `ProfileVisibilityResolver` cubre VIS-001..004 con tests en `commonTest`
- [ ] Ningún `collection.snapshots` sin filtro sobre `profiles` en el código
- [ ] `CuentaMorososApp.kt` ya no contiene lógica de visibilidad en línea
- [ ] `./gradlew :shared:jvmTest` y `:app:testDebugUnitTest` en verde
