# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Comandos

```bash
./gradlew :app:assembleDebug                 # APK debug (lo que corre CI)
./gradlew :app:testDebugUnitTest --continue  # tests del módulo app (JUnit4 + Robolectric)
./gradlew :shared:allTests                   # tests del módulo shared (todos los targets)
./gradlew :shared:jvmTest                    # solo target JVM — el más rápido para iterar
./gradlew compileDebugKotlin                 # comprobación de tipos sin empaquetar
./gradlew installDebug                       # instalar en emulador/dispositivo
```

Un solo test:

```bash
./gradlew :shared:jvmTest --tests "com.cuentamorosos.model.SplitCalculatorTest"
```

```bash
./gradlew :app:testDebugUnitTest --tests "com.cuentamorosos.ui.auth.RegisterScreenTest"
```

No hay linter ni formatter configurado (ni detekt, ni ktlint): la única puerta de calidad es
el compilador de Kotlin y los tests. iOS solo se configura cuando el host es macOS
(`shared/build.gradle.kts` lo comprueba en tiempo de configuración), así que en Linux/Windows
los targets `iosX64/iosArm64/iosSimulatorArm64` simplemente no existen.

## Arquitectura

Dos módulos Gradle. `app/` es un shell Android delgado (entry point, SDK nativo de Firebase,
WorkManager, permisos, deep links); **todo lo demás vive en `shared/`**, incluida la UI Compose.
Si vas a tocar una pantalla, el archivo está en `shared/src/commonMain`, no en `app/`.

### Datos: repositorios offline-first en tres capas

Para cada entidad (Event, Expense, Debt, Profile, Invitation, Adjustment, Audit,
CalculationVersion) hay tres piezas en `shared/src/commonMain/kotlin/com/cuentamorosos/data/repository/`:

1. `XRepository` — interfaz.
2. `FirestoreXRepository` — implementación remota (gitlive firebase-firestore, multiplataforma).
3. `OfflineFirstXRepository` — la que usa la app: escribe primero en SQLDelight (la UI se
   actualiza al instante), luego intenta el remoto; si falla, encola en `PendingOperationQueue`
   y reintenta al reconectar con backoff exponencial.

La UI **siempre** consume `OfflineFirst*`. `RepositoryProvider` los cablea a mano (no hay DI
framework) y `AppViewModelFactory` construye los ViewModels. Ambos están ahora en `commonMain`.

`CuentaMorososLocalStore` (SharedPreferences, en `app/`) ya **no** es la persistencia principal
— solo queda para migración de datos legacy y deduplicación de notificaciones. La nota contraria
en `AGENTS.md` está obsoleta.

### Modelo: motores puros

`shared/src/commonMain/kotlin/com/cuentamorosos/model/` contiene motores sin dependencias de
framework, 100 % testeables: `SplitCalculator` / `CalculatorEngine` (reparto), `SettlementEngine`
(minimiza el número de transferencias), `PermissionEngine` (rol → acciones permitidas),
`StateMachine` (OPEN → CALCULATED → CLOSED con avisos y validaciones),
`EventCreditorResolver`, `IntegrityGuard` y `model/validation/`. Cualquier regla de negocio nueva
debería aterrizar aquí, no en un Composable ni en un ViewModel.

### Permisos y estado del evento

El acceso se decide en dos ejes que se combinan: el rol del participante
(`EventRole.OWNER/EDITOR/READER`, vía `PermissionEngine.getRole(uid, event)`) y el estado del
evento (`EventState`). La UI pregunta siempre a través de `canDo(EventAction.X)` del
`EventDetailViewModel` — no compruebes roles a mano en un Composable. Las mismas reglas están
duplicadas (a propósito) en `firestore.rules`; si cambias unas, cambia las otras.

La visibilidad de perfiles (reglas VIS-001..004) se calcula en `CuentaMorososApp.kt`: perfil
propio + ghosts propios + coparticipantes de eventos compartidos. Todo lo demás se oculta.

### Sistema de diseño "Neo-Fintech"

`shared/src/commonMain/kotlin/com/cuentamorosos/ui/NeoFintech*.kt` define tokens de color,
tipografía (Geist + JetBrains Mono, empaquetadas en `composeResources/font/`), espaciado, formas,
elevación y animación. `CuentaMorososTheme` mapea `NeoFintechColorSet` a un `ColorScheme` de M3 y
además lo expone entero vía `LocalNeoFintechColors`, porque varios tokens
(`buttonContainer`, `onButton`, `warning`, `primaryFixedDim`) no tienen slot en Material 3.

Regla práctica: usa `MaterialTheme.colorScheme` para lo que M3 ya cubre y
`LocalNeoFintechColors.current` para el resto. No metas `Color(0xFF…)` en pantallas.

### Portabilidad iOS: qué es común y qué no

Los targets iOS solo se configuran cuando el host es macOS, así que en Linux **no se puede
compilar ni ejecutar nada de iOS**: la única validación es el workflow `ios-build.yml`
(runner `macos-15`), que linka el framework y compila los tests Native. El target `jvm()` es
el sustituto local: si `:shared:compileKotlinJvm` pasa, `commonMain` no tiene fugas de Android.

Todo el cableado (`RepositoryProvider`, `AppViewModelFactory`), la UI, los ViewModels y las
reglas de notificación viven en `commonMain`. Lo que un host debe aportar son los puertos de
`notifications/NotificationPorts.kt`: `NotificationPresenter`, `NotificationDedupStore` y
`ReminderScheduler`. `NotificationContentFactory` y `PushPayloadParser` resuelven textos,
canales, huellas de deduplicación y payloads push, y son comunes a propósito: si el host los
reimplementara, las dos plataformas divergirían en silencio.

Aunque en Linux no se pueda compilar para iOS, **sí se puede verificar la interop con Apple**
sin un Mac. La distribución prebuilt de Kotlin/Native trae las platform libraries de Apple y se
pueden volcar:

```bash
KONAN=~/.konan/kotlin-native-prebuilt-linux-x86_64-2.1.0
$KONAN/bin/klib dump-metadata \
  $KONAN/klib/platform/ios_simulator_arm64/org.jetbrains.kotlin.native.platform.UserNotifications
```

Da la firma Kotlin exacta de cada API de UIKit, Foundation o UserNotifications. Sirve aunque el
proyecto esté en 1.9.24 y solo esté descargada la 2.1.0: son las mismas cabeceras de Apple. Es la
diferencia entre adivinar y comprobar — así se detectaron dos errores que no habrían compilado:
`setUserInfo` exige `Map<Any?, *>` (y `Map` es invariante en su clave) y el parámetro de
`triggerWithDateMatchingComponents` se llama `dateComponents`, no `dateMatching`.

Ojo con dos falsos amigos de la interop: las propiedades de solo lectura que una subclase
redeclara como escribibles se exponen como métodos `setX()` y no como `var`
(`UNMutableNotificationContent.setTitle`), pero otras sí son `var` de verdad
(`NSDateComponents.hour`). Volcar el klib es la única forma de saber cuál es cuál.

Dos cosas **no** pueden subir a `commonMain`, y no por descuido:
- La subida de la foto de perfil — `dev.gitlive.firebase.storage.Data` es una `expect class`
  por plataforma y su actual de JVM es un stub vacío. Solo se comparten las convenciones, en
  `data/AvatarStorage.kt`.
- El propio host: no existe todavía `iosApp/` (ni proyecto Xcode, ni Podfile, ni
  `GoogleService-Info.plist`). El framework se compila pero nadie lo consume.

### Restricciones de la versión de Compose

Compose Multiplatform **1.6.11** / Kotlin 1.9.24. Esto excluye APIs que sí existen en 1.7+:
`Modifier.animateItem()` (usa `animateItemPlacement()`), transiciones de elemento compartido,
`Modifier.blur` fuera de Android, y el motion "expressive" de M3. `NeonGlowModifier` existe
precisamente porque `RenderEffect.createBlurEffect` no está disponible aquí.

## Flujo de trabajo con OpenSpec

El repo usa OpenSpec (`openspec/config.yaml`, `strict_tdd: true`). Las propuestas vivas están en
`openspec/changes/<nombre>/` (proposal → spec → design → tasks) y al terminar se sincronizan a
`openspec/specs/` y se archivan en `openspec/changes/archive/`. Si trabajas sobre un cambio
existente, lee primero su carpeta: `migrate-ios-mvp` recoge el plan de portabilidad a iOS
(su Fase 0 ya está aplicada; las fases 1-4 requieren el runner macOS de CI).

## Notas de estado

- La app está en producción (v1.2.1, `applicationId com.cuentamorosos`); `main` es la rama de
  release y CI compila y testea en cada push.
- `derivedStateOf` se usa mucho para agregados del panel. Con el delegado `by` **no** accedas a
  `.value`.
- Las propiedades con getter propio (las delegadas por `remember`) no admiten smart cast:
  asígnalas a una variable local antes de comprobar `null`.
