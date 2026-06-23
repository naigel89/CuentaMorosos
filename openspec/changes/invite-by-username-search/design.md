# Design: invite-by-username-search

## Architecture Overview

Firestore-backed remote search with debounced UI. The dialog queries Firestore directly on each debounced keystroke — finds ANY user by @username, not just cached contacts.

```
UI (InviteMemberDialog) ──suspend call──→ ProfileRepository.searchByUsername(prefix)
                                               ↑
                                          FirestoreProfileRepository (real impl: prefix query)
                                          OfflineFirstProfileRepository (stub: throws)
                                               ↑
                                          Firestore: profiles collection
                                          (composite index on username ASC)
```

After invitation is sent, the invited profile syncs to local cache via existing `observeProfiles()` → `OfflineFirstProfileRepository` flow. The Profiles tab picks it up automatically.

---

## Component Design

### 1. Firestore Prefix Query — `FirestoreProfileRepository`

**New method** after `isUsernameAvailable`:

```kotlin
override suspend fun searchByUsername(prefix: String): List<ProfileItem> {
    val lowerPrefix = prefix.lowercase()
    val endPrefix = lowerPrefix + "\uf8ff"
    return collection
        .whereGreaterThanOrEqualTo("username", lowerPrefix)
        .whereLessThanOrEqualTo("username", endPrefix)
        .limit(20)
        .get()
        .documents
        .mapNotNull { it.toProfileItem() }
        .filter { !it.isGhost && it.linkedEmail != null }
}
```

Key details:
- `\uf8ff` is the highest Unicode character — bounds the prefix range server-side
- `limit(20)` prevents excessive reads on common prefixes (e.g., "a")
- `suspend` function — called from coroutine scope in the dialog
- Ghost + no-linkedEmail filtered post-query (Firestore can't filter on null/empty in range queries)
- `lowercase()` handles case-insensitivity at query level

### 2. Repository Layer

**`ProfileRepository.kt`** — new interface method:

```kotlin
/** Searches ALL Firestore profiles whose username starts with [prefix]. */
suspend fun searchByUsername(prefix: String): List<ProfileItem>
```

**`OfflineFirstProfileRepository.kt`** — stub:

```kotlin
override suspend fun searchByUsername(prefix: String): List<ProfileItem> {
    throw UnsupportedOperationException("searchByUsername is remote-only; use FirestoreProfileRepository")
}
```

### 3. InviteMemberDialog Redesign

**Location**: `EventDetailScreen.kt:591-639` (private composable, replaced in-place).

**Signature**:

```kotlin
@Composable
private fun InviteMemberDialog(
    onDismiss: () -> Unit,
    onInvite: (email: String) -> Unit,     // unchanged
    profileRepository: ProfileRepository,   // NEW: for Firestore search
    currentUserUid: String?,                // NEW: exclude self
)
```

**State**:

| State | Type | Purpose |
|-------|------|---------|
| `searchQuery` | `String` (mutableStateOf) | Text field value |
| `searchResults` | `List<ProfileItem>` (mutableStateOf) | Firestore results |
| `selectedProfile` | `ProfileItem?` (mutableStateOf) | Highlighted profile |
| `searchState` | `SearchState` (mutableStateOf) | idle / loading / results / empty / offline |

```kotlin
sealed class SearchState {
    data object Idle : SearchState()
    data object Loading : SearchState()
    data class Results(val profiles: List<ProfileItem>) : SearchState()
    data class Empty(val query: String) : SearchState()
    data object Offline : SearchState()
}
```

**Debounce**: `LaunchedEffect(searchQuery)` + `delay(500)` + `distinctUntilChanged`:

```kotlin
LaunchedEffect(searchQuery) {
    if (searchQuery.length < 2) {
        searchState = SearchState.Idle
        searchResults = emptyList()
        return@LaunchedEffect
    }
    searchState = SearchState.Loading
    delay(500)
    if (!isActive) return@LaunchedEffect
    try {
        val results = profileRepository.searchByUsername(searchQuery)
            .filter { it.id != currentUserUid }
        searchResults = results
        searchState = if (results.isEmpty()) SearchState.Empty(searchQuery)
                      else SearchState.Results(results)
    } catch (e: Exception) {
        searchState = if (e is java.io.IOException) SearchState.Offline
                      else SearchState.Empty(searchQuery)
    }
}
```

**UI structure**:

```
AlertDialog
├── title: "Invitar miembro"
├── OutlinedTextField(label="@nombre de usuario", prefix="@", singleLine=true)
├── when (searchState)
│   ├── Idle → Text("Escribe 2+ caracteres para buscar")
│   ├── Loading → CircularProgressIndicator
│   ├── Offline → Text("Sin conexión", color=error)
│   ├── Empty(query) → Text("Sin resultados para '$query'")
│   └── Results(list) → LazyColumn (max 6 visible, scrollable)
│       └── Row per result: ProfileAvatar + name + @username, clickable
├── confirmButton: "Invitar" (enabled: selectedProfile != null)
└── dismissButton: "Cancelar"
```

**Selection → invitation**: Tapping a row sets `selectedProfile`, confirm resolves `selectedProfile.linkedEmail!!` and calls `onInvite(email)`.

### 4. Firestore Composite Index

Required before deployment. Created via Firebase Console or `firestore.indexes.json`:

```json
{
  "indexes": [
    {
      "collectionGroup": "profiles",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "username", "order": "ASCENDING" }
      ]
    }
  ]
}
```

### 5. Wiring

`EventDetailScreen.kt:376-384` invocation updated to pass `profileRepository` and `currentUserUid`:

```kotlin
InviteMemberDialog(
    onDismiss = { showInviteMemberDialog = false },
    onInvite = { email -> showInviteMemberDialog = false; onInviteMember(email) },
    profileRepository = profileRepository,   // from EventDetailScreen params or DI
    currentUserUid = currentUserUid,
)
```

`EventDetailScreen` already receives `profiles: List<ProfileItem>` — but for the dialog we need the repository, not the list. The repository must be passed down from `CuentaMorososApp` or injected via the existing DI.

`onInviteMember` callback in `CuentaMorososApp.kt:494-509` is **unchanged**.

---

## Data Flow

```
User types "@na" → searchQuery updated → LaunchedEffect debounces 500ms
  → profileRepository.searchByUsername("na") → Firestore prefix query
  → Firestore returns matching documents → filter ghost + no-linkedEmail + self
  → searchState = Results(list) → LazyColumn rendered
  → User taps row → selectedProfile set → confirm button enabled
  → "Invitar" → onInvite(selectedProfile.linkedEmail)
  → onInviteMember(email) → EventInvitation built → sendInvitation()
  → (async) observeProfiles() syncs invited profile to local cache → Profiles tab
```

---

## Key Decisions

| Decision | Alternatives | Rationale |
|----------|-------------|-----------|
| Firestore remote over local SQLite | Local LIKE query on CachedProfile | Must find ANY user by @username, not just cached contacts. Local cache is a subset. |
| `suspend` function, not `Flow` | `Flow<List<ProfileItem>>` with snapshot listener | One-shot query per debounce; real-time snapshot listener is overkill for autocomplete |
| `limit(20)` on query | No limit | Common prefixes (e.g., "a") could return hundreds of docs. 20 is enough for autocomplete dropdown. |
| 500ms debounce over 300ms | 300ms like AccountViewModel SQLite | Network latency adds 100-300ms; 500ms balances responsiveness with read cost |
| `SearchState` sealed class over nullable fields | Multiple nullable state variables | Single source of truth for UI state; exhaustive `when` prevents missing states |
| No `EventInvitation` model changes | Add `invitedByUsername` field | Additive; email resolution at UI avoids cascading model/serialization changes |

---

## Files Changed

| File | Action | Est. Lines | Description |
|------|--------|-----------|-------------|
| `shared/.../repository/ProfileRepository.kt` | Add | +6 | `searchByUsername()` interface method |
| `shared/.../repository/FirestoreProfileRepository.kt` | Add | +25 | Real implementation with Firestore prefix query |
| `shared/.../repository/OfflineFirstProfileRepository.kt` | Add | +5 | Stub throwing `UnsupportedOperationException` |
| `shared/.../ui/EventDetailScreen.kt` | Modify | +70/-10 | Redesigned `InviteMemberDialog` with `SearchState` + Firestore-backed search |
| `firestore.indexes.json` | Create | +5 | Composite index on `profiles.username ASC` |
| `shared/.../commonTest/.../FirestoreProfileRepositoryTest.kt` | Add | +40 | `searchByUsername` tests with mock Firestore |
| `shared/.../commonTest/.../InviteMemberDialogTest.kt` | Create | +55 | Compose dialog tests with fake repository |

**Total**: ~206 lines added, ~10 removed. Under 400-line review budget.

---

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Repository | `searchByUsername()` returns correct profiles, excludes ghost/self | Mock Firestore `collection.where*().get()` |
| Repository | `searchByUsername()` handles empty results | Mock empty document list |
| Repository | Offline stub throws `UnsupportedOperationException` | `assertFailsWith` |
| Dialog | Debounce triggers at 2+ chars after 500ms | `runTest` + `advanceTimeBy(500)` |
| Dialog | SearchState transitions: idle → loading → results/empty/offline | Assert state flow with fake repository |
| Dialog | Self-exclusion, ghost exclusion, no-linkedEmail exclusion | Fake repo with varied profiles |
| Dialog | Selection resolves email and calls `onInvite` | Click row, assert `onInvite` called with correct email |
