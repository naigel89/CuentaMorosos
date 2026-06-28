package com.cuentamorosos.auth

/**
 * Result of a sign-in attempt performed by [SignInWithRetry].
 *
 * `Success` indicates authentication completed without errors.
 * `Error` carries a [message] with a user-facing (Spanish) description
 * of what went wrong, already mapped through [AuthErrorMapper].
 *
 * @see SignInWithRetry
 * @see AuthErrorMapper
 */
sealed class SignInResult {
    data object Success : SignInResult()
    data class Error(val message: String) : SignInResult()
}
