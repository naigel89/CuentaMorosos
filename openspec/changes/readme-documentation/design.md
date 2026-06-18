# Design: Actualización Integral del README.md

## Technical Approach

Reescritura completa del `README.md` raíz con 9 secciones estructuradas, verificando cada afirmación contra los fuentes (`Models.kt`, `CalculatorEngine.kt`, `StateMachine.kt`, build files). El documento se redacta en castellano formal con tono profesional y cálido, orientado a servir como puerta de entrada para nuevos colaboradores. No se modifica código — es un cambio puramente documental.

## Architecture Decisions

### Decision: Lengua del README

**Choice**: Castellano formal con términos técnicos en inglés.
**Alternatives considered**: Bilingüe (ES/EN), inglés puro.
**Rationale**: El README actual está en español, la documentación en `/documentation/` está en español, y el público objetivo es hispanohablante. Los términos técnicos (Kotlin, SQLDelight, Firestore) se mantienen en inglés para evitar ambigüedad. Esto es consistente con el `proposal.md` y los comentarios del código fuente (`Models.kt` tiene labels en español).

### Decision: Nivel de detalle

**Choice**: README exhaustivo (~300-400 líneas) con enlaces a `documentation/` para detalles.
**Alternatives considered**: README minimalista con solo links, README que duplica toda la documentación.
**Rationale**: El README debe ser autosuficiente para un primer contacto (stack, setup, funcionalidades, arquitectura) pero no duplicar FR/NFR/sprints. Los enlaces a `documentation/` ofrecen profundidad sin sobrecargar. El `proposal.md` explícitamente excluye duplicar `/documentation/`.

### Decision: Verificación de precisión

**Choice**: Cada afirmación del README se cruza contra código fuente real, no contra el exploration ni de memoria.
**Alternatives considered**: Confiar ciegamente en el exploration.md, verificar contra el README anterior.
**Rationale**: El exploration.md es completo y preciso (verificado contra `Models.kt` y `CalculatorEngine.kt`), pero el diseño exige que el apply verifique contra fuentes para los datos citados: enumerados exactos (6 SplitModes, 3 EventStates, 11 categorías), versiones de dependencias, estructura de directorios real.

### Decision: Placeholders de screenshots

**Choice**: Formato `<!-- SCREENSHOT: descripción de qué capturar -->` como comentarios HTML invisibles en el renderizado.
**Alternatives considered**: Usar imágenes placeholder (gray boxes), links a archivos inexistentes, omitirlos.
**Rationale**: Los comentarios HTML no se renderizan pero son visibles en el Markdown fuente — el usuario ve exactamente qué capturar y dónde insertarlo. Es un nice-to-have explícito del `proposal.md`.

### Decision: Tabla de stack tecnológico

**Choice**: Agrupación por capa funcional (Core, UI, Datos/Backend, Almacenamiento Local, Async/Concurrencia, Testing) con columnas `Componente | Versión | Capa`.
**Alternatives considered**: Tabla plana sin agrupar, tabla por módulo (app vs shared).
**Rationale**: La agrupación por capa es más informativa para entender la arquitectura que una lista plana. Facilita identificar qué bibliotecas pertenecen a qué responsabilidad. La columna `Capa` permite al lector mapear inmediatamente cada dependencia a su rol en la arquitectura.

## Data Flow

El README no modifica datos — es un documento estático. El flujo de trabajo para producirlo es:

```
Verificación de fuentes (Models.kt, CalculatorEngine.kt, StateMachine.kt, build.gradle.kts)
    │
    ▼
Escritura sección por sección siguiendo el diseño
    │
    ▼
Revisión contra proposal.md success criteria
    │
    ▼
README.md final
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `README.md` | Rewrite | Reemplazo completo: se preservan logo, tagline y tono general; se corrigen estados, modos y estructura; se añaden 6 secciones nuevas. |

## Sección 1: Encabezado

### Estructura Markdown

```markdown
# CuentaMorosos

<div align="center">
  <img src="logo.png" alt="CuentaMorosos Logo" width="120" />
  <p><em>Divide gastos. Sin vueltas. Sin cuentas pendientes.</em></p>
</div>

---

{Párrafo 1: qué es, problema que resuelve, para quién}

{Párrafo 2: características diferenciales — offline-first, liquidación inteligente, open source}

{Párrafo 3: estado del proyecto y llamado a la acción (contribuir, probar)}
```

### Contenido

- **Párrafo 1**: Definir CuentaMorosos como app Android/KMP para gestionar gastos compartidos en eventos (viajes, cenas, fiestas). Explicar el problema: gente que paga por otros, cuentas pendientes, quién debe a quién. Público: grupos de amigos, familias, compañeros de piso.
- **Párrafo 2**: Tres diferenciales — (a) cálculo de liquidación que minimiza transferencias entre personas, (b) funciona sin conexión y sincroniza cuando hay red, (c) código abierto (MIT) construido con Kotlin Multiplatform.
- **Párrafo 3**: Proyecto activo. Invitar a probar, reportar issues, contribuir. Enlace a sección de instalación.

### Tono

Cálido y directo, profesional. Usar tuteo (tú) en todo el documento: "crea", "descarga", "activa", "colócalo". NUNCA usar voseo ("creá", "descargá"). El tagline debe ser en tuteo o eliminarse.

### Screenshots

No lleva screenshots — el logo ya está incluido.

---

## Sección 2: Stack Tecnológico

### Estructura Markdown

Tabla agrupada con encabezados de subsección por capa.

```markdown
## 🛠️ Stack Tecnológico

### Core

| Componente | Versión | Capa |
|-----------|---------|------|
| Kotlin | 1.9.24 | Lenguaje |
| Android Gradle Plugin | 8.5.2 | Build |
| Compose Multiplatform | 1.6.11 | UI Framework |
| Kotlin Compiler Extension (Compose) | 1.5.14 | Compilador |
| JVM Target | 17 | Runtime |
| compileSdk / targetSdk | 35 | Android |
| minSdk | 24 | Android |

### UI

| Componente | Versión | Capa |
|-----------|---------|------|
| Jetpack Compose BOM | 2024.06.00 | UI |
| Material 3 | via BOM | Design System |
| Coil (KMP) | 3.0.4 | Imágenes |
| Coil Compose | 3.0.4 | Imágenes |
| Coil Network (OkHttp) | 3.0.4 | Imágenes |
| Lifecycle ViewModel KMP | 2.8.0 | Arquitectura |
| Lifecycle Runtime Compose KMP | 2.8.0 | Arquitectura |
| Activity Compose | 1.9.1 | Android |
| Core KTX | 1.13.1 | Android |
| Lifecycle Runtime KTX | 2.8.4 | Android |

### Datos / Backend

| Componente | Versión | Capa |
|-----------|---------|------|
| Firebase BOM | 33.6.0 | Backend |
| Firebase Auth KTX | via BOM | Autenticación |
| Firebase Firestore KTX | via BOM | Base de datos |
| Firebase Storage KTX | via BOM | Almacenamiento |
| Firebase Messaging KTX | via BOM | Notificaciones |
| dev.gitlive:firebase-auth | 1.13.0 | Auth (KMP) |
| dev.gitlive:firebase-firestore | 1.13.0 | Firestore (KMP) |
| Google Services Plugin | 4.4.2 | Build |

### Almacenamiento Local

| Componente | Versión | Capa |
|-----------|---------|------|
| SQLDelight | 2.0.2 | Base de datos |
| SQLDelight Coroutines Extensions | 2.0.2 | Async |
| SQLDelight Android Driver | 2.0.2 | Android |
| SQLDelight SQLite Driver | 2.0.2 | JVM |

### Async / Concurrencia

| Componente | Versión | Capa |
|-----------|---------|------|
| Kotlinx Coroutines Core | 1.8.1 | Concurrencia |
| Kotlinx Coroutines Android | 1.8.1 | Android |
| Kotlinx DateTime | 0.6.1 | Fechas |
| WorkManager Runtime KTX | 2.9.1 | Tareas en segundo plano |

### Testing

| Framework | Versión | Tipo |
|-----------|---------|------|
| JUnit 4 | 4.13.2 | Unit (app) |
| Kotlin Test | via kotlin("test") | Unit (shared) |
| Kotlinx Coroutines Test | 1.8.1 | Unit |
| Robolectric | 4.13 | Unit (Android) |
| AndroidX Test Core | 1.6.1 | Unit |
| AndroidX Test Ext JUnit | 1.2.1 | Unit / Instrumented |
| Espresso | 3.6.1 | Instrumented |
| Compose UI Test JUnit4 | via BOM | Instrumented |
| JSON (org.json) | 20231013 | Unit (app) |

> **Nota**: El proyecto **no utiliza** framework de inyección de dependencias (Hilt/Koin), linter (detekt/ktlint) ni formateador (ktfmt). La DI se gestiona manualmente mediante `AppViewModelFactory`.
```

### Contenido

- 6 subsecciones (Core, UI, Datos/Backend, Almacenamiento Local, Async/Concurrencia, Testing) con tablas.
- Cada tabla: `Componente | Versión | Capa` (o `Tipo` para testing).
- Total: 36 componentes listados.
- Nota al pie sobre ausencias deliberadas (no DI framework, no linter, no formatter).

### Tono

Técnico, preciso, sin adjetivos. Solo datos.

### Screenshots

No aplica.

---

## Sección 3: Instalación y Ejecución

### Estructura Markdown

```markdown
## 🚀 Instalación y Ejecución

### Prerrequisitos

- **JDK 17** — configurado en `gradle.properties`
- **Android SDK** — compileSdk 35, minSdk 24
- **Android Studio** — Hedgehog (2023.1.1) o superior recomendado
- **Firebase project** — ver configuración abajo
- **macOS** — solo necesario para compilar el target iOS (excluido automáticamente en Linux/Windows)

### Configuración de Firebase

1. Crea un proyecto en [Firebase Console](https://console.firebase.google.com/)
2. Activa **Authentication** → proveedor Email/Password
3. Crea **Firestore Database** en modo producción y configura las reglas de seguridad
4. Crea un bucket de **Storage** para las fotos de perfil
5. Descarga `google-services.json` y colócalo en el directorio `app/`
6. (Opcional) Activa **Cloud Messaging** para notificaciones push

### Variables de entorno / archivos de configuración

- `local.properties`: debe contener `sdk.dir={ruta al Android SDK}` (Gradle suele autodetectar)
- `google-services.json`: en `app/`, excluido de git (`.gitignore`)
- `gradle.properties`: ya incluye configuración de JVM (-Xmx4g), daemon de Kotlin (-Xmx2g), build paralelo y caché

### Comandos

```bash
# Compilar APK debug
./gradlew assembleDebug

# Ejecutar tests unitarios del módulo app (JUnit + Robolectric)
./gradlew test

# Ejecutar tests del módulo shared (Kotlin test + coroutines)
./gradlew :shared:allTests

# Ejecutar tests de instrumentación (requiere emulador o dispositivo conectado)
./gradlew connectedAndroidTest

# Instalar en emulador o dispositivo conectado
./gradlew installDebug
```
```

### Contenido

- Prerrequisitos con viñetas (JDK 17, Android SDK, Android Studio, Firebase, macOS opcional).
- 6 pasos numerados para Firebase.
- Archivos de configuración necesarios (`local.properties`, `google-services.json`, `gradle.properties`).
- 5 comandos gradle exactos con descripciones en comentarios.
- **No incluir rutas de keystore ni passwords** — riesgo de seguridad documentado en `proposal.md`.

### Tono

Instructivo, paso a paso. Usar imperativo en tuteo ("Crea", "Activa", "Descarga"). Los comandos en bloques de código bash.

### Screenshots

No aplica.

---

## Sección 4: Estructura del Proyecto

### Estructura Markdown

```markdown
## 📁 Estructura del Proyecto

```
CuentaMorosos/
├── app/                                    # Módulo Android (shell)
│   ├── build.gradle.kts                    # AGP, Firebase BOM, signing, dependencias
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── google-services.json        # Firebase (gitignored)
│       │   └── java/com/cuentamorosos/
│       │       ├── CuentaMorososApp.kt     # Application class, singleton
│       │       ├── MainActivity.kt         # Entry point, auth flow, deep links
│       │       ├── data/                   # Firebase sync, notificaciones, WorkManager
│       │       ├── notifications/          # Android notification dispatch
│       │       └── ui/auth/               # MigrationScreen (legacy data)
│       └── test/                           # 8 tests (LocalStore, FCM, dedup)
│
├── shared/                                 # Módulo KMP (lógica + UI compartida)
│   ├── build.gradle.kts                    # KMP plugin, Compose, SQLDelight, targets
│   └── src/
│       ├── commonMain/kotlin/com/cuentamorosos/
│       │   ├── model/                      # Motores puros: SplitCalculator, SettlementEngine,
│       │   │                               #   PermissionEngine, IntegrityGuard, StateMachine,
│       │   │                               #   CalculatorEngine (6 modos), validación
│       │   ├── ui/                         # Pantallas Compose, ViewModels, tema NeoFintech
│       │   │   └── auth/                   # Login, Register, ForgotPassword, UserProfile
│       │   ├── data/repository/            # Patrón repositorio offline-first
│       │   ├── db/                         # SQLDelight driver factory (expect)
│       │   └── notifications/             # Modelos de notificación, deep links
│       ├── commonTest/                     # 39 tests (engines, ViewModels, repos, UI)
│       ├── androidMain/                    # Platform.android, AppViewModelFactory,
│       │                                   #   RepositoryProvider, DriverFactory, NetworkMonitor
│       ├── jvmMain/                        # JVM SQLite driver
│       ├── iosMain/                        # iOS driver (condicional en macOS)
│       └── sqldelight/                     # 8 esquemas .sq (CachedEvent, CachedProfile, etc.)
│
├── iosApp/                                 # Xcode project (wrapper iOS, condicional)
├── keystore/                               # Clave de firma (release)
├── documentation/                          # CHANGELOG, FR, NFR, sprints, UI docs
├── openspec/                               # Artefactos SDD (specs, changes, archive)
├── gradle/                                 # Wrapper y version catalog
│   └── libs.versions.toml
├── build.gradle.kts                        # Root: plugins y versiones
├── settings.gradle.kts                     # Includes de módulos, repositorios
├── gradle.properties                       # JVM args, propiedades de build
├── gradlew / gradlew.bat                   # Gradle wrapper
├── README.md                               # Este archivo
├── AGENTS.md                               # Guía para asistentes de IA
├── LICENSE                                 # MIT
└── logo.png                                # Logo de la app
```

### Módulos

| Módulo | Tipo | Descripción |
|--------|------|-------------|
| `app/` | Android Application | Shell Android: entry point, Firebase SDK nativo, notificaciones, WorkManager |
| `shared/` | Kotlin Multiplatform | Lógica de negocio + UI Compose compartida entre Android, JVM e iOS |
| `iosApp/` | iOS Xcode Project | Envoltorio SwiftUI para iOS (compilación condicional en macOS) |
```

### Contenido

- Árbol completo con ~45 entradas, cada una con descripción funcional.
- Tabla de módulos (3 filas) explicando el rol de `app/`, `shared/`, `iosApp/`.
- Los comentarios en el árbol deben ser concisos (una línea) pero informativos.
- Incluir `documentation/`, `openspec/`, `AGENTS.md`, `LICENSE`, `logo.png`, `keystore/`.
- **No incluir rutas de archivos dentro de keystore**.
- El exploration.md tiene la estructura canónica — verificar contra el filesystem real para precisión.

### Tono

Técnico, descriptivo. Los comentarios usan voz activa.

### Screenshots

No aplica.

---

## Sección 5: Funcionalidades Principales

### Estructura Markdown

7 subsecciones con `###`. Cada subsección describe una funcionalidad con viñetas.

### 5.1 Eventos

```markdown
### 📅 Eventos

- **CRUD completo**: crea, edita y elimina eventos con nombre, fechas de inicio/fin y moneda base.
- **Ciclo de vida estricto**: los eventos transitan por tres estados controlados por `StateMachine`:
  - **Abierto** (`OPEN`): se pueden agregar y quitar gastos.
  - **Calculado** (`CALCULATED`): las deudas están calculadas, se registran pagos.
  - **Cerrado** (`CLOSED`): solo lectura, todas las transferencias están saldadas.
- **Roles por participante**:
  - **Propietario** (`OWNER`): control total — calcular, cerrar, reabrir, eliminar, gestionar roles.
  - **Colaborador** (`CONTRIBUTOR`): crea y edita sus propios gastos, ve todo.
  - **Lector** (`READER`): acceso de solo lectura.
- **Invitaciones por email**: invita a otras personas a unirse a tus eventos. Flujo pendiente → aceptar → rechazar.
- **Vista de calendario**: navegación visual por la línea temporal de eventos.
```

### 5.2 Gastos

```markdown
### 💸 Gastos

- **6 modos de reparto** disponibles al crear un gasto:
  | Modo | ID | ¿Cómo funciona? |
  |------|----|-----------------|
  | Consumo real | `real_consumption` | Cada ítem se reparte solo entre los perfiles asignados |
  | Media simple | `simple_avg` | División a partes iguales entre todos los participantes |
  | Por categoría | `by_category` | Ítems compartidos se dividen entre todos; el resto, entre asignados |
  | % personalizado | `custom_percentage` | Cada perfil paga un porcentaje (debe sumar 100 %) |
  | Importe exacto | `exact` | Cada perfil paga un importe fijo (debe coincidir con el total) |
  | Por partes | `parts` | Cada perfil pone partes enteras (1-100); reparto proporcional |
- **11 categorías** con iconos y colores: Compartido, Vuelo, Alojamiento, Comida, Transporte, Ocio, Compras, Salud, Educación, Servicios, Otro.
- **Soporte multi-moneda**: campos `exchangeRate` e `itemCurrency` preparados para conversión futura (actualmente EUR).
- **Auditoría inmutable**: cada operación CRUD sobre gastos genera un registro `ExpenseAuditEntry` inalterable.
```

### 5.3 Liquidación

```markdown
### ⚖️ Liquidación

- **Algoritmo greedy**: calcula la cantidad mínima de transferencias necesarias para saldar todas las deudas (`SettlementEngine.kt`).
- **Casos borde detectados**: saldos compensados internamente, acreedores eliminados, balances en cero.
- **Versionado de cálculos**: cada ejecución genera un `CalculationVersion` inmutable; las versiones anteriores se preservan.
- **Ajustes**: `AdjustmentEntry` permite corregir transferencias cobradas sin modificar la deuda original.
- **Moneda**: EUR para esta versión (`SUPPORTED_CURRENCY = "EUR"`).
- **Guardián de integridad**: `IntegrityGuard` impide recalcular si faltan participantes previos y valida los datos antes del cálculo.
```

### 5.4 Perfiles

```markdown
### 👤 Perfiles

- **Gestión completa**: crea, edita y elimina perfiles con nombre, emoji de icono y email vinculado.
- **Perfiles fantasma**: perfiles temporales (`isGhost = true`) para participantes puntuales sin cuenta.
- **Nombres personalizados**: cada usuario puede asignar un nombre visible distinto al de otros perfiles (`customNames`).
- **Foto de perfil**: subida de imagen a Firebase Storage, vinculada al perfil de autenticación.
- **Resumen de balances**: posición neta por perfil (saldo positivo = te deben, negativo = debés).
- **Configuración de cuenta**: cambio de contraseña, nombre de usuario, foto y nombre visible.
```

### 5.5 Panel (Dashboard)

```markdown
### 📊 Panel

- **Resumen financiero**: total gastado, tu parte vs. lo que te deben, por evento.
- **Balances por perfil**: quién debe a quién, posiciones netas.
- **Deudas pendientes por perfil**: desglose de qué debe cada persona.
- **Acceso al calendario**: navegación rápida a la vista de calendario.
```

### 5.6 Notificaciones

```markdown
### 🔔 Notificaciones

- **Recordatorios locales**: `ReminderWorker` (WorkManager, diario) para deudas pendientes, eventos incompletos y próximos eventos.
- **Notificaciones push**: `CuentaMorososFirebaseMessagingService` (FCM) para invitaciones y cálculos nuevos.
- **Desduplicación**: sistema de huellas (`fingerprint`) en `CuentaMorososLocalStore` que evita notificaciones repetidas.
- **Deep links**: las notificaciones navegan directamente al evento o invitación correspondiente.
```

### 5.7 Soporte Offline

```markdown
### 📡 Soporte Offline

- **Repositorios offline-first**: todos los datos se cachean en SQLDelight y se sincronizan con Firestore al reconectar.
- **Cola de operaciones pendientes**: `PendingOperationQueue` persiste en SQL las operaciones remotas fallidas y las reintenta al recuperar conexión.
- **Sincronización escalonada**: eventos → deudas → gastos → perfiles, con 500 ms de retardo entre cada repositorio.
- **Indicador visual**: banner que muestra el estado de la conexión en la UI.
- **Monitor de red**: interfaz `NetworkMonitor` con implementación específica por plataforma (`ConnectivityManager` en Android).
```

### Tono

Descriptivo, preciso. Cada ítem debe mencionar el tipo/clase/archivo clave entre paréntesis como referencia técnica (ej: `` `StateMachine` ``, `` `SettlementEngine.kt` ``). Los términos en inglés van en backticks, la UI en español.

### Screenshots

No aplica en esta sección. Las capturas van en la Sección 9.

---

## Sección 6: Arquitectura

### Estructura Markdown

```markdown
## 🏗️ Arquitectura

### Patrón: Repositorio Offline-First

```
┌─────────────────────────────────────────────────────────────┐
│                     App Module (Android)                     │
│  MainActivity → CuentaMorososApp (singleton)                │
│  Notification services, Firebase native SDK, WorkManager     │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                 Shared Module (KMP)                          │
│                                                              │
│  ┌─────────┐    ┌──────────────┐    ┌────────────────────┐  │
│  │   UI    │───▶│  ViewModels   │───▶│    Repositories    │  │
│  │ Compose │    │ StateFlow +   │    │  OfflineFirst wrap │  │
│  │ Screens │    │ derivedStateOf│    │  Firestore remotes │  │
│  └─────────┘    └──────┬───────┘    └────────┬───────────┘  │
│                        │                      │              │
│                        ▼                      ▼              │
│              ┌─────────────────┐    ┌──────────────────┐    │
│              │  Model Engines  │    │  Local Cache      │    │
│              │  SplitCalculator│    │  SQLDelight (SQL) │    │
│              │  SettlementEng. │    │  8 tables         │    │
│              │  PermissionEng. │    └──────────────────┘    │
│              │  IntegrityGuard │                            │
│              │  StateMachine   │    ┌──────────────────┐    │
│              └─────────────────┘    │  Pending Queue    │    │
│                                     │  Offline retry    │    │
│                                     └──────────────────┘    │
│                                                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              Design System: NeoFintech                │  │
│  │  Dark theme, neon accents, custom typography/spacing  │  │
│  │  MoneyExplosionAnimation, animated transitions        │  │
│  └───────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

### Decisiones Clave de Diseño

1. **Módulo compartido KMP**: la lógica de negocio y la UI Compose viven en `shared/` para reutilización entre plataformas. El módulo `app/` es un shell Android delgado.
2. **Patrón repositorio OfflineFirst**: cada entidad (`EventRepository`, `DebtRepository`, `ExpenseRepository`, `ProfileRepository`) tiene una interfaz, una implementación Firestore y un wrapper que cachea en SQLDelight.
3. **Persistencia dual**: SQLDelight (primaria, reactiva vía `Flow`) + `CuentaMorososLocalStore` (SharedPreferences, para datos legacy y desduplicación).
4. **Motores de modelo puros**: `SplitCalculator`, `SettlementEngine`, `PermissionEngine`, `IntegrityGuard`, `StateMachine` son Kotlin puro sin dependencias de framework — 100 % testeables.
5. **DI manual**: sin Hilt ni Koin. `AppViewModelFactory` construye ViewModels, `RepositoryProvider` cablea los repositorios.
6. **`derivedStateOf` para propiedades computadas**: agregados del panel, balances netos y mensajes de recordatorio se calculan reactivamente.
7. **Sincronización escalonada**: 500 ms de retardo entre repositorios al iniciar para no saturar Firestore.
8. **`PendingOperationQueue`**: operaciones remotas fallidas se persisten en SQLDelight y se reintentan con backoff exponencial al reconectar.

### Ejemplo de Flujo de Datos: Crear un Gasto

```
Usuario rellena formulario en EventDetailScreen
  → EventDetailViewModel.saveExpense(expense)
    → OfflineFirstExpenseRepository.saveExpense(expense)
      → SQLDelight INSERT (cache local — actualización inmediata en UI)
      → FirestoreExpenseRepository.saveExpense(expense) (remoto — intento)
        → si falla: PendingOperationQueue.enqueue(...)
```
```

### Contenido

- Diagrama ASCII del repositorio offline-first (tomado del exploration.md, verificado contra estructura real).
- 8 decisiones clave de diseño en lista numerada.
- Ejemplo de flujo de datos (crear gasto) con pasos indentados.

### Tono

Arquitectónico, formal. Las descripciones técnicas en español, los nombres de clases y archivos en backticks.

### Screenshots

No aplica.

---

## Sección 7: Testing

### Estructura Markdown

```markdown
## 🧪 Testing

### Frameworks

| Framework | Tipo | Módulo | Comando |
|-----------|------|--------|---------|
| JUnit 4 + Robolectric | Unit tests | `app/` | `./gradlew test` |
| Kotlin Test + kotlinx-coroutines-test | Unit tests | `shared/` | `./gradlew :shared:allTests` |
| Espresso + Compose UI Test | Instrumented | `app/` | `./gradlew connectedAndroidTest` |

### Ejecución

```bash
# Tests unitarios del módulo app
./gradlew test

# Tests unitarios del módulo shared (KMP)
./gradlew :shared:allTests

# Tests de instrumentación (requiere emulador/dispositivo)
./gradlew connectedAndroidTest

# Todos los tests
./gradlew test :shared:allTests connectedAndroidTest
```

> **Cobertura actual**: 47 archivos de test (~45 tests unitarios en `shared/commonTest`, ~8 en `app/src/test`). Sin herramienta de cobertura (JaCoCo) configurada.
```

### Contenido

- Tabla de frameworks con columna de comando.
- 4 comandos en bloque bash.
- Nota de cobertura (47 test files).
- No incluir cobertura falsa — JaCoCo no está configurado.

### Tono

Directo, técnico.

### Screenshots

No aplica.

---

## Sección 8: Recursos Adicionales

### Estructura Markdown

```markdown
## 📚 Recursos Adicionales

- **[Documentación del proyecto](documentation/README.md)**: índice completo con requisitos funcionales, no funcionales, diseño, sprints y API.
- **[Guía para agentes de IA](AGENTS.md)**: convenciones de desarrollo, arquitectura, patrones y reglas para asistentes de código.
- **[Licencia MIT](LICENSE)**: texto completo de la licencia.
- **[SDD (Spec-Driven Development)](openspec/)**: artefactos de especificación, cambios activos y archivo histórico.
```

### Contenido

- 4 enlaces con descripciones.
- Rutas relativas que funcionan en GitHub y localmente.

### Tono

Informativo, conciso.

### Screenshots

No aplica.

---

## Sección 9: Capturas de Pantalla

### Estructura Markdown

```markdown
## 📸 Capturas de Pantalla

> ⚠️ **Sección en construcción**. Las capturas se irán añadiendo a medida que estén disponibles.

<!-- SCREENSHOT: Pantalla principal del Dashboard — resumen financiero, balances por perfil, accesos rápidos -->
<!-- SCREENSHOT: Lista de Eventos — tarjetas con nombre, fechas, estado (Abierto/Calculado/Cerrado), número de participantes -->
<!-- SCREENSHOT: Detalle de Evento — gastos del evento, deudas calculadas, acceso a calculadora y liquidación -->
<!-- SCREENSHOT: Calculadora de gastos (CalculatorSheet) — selector de modo de reparto, asignación de perfiles, vista previa de importes -->
<!-- SCREENSHOT: Panel de Liquidación (SettlementPanel) — lista de transferencias, pagos cobrados/pendientes -->
<!-- SCREENSHOT: Vista de Calendario — timeline visual con eventos por mes -->
```

### Contenido

- Párrafo introductorio indicando que está en construcción.
- 6 comentarios HTML `<!-- SCREENSHOT: ... -->` con descripciones de qué capturar:
  1. Dashboard (resumen financiero)
  2. Lista de eventos (tarjetas con estados)
  3. Detalle de evento (gastos, deudas)
  4. Calculadora (CalculatorSheet, selector de modo)
  5. Liquidación (SettlementPanel, transferencias)
  6. Calendario (vista mensual)

### Tono

Informativo, humilde ("en construcción").

### Screenshots

Esta sección ES el placeholder para screenshots. Cada comentario HTML describe exactamente qué pantalla capturar y qué elementos deben ser visibles.

---

## Tone and Style — Global Notes

- **Idioma**: Castellano formal con tuteo (tú). NUNCA usar voseo. Ejemplos correctos: "crea", "descarga", "activa", "debes", "colócalo". Incorrectos: "creá", "descargá", "debés".
- **Tuteo**: Consistente en todo el documento. Imperativos, conjugaciones y pronombres en segunda persona singular (tú).
- **Backticks**: Nombres de archivos (`.kt`), clases (`StateMachine`), métodos, variables y términos técnicos en inglés siempre entre backticks.
- **Emojis en títulos**: Solo en encabezados de sección y con moderación. Máximo uno por sección. No abusar — debe mantener un tono profesional.
- **Enlaces**: Usar rutas relativas (`documentation/README.md`, `AGENTS.md`, `LICENSE`, `openspec/`). Enlace a Firebase Console como URL completa.
- **Extensiones de archivo**: Incluir `.kt` en referencias a archivos Kotlin, `.json` para configuraciones, `.md` para documentos.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Factual accuracy | Enums, estados, versiones | Verificar cada afirmación contra `Models.kt`, `CalculatorEngine.kt`, `StateMachine.kt`, build files |
| Link validity | Enlaces relativos | Comprobar que `documentation/README.md`, `AGENTS.md`, `LICENSE`, `openspec/` existen en disco |
| Completeness | Secciones requeridas | Contrastar contra las 9 secciones del `proposal.md` y sus success criteria |
| Security | Datos sensibles | Asegurar que NO se mencionan rutas de keystore, contraseñas, ni valores de `google-services.json` |

## Migration / Rollout

No migration required — cambio puramente documental. Rollback: `git checkout HEAD -- README.md`.

## Open Questions

- [x] ~~¿Confirmar si el tagline actual se mantiene?~~ → Modificado a tuteo: "Divide gastos. Sin vueltas. Sin cuentas pendientes."
- [x] ~~¿Los emojis en los encabezados de sección son aceptables?~~ → Sí, pero con moderación. Máximo uno por sección, tono profesional.
