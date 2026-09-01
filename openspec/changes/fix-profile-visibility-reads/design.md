# Design: Fix Profile Visibility Reads

## Architecture

Hoy la visibilidad se decide en tres sitios distintos y ninguno es el correcto:

| Capa | Qué hace hoy | Problema |
|------|--------------|----------|
| `firestore.rules:57` | `allow read: if isAuthenticated()` | Cualquiera puede leer cualquier perfil |
| `FirestoreProfileRepository:28` | `collection.snapshots` | Se trae la colección entera |
| `CuentaMorososApp.kt:357` | Filtro VIS-001..004 en un `remember` | Regla de negocio dentro de un Composable, y tardía |

El cambio mueve la regla a un único sitio autoritativo — un motor puro en `model/` — y la aplica
**antes** de la consulta, no después de la respuesta.

## Data Flow

```
eventRepository.observeEvents()          (ya existe, caché local SQLDelight)
        │
        ▼
ProfileVisibilityResolver.visibleProfileIds(uid, events)     ← puro, testeable
        │  Flow<Set<String>>  (distinctUntilChanged)
        ▼
OfflineFirstProfileRepository.startSyncLoop()
        │  flatMapLatest
        ▼
FirestoreProfileRepository.observeVisibleProfiles(ids)
        │
        ├── where { "ownerId" equalTo uid }            → VIS-001 + VIS-002   (~1-5 docs)
        └── where { "id" inArray chunk }  × ceil(n/30) → VIS-003             (~n docs)
        │  combine + dedupe por id
        ▼
   upsertProfiles() → SQLDelight → la UI lee del caché (gratis)
```

La UI **no cambia de fuente**: sigue leyendo `queries.selectAll()` del caché local. Lo único que
cambia es qué entra en ese caché.

## Component Design

### `ProfileVisibilityResolver` (nuevo, `model/`)

```kotlin
object ProfileVisibilityResolver {
    /** IDs de perfil que [uid] puede ver: propios + coparticipantes de sus eventos. */
    fun visibleProfileIds(uid: String, events: List<EventItem>): Set<String>

    /** VIS-001..004 sobre un perfil concreto. Usado por la UI como defensa en profundidad. */
    fun isVisible(profile: ProfileItem, uid: String, events: List<EventItem>): Boolean

    /** Filtra y ordena, reemplazando el bloque en línea de CuentaMorososApp.kt. */
    fun filterVisible(profiles: List<ProfileItem>, uid: String, events: List<EventItem>): List<ProfileItem>
}
```

Sin dependencias de Firebase ni de Compose. `EventItem.effectiveMemberIds` ya resuelve el
fallback `participants` → `memberIds`, así que el resolver lo usa en lugar de duplicar esa lógica.

### `ProfileRepository`

Se añade un método; `observeProfiles()` se mantiene porque el camino local (SQLDelight) lo sigue
usando y ahí no cuesta nada.

```kotlin
/** Solo los perfiles visibles. [coParticipantIds] viene del resolver. */
fun observeVisibleProfiles(coParticipantIds: Set<String>): Flow<List<ProfileItem>>
```

Implementación por defecto en la interfaz = `observeProfiles()`, para que
`OfflineFirstProfileRepository` y los fakes de test no tengan que implementarlo dos veces.

### Troceado de `inArray`

Firestore limita `inArray` a 30 valores. Los IDs se ordenan antes de trocear para que la
pertenencia a cada trozo sea estable entre emisiones: si entra un participante nuevo, solo se
re-suscribe el trozo afectado en vez de todos.

## Tradeoffs

**Más listeners, muchísimas menos lecturas.** Pasamos de 1 consulta que lee miles de documentos
a `1 + ceil(n/30)` consultas que leen `n` documentos. Con 40 coparticipantes son 3 listeners y
~45 documentos, frente a los miles de antes. Firestore factura por documento leído, no por
listener, así que el cambio es favorable en cuanto hay más de un puñado de usuarios.

**El caché local puede quedar obsoleto.** Si dejas de compartir un evento con alguien, su perfil
se queda en tu SQLite hasta que se pode. La poda es necesaria pero peligrosa: un conjunto visible
vacío en el arranque borraría todo. Por eso solo actúa tras una emisión real de eventos y nunca
toca el perfil propio.

**No cierra el agujero de las reglas.** Un cliente modificado seguiría pudiendo leer la colección
entera, porque `firestore.rules` lo permite. Cerrarlo de verdad exige un campo `visibleTo` en cada
documento de perfil, mantenido en escritura y con backfill de los existentes. Es un cambio con
migración de datos y merece su propia propuesta; este se queda en dejar de hacerlo desde el
cliente, que es lo que quita el techo.

**Alternativa descartada: denormalizar el nombre en `EventParticipant`.** Llevaría las lecturas de
perfil a cero, no solo a O(coparticipantes). Se descarta *para este cambio* porque exige migrar el
esquema del documento de evento y hacer backfill, y porque introduce nombres obsoletos cuando
alguien se renombra. Queda como optimización posterior, encima de esta.
