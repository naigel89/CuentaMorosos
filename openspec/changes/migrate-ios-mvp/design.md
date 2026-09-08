# Design: Migrate iOS MVP

## Architecture

### Punto de partida real

Auditado sobre `main` el 2026-08-26. Lo que **ya funciona**:

| Pieza | Estado |
|---|---|
| Targets `iosX64/iosArm64/iosSimulatorArm64` | Declarados bajo guarda `isMac` en `shared/build.gradle.kts` |
| `expect/actual` para iOS | Los 4 completos: `Platform`, `SystemBackHandler`, `DriverFactory`, `NetworkMonitor` |
| UI Compose | 60 archivos en `commonMain/ui`, incluidas todas las pantallas de auth |
| ViewModels | Los 6 en `commonMain` |
| Cableado | `RepositoryProvider` + `AppViewModelFactory` en `commonMain` |
| Reglas de notificación | `NotificationContentFactory`, `PushPayloadParser`, `NotificationCoordinator` en `commonMain` |
| CI | `ios-build.yml` linka el framework debug en `macos-15` |

Lo que **falta** es exclusivamente el host y sus tres puertos.

### La restricción que domina el diseño

No hay Mac. `shared/build.gradle.kts` comprueba el SO en tiempo de configuración, así que en
Linux los targets iOS **no existen**: no es que fallen, es que no se declaran. Consecuencias:

- El target `jvm()` es el proxy de portabilidad. Si `:shared:compileKotlinJvm` pasa, `commonMain`
  no tiene fugas de Android. Es barato y local; hay que exprimirlo antes de tocar iOS.
- Todo error específico de iOS (interop, link, pods, Xcode) solo aparece en CI. Cada uno cuesta
  un ciclo completo de runner macOS.
- Nada puede depurarse paso a paso. El diseño debe minimizar la superficie que solo iOS ejercita.

Por eso el host iOS se diseña **lo más delgado posible**: cuanto menos código viva únicamente en
`iosMain` y en Swift, menos hay que depurar a ciegas.

## Data Flow

### Arranque

```
iOSApp.swift (@main)
  └─ FirebaseApp.configure()              ← lee GoogleService-Info.plist
  └─ ComposeViewControllerKt.MainViewController(...)
       └─ [Kotlin, iosMain] MainViewController.kt
            └─ ComposeUIViewController {
                 CuentaMorososTheme {
                   CuentaMorososApp(
                     viewModelFactory = AppViewModelFactory(RepositoryProvider(...)),
                     onScheduleReminders = iosReminderScheduler::schedule,
                     onPickPhoto        = null,   ← fuera de alcance en el MVP
                     ...
                   )
                 }
               }
```

`CuentaMorososApp` ya recibe todo lo dependiente del host por parámetro (factory, callbacks de
recordatorios, foto, deep link, sign-out). Esa firma **es** la costura de portabilidad, y no hay
que modificarla: el host solo tiene que rellenarla.

### Notificación entrante

```
APNs → Firebase Messaging iOS → UNUserNotificationCenterDelegate (Swift)
  └─ userInfo: [String: String]
       └─ [común] PushPayloadParser.parse(data) → NotificationEvent?
            └─ [común] NotificationCoordinator.dispatch(event)
                 ├─ presenter.areNotificationsEnabled()   ← iOS: UNUserNotificationCenter
                 ├─ dedupStore.hasBeenSent(fingerprint)   ← iOS: NSUserDefaults
                 ├─ [común] NotificationContentFactory.from(event)  → título, cuerpo, canal, deep link
                 └─ presenter.present(content)            ← iOS: UNMutableNotificationContent
```

Solo las tres cajas marcadas "iOS" son código nuevo. Los textos, la deduplicación y el destino
del deep link son los mismos objetos que ya usa Android, así que **no pueden divergir**.

## Component Design

### 1. `project.yml` (XcodeGen) — no `.pbxproj`

Un `project.pbxproj` son cientos de líneas de UUIDs generados. Versionarlo desde una máquina sin
Xcode significa no poder revisar sus diffs ni regenerarlo si se corrompe. XcodeGen invierte la
relación: se versiona un YAML legible y el `.xcodeproj` es un artefacto de build, como el APK.

XcodeGen está disponible vía Homebrew en los runners `macos-15`.

### 2. `MainViewController.kt` (`iosMain`)

Única función exportada al framework. Construye el `DriverFactory`, el `RepositoryProvider`, la
`AppViewModelFactory` y los tres puertos, y devuelve un `UIViewController`. Swift no debe conocer
nada más del módulo compartido: cada símbolo extra que cruce la frontera es superficie que solo
CI puede validar.

### 3. Los tres puertos en `iosMain`

| Puerto | Implementación iOS | Nota |
|---|---|---|
| `NotificationPresenter` | `UNUserNotificationCenter` + `UNMutableNotificationContent` | `ensureChannels()` registra `UNNotificationCategory` con las acciones de `content.actions` |
| `NotificationDedupStore` | `NSUserDefaults` | Basta un `Set<String>` de huellas; el volumen es mínimo |
| `ReminderScheduler` | `UNCalendarNotificationTrigger` diario | **No** hay equivalente a WorkManager (ver tradeoffs) |

### 4. Carga de imágenes

Hueco detectado en la auditoría: `commonMain` trae `coil` y `coil-compose`, pero el fetcher de
red solo está en `androidMain` (`coil-network-okhttp`). En iOS, Coil no sabría descargar nada y
**los avatares no cargarían**, sin ningún error de compilación que lo delate.

`iosMain` necesita `coil-network-ktor3:3.0.4` (verificado que existe) más un motor Ktor Darwin.

## Tradeoffs

### Los recordatorios en iOS no son equivalentes

Android usa un `PeriodicWorkRequest` de WorkManager: el sistema garantiza que se ejecuta y el
worker **calcula** los recordatorios en ese momento leyendo repositorios frescos.

iOS no tiene eso. `BGTaskScheduler` es best-effort y el sistema puede no ejecutarlo nunca si el
usuario abre poco la app. La alternativa realista es programar notificaciones locales por
adelantado con `UNCalendarNotificationTrigger`, pero eso invierte el modelo: el contenido se
congela cuando se programa, no cuando se dispara.

**Decisión**: en el MVP, `ReminderScheduler` de iOS programa un recordatorio genérico diario que
solo invita a abrir la app; el detalle por deuda se calcula al abrirla. Es peor que Android, y es
honesto reconocerlo en la spec en vez de fingir paridad.

### La subida de foto se queda fuera

Se evaluó subirla a `commonMain`. `dev.gitlive:firebase-storage:1.13.0` resuelve y compila, pero
su tipo `Data` es una `expect class` con `actual` distinto por plataforma (`ByteArray` en Android,
`NSData` en iOS) y **no se puede construir desde código común**. Peor: su `actual` de JVM es un
stub vacío, así que meterla en `commonMain` rompería el target `jvm()` — el único bucle de
verificación local disponible sin Mac.

**Decisión**: solo se comparten las convenciones (`AvatarStorage`: ruta, 256 px, calidad 85), que
es exactamente lo que fallaría en silencio si divergiera. La subida se queda como responsabilidad
del host, vía el callback `onPickPhoto` que `CuentaMorososApp` ya recibía. En el MVP iOS se pasa
`null`: la foto se podrá ver, no cambiar.

### Compose Multiplatform 1.6.11 en iOS

El proyecto está clavado en CMP 1.6.11 / Kotlin 1.9.24, versión en la que el soporte de iOS aún
no era estable. Subir de versión arrastraría el sistema de diseño entero (`NeoFintech*`), el
motion system y las restricciones documentadas en `CLAUDE.md`.

**Decisión**: no subir versión en este cambio. Aceptar el riesgo de que scroll, entrada de texto
o accesibilidad no estén a la altura, y medirlo con la captura de simulador antes de prometer
nada. Si resulta bloqueante, la migración de CMP es un cambio aparte con su propio alcance.

### `isStatic = true`

El framework está declarado estático. Un framework estático se linka, no se embebe, así que el
proyecto Xcode necesita `FRAMEWORK_SEARCH_PATHS` en vez de una fase "Embed Frameworks". Si la
integración con CocoaPods da problemas, pasar a dinámico es un cambio de una línea — pero
encarece el arranque de la app, así que solo si hace falta.
