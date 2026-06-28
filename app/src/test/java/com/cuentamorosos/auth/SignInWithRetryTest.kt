package com.cuentamorosos.auth

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TDD tests for [SignInWithRetry].
 *
 * Scenarios (login spec):
 *  - timeout after Firebase does not respond
 *  - retry succeeds on transient failure
 *  - retries exhaust and surface mapped error
 */
class SignInWithRetryTest {

    @Test
    fun `returns timeout error when sign in hangs`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val result = SignInWithRetry(
            email = "a@b.com",
            password = "password",
            dispatcher = dispatcher,
            timeoutMs = 100L,
            signIn = { _, _ ->
                kotlinx.coroutines.delay(Long.MAX_VALUE)
                Result.success(Unit)
            }
        )

        assertTrue("Expected Error result on timeout", result is SignInResult.Error)
        assertEquals(
            "El inicio de sesión está tardando demasiado. Intenta de nuevo.",
            (result as SignInResult.Error).message
        )
    }

    @Test
    fun `retries on transient failure and succeeds`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        var attempts = 0
        val result = SignInWithRetry(
            email = "a@b.com",
            password = "password",
            dispatcher = dispatcher,
            timeoutMs = 1000L,
            maxRetries = 2,
            signIn = { _, _ ->
                attempts++
                if (attempts == 1) Result.failure(NetworkExceptionStub())
                else Result.success(Unit)
            }
        )

        assertEquals(SignInResult.Success, result)
        assertEquals("Should retry once before succeeding", 2, attempts)
    }

    @Test
    fun `returns mapped network error when all retries exhaust`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        var attempts = 0
        val result = SignInWithRetry(
            email = "a@b.com",
            password = "password",
            dispatcher = dispatcher,
            timeoutMs = 1000L,
            maxRetries = 2,
            signIn = { _, _ ->
                attempts++
                Result.failure(NetworkExceptionStub())
            }
        )

        assertTrue("Expected Error result after retries exhaust", result is SignInResult.Error)
        assertEquals(
            "Error de red. Verifica tu conexión e intenta de nuevo.",
            (result as SignInResult.Error).message
        )
        assertEquals("Should attempt initial + 2 retries", 3, attempts)
    }

    @Test
    fun `returns mapped auth error when all retries exhaust`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        var attempts = 0
        val result = SignInWithRetry(
            email = "a@b.com",
            password = "password",
            dispatcher = dispatcher,
            timeoutMs = 1000L,
            maxRetries = 1,
            signIn = { _, _ ->
                attempts++
                Result.failure(WrongPasswordExceptionStub())
            }
        )

        assertTrue("Expected Error result after retries exhaust", result is SignInResult.Error)
        assertEquals(
            "Email o contraseña incorrectos",
            (result as SignInResult.Error).message
        )
        assertEquals("Should attempt initial + 1 retry", 2, attempts)
    }

    @Test
    fun `with zero retries attempts only once`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        var attempts = 0
        val result = SignInWithRetry(
            email = "a@b.com",
            password = "password",
            dispatcher = dispatcher,
            timeoutMs = 1000L,
            maxRetries = 0,
            signIn = { _, _ ->
                attempts++
                Result.failure(NetworkExceptionStub())
            }
        )

        assertTrue(result is SignInResult.Error)
        assertEquals(1, attempts)
    }
}

private class NetworkExceptionStub : Exception("network") {
    @Suppress("unused")
    fun getErrorCode(): String = "ERROR_NETWORK_REQUEST_FAILED"
}

private class WrongPasswordExceptionStub : Exception("wrong password") {
    @Suppress("unused")
    fun getErrorCode(): String = "ERROR_WRONG_PASSWORD"
}
