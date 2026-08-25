package com.cuentamorosos.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * Tokens de movimiento del sistema Neo-Fintech.
 *
 * Regla de reparto:
 *
 *  - **Muelles** ([snappy], [smooth], [gentle], [bouncy]) para todo lo que responde
 *    al dedo o reacomoda el layout: pulsaciones, reordenaciones de lista, expansiones,
 *    indicadores que siguen un gesto. Un muelle conserva la velocidad de entrada, así
 *    que interrumpirlo a mitad no produce el salto que sí produce un `tween`.
 *  - **Duraciones + easing** ([SHORT_MS], [MEDIUM_MS], [LONG_MS]) para entradas y
 *    salidas, donde el elemento no tiene un estado previo con el que empalmar.
 *
 * Las constantes de duración y el resto de tokens de animación de contenido siguen en
 * [NeoFintechAnimations]; este objeto cubre solo las *curvas*.
 */
object NeoFintechMotion {

    // ── Duraciones (solo entradas / salidas) ──────────────────────────────────

    /** Micro-feedback: cambios de tinte, alternar iconos. */
    const val QUICK_MS = 120

    /** Entrada de elementos pequeños: chips, badges, filas de lista. */
    const val SHORT_MS = 180

    /** Transición estándar entre estados de una misma superficie. */
    const val MEDIUM_MS = 260

    /** Entrada de superficies grandes: pantallas, hojas, paneles. */
    const val LONG_MS = 400

    // ── Easings ───────────────────────────────────────────────────────────────

    /** Curva estándar: acelera y frena. Para cambios de estado. */
    val standard: Easing = FastOutSlowInEasing

    /**
     * Curva enfática: arranca rápido y frena largo. Para entradas que deben
     * "aterrizar" en lugar de simplemente aparecer.
     */
    val emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Curva de solo frenada: para elementos que entran desde fuera de pantalla. */
    val decelerate: Easing = LinearOutSlowInEasing

    // ── Muelles ───────────────────────────────────────────────────────────────

    /**
     * Respuesta inmediata y sin rebote. Para feedback de pulsación, donde
     * cualquier oscilación se lee como imprecisión.
     */
    fun <T> snappy(): SpringSpec<T> = spring(dampingRatio = 1f, stiffness = 1400f)

    /**
     * El muelle por defecto. Para reacomodos de layout y transiciones de estado
     * que el usuario mira pero no dirige.
     */
    fun <T> smooth(): SpringSpec<T> = spring(dampingRatio = 1f, stiffness = 400f)

    /** Muelle lento, para superficies grandes que se expanden o se recolocan. */
    fun <T> gentle(): SpringSpec<T> = spring(dampingRatio = 1f, stiffness = 180f)

    /**
     * Muelle con rebote leve. Reservado para confirmaciones positivas
     * (un importe que se salda, una invitación aceptada). Usar con cuentagotas:
     * el rebote llama la atención y pierde efecto si está en todas partes.
     */
    fun <T> bouncy(): SpringSpec<T> = spring(dampingRatio = 0.62f, stiffness = 520f)

    // ── Muelles con umbral de visibilidad tipado ──────────────────────────────
    //
    // Un muelle se detiene cuando queda por debajo de su "visibility threshold".
    // Para Float el valor por defecto (0.01) es correcto, pero para tipos con
    // unidades (posición, tamaño, color) hay que dar el umbral explícito o el
    // muelle sigue animando cambios ya imperceptibles.

    /** Reordenación de items en listas lazy ([androidx.compose.foundation.lazy.LazyItemScope.animateItemPlacement]). */
    val placement: FiniteAnimationSpec<IntOffset> = spring(
        dampingRatio = 1f,
        stiffness = 400f,
        visibilityThreshold = IntOffset.VisibilityThreshold,
    )

    /** Cambios de tamaño animados (`animateContentSize`). */
    val resize: FiniteAnimationSpec<IntSize> = spring(
        dampingRatio = 1f,
        stiffness = 400f,
        visibilityThreshold = IntSize.VisibilityThreshold,
    )

    /** Desplazamientos expresados en Dp (indicadores, offsets de layout). */
    val offsetDp: FiniteAnimationSpec<Dp> = spring(
        dampingRatio = 1f,
        stiffness = 700f,
        visibilityThreshold = Dp.VisibilityThreshold,
    )

    /**
     * Interpolación de color. Un muelle no aporta nada aquí (el color no tiene
     * inercia perceptible), así que curva corta y predecible.
     */
    val color: FiniteAnimationSpec<Color> = tween(
        durationMillis = MEDIUM_MS,
        easing = FastOutSlowInEasing,
    )
}
