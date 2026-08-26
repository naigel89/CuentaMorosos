# Tasks: Fix Profile Visibility Reads

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 180–240 (código), 300–380 (con tests) |
| 400-line budget risk | Media |
| Chained PRs recommended | No |
| Delivery strategy | ask-on-risk |

### Suggested Work Units

| Unit | Goal | Notes |
|------|------|-------|
| 1 | Motor puro de visibilidad + tests | Aislado, verificable con `:shared:jvmTest` |
| 2 | Consulta remota acotada | El cambio que quita el techo |
| 3 | Cableado y poda de caché | La parte con riesgo de regresión |
| 4 | UI delega al resolver | Limpieza; sin cambio de comportamiento |

## Phase 1: Motor puro (TDD)

- [x] 1.1 Tests de `ProfileVisibilityResolver` en `shared/src/commonTest/.../model/ProfileVisibilityResolverTest.kt` — VIS-001 perfil propio siempre visible; VIS-002 fantasmas propios visibles; VIS-003 coparticipantes de eventos compartidos visibles; VIS-004 el resto oculto; casos borde: `uid` en blanco, lista de eventos vacía, evento sin participantes, fallback `participants` → `memberIds` (~90 líneas, **Baja**)
- [x] 1.2 Implementar `shared/src/commonMain/.../model/ProfileVisibilityResolver.kt` hasta poner los tests en verde — `visibleProfileIds`, `isVisible`, `filterVisible`; sin dependencias de Firebase ni Compose; reutiliza `EventItem.effectiveMemberIds` (~50 líneas, **Baja**)

## Phase 2: Consulta remota acotada

- [x] 2.1 Añadir `observeVisibleProfiles(coParticipantIds: Set<String>)` a `ProfileRepository` con implementación por defecto que delega en `observeProfiles()` — evita romper fakes de test y el camino local (~8 líneas, **Baja**)
- [x] 2.2 Implementar `observeVisibleProfiles` en `FirestoreProfileRepository` — `where { "ownerId" equalTo uid }` combinado con `where { "id" inArray chunk }` en trozos ordenados de 30; `combine` + dedupe por id + orden por nombre; registrar el número de trozos vía `LogSanitizer` (~55 líneas, **Media** — es la consulta crítica)
- [x] 2.3 Eliminar el `collection.snapshots` sin filtro de `observeProfiles()` — pasa a ser `observeVisibleProfiles(emptySet())`, o sea solo los propios; documentar en el KDoc que ya no devuelve la colección entera (~10 líneas, **Media** — verificar que nadie dependía del comportamiento antiguo)
- [x] 2.4 Arreglar la segunda lectura completa en `OfflineFirstProfileRepository.profileRemoteOps.saveProfile` — el fallback hace `remoteRepository.observeProfiles().first()`; sustituir por una lectura del documento concreto (~12 líneas, **Baja**)

## Phase 3: Cableado y caché

- [x] 3.1 `OfflineFirstProfileRepository` acepta `visibleProfileIds: Flow<Set<String>>` en el constructor y el bucle de sync hace `flatMapLatest` sobre él con `distinctUntilChanged` (~30 líneas, **Media**)
- [x] 3.2 `RepositoryProvider` construye el flow desde `eventRepository.observeEvents()` + `ProfileVisibilityResolver.visibleProfileIds(uid, events)` y lo inyecta (~15 líneas, **Baja**)
- [~] 3.3 **Descartada.** La poda del caché era la única tarea de riesgo alto y su beneficio es
  nulo: los perfiles obsoletos ya no se muestran (`filterVisible` en 4.1) y no cuestan lecturas.
  A cambio, el conjunto visible arranca vacío mientras los eventos cargan, así que cualquier poda
  no guardada borraría el caché entero y lo repoblaría — un parpadeo visible. Se deja el caché
  crecer; si algún día molesta, es un cambio aparte con su propia guarda.
- [x] 3.4 Test de integración con SQLDelight en memoria: dado un fake remoto que registra qué IDs se le piden, verificar que solo se piden los visibles y que el caché queda con exactamente esos (~70 líneas, **Baja**)

## Phase 4: UI

- [x] 4.1 Sustituir el bloque de visibilidad en línea de `CuentaMorososApp.kt:357` por `ProfileVisibilityResolver.filterVisible(...)` — mismo comportamiento, la regla deja de vivir en un Composable (~15 líneas, **Baja**)
- [x] 4.2 Ejecutar `./gradlew :shared:jvmTest :app:testDebugUnitTest --continue` y `:shared:compileKotlinJvm` para confirmar que `commonMain` no ha ganado dependencias de Android (**Baja**)

## Notas de verificación

`:shared:jvmTest` es la vuelta rápida. Los targets iOS no existen en este host (Linux), así que la
comprobación de portabilidad es `:shared:compileKotlinJvm`; el linkado real de iOS lo hace
`ios-build.yml` en CI sobre `macos-15`.

No se puede medir el ahorro de lecturas en local: hace falta la consola de Firebase o el
emulador. El criterio verificable aquí es estructural — que no quede ningún
`collection.snapshots` sin filtrar sobre `profiles`.


## Resultado

| Comprobación | Estado |
|---|---|
| `./gradlew :shared:jvmTest` | 818 tests, 0 fallos |
| `./gradlew :app:testDebugUnitTest` | 114 tests, 0 fallos |
| `./gradlew :shared:compileKotlinJvm` | OK — `commonMain` sin fugas de Android |
| `collection.snapshots` sin filtrar sobre `profiles` | ninguno |

Tests nuevos: 16 en `ProfileVisibilityResolverTest` (VIS-001..004 y casos borde) y 5 en
`ProfileVisibilityScopingTest`, cuyo doble de remoto lanza `AssertionError` si el bucle de sync
vuelve a llamar al `observeProfiles()` sin acotar.

## Trampa encontrada, para que no se repita

`combine(Iterable<Flow<T>>)` es `reified`. Reificar `QuerySnapshot` de gitlive —que es una
`expect class`— hace que la compilación de metadata común produzca un klib desde el que
`commonTest` no resuelve **ninguna** función de nivel superior del paquete `model`: 505 errores
en cascada, repartidos por ficheros de test que no tienen nada que ver, y **ni un solo error
señalando al fichero culpable**. `:shared:compileKotlinJvm` y `:shared:compileCommonMainKotlinMetadata`
pasan sin quejarse; solo falla `:shared:compileTestKotlinJvm`.

La solución es combinar por pares con el `combine(Flow<A>, Flow<B>)` de dos argumentos, que no
es `reified`. Queda un comentario en `FirestoreProfileRepository` explicándolo.

## Pendiente, anotado aquí para no perderlo

- `FirestoreProfileRepository.cleanupOrphans()` sigue haciendo `collection.get()` sobre la
  colección entera. Está guardado para ejecutarse una sola vez tras actualizar, así que no es
  el techo, pero es la última lectura O(todos los perfiles) que queda.
- `firestore.rules` sigue permitiendo `allow read: if isAuthenticated()` sobre `profiles`. El
  cliente ya no lo aprovecha, pero no es una garantía. Cerrarlo exige un campo `visibleTo`.
- `InvitationRepository` no tiene `OfflineFirst*`: invitar sin cobertura falla en silencio.
