# Proposal: Migrate iOS MVP

## Intent

El módulo `shared` ya compila un framework de iOS y CI lo linka en cada push, pero **nadie lo
consume**: no existe `iosApp/`, ni proyecto Xcode, ni `Podfile`, ni `GoogleService-Info.plist`,
ni un `@main`. La portabilidad está hecha a nivel de código y sin terminar a nivel de producto.

Además, el equipo **no dispone de un Mac**. La única validación posible es el workflow
`ios-build.yml` sobre el runner `macos-15`. Eso condiciona todo el diseño: nada se puede
depurar interactivamente, así que todo lo que pueda verificarse en `commonMain` (target `jvm()`)
debe verificarse ahí antes de tocar el host.

## Scope

### In Scope
- Host iOS mínimo: `iosApp/` generado con XcodeGen desde un `project.yml` versionable
- `MainViewController.kt` en `iosMain` que envuelve `CuentaMorososApp` en `ComposeUIViewController`
- `Podfile` que linka el SDK nativo de Firebase (Core, Auth, Firestore) — requisito de gitlive
- `actual` iOS de los tres puertos: `NotificationPresenter`, `NotificationDedupStore`, `ReminderScheduler`
- Fetcher de red de Coil para iOS (hoy solo existe en `androidMain`; sin él no cargan los avatares)
- Ampliar `ios-build.yml`: `xcodegen` + `pod install` + `xcodebuild` + reactivar los tests Native
- Captura de pantalla en simulador desde CI como única prueba visual disponible

### Out of Scope
- Firma, TestFlight y App Store — requieren cuenta Apple Developer (99 $/año) y un Mac
- Ejecución en dispositivo físico
- Subida de foto de perfil en iOS — `gitlive…storage.Data` es `expect` por plataforma (ver design.md)
- Migración de datos legacy — solo existen en SharedPreferences de Android; iOS nace limpio
- Push reales por APNs — necesitan clave APNs en la consola de Firebase, que es trabajo de F5
- Widgets, Siri, Handoff, iPad

## Capabilities

### New Capabilities
- `ios-host-app`: proyecto Xcode generado, entry point Swift, integración del framework, pods de Firebase
- `ios-notifications`: implementación iOS de los tres puertos sobre `UNUserNotificationCenter`

### Modified Capabilities
Ninguna. La portabilidad ya completada (cableado en `commonMain`, reglas de notificación
compartidas, convenciones de avatar) no altera ninguna spec existente: se verificó que los 897
tests siguen pasando sin tocar una sola aserción.

## Approach

**El proyecto Xcode se genera, no se versiona.** Un `project.pbxproj` escrito a mano es un blob
de UUIDs imposible de revisar y de mantener sin abrir Xcode. En su lugar se versiona un
`project.yml` de XcodeGen (~40 líneas legibles) y el `.xcodeproj` se materializa en el runner.
Es la diferencia entre poder revisar un diff y no poder.

1. **`project.yml`** → `xcodegen generate` produce `iosApp.xcodeproj` en CI
2. **`Podfile`** → los pods de Firebase, que gitlive necesita linkados para funcionar en iOS
3. **Framework**: se mantiene el `binaries.framework` que ya existe; Xcode lo linka vía
   `FRAMEWORK_SEARCH_PATHS` apuntando a la salida de `linkDebugFrameworkIosSimulatorArm64`
4. **`MainViewController.kt`** en `iosMain`: única superficie que el Swift necesita conocer
5. **Puertos**: `actual` iOS sobre `UNUserNotificationCenter` + `NSUserDefaults`
6. **CI** valida en cascada: compila framework → genera proyecto → pods → `xcodebuild` → simulador

## Risks

| Riesgo | Impacto | Mitigación |
|--------|---------|------------|
| Compose Multiplatform 1.6.11 no tiene iOS estable | Alto — scroll, entrada de texto y accesibilidad pueden fallar | Aceptarlo como MVP; medir en la captura de simulador antes de prometer paridad |
| Sin Mac, cada iteración cuesta un ciclo de CI | Alto — depuración lentísima | Agotar `:shared:compileKotlinJvm` en local; agrupar cambios por ciclo |
| Los runners macOS consumen 10× cuota de Actions | Medio | Limitar `ios-build.yml` a `workflow_dispatch` + `main` mientras se itera |
| Desalineación de versión entre gitlive 1.13.0 y el SDK iOS de Firebase | Medio — fallo de link opaco | Pinnear versiones exactas en el `Podfile` y fijarlas en `Gemfile.lock` |
| `isStatic = true` complica el embed en Xcode | Bajo | Un framework estático se linka, no se embebe; si da guerra, cambiar a dinámico |

## Success Criteria

1. ✅ `ios-build.yml` termina en verde con `xcodebuild` construyendo el esquema `iosApp`
2. ⚠️ Los tests Native se **compilan**, no se ejecutan. Los pods se integran en el target de
   Xcode, no en el ejecutable que linka Gradle, así que faltan los símbolos de FirebaseCore.
   Hacerlo requiere el plugin de CocoaPods de KMP — alcance propio (ver tasks.md 3.2)
3. ✅ CI publica una captura del simulador con la pantalla de login renderizada por Compose
4. ✅ 911 tests en verde; los de `commonTest` compilan también para el target iOS
5. ✅ `project.yml` y `Podfile` son legibles en un diff; no hay `.pbxproj` versionado
