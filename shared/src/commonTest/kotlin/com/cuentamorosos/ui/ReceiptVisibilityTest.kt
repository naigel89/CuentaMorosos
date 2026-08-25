package com.cuentamorosos.ui

import com.cuentamorosos.model.EventState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * El botón del recibo y el diálogo que abre tienen que decidir con la misma
 * regla. Cuando estaban duplicadas divergieron: el botón se habilitaba en
 * eventos cerrados y el diálogo no aparecía.
 */
class ReceiptVisibilityTest {

    @Test
    fun `un evento cerrado con cálculo muestra el recibo`() {
        // La regresión concreta: aquí el botón se encendía y no abría nada.
        assertTrue(canShowReceipt(EventState.CLOSED, hasSnapshot = true))
    }

    @Test
    fun `un evento calculado muestra el recibo`() {
        assertTrue(canShowReceipt(EventState.CALCULATED, hasSnapshot = true))
    }

    @Test
    fun `un evento abierto no tiene recibo que mostrar`() {
        assertFalse(canShowReceipt(EventState.OPEN, hasSnapshot = true))
    }

    @Test
    fun `sin cálculo guardado no hay recibo en ningún estado`() {
        EventState.entries.forEach { state ->
            assertFalse(
                canShowReceipt(state, hasSnapshot = false),
                "$state no debería ofrecer recibo sin snapshot",
            )
        }
    }
}
