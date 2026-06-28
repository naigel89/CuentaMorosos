package com.cuentamorosos.auth

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * TDD tests for [AuthErrorMapper].
 *
 * Scenarios (login spec):
 *  - invalid email format  → "Email inválido"
 *  - wrong password        → "Email o contraseña incorrectos"
 *  - disabled account      → "Cuenta deshabilitada. Contacta a soporte."
 *  - unknown exception     → generic login error
 */
class AuthErrorMapperTest {

    @Test
    fun `maps invalid email exception to user message`() {
        val result = AuthErrorMapper.map(authException("ERROR_INVALID_EMAIL"))

        assertEquals("Email inválido", result)
    }

    @Test
    fun `maps wrong password exception to user message`() {
        val result = AuthErrorMapper.map(authException("ERROR_WRONG_PASSWORD"))

        assertEquals("Email o contraseña incorrectos", result)
    }

    @Test
    fun `maps disabled user exception to user message`() {
        val result = AuthErrorMapper.map(authException("ERROR_USER_DISABLED"))

        assertEquals("Cuenta deshabilitada. Contacta a soporte.", result)
    }

    @Test
    fun `maps unknown exception to generic login error`() {
        val result = AuthErrorMapper.map(Exception("unknown"))

        assertEquals("Error al iniciar sesión. Intenta de nuevo.", result)
    }

    @Test
    fun `maps user not found to same message as wrong password`() {
        val result = AuthErrorMapper.map(authException("ERROR_USER_NOT_FOUND"))

        assertEquals("Email o contraseña incorrectos", result)
    }

    @Test
    fun `maps network error code to network error message`() {
        val result = AuthErrorMapper.map(authException("ERROR_NETWORK_REQUEST_FAILED"))

        assertEquals("Error de red. Verifica tu conexión e intenta de nuevo.", result)
    }
}

/**
 * Builds a fake auth exception carrying the given Firebase-style error code.
 * Production code reads the code reflectively via [Throwable.getErrorCode],
 * so a plain JVM exception with that method is enough for unit tests.
 */
private fun authException(errorCode: String): Exception =
    AuthExceptionStub(errorCode)

private class AuthExceptionStub(private val errorCode: String) : Exception("stub") {
    @Suppress("unused")
    fun getErrorCode(): String = errorCode
}
