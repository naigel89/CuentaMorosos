package com.cuentamorosos.ui.auth

import kotlin.test.Test
import kotlin.test.assertEquals

class PasswordStrengthTest {

    @Test
    fun emptyPasswordScoresZero() {
        assertEquals(0, passwordStrength(""))
        assertEquals("", passwordStrengthLabel(0))
    }

    @Test
    fun shortPasswordIsAlwaysWeakRegardlessOfMix() {
        assertEquals(1, passwordStrength("aB3!"))
        assertEquals("Débil", passwordStrengthLabel(passwordStrength("aB3!")))
    }

    @Test
    fun eightPlainLettersAreWeak() {
        assertEquals(1, passwordStrength("aaaaaaaa"))
    }

    @Test
    fun lettersAndDigitsAreAcceptable() {
        assertEquals(2, passwordStrength("clave123"))
        assertEquals("Aceptable", passwordStrengthLabel(2))
    }

    @Test
    fun caseMixOrSymbolRaisesToGood() {
        assertEquals(3, passwordStrength("Clave123"))
        assertEquals(3, passwordStrength("clave12!"))
        assertEquals("Buena", passwordStrengthLabel(3))
    }

    @Test
    fun longMixedPasswordIsStrong() {
        assertEquals(4, passwordStrength("Clave123!extra"))
        assertEquals("Fuerte", passwordStrengthLabel(4))
    }
}
