package com.cuentamorosos.model.validation

import com.cuentamorosos.model.ProfileItem

/** Validates [ProfileItem] instances against domain rules PV-01 through PV-04. */
object ProfileValidator {

    /**
     * Validates a profile, accumulating all errors.
     *
     * @param profile the profile to validate
     * @param existingProfiles list of all existing profiles (including self if editing)
     * @return [ValidationResult] with accumulated errors
     */
    fun validate(
        profile: ProfileItem,
        existingProfiles: List<ProfileItem>,
    ): ValidationResult {
        val errors = mutableListOf<ValidationError>()

        // Sanitize profile fields to strip Unicode control chars (data-leak R003)
        val sanitizedName = sanitize(profile.name)
        val sanitizedLinkedEmail = profile.linkedEmail?.let { sanitize(it) }

        // PV-01: Name required
        if (sanitizedName.isBlank()) {
            errors.add(
                ValidationError("El nombre del perfil es obligatorio", "name"),
            )
        }

        // PV-02: Duplicate name check (case-insensitive, same owner, excluding self)
        if (sanitizedName.isNotBlank()) {
            val duplicate = existingProfiles
                .filter { it.id != profile.id } // exclude self
                .filter { it.ownerId == profile.ownerId } // same owner
                .any { sanitize(it.name).equals(sanitizedName, ignoreCase = true) }

            if (duplicate) {
                errors.add(
                    ValidationError("Ya existe un perfil con ese nombre", "name"),
                )
            }
        }

        // PV-03 (ghost-sync): linkedEmail uniqueness (GPS-REQ-005)
        if (sanitizedLinkedEmail != null) {
            val duplicate = existingProfiles
                .filter { it.id != profile.id } // exclude self
                .any { it.linkedEmail?.let { sanitize(it) }.equals(sanitizedLinkedEmail, ignoreCase = true) }

            if (duplicate) {
                errors.add(
                    ValidationError("linkedEmail duplicado", "linkedEmail"),
                )
            }
        }

        return buildResult(errors, emptyList())
    }

    /**
     * Checks for warnings before deleting a profile.
     *
     * @param profile the profile being deleted
     * @param activeEventIds set of event IDs that are currently active
     * @return list of warning messages (empty if no warnings)
     */
    @Suppress("UNUSED_PARAMETER")
    fun checkDeleteWarning(
        _profile: ProfileItem,
        activeEventIds: Set<String>,
    ): List<ValidationError> {
        val warnings = mutableListOf<ValidationError>()

        // PV-03: Profile participates in active events
        for (eventId in activeEventIds) {
            // We check if the profile ID appears as a member in any active event.
            // The caller must pass event IDs where this profile is a member.
            warnings.add(
                ValidationError(
                    "Este perfil participa en eventos activos. Se mantendrá como '[perfil eliminado]'",
                    "delete",
                ),
            )
            break // one warning is enough
        }

        return warnings
    }

    private fun buildResult(
        errors: List<ValidationError>,
        warnings: List<ValidationError>,
    ): ValidationResult = when {
        errors.isEmpty() && warnings.isEmpty() -> ValidationResult.Success
        errors.isEmpty() -> ValidationResult.SuccessWithWarnings(warnings)
        else -> ValidationResult.Failure(errors, warnings)
    }
}
