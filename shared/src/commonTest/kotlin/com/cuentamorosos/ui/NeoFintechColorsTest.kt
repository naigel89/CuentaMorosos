package com.cuentamorosos.ui

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class NeoFintechColorsTest {

    @Test
    fun `light theme onPrimaryContainer is dark for legibility on neon green`() {
        val colors = NeoFintechColors.light()
        assertEquals(Color(0xFF191C1D), colors.onPrimaryContainer)
    }

    @Test
    fun `dark theme onPrimaryContainer is also dark for legibility on neon green`() {
        val colors = NeoFintechColors.dark()
        assertEquals(Color(0xFF191C1D), colors.onPrimaryContainer)
    }
}
