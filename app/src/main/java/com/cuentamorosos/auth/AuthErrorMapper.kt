package com.cuentamorosos.auth

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
        throwable.errorCode() == ERROR_INVALID_EMAIL -> "Email inválido"
        throwable.errorCode() == ERROR_WRONG_PASSWORD -> "Email o contraseña incorrectos"
        throwable.errorCode() == ERROR_USER_NOT_FOUND -> "Email o contraseña incorrectos"
        throwable.errorCode() == ERROR_USER_DISABLED -> "Cuenta deshabilitada. Contacta a soporte."
        throwable.errorCode() == ERROR_EMAIL_ALREADY_IN_USE -> "Este email ya está registrado"
        throwable.errorCode() == ERROR_WEAK_PASSWORD -> "La contraseña debe tener al menos 6 caracteres"
        throwable.errorCode() == ERROR_OPERATION_NOT_ALLOWED -> "El registro no está habilitado. Contacta a soporte."
        throwable is FirebaseNetworkException -> "Error de red. Verifica tu conexión e intenta de nuevo."
        throwable.errorCode() == ERROR_NETWORK_REQUEST_FAILED -> "Error de red. Verifica tu conexión e intenta de nuevo."
        else -> "Error al iniciar sesión. Intenta de nuevo."
    }

    /**
     * Extracts the Firebase error code via reflection.
     * Works with both real [com.google.firebase.auth.FirebaseAuthException] and test stubs.
     */
    private fun Throwable.errorCode(): String? = runCatching {
        this::class.java.getMethod("getErrorCode").invoke(this) as? String
    }.getOrNull()
}
