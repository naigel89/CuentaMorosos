# Tasks: Migrate iOS MVP

## Review Workload Forecast

| Campo | Valor |
|-------|-------|
| Líneas estimadas | ~700–900 (Fase 0 ya aporta 939) |
| Riesgo de presupuesto de 400 líneas | Alto |
| PRs encadenados recomendados | Sí |
| División sugerida | PR 1 (Fase 0, hecha) → PR 2 (puertos iOS) → PR 3 (host Xcode) → PR 4 (CI + captura) |
| Estrategia de entrega | ask-on-risk |
| Estrategia de encadenado | stacked-to-main |

> **Hallazgo (2026-08-26)**: al empezar la Fase 1 se descubrió que el workflow de iOS
> llevaba roto en `main` desde `84dfe55`. El framework compilaba; fallaba la compilación de
> los tests por un nombre entre backticks con una coma, que Kotlin/Native rechaza. Corregido
> en `aec371f`. Conviene mirar el estado de `ios-build.yml` antes de dar por bueno cualquier
> supuesto sobre iOS.

**Ciclo de verificación**: sin Mac, cada tarea de las fases 1–4 solo se valida en el runner
`macos-15`. Agrupa los cambios: un ciclo de CI por tarea es caro y lento. Antes de cada push,
agota `:shared:compileKotlinJvm` y `:shared:jvmTest` en local — atrapan casi todas las fugas
de `commonMain` a coste cero.

### Unidades de trabajo

| Unidad | Objetivo | PR | Notas |
|--------|----------|----|-------|
| 0 | Portabilidad de `commonMain` | PR 1 | **Hecha**. Verificable en Linux |
| 1 | Los tres puertos en `iosMain` | PR 2 | Compila en CI; aún sin host |
| 2 | Host Xcode + pods de Firebase | PR 3 | Depende de 1; primera app arrancable |
| 3 | CI completo + captura de simulador | PR 4 | Depende de 2; cierra el criterio de éxito |

## Fase 0: Portabilidad de commonMain — HECHA

Rama `refactor/ios-shell-ports`, 5 commits, 897 tests en verde.

- [x] 0.1 Mover `RepositoryProvider` y `AppViewModelFactory` de `androidMain` a `commonMain`.
      Portar la firma a `create(KClass, CreationExtras)`, la que expone el artefacto KMP de
      lifecycle. **Acceptance**: `:shared:compileKotlinJvm` y `:app:compileDebugKotlin` en verde.
- [x] 0.2 Crear `notifications/NotificationContent.kt` — `NotificationContentFactory` resuelve
      título, cuerpo, canal, huella, deep link y acciones. **Acceptance**: los tests Robolectric
      que afirman los textos exactos pasan **sin modificarse**.
- [x] 0.3 Crear `notifications/NotificationPorts.kt` — los tres puertos y `NotificationCoordinator`.
      **Acceptance**: 21 tests nuevos en `commonTest`.
- [x] 0.4 Reescribir el `NotificationDispatcher` de Android como `NotificationPresenter`
      (381 → 221 líneas), conservando su API pública. **Acceptance**: 114 tests de `app` en verde.
- [x] 0.5 Extraer `WorkManagerReminderScheduler`; `ReminderWorker.schedule/cancel` delegan en él.
- [x] 0.6 Mover el parseo de payloads push a `PushPayloadParser` en `commonMain` (servicio FCM:
      103 → 47 líneas). **Acceptance**: `FcmIntegrationTest` pasa sin tocarse; 8 tests nuevos.
- [x] 0.7 Crear `data/AvatarStorage.kt` con ruta, tamaño y calidad; `MainActivity` los consume.
- [x] 0.8 Corregir las notas obsoletas de `CLAUDE.md` sobre el estado de iOS.

## Fase 1: Los tres puertos en iosMain — HECHA (pendiente de validar en CI)

- [x] 1.1 `shared/src/iosMain/.../notifications/IosNotificationPresenter.kt` — implementa
      `NotificationPresenter` sobre `UNUserNotificationCenter`. `ensureChannels()` registra una
      `UNNotificationCategory` por `NotificationType` (no por canal: las acciones dependen del
      tipo) usando `NotificationContentFactory.actionsForType`.
      **Files**: `IosNotificationPresenter.kt`. **Acceptance**: R101, R102; `linkDebugFrameworkIosSimulatorArm64` en verde.
- [x] 1.2 `shared/src/iosMain/.../notifications/IosNotificationDedupStore.kt` — `NSUserDefaults`.
      La codificación de entradas sube a `NotificationDedupEntries` en `commonMain` y
      **Android la adopta también**, para que compartirla no sea solo aspiracional.
      **Acceptance**: R103; los 114 tests de Android pasan sin tocar aserciones.
- [x] 1.3 `shared/src/iosMain/.../notifications/IosReminderScheduler.kt` — notificación diaria
      genérica con `UNCalendarNotificationTrigger`. Documentar en KDoc por qué el contenido es
      genérico y no por deuda. **Acceptance**: R105.
- [x] 1.4 Añadir `coil-network-ktor3:3.0.4` y el motor Ktor Darwin al source set `iosMain` de
      `shared/build.gradle.kts`, dentro de la guarda `isMac`. **Files**: `shared/build.gradle.kts`.
      **Acceptance**: R004; el framework linka con Coil resoluble.

## Fase 2: Host Xcode

- [ ] 2.1 `iosApp/project.yml` — target `iosApp`, bundle ID `com.cuentamorosos`, despliegue iOS 15+,
      `FRAMEWORK_SEARCH_PATHS` a la salida de `linkDebugFrameworkIosSimulatorArm64`, capability de
      notificaciones. Añadir `iosApp/*.xcodeproj` y `*.xcworkspace` al `.gitignore`.
      **Acceptance**: R001; `xcodegen generate` produce el proyecto en CI.
- [ ] 2.2 `iosApp/Podfile` — `FirebaseCore`, `FirebaseAuth`, `FirebaseFirestore` con versión
      exacta compatible con gitlive 1.13.0. Generar y versionar `Gemfile.lock` desde CI.
      **Acceptance**: R003; `pod install` en verde.
- [ ] 2.3 `shared/src/iosMain/.../MainViewController.kt` — única función exportada. Construye
      `DriverFactory`, `RepositoryProvider`, `AppViewModelFactory` y los tres puertos; devuelve un
      `UIViewController` con `ComposeUIViewController { CuentaMorososTheme { CuentaMorososApp(...) } }`.
      Pasar `onPickPhoto = null` (fuera de alcance). **Acceptance**: R002.
- [ ] 2.4 `iosApp/Sources/iOSApp.swift` — `@main`, `FirebaseApp.configure()`, monta
      `MainViewController()`. `UNUserNotificationCenterDelegate` que enruta `userInfo` a
      `PushPayloadParser` y emite el `DeepLinkTarget` al pulsar. **Acceptance**: R002, R104.
- [ ] 2.5 Plist de marcador para `GoogleService-Info.plist`, inyectado por el workflow. El real
      **no** se versiona. **Acceptance**: R003, escenario de archivo ausente.

## Fase 3: CI y prueba visual

- [ ] 3.1 Ampliar `.github/workflows/ios-build.yml`: instalar XcodeGen, `xcodegen generate`,
      `pod install`, `xcodebuild` del esquema para simulador. Limitar el disparo a `main` y
      `workflow_dispatch` mientras se itera, por cuota de runners macOS.
      **Acceptance**: R005.
- [ ] 3.2 Sustituir `compileTestKotlinIosSimulatorArm64` por la tarea de test real, ya con
      Firebase linkado. **Acceptance**: R003; los tests de `commonTest` corren en iOS.
- [ ] 3.3 Arrancar simulador con `xcrun simctl`, instalar el `.app`, `simctl io screenshot`,
      publicar el PNG como artefacto. **Acceptance**: R006; se distingue de pantalla en blanco.

## Fase 4: Documentación de cierre

- [ ] 4.1 README: sección de iOS con el estado real, lo que funciona y lo que no (foto de perfil,
      recordatorios genéricos, sin distribución).
- [ ] 4.2 Sincronizar las specs a `openspec/specs/` y archivar el cambio.

## Fuera de alcance — requieren Mac y cuenta Apple Developer

Estas no son tareas pendientes de este cambio; se listan para que no se confundan con olvidos:

- Firma de código, perfiles de aprovisionamiento, TestFlight, App Store
- Clave APNs en la consola de Firebase → sin ella no hay push reales en iOS
- Ejecución en dispositivo físico
- Subida de foto de perfil en iOS (ver design.md § Tradeoffs)
