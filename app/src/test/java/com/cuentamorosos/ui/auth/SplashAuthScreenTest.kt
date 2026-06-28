@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package com.cuentamorosos.ui.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TDD tests for [SplashAuthScreen].
 *
 * Scenarios (login spec):
 *  - Loading indicator is visible when [isLoading] = true
 *  - Loading indicator is hidden when [isLoading] = false
 */
class SplashAuthScreenTest : BaseComposeTest() {

    @Test
    fun `shows CircularProgressIndicator when isLoading is true`() {
        composeTestRule.setContent {
            SplashAuthScreen(
                logo = { },
                isLoading = true,
                onLoginSuccess = { },
                onNavigateToRegister = { },
                onNavigateToForgotPassword = { },
                onLogin = { _, _, _ -> },
            )
        }

        // When loading, the button replaces "Iniciar sesión" text with a
        // CircularProgressIndicator, so the label text must NOT be present.
        assertTrue(
            "Expected no 'Iniciar sesión' text when isLoading=true",
            composeTestRule.onAllNodesWithText("Iniciar sesión").fetchSemanticsNodes().isEmpty()
        )
    }

    @Test
    fun `hides CircularProgressIndicator when isLoading is false`() {
        composeTestRule.setContent {
            SplashAuthScreen(
                logo = { },
                isLoading = false,
                onLoginSuccess = { },
                onNavigateToRegister = { },
                onNavigateToForgotPassword = { },
                onLogin = { _, _, _ -> },
            )
        }

        // When not loading, the button shows "Iniciar sesión" text
        composeTestRule.onNodeWithText("Iniciar sesión").assertIsDisplayed()
    }
}
