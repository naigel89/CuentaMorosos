# Especificaciones Técnicas

## 1. Visión General

CuentaMorosos evolucionará de una aplicación local (Android) a una plataforma multi-usuario y multi-plataforma (Android + iOS) con sincronización en tiempo real mediante Firebase.

---

## 2. Arquitectura General

```
┌─────────────────────┐     ┌─────────────────────┐
│   App Android       │     │   App iOS           │
│   (Kotlin/Compose)  │     │   (KMP/Compose MP)  │
└────────┬────────────┘     └──────────┬──────────┘
         │                             │
         └─────────────┬───────────────┘
                       │
              ┌────────▼────────┐
              │  commonMain     │  ← Lógica compartida (KMP)
              │  Repositorios   │
              │  ViewModels     │
              │  Modelos        │
              └────────┬────────┘
                       │
              ┌────────▼────────┐
              │    Firebase     │
              ├─────────────────┤
              │  Auth           │  ← Autenticación
              │  Firestore      │  ← Base de datos
              │  FCM            │  ← Notificaciones push
              └─────────────────┘
```

---

## 3. Esquema de Base de Datos (Firestore)

Firestore es una base de datos documental organizada en colecciones y documentos.

### Colección: `users`
```
users/{userId}
  - uid: String
  - email: String
  - displayName: String
  - createdAt: Timestamp
```

### Colección: `events`
```
events/{eventId}
  - id: String
  - name: String
  - dateMillis: Long
  - ownerId: String           ← UID del creador
  - memberIds: List<String>   ← UIDs de los participantes
  - lastCalculationMode: String?
  - lastCalculationTotal: Double?
  - lastCalculationTimestamp: Long?
  - lastCalculationSummary: String?
```

### Sub-colección: `events/{eventId}/debts`
```
events/{eventId}/debts/{debtId}
  - id: String
  - eventId: String
  - profileId: String
  - amountEuros: Double
  - notes: String
  - paid: Boolean
  - calculationMode: String?
```

### Sub-colección: `events/{eventId}/expenses`
```
events/{eventId}/expenses/{expenseId}
  - id: String
  - eventId: String
  - name: String
  - amountEuros: Double
  - category: String
  - assignedProfileIds: List<String>
```

### Colección: `profiles`
```
profiles/{profileId}
  - id: String
  - ownerId: String           ← UID del usuario que creó el perfil
  - name: String
  - icon: String
  - totalPendingEuros: Double
```

### Colección: `invitations`
```
invitations/{invitationId}
  - id: String
  - eventId: String
  - invitedByUid: String
  - invitedEmail: String
  - status: String            ← "pending" | "accepted" | "rejected"
  - createdAt: Timestamp
  - expiresAt: Timestamp
```

---

## 4. Flujo de Autenticación

```
Usuario abre la app
        │
        ▼
¿Tiene sesión activa?
   │            │
  Sí            No
   │            │
   ▼            ▼
Home      Pantalla Login
           │        │
        Login    Registro
           │        │
           └───┬────┘
               ▼
         Firebase Auth
               │
               ▼
    Crear/Recuperar user doc
    en Firestore (users/{uid})
               │
               ▼
             Home
```

---

## 5. Reglas de Seguridad de Firestore

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Un usuario solo puede leer y escribir su propio documento
    match /users/{userId} {
      allow read, write: if request.auth.uid == userId;
    }

    // Un evento solo es accesible por sus miembros
    match /events/{eventId} {
      allow read: if request.auth.uid in resource.data.memberIds;
      allow create: if request.auth != null;
      allow update, delete: if request.auth.uid == resource.data.ownerId;

      match /debts/{debtId} {
        allow read, write: if request.auth.uid in get(/databases/$(database)/documents/events/$(eventId)).data.memberIds;
      }

      match /expenses/{expenseId} {
        allow read, write: if request.auth.uid in get(/databases/$(database)/documents/events/$(eventId)).data.memberIds;
      }
    }

    // Perfiles accesibles por su propietario
    match /profiles/{profileId} {
      allow read, write: if request.auth.uid == resource.data.ownerId;
    }

    // Invitaciones
    match /invitations/{invitationId} {
      allow read: if request.auth.token.email == resource.data.invitedEmail
                  || request.auth.uid == resource.data.invitedByUid;
      allow create: if request.auth != null;
      allow update: if request.auth.token.email == resource.data.invitedEmail;
    }
  }
}
```

---

## 6. Patrón de Repositorio

Se reemplazará `CuentaMorososLocalStore` por una capa de repositorio abstracta:

```
interface EventRepository {
    fun observeEvents(): Flow<List<EventItem>>
    suspend fun saveEvent(event: EventItem)
    suspend fun deleteEvent(eventId: String)
}

// Implementación Firestore (online)
class FirestoreEventRepository : EventRepository { ... }

// Implementación local para caché offline (Room)
class LocalEventRepository : EventRepository { ... }

// Implementación combinada (offline-first)
class OfflineFirstEventRepository(
    local: LocalEventRepository,
    remote: FirestoreEventRepository
) : EventRepository { ... }
```

---

## 7. Dependencias a Añadir

```kotlin
// Firebase
implementation(platform("com.google.firebase:firebase-bom:32.x.x"))
implementation("com.google.firebase:firebase-auth-ktx")
implementation("com.google.firebase:firebase-firestore-ktx")
implementation("com.google.firebase:firebase-messaging-ktx")

// Room (caché offline)
implementation("androidx.room:room-runtime:2.x.x")
implementation("androidx.room:room-ktx:2.x.x")

// Kotlin Multiplatform (fase iOS)
implementation("org.jetbrains.compose.runtime:runtime:x.x.x")
```
