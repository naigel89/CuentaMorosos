package com.cuentamorosos.ui.auth

/**
 * Pure function gate for email verification state.
 *
 * Extracted from the UI layer to be independently testable without
 * Compose UI or Firebase dependencies.
 */
object VerificationGate {

    /**
     * Returns true if the user must verify their email before accessing the app.
     * Defaults to requiring verification when status is unknown (null) for safety.
     */
    fun requiresVerification(isEmailVerified: Boolean?): Boolean =
        isEmailVerified != true

    /**
     * Resolves the verification state enum from the Firebase boolean flag.
     * Null (unknown) is treated as unverified — defence in depth.
     */
    fun resolveVerificationState(isEmailVerified: Boolean?): VerificationState =
        when (isEmailVerified) {
            true -> VerificationState.Verified
            else -> VerificationState.NeedsVerification
        }
}

/**
 * Describes the email verification state for the auth flow.
 */
enum class VerificationState {
    /** User has verified their email — proceed to main app. */
    Verified,
    /** User must verify their email before proceeding. */
    NeedsVerification,
}
