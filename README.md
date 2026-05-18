# CuentaMorosos

<div align="center">
  <img src="logo.png" alt="CuentaMorosos Logo" width="120" />
  <p><em>Dividí gastos. Sin drama. Sin cuentas pendientes.</em></p>
</div>

---

Aplicación Android para gestionar gastos compartidos en eventos — viajes, cenas, fiestas, lo que sea. Calculá quién le debe a quién con el mínimo de transferencias posibles.

## Funcionalidades

### Eventos
- Creá eventos con fechas de inicio y fin
- Ciclo de vida completo: **Borrador → Abierto → Calculado → Cerrado**
- Roles por participante: **Owner** (control total), **Contributor** (agrega y edita), **Reader** (solo lectura)
- Invitaciones por email para sumar participantes

### Gastos
- Categorías con iconos: Compartido, Vuelo, Alojamiento, Comida, Transporte, Otro
- **4 modos de división** por gasto:
  - **Igual** — reparte a partes iguales
  - **Exacto** — cada uno paga un monto específico
  - **Porcentaje** — cada uno paga un porcentaje
  - **Partes** — reparto por fracciones (1-100)
- Soporte multi-moneda con conversión (stub listo para FX real)
- Gastos compartidos y personales en el mismo evento

### Liquidación
- Algoritmo **greedy** que calcula el mínimo de transferencias necesarias para saldar todas las deudas
- Detección de casos borde: saldos compensados internamente, acreedores eliminados, balances en cero
- Snapshots de cálculo versionados — nunca se pierde una versión anterior
- Trail de auditoría inmutable para cada gasto (creación, edición, eliminación)
- Ajustes rectificados sin modificar la deuda original

### Perfiles
- Gestión de participantes con nombre, icono y email vinculado
- Resumen de balances por perfil (a favor / en deuda)
- Perfiles "fantasma" para cuentas temporales

### UX
- Tema **neo-fintech** oscuro con acentos neón y animaciones fluidas
- Tipografía y espaciado diseñados para legibilidad financiera
- Recordatorios configurables con WorkManager
- Sincronización en la nube con Firebase
- Cola de operaciones pendientes para trabajo offline

## Tech Stack

| Capa | Tecnología |
|------|------------|
| UI | Jetpack Compose (Kotlin Multiplatform) |
| Arquitectura | MVVM + ViewModel + StateFlow |
| Base de datos local | SQLDelight |
| Auth & Sync | Firebase Auth + Firestore |
| Notificaciones | WorkManager |
| Testing | JUnit (cálculos puros) |

## Estructura

```
shared/src/commonMain/
├── com/cuentamorosos/
│   ├── model/          # Engines puros: SplitCalculator, SettlementEngine, IntegrityGuard
│   ├── ui/             # Screens y componentes Compose
│   ├── data/           # Repositorios, cola offline, monitor de red
│   └── db/             # SQLDelight generated
```

## Próximos pasos

1. Abrir el proyecto en Android Studio
2. Sincronizar Gradle
3. Configurar `local.properties` con tu Android SDK si hace falta
4. Ejecutar en emulador o dispositivo

---

> Hecho con Kotlin y mucho café ☕
