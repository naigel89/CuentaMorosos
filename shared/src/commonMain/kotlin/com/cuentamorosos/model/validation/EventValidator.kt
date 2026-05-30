package com.cuentamorosos.model.validation

import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.SUPPORTED_CURRENCY

/** Validates [EventItem] instances against domain rules EV-01 through EV-07. */
object EventValidator {

    private const val MAX_NAME_LENGTH = 100
    private const val MIN_NAME_LENGTH = 2

    /**
     * Validates an event, accumulating all errors and warnings.
     *
     * @param event the event to validate
     * @param itemCount number of expenses currently registered (default 0)
     * @return [ValidationResult] with accumulated errors and warnings
     */
    fun validate(event: EventItem, itemCount: Int = 0): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        val warnings = mutableListOf<ValidationError>()
        val isDraft = event.state == com.cuentamorosos.model.EventState.DRAFT

        // EV-01: Name validation
        validateName(event.name, errors)

        // EV-02: Date order
        if (event.startDateMillis > event.endDateMillis) {
            errors.add(
                ValidationError(
                    "La fecha de inicio debe ser anterior o igual a la fecha de fin",
                    "dates",
                ),
            )
        }

        // EV-03: Duration limit
        val spanMillis = event.endDateMillis - event.startDateMillis
        val maxSpanMillis = MAX_EVENT_SPAN_DAYS * 24L * 60L * 60L * 1000L
        if (spanMillis > maxSpanMillis) {
            errors.add(
                ValidationError("El evento no puede durar más de 5 años", "dates"),
            )
        }

        // EV-04: Base currency must be EUR (SUPPORTED_CURRENCY)
        if (event.baseCurrency.isBlank()) {
            errors.add(
                ValidationError("La divisa base es obligatoria", "baseCurrency"),
            )
        } else if (event.baseCurrency != SUPPORTED_CURRENCY) {
            errors.add(
                ValidationError("La moneda del evento debe ser EUR", "baseCurrency"),
            )
        }

        // EV-05: Minimum participants (error for calculation, warning for draft)
        if (event.effectiveMemberIds.size < 2) {
            if (isDraft) {
                warnings.add(
                    ValidationError("Se necesitan al menos 2 participantes para calcular", "members"),
                )
            } else {
                errors.add(
                    ValidationError("Se necesitan al menos 2 participantes para calcular", "members"),
                )
            }
        }

        // EV-06: No items warning (only for non-draft events)
        if (!isDraft && itemCount == 0) {
            warnings.add(
                ValidationError("El evento no tiene gastos registrados"),
            )
        }

        // EV-07: Creator membership warning
        if (event.creatorId.isBlank()) {
            warnings.add(
                ValidationError("No se identificó el creador del evento"),
            )
        } else if (event.creatorId !in event.effectiveMemberIds) {
            warnings.add(
                ValidationError("El creador no figura como participante"),
            )
        }

        return buildResult(errors, warnings)
    }

    private fun validateName(name: String, errors: MutableList<ValidationError>) {
        if (name.isBlank()) {
            errors.add(ValidationError("El nombre es obligatorio", "name"))
        } else if (name.length < MIN_NAME_LENGTH) {
            errors.add(
                ValidationError("El nombre debe tener al menos 2 caracteres", "name"),
            )
        } else if (name.length > MAX_NAME_LENGTH) {
            errors.add(
                ValidationError("El nombre no puede tener más de 100 caracteres", "name"),
            )
        }
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
