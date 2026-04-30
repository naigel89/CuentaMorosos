# Guía de Migración de Datos Locales a Firebase

## Objetivo

Esta guía describe el proceso para trasladar los datos existentes almacenados en `SharedPreferences` del dispositivo del usuario a la base de datos en la nube (Firestore), sin pérdida de información.

---

## 1. Contexto del Almacenamiento Actual

Actualmente la app guarda toda la información en `SharedPreferences` bajo el fichero `cuenta_morosos_store` mediante `CuentaMorososLocalStore`. Las claves son:

| Clave          | Tipo de dato                  |
|----------------|-------------------------------|
| `events`       | `List<EventItem>` (JSON)      |
| `profiles`     | `List<ProfileItem>` (JSON)    |
| `debts`        | `List<EventDebtItem>` (JSON)  |
| `expenses`     | `List<EventExpenseItem>` (JSON)|
| `preferences`  | `UserPreferences` (JSON)      |

---

## 2. Estrategia de Migración

La migración se realizará de forma **automática y silenciosa** la primera vez que el usuario inicie sesión o se registre en la nueva versión de la app.

### Flujo

```
Usuario abre la nueva versión
         │
         ▼
¿Tiene sesión activa (Firebase Auth)?
   │                      │
  No                      Sí
   │                      │
   ▼                      ▼
Login / Registro    ¿Flag "migrated" en Firestore?
                       │           │
                      Sí           No
                       │           │
                       ▼           ▼
                     Home    Ejecutar migración
                                   │
                                   ▼
                           Leer SharedPreferences
                                   │
                                   ▼
                           Subir datos a Firestore
                                   │
                                   ▼
                           Marcar flag "migrated"
                                   │
                                   ▼
                                 Home
```

---

## 3. Pasos Detallados de la Migración

### Paso 1: Detectar si hay datos locales

Al iniciar sesión, comprobar si `SharedPreferences` contiene datos previos:

```kotlin
fun hasLocalData(context: Context): Boolean {
    val store = CuentaMorososLocalStore(context)
    return store.loadEvents().isNotEmpty() || store.loadProfiles().isNotEmpty()
}
```

### Paso 2: Leer todos los datos locales

```kotlin
val events    = localStore.loadEvents()
val profiles  = localStore.loadProfiles()
val debts     = localStore.loadDebts()
val expenses  = localStore.loadExpenses()
```

### Paso 3: Subir datos a Firestore

Los datos se suben en un batch para garantizar atomicidad:

```kotlin
suspend fun migrateToFirestore(
    uid: String,
    events: List<EventItem>,
    profiles: List<ProfileItem>,
    debts: List<EventDebtItem>,
    expenses: List<EventExpenseItem>
) {
    val db = FirebaseFirestore.getInstance()
    val batch = db.batch()

    // Eventos: el usuario actual es propietario y miembro
    events.forEach { event ->
        val ref = db.collection("events").document(event.id)
        batch.set(ref, event.toFirestoreMap(ownerUid = uid))
    }

    // Gastos y deudas como sub-colecciones del evento
    debts.forEach { debt ->
        val ref = db.collection("events").document(debt.eventId)
                    .collection("debts").document(debt.id)
        batch.set(ref, debt.toFirestoreMap())
    }

    expenses.forEach { expense ->
        val ref = db.collection("events").document(expense.eventId)
                    .collection("expenses").document(expense.id)
        batch.set(ref, expense.toFirestoreMap())
    }

    // Perfiles vinculados al usuario
    profiles.forEach { profile ->
        val ref = db.collection("profiles").document(profile.id)
        batch.set(ref, profile.toFirestoreMap(ownerUid = uid))
    }

    batch.commit().await()

    // Marcar la migración como completada
    db.collection("users").document(uid)
      .update("migrated", true).await()
}
```

### Paso 4: Limpiar los datos locales (opcional)

Tras confirmar que la migración ha sido exitosa, se pueden borrar los datos de `SharedPreferences` para liberar espacio:

```kotlin
fun clearLocalData(context: Context) {
    context.getSharedPreferences("cuenta_morosos_store", Context.MODE_PRIVATE)
           .edit().clear().apply()
}
```

> **Nota de seguridad**: Se recomienda mantener los datos locales durante al menos 30 días antes de borrarlos, como copia de seguridad ante posibles errores en la migración.

---

## 4. Manejo de Errores

| Escenario                          | Comportamiento esperado                                              |
|------------------------------------|----------------------------------------------------------------------|
| Sin conexión durante la migración  | Reintentar automáticamente al recuperar la conexión.                 |
| Fallo parcial del batch            | Firestore garantiza atomicidad: o todo sube o nada. Reintentar.     |
| El usuario cancela durante la migración | No se marca el flag `migrated`. Se reintentará en el siguiente login. |
| Datos corruptos en SharedPreferences | Omitir los registros inválidos. `CuentaMorososLocalStore` ya filtra los items sin `id` o `name`. |

---

## 5. Comunicación al Usuario

Durante la migración se mostrará una pantalla de carga con el mensaje:

> "Migrando tus datos a la nube, esto solo ocurrirá una vez..."

Se evitará que el usuario pueda navegar a otra pantalla hasta que la migración concluya o falle definitivamente.

---

## 6. Rollback

Si por cualquier motivo fuera necesario volver a la versión local:

1. El flag `migrated` en Firestore **no** se borra, solo se desactiva con `migrated: false`.
2. La app detectaría la ausencia del flag y volvería a leer de `SharedPreferences` si los datos aún existen.
3. Esta situación debería ser excepcional y gestionada por el equipo de desarrollo.
