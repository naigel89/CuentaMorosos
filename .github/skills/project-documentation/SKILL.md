---
name: project-documentation
description: 'Genera documentación del proyecto en Markdown: DD, FR, NFR, R-NEG, API, UI y changelog. Úsalo para documentar requisitos, historias de usuario, reglas de negocio, diseño detallado, interfaces de pantalla, contratos de endpoints y control de versiones dentro de `documentation/`.'
argument-hint: 'Ej.: "FR del módulo de clientes" o "documentación completa del proyecto"'
user-invocable: true
---

# Documentación del proyecto

## Objetivo
Crear y mantener documentación clara, trazable, versionada y útil del proyecto en formato Markdown, organizada por tipo de requisito dentro de `documentation/`.

## Estructura obligatoria
Siempre trabajar dentro de esta estructura:

```text
documentation/
├── dd/
├── fr/
├── nfr/
├── r-neg/
├── api/
├── ui/
├── sprints/
├── CHANGELOG.md
└── README.md
```

- `dd/`: diseño detallado, arquitectura, flujos y decisiones técnicas.
- `fr/`: requisitos funcionales.
- `nfr/`: requisitos no funcionales.
- `r-neg/`: reglas de negocio.
- `api/`: documentación de endpoints, contratos y ejemplos.
- `ui/`: documentación de pantallas, componentes visuales, navegación e interacción.
- `sprints/`: planificación del trabajo pendiente por iteraciones.
- `CHANGELOG.md`: registro global de altas y revisiones documentales.

Todos los archivos deben terminar en `.md`.

## Cuándo usar esta skill
Usa esta skill cuando necesites:
- generar documentación inicial del proyecto
- documentar una funcionalidad nueva
- actualizar requisitos tras cambios en el código
- crear documentación técnica o de API
- documentar interfaces de usuario y comportamiento de pantallas
- convertir análisis del sistema en historias de usuario y requisitos
- registrar nuevas versiones o revisiones documentales

## Reglas obligatorias
1. **No inventar funcionalidades**: primero inspeccionar `README.md`, código fuente, configuración y nombres reales de clases, pantallas, servicios o endpoints.
2. **Usar historias de usuario** cuando aplique, especialmente en `FR` y `DD`, con el formato:
   - `Como <rol>, quiero <objetivo>, para <beneficio>.`
3. **Identificar supuestos** en una sección `## Suposiciones` si falta información.
4. **Mantener trazabilidad** con códigos como `FR0001A1`, `NFR0001A1`, `RN0001A1`, `DD0001A1`, `API0001A1` y `UI0001A1`.
5. **Aplicar la nomenclatura obligatoria** en el nombre del archivo:
   - Formato: `<TIPO><XXXX><VERSIÓN><REVISIÓN>-<slug>.md`
   - Ejemplos: `DD0001A1-arquitectura.md`, `FR0002A1-login.md`, `API0003B2-clientes.md`, `UI0004A1-pantalla-login.md`
6. **Usar versionado por letra y revisión por número**:
   - la primera versión siempre será `A`
   - la revisión inicial por defecto siempre será `1`
   - cuando el contenido evolucione sin cambiar de versión, subir la revisión (`A2`, `A3`...)
   - cuando haya un cambio de alcance importante, avanzar la versión (`B1`, `C1`...)
7. **Incluir metadatos al inicio de cada documento**:

```md
> **Código:** FR0001A1
> **Versión:** A
> **Revisión:** 1
> **Fecha:** YYYY-MM-DD
```

8. **Añadir un `## Changelog` en cada documento** y actualizar también `documentation/CHANGELOG.md` cuando se cree o revise un archivo.
9. **Redactar de forma clara y verificable**: usar títulos jerárquicos, tablas simples y criterios de aceptación concretos.
10. **Redactar en español por defecto**, salvo que el usuario pida otro idioma.

## Procedimiento

### 1. Analizar el contexto
Antes de escribir, revisar lo necesario del proyecto:
- `README.md`
- archivos de configuración y build
- código relacionado con la funcionalidad
- modelos de datos, pantallas, servicios y controladores

### 2. Identificar el tipo de documento
Elegir uno o varios tipos según la petición:
- **DD**: diseño detallado y enfoque técnico
- **FR**: comportamiento esperado por el usuario o negocio
- **NFR**: rendimiento, seguridad, disponibilidad, mantenibilidad, etc.
- **R-NEG**: políticas y reglas que condicionan el comportamiento
- **API**: endpoints, payloads, respuestas, errores y autenticación
- **UI**: definición de pantallas, componentes visibles, estados, navegación y comportamiento de la interfaz

### 3. Crear o verificar la estructura
Asegurar que existan estas carpetas y archivos:
- `documentation/dd/`
- `documentation/fr/`
- `documentation/nfr/`
- `documentation/r-neg/`
- `documentation/api/`
- `documentation/ui/`
- `documentation/sprints/`
- `documentation/CHANGELOG.md`

Si falta un índice general, crear o actualizar `documentation/README.md` con enlaces a los documentos generados.

### 4. Preparar versionado y control de cambios
Antes de redactar el contenido:
1. asignar el código del documento (`FR0001A1`, `DD0002A1`, etc.)
2. nombrar el archivo con ese prefijo
3. registrar la fecha de creación o actualización
4. añadir una primera línea al `## Changelog`
5. actualizar `documentation/CHANGELOG.md`

### 5. Redactar el documento con la plantilla correcta

#### Bloque obligatorio común
Todos los documentos deben empezar con este bloque:

```md
# FR0001A1: <nombre del requisito>

> **Código:** FR0001A1
> **Versión:** A
> **Revisión:** 1
> **Fecha:** YYYY-MM-DD
```

Y terminar con este bloque:

```md
## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| YYYY-MM-DD | A | 1 | Alta | Creación inicial del documento. |
```

#### Plantilla `FR`
Guardar en `documentation/fr/`.

```md
## Resumen
Breve descripción del requisito funcional.

## Historia de usuario
Como <rol>, quiero <objetivo>, para <beneficio>.

## Descripción funcional
Explicar el comportamiento esperado, entradas, salidas y flujo principal.

## Reglas relacionadas
- RN0001A1

## Criterios de aceptación
- [ ] criterio 1
- [ ] criterio 2

## Casos límite
- caso límite 1
- caso límite 2

## Dependencias
Módulos, pantallas, servicios o integraciones relacionadas.
```

#### Plantilla `NFR`
Guardar en `documentation/nfr/`.

```md
## Objetivo
Qué cualidad debe cumplir el sistema.

## Categoría
Rendimiento / seguridad / disponibilidad / escalabilidad / usabilidad / mantenibilidad.

## Métrica o criterio medible
Valor objetivo o umbral esperado.

## Justificación
Por qué este requisito es importante.

## Validación
Cómo se comprobará su cumplimiento.

## Impacto técnico
Componentes o decisiones afectadas.
```

#### Plantilla `R-NEG`
Guardar en `documentation/r-neg/`.

```md
## Descripción
Regla de negocio expresada de forma directa.

## Motivación
Razón de negocio u operativa.

## Condiciones
Cuándo aplica y qué excepciones existen.

## Impacto funcional
Qué comportamientos restringe o habilita.

## Ejemplos
- Ejemplo válido
- Ejemplo no válido
```

#### Plantilla `DD`
Guardar en `documentation/dd/`.

```md
## Resumen técnico
Descripción general de la solución.

## Historia de usuario relacionada
Como <rol>, quiero <objetivo>, para <beneficio>.

## Componentes involucrados
Lista de clases, módulos, pantallas, servicios o capas.

## Flujo de solución
Paso a paso técnico del proceso.

## Modelo de datos
Entidades, atributos y relaciones relevantes.

## Dependencias y riesgos
Librerías, integraciones y riesgos conocidos.

## Decisiones técnicas
Decisiones clave y justificación.
```

#### Plantilla `API`
Guardar en `documentation/api/`.

````md
## Resumen
Objetivo del endpoint o contrato.

## Historia de usuario relacionada
Como <rol>, quiero <objetivo>, para <beneficio>.

## Endpoint
`METHOD /ruta`

## Autenticación
Tipo de autenticación o autorización requerida.

## Parámetros de entrada
| Campo | Tipo | Requerido | Descripción |
|---|---|---|---|

## Ejemplo de request
```json
{}
```

## Ejemplo de response
```json
{}
```

## Códigos de error
| Código | Significado |
|---|---|

## Observaciones
Notas de contrato, validaciones y compatibilidad.
````

#### Plantilla `UI`
Guardar en `documentation/ui/`.

```md
## Resumen
Descripción breve de la pantalla o interfaz.

## Historia de usuario relacionada
Como <rol>, quiero <objetivo>, para <beneficio>.

## Objetivo de la pantalla
Qué necesidad cubre dentro del flujo del usuario.

## Componentes visibles
| Componente | Tipo | Descripción | Obligatorio |
|---|---|---|---|

## Estados de la interfaz
- estado inicial
- carga
- vacío
- error
- éxito

## Reglas de interacción
- acción del usuario 1
- validación o restricción 1

## Navegación
- pantalla origen
- pantalla destino

## Consideraciones UX/UI
Accesibilidad, feedback visual, consistencia y responsive si aplica.
```

### 6. Revisar calidad antes de terminar
Comprobar siempre:
- que el documento está en la carpeta correcta
- que el nombre sigue la nomenclatura `TIPOXXXXA1-nombre.md`
- que el archivo es Markdown
- que existe el bloque de metadatos
- que el `## Changelog` está actualizado
- que los documentos UI describen pantallas, componentes, estados y navegación
- que hay lenguaje claro y estructura legible
- que los criterios son verificables
- que las historias de usuario están incluidas cuando aportan valor
- que no se presentan supuestos como hechos confirmados

## Resultado esperado
Cuando se solicite documentación, producir archivos `.md` bien organizados dentro de `documentation/`, con contenido claro, reusable, versionado y alineado con el código real del proyecto.