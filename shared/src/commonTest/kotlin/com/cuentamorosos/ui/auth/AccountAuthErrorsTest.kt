package com.cuentamorosos.ui.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AccountAuthErrorsTest {

    // ── validatePasswordChange ────────────────────────────────────────────────

    @Test
    fun `campos vacios rechazados`() {
        assertEquals(
            "Completa todos los campos para cambiar la contraseña.",
            AccountAuthErrors.validatePasswordChange("", "nueva123", "nueva123"),
        )
        assertEquals(
            "Completa todos los campos para cambiar la contraseña.",
            AccountAuthErrors.validatePasswordChange("actual", "", ""),
        )
        assertEquals(
            "Completa todos los campos para cambiar la contraseña.",
            AccountAuthErrors.validatePasswordChange("actual", "nueva123", ""),
        )
    }

    @Test
    fun `nueva contrasena demasiado corta`() {
        assertEquals(
            "La nueva contraseña necesita al menos 6 caracteres.",
            AccountAuthErrors.validatePasswordChange("actual", "abc12", "abc12"),
        )
    }

    @Test
    fun `confirmacion no coincide`() {
        assertEquals(
            "Las contraseñas nuevas no coinciden.",
            AccountAuthErrors.validatePasswordChange("actual", "nueva123", "nueva124"),
        )
    }

    @Test
    fun `nueva igual a la actual rechazada`() {
        assertEquals(
            "La nueva contraseña debe ser distinta de la actual.",
            AccountAuthErrors.validatePasswordChange("nueva123", "nueva123", "nueva123"),
        )
    }

    @Test
    fun `cambio valido pasa`() {
        assertNull(AccountAuthErrors.validatePasswordChange("actual1", "nueva123", "nueva123"))
    }

    // ── mapChangePasswordError ────────────────────────────────────────────────

    @Test
    fun `requiere login reciente`() {
        assertEquals(
            "Tu sesión expiró. Cerrá sesión y volvé a iniciarla para cambiar la contraseña.",
            AccountAuthErrors.mapChangePasswordError(
                Exception("This operation is sensitive and requires recent authentication. Log in again before retrying this request. [ CREDENTIAL_TOO_OLD_LOGIN_AGAIN ]")
            ),
        )
    }

    @Test
    fun `contrasena actual incorrecta`() {
        assertEquals(
            "La contraseña actual no es correcta.",
            AccountAuthErrors.mapChangePasswordError(
                Exception("The supplied auth credential is incorrect, malformed or has expired. [ INVALID_LOGIN_CREDENTIALS ]")
            ),
        )
        assertEquals(
            "La contraseña actual no es correcta.",
            AccountAuthErrors.mapChangePasswordError(
                Exception("The password is invalid or the user does not have a password. [ ERROR_WRONG_PASSWORD ]")
            ),
        )
        // Mensaje real observado en el emulador (sin código entre corchetes)
        assertEquals(
            "La contraseña actual no es correcta.",
            AccountAuthErrors.mapChangePasswordError(
                Exception("The supplied auth credential is incorrect, malformed or has expired.")
            ),
        )
        // Por nombre de clase del SDK (sin guiones bajos)
        class FirebaseAuthInvalidCredentialsException : Exception(null as String?)
        assertEquals(
            "La contraseña actual no es correcta.",
            AccountAuthErrors.mapChangePasswordError(FirebaseAuthInvalidCredentialsException()),
        )
    }

    @Test
    fun `contrasena debil`() {
        assertEquals(
            "La nueva contraseña es demasiado débil. Prueba con una más larga.",
            AccountAuthErrors.mapChangePasswordError(Exception("WEAK_PASSWORD: should be at least 6 characters")),
        )
    }

    @Test
    fun `error de red`() {
        assertEquals(
            "No hay conexión. Revisa tu red e inténtalo de nuevo.",
            AccountAuthErrors.mapChangePasswordError(
                Exception("A network error (such as timeout, interrupted connection or unreachable host) has occurred.")
            ),
        )
    }

    @Test
    fun `demasiados intentos`() {
        assertEquals(
            "Demasiados intentos. Espera unos minutos y vuelve a probarlo.",
            AccountAuthErrors.mapChangePasswordError(Exception("TOO_MANY_ATTEMPTS_TRY_LATER")),
        )
    }

    @Test
    fun `error en la causa anidada tambien se detecta`() {
        val wrapped = Exception("wrapper", Exception("requires recent authentication"))
        assertEquals(
            "Tu sesión expiró. Cerrá sesión y volvé a iniciarla para cambiar la contraseña.",
            AccountAuthErrors.mapChangePasswordError(wrapped),
        )
    }

    @Test
    fun `error desconocido usa mensaje generico amigable`() {
        assertEquals(
            "No se pudo cambiar la contraseña. Inténtalo de nuevo.",
            AccountAuthErrors.mapChangePasswordError(Exception("boom")),
        )
    }
}
