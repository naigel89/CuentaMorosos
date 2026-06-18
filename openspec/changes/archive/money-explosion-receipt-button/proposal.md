# Proposal: Money Explosion & Receipt Button

## Intent

Cálculo exitoso sin feedback celebratorio. Usuarios no ven desglose completo fuera del sheet. Agregar animación post-cálculo y botón recibo circular.

## Scope

### In Scope
- Canvas animación billetes verdes #00A651/#00C853 ~1.5s post-cálculo
- Overlay dismissable por tap
- Botón circular `Receipt` en `SettlementPanel`
- `ReceiptPanel`: ModalBottomSheet con breakdown completo
- Integración `shouldAnimate()` / `LocalAnimationsEnabled`

### Out of Scope
- Lottie / librerías externas, sonido, vibración
- Cambios en `SettlementEngine`
- Persistencia del recibo

## Capabilities

### New
- `money-explosion-animation`: Canvas particles con `Animatable` + `LaunchedEffect`
- `receipt-breakdown`: ModalBottomSheet con breakdown desde `CalculationSnapshot`

### Modified
- `calculation-result-display`: Animación 1.5s antecede a resultados en flujo exitoso
- `settlement-panel`: `IconButton` circular Receipt + callback `onShowReceipt`

## Approach

1. **MoneyExplosionAnimation.kt**: `Canvas` + `drawRoundRect`, `Animatable` pos/rot/alpha, `LaunchedEffect` stagger, respeta `LocalAnimationsEnabled`
2. **CalculatorSheet.kt**: Flag `showCelebration` en `runCalculation()` exitoso, Box overlay condicional, auto-dismiss 1.5s o tap
3. **ReceiptPanel.kt**: ModalBottomSheet con evento, total animado (`rememberAnimatedAmount`), transfers, balances, close
4. **SettlementPanel.kt**: `IconButton` CircleShape junto a "Calcular Totales", enabled con estado calculado + snapshot
5. **EventDetailScreen.kt**: Estado `showReceipt` + render condicional

## Affected Areas

| File | Change |
|------|--------|
| `ui/MoneyExplosionAnimation.kt` | New |
| `ui/ReceiptPanel.kt` | New |
| `ui/CalculatorSheet.kt` | Modified |
| `ui/SettlementPanel.kt` | Modified |
| `ui/EventDetailScreen.kt` | Modified |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Canvas perf 30+ particles gama baja | Medium | Max 25 bills, release on dismiss |
| 1.5s delay resultados | Low | Tap early-dismiss; datos ya en memoria |
| KMP compat | Low | Solo Compose primitives |
| `Receipt` icon inexistente | Medium | Fallback `Icons.Default.Receipt` |

## Rollback

`git revert` merge commit — solo UI, sin migraciones. Animaciones aditivas sin side effects.

## Dependencies

- Compose Multiplatform 1.6.11
- Material Icons Extended (`Receipt`)
- `CalculationSnapshot` y `NeoFintechAnimations` existentes

## Success Criteria

- [ ] Overlay billetes ~1.5s post-cálculo, dismissable por tap
- [ ] Con animaciones off, overlay no aparece
- [ ] Botón receipt circular solo enabled con estado CALCULATED + snapshot
- [ ] ReceiptPanel muestra breakdown completo
- [ ] `./gradlew compileDebugKotlin` sin errores
