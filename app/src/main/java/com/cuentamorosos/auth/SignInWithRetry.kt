package com.cuentamorosos.auth

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.pow

/**
 * Signs in with timeout and exponential-backoff retry.
 *
 * Attempts authentication up to (maxRetries + 1) times. Each attempt is bounded by
 * [timeoutMs]; if Firebase does not respond within that window, the attempt is treated
 * as a timeout. Between retries the delay grows exponentially: 1s, 2s, 4s, …
 *
 * All errors are passed through [AuthErrorMapper] so the caller receives
 * a user-facing Spanish message rather than a raw Firebase exception.
 *
 * @param email the email address to authenticate with
 * @param password the plain-text password
 * @param dispatcher the dispatcher for the retry loop; defaults to [Dispatchers.IO]
 * @param timeoutMs maximum wall-clock time per single sign-in attempt (default 10s)
 * @param maxRetries number of retries after the initial failure (default 2)
 * @param signIn suspending operation that attempts authentication; returns [Result.success] on success
 * @return [SignInResult.Success] on successful authentication, or [SignInResult.Error]
 *   with a user-facing message on failure (timeout, network error, or auth error).
 * @see AuthErrorMapper
 * @see SignInResult
 */
object SignInWithRetry {

    suspend operator fun invoke(
        email: String,
        password: String,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        timeoutMs: Long = 10_000,
        maxRetries: Int = 2,
        signIn: suspend (String, String) -> Result<Unit>,
    ): SignInResult = withContext(dispatcher) {
        val attempts = maxRetries + 1
        repeat(attempts) { attempt ->
            val outcome = withTimeoutOrNull(timeoutMs) {
                runCatching { signIn(email, password) }.fold(
                    onSuccess = { it },
                    onFailure = { Result.failure(it) },
                )
            }

            when {
                outcome == null -> return@withContext SignInResult.Error(TIMEOUT_MESSAGE)
                outcome.isSuccess -> return@withContext SignInResult.Success
                attempt == attempts - 1 -> return@withContext SignInResult.Error(AuthErrorMapper.map(outcome.exceptionOrNull()!!))
                else -> delay(backoffDelay(attempt))
            }
        }

        SignInResult.Error(GENERIC_ERROR_MESSAGE)
    }

    private fun backoffDelay(attempt: Int): Long = (BASE_BACKOFF_MS * 2.0.pow(attempt)).toLong()

    private const val BASE_BACKOFF_MS = 1_000L
    private const val TIMEOUT_MESSAGE = "El inicio de sesión está tardando demasiado. Intenta de nuevo."
    private const val GENERIC_ERROR_MESSAGE = "Error al iniciar sesión. Intenta de nuevo."
}
