package com.cuentamorosos.model.validation

/** Tolerance for contribution sum comparison (IV-03). */
const val CONTRIBUTION_TOLERANCE = 0.01

/** Maximum event duration in days (EV-03). */
const val MAX_EVENT_SPAN_DAYS = 5 * 365

/** A single validation issue with an optional field identifier. */
data class ValidationError(val message: String, val field: String? = null)

/**
 * Result of a validation run.
 * - [Success]: all checks passed, no warnings.
 * - [SuccessWithWarnings]: all error checks passed but non-blocking warnings exist.
 * - [Failure]: one or more blocking errors exist (warnings may also be present).
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class SuccessWithWarnings(val warnings: List<ValidationError>) : ValidationResult()
    data class Failure(
        val errors: List<ValidationError>,
        val warnings: List<ValidationError> = emptyList(),
    ) : ValidationResult()
}

/** True when there are no blocking errors. */
fun ValidationResult.isValid(): Boolean = this is ValidationResult.Success || this is ValidationResult.SuccessWithWarnings

/** True when at least one blocking error exists. */
fun ValidationResult.hasErrors(): Boolean = this is ValidationResult.Failure

/** True when at least one warning exists (on warnings-only or failure-with-warnings). */
fun ValidationResult.hasWarnings(): Boolean =
    this is ValidationResult.SuccessWithWarnings ||
        (this is ValidationResult.Failure && warnings.isNotEmpty())

/** Collects all warning messages from any result type. */
fun ValidationResult.allWarnings(): List<ValidationError> = when (this) {
    is ValidationResult.SuccessWithWarnings -> warnings
    is ValidationResult.Failure -> warnings
    else -> emptyList()
}

/** Collects all error messages from any result type. */
fun ValidationResult.allErrors(): List<ValidationError> = when (this) {
    is ValidationResult.Failure -> errors
    else -> emptyList()
}
