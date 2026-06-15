package com.cuentamorosos.ui

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class NeoFintechColorsTest {

    // ── Phase 1: Updated color tokens (WCAG AA) ──────────────────────

    @Test
    fun `light theme primaryContainer is updated green for WCAG AA`() {
        val colors = NeoFintechColors.light()
        assertEquals(Color(0xFF00A651), colors.primaryContainer)
    }

    @Test
    fun `light theme primaryFixedDim is updated green for WCAG AA`() {
        val colors = NeoFintechColors.light()
        assertEquals(Color(0xFF008F47), colors.primaryFixedDim)
    }

    @Test
    fun `light theme secondary is updated teal`() {
        val colors = NeoFintechColors.light()
        assertEquals(Color(0xFF00897B), colors.secondary)
    }

    @Test
    fun `light theme onSecondary is white for teal contrast`() {
        val colors = NeoFintechColors.light()
        assertEquals(Color(0xFFFFFFFF), colors.onSecondary)
    }

    @Test
    fun `dark theme primaryContainer is updated green for WCAG AA`() {
        val colors = NeoFintechColors.dark()
        assertEquals(Color(0xFF00C853), colors.primaryContainer)
    }

    @Test
    fun `dark theme primaryFixedDim is updated green for WCAG AA`() {
        val colors = NeoFintechColors.dark()
        assertEquals(Color(0xFF00B84A), colors.primaryFixedDim)
    }

    @Test
    fun `dark theme secondary is updated teal`() {
        val colors = NeoFintechColors.dark()
        assertEquals(Color(0xFF26A69A), colors.secondary)
    }

    @Test
    fun `dark theme onSecondary is dark for teal contrast`() {
        val colors = NeoFintechColors.dark()
        assertEquals(Color(0xFF1A1A1A), colors.onSecondary)
    }

    // ── Preserved tokens (non-regression) ───────────────────────────

    @Test
    fun `light theme onPrimaryContainer is dark for legibility`() {
        val colors = NeoFintechColors.light()
        assertEquals(Color(0xFF191C1D), colors.onPrimaryContainer)
    }

    @Test
    fun `dark theme onPrimaryContainer is dark for legibility`() {
        val colors = NeoFintechColors.dark()
        assertEquals(Color(0xFF191C1D), colors.onPrimaryContainer)
    }

    @Test
    fun `light theme background is unchanged`() {
        val colors = NeoFintechColors.light()
        assertEquals(Color(0xFFF8F9FA), colors.background)
    }

    @Test
    fun `light theme error is unchanged`() {
        val colors = NeoFintechColors.light()
        assertEquals(Color(0xFFBA1A1A), colors.error)
    }

    @Test
    fun `dark theme onSurface is unchanged`() {
        val colors = NeoFintechColors.dark()
        assertEquals(Color(0xFFE5E2E1), colors.onSurface)
    }

    @Test
    fun `dark theme errorContainer is unchanged`() {
        val colors = NeoFintechColors.dark()
        assertEquals(Color(0xFF93000A), colors.errorContainer)
    }
}
