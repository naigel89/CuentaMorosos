package com.cuentamorosos.auth

import com.cuentamorosos.data.LogSanitizer
import com.google.firebase.FirebaseNetworkException

/**
 * Maps Firebase Auth (and network) exceptions to concise, user-facing Spanish messages.
 *
 * Uses reflection to extract the Firebase error code from [Throwable] instances that
 * carry a `getErrorCode()` method (FirebaseAuthException and stubs in tests).
 * Network errors from [FirebaseNetworkException] are matched by type rather than code.
 *
 * @see SignInWithRetry
 * @see SignInResult
 */
object AuthErrorMapper {

    private const val ERROR_INVALID_EMAIL = "ERROR_INVALID_EMAIL"
    private const val ERROR_WRONG_PASSWORD = "ERROR_WRONG_PASSWORD"
    private const val ERROR_USER_NOT_FOUND = "ERROR_USER_NOT_FOUND"
    private const val ERROR_USER_DISABLED = "ERROR_USER_DISABLED"
    private const val ERROR_NETWORK_REQUEST_FAILED = "ERROR_NETWORK_REQUEST_FAILED"
    private const val ERROR_EMAIL_ALREADY_IN_USE = "ERROR_EMAIL_ALREADY_IN_USE"
    private const val ERROR_WEAK_PASSWORD = "ERROR_WEAK_PASSWORD"
    private const val ERROR_OPERATION_NOT_ALLOWED = "ERROR_OPERATION_NOT_ALLOWED"
    private const val ERROR_TOO_MANY_REQUESTS = "ERROR_TOO_MANY_REQUESTS"
    private const val ERROR_INTERNAL_ERROR = "ERROR_INTERNAL_ERROR"
    private const val ERROR_APP_NOT_AUTHORIZED = "ERROR_APP_NOT_AUTHORIZED"

    /**
     * Maps a [throwable] to a user-facing Spanish error message.
     *
     * Covers both login and registration Firebase Auth exceptions:
     * - Login: invalid email, wrong password, user not found, disabled
     * - Register: email already in use, weak password, operation not allowed
     *
     * @param throwable the exception to map — typically a FirebaseAuthException,
     *   FirebaseNetworkException, or a stub with `getErrorCode()`.
     * @return a Spanish description suitable for showing directly to the user.
     */
    fun map(throwable: Throwable): String = when {
        throwable.findErrorCode() == ERROR_INVALID_EMAIL -> "Email inválido"
        throwable.findErrorCode() == ERROR_WRONG_PASSWORD -> "Email o contraseña incorrectos"
        throwable.findErrorCode() == ERROR_USER_NOT_FOUND -> "Email o contraseña incorrectos"
        throwable.findErrorCode() == ERROR_USER_DISABLED -> "Cuenta deshabilitada. Contacta a soporte."
        throwable.findErrorCode() == ERROR_EMAIL_ALREADY_IN_USE -> "Este email ya está registrado"
        throwable.findErrorCode() == ERROR_WEAK_PASSWORD -> "La contraseña debe tener al menos 6 caracteres"
        throwable.findErrorCode() == ERROR_OPERATION_NOT_ALLOWED -> "El registro no está habilitado. Contacta a soporte."
        throwable.findErrorCode() == ERROR_TOO_MANY_REQUESTS -> "Demasiados intentos. Espera unos minutos antes de volver a intentar."
        throwable.findErrorCode() == ERROR_INTERNAL_ERROR -> "Error interno del servidor. Intenta de nuevo más tarde."
        throwable.findErrorCode() == ERROR_APP_NOT_AUTHORIZED -> "App no autorizada. Verificá la configuración en Firebase Console."
        throwable.hasCauseOf<FirebaseNetworkException>() -> "Error de red. Verifica tu conexión e intenta de nuevo."
        throwable.findErrorCode() == ERROR_NETWORK_REQUEST_FAILED -> "Error de red. Verifica tu conexión e intenta de nuevo."
        throwable.isCertOrTlsError() -> "Error de conexión segura. Reinstalá la app para actualizar los certificados."
        else -> {
            LogSanitizer.log("AuthErrorMapper", "Unmapped: ${throwable.javaClass.name} — ${throwable.message}")
            "Error al iniciar sesión. Intenta de nuevo."
        }
    }

    /**
     * Detects SSL/TLS certificate errors (including pinning failures) regardless of
     * how they are wrapped by Firebase / OkHttp.
     */
    private fun Throwable.isCertOrTlsError(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            val name = current::class.java.name
            if (name.contains("SSL", ignoreCase = true) ||
                name.contains("CertPath", ignoreCase = true) ||
                name.contains("Certificate", ignoreCase = true) ||
                name.contains("CertPin", ignoreCase = true) ||
                current.message?.contains("certificate", ignoreCase = true) == true ||
                current.message?.contains("Certificate pin", ignoreCase = true) == true
            ) return true
            current = current.cause
        }
        return false
    }

    /**
     * Finds a Firebase error code by walking this [Throwable] and its cause chain,
     * using reflection to call `getErrorCode()`. This handles exceptions wrapped
     * by Play Services [com.google.android.gms.tasks.RuntimeExecutionException].
     * Works with both real [com.google.firebase.auth.FirebaseAuthException] and test stubs.
     */
    private fun Throwable.findErrorCode(): String? {
        var current: Throwable? = this
        while (current != null) {
            val t = current
            val code = runCatching {
                t::class.java.getMethod("getErrorCode").invoke(t) as? String
            }.getOrNull()
            if (code != null) return code
            current = current.cause
        }
        return null
    }

    /**
     * Checks whether any exception in the cause chain is an instance of [T].
     */
    private inline fun <reified T : Throwable> Throwable.hasCauseOf(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (current is T) return true
            current = current.cause
        }
        return false
    }
}
