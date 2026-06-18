# Proposal: Actualización Integral del README.md

## Intent

El README actual (~80 líneas) contiene errores factuales críticos:
- Menciona un estado «Borrador» que no existe en el código (el ciclo real es OPEN → CALCULATED → CLOSED).
- Lista 4 modos de división cuando hay 6 en `CalculatorEngine.kt`.
- La estructura de directorios solo muestra `shared/src/commonMain/`, omitiendo `app/`, `iosApp/`, `androidMain/`, `iosMain/`, `jvmMain/`, `sqldelight/`, `documentation/` y `openspec/`.

Faltan 14 áreas completas: instrucciones de instalación, arquitectura offline-first, stack tecnológico completo con versiones, comandos de build/test, testing, licencia, diseño NeoFintech, entre otras. El README no refleja la complejidad real del proyecto KMP ni sirve como puerta de entrada para nuevos colaboradores.

## Scope

### In Scope
- Reescritura completa del README en castellano formal con tono cálido
- Stack tecnológico completo con versiones (tabla)
- Prerrequisitos y setup paso a paso (JDK 17, Android SDK, Firebase)
- Comandos de build, test e instalación
- Estructura de directorios completa con descripciones funcionales
- Diagrama de arquitectura offline-first y flujo de datos
- Inventario de funcionalidades preciso (6 split modes, ciclo OPEN→CALCULATED→CLOSED, roles, settlement, offline)
- Enlace a LICENSE, `documentation/`, y `AGENTS.md`
- Espacios reservados para screenshots (nice-to-have)

### Out of Scope
- Traducción al inglés (futura si se requiere)
- Duplicación del contenido de `/documentation/` (solo enlaces)
- Creación de nuevos documentos de arquitectura
- Badges, GIFs o videos (nice-to-have diferidos)

## Capabilities

### New Capabilities
None — cambio puramente documental. No se crean ni modifican specs de comportamiento.

### Modified Capabilities
None — ningún spec existente cambia sus requisitos.

## Approach

Reescritura del README con 9 secciones estructuradas:

1. **Encabezado** — logo, tagline, descripción general del proyecto
2. **Stack Tecnológico** — tabla por capa (core, UI, datos, async, testing) con versiones exactas del exploration
3. **Instalación y Ejecución** — prerrequisitos, setup Firebase (6 pasos), variables de entorno, comandos gradle
4. **Estructura del Proyecto** — árbol completo con descripciones de cada módulo (app, shared, iosApp) y sus fuentes
5. **Funcionalidades Principales** — 7 subsecciones: eventos (ciclo correcto, roles), gastos (6 modos, 11 categorías), liquidación (greedy, versionado, ajustes), perfiles, dashboard, notificaciones, offline
6. **Arquitectura** — diagrama ASCII del patrón offline-first, decisiones clave de diseño, flujo de datos
7. **Testing** — frameworks, comandos, 45+ tests
8. **Recursos Adicionales** — enlaces a `documentation/`, `AGENTS.md`, `LICENSE`, `openspec/`
9. **Capturas de Pantalla** — placeholders con indicación de qué capturar

Cada sección se verifica contra `Models.kt`, `CalculatorEngine.kt`, `StateMachine.kt` y los build files para garantizar precisión.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `README.md` (raíz) | Rewritten | Reemplazo completo del contenido actual |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Desactualización futura del README respecto al código | Medium | Verificar estados, modos y versiones contra los fuentes antes de escribir; incluir referencia a `AGENTS.md` como fuente de verdad para desarrollo |
| Divergencia de idioma con AGENTS.md (inglés) | Low | README en castellano, términos técnicos en inglés. Consistente con el README actual y documentación en `/documentation/` |
| Incluir datos sensibles del build (keystore passwords) | Low | No referenciar rutas ni credenciales del keystore; mencionar solo que existe signing config sin detalles |

## Rollback Plan

El README actual se preserva en git. Revertir es inmediato: `git checkout HEAD -- README.md`.

## Dependencies

- Exploration completada (`openspec/changes/readme-documentation/explore.md`)
- Acceso a `Models.kt`, `CalculatorEngine.kt`, `StateMachine.kt`, `build.gradle.kts` para verificación de precisión

## Success Criteria

- [ ] El README no contiene el estado «Borrador» ni 4 modos de split — refleja fielmente OPEN/CALCULATED/CLOSED y los 6 modos
- [ ] Stack tecnológico listado con ≥20 componentes y sus versiones exactas
- [ ] Instrucciones de setup reproducibles (JDK 17, Firebase, google-services.json, comandos gradle)
- [ ] Estructura de directorios incluye `app/`, `shared/` (todos los source sets), `iosApp/`, `documentation/`, `openspec/`
- [ ] Sección de arquitectura explica el patrón offline-first con repositorios, SQLDelight y Firestore
- [ ] Enlaces funcionales a `LICENSE`, `documentation/README.md`, y `AGENTS.md`
- [ ] Placeholders de screenshots presentes con etiquetas descriptivas
- [ ] Sin información sensible (passwords, rutas de keystore)
