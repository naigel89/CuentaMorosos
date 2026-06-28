package com.cuentamorosos.ui.auth

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Base class for Compose UI tests running with Robolectric.
 * Provides the shared [ComposeTestRule] and [createComposeRule] setup
 * so subclasses only need to write the behavior-specific tests.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "normal-port")
abstract class BaseComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()
}
