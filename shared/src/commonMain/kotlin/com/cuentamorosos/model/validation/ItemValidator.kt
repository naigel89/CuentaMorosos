package com.cuentamorosos.model.validation

import com.cuentamorosos.model.EventExpenseItem
import com.cuentamorosos.model.SUPPORTED_CURRENCY
import kotlin.math.abs

/** Validates [EventExpenseItem] instances against domain rules IV-01 through IV-09. */
object ItemValidator {

    /**
     * Validates an expense item, accumulating all errors and warnings.
     *
     * @param item the expense item to validate
     * @param eventMemberIds set of participant IDs in the parent event
     * @param profileNameResolver resolves a profile ID to a display name (defaults to returning the ID)
     * @param eventBaseCurrency the base currency of the parent event (for IV-09 cross-currency check)
     * @return [ValidationResult] with accumulated errors and warnings
     */
    fun validate(
        item: EventExpenseItem,
        eventMemberIds: Set<String>,
        profileNameResolver: (String) -> String = { it },
        eventBaseCurrency: String = "",
    ): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        val warnings = mutableListOf<ValidationError>()

        // IV-01: Amount must be positive
        if (item.amountEuros <= 0) {
            errors.add(
                ValidationError("El importe debe ser mayor a cero", "amount"),
            )
        }

        // IV-02: At least one payer
        if (item.payerContributions.isEmpty()) {
            errors.add(
                ValidationError("Debe haber al menos un pagador", "payers"),
            )
        }

        // IV-03: Payer contributions sum must match amount within tolerance
        if (item.payerContributions.isNotEmpty()) {
            val sum = item.payerContributions.values.sum()
            if (abs(sum - item.amountEuros) > CONTRIBUTION_TOLERANCE) {
                errors.add(
                    ValidationError(
                        "La suma de las contribuciones no coincide con el importe total",
                        "contributions",
                    ),
                )
            }
        }

        // IV-04: At least one debtor
        if (item.debtorIds.isEmpty()) {
            errors.add(
                ValidationError("Debe haber al menos un deudor", "debtors"),
            )
        }

        // IV-05: Split mode required
        if (item.splitMode.isBlank()) {
            errors.add(
                ValidationError("El modo de reparto es obligatorio", "splitMode"),
            )
        }

        // IV-06: Payers must be event participants
        for (payerId in item.payerContributions.keys) {
            if (payerId !in eventMemberIds) {
                val payerName = profileNameResolver(payerId)
                errors.add(
                    ValidationError(
                        "El pagador '$payerName' no es participante del evento",
                        "payers",
                    ),
                )
            }
        }

        // IV-07: Debtors must be event participants
        for (debtorId in item.debtorIds) {
            if (debtorId !in eventMemberIds) {
                val debtorName = profileNameResolver(debtorId)
                errors.add(
                    ValidationError(
                        "El deudor '$debtorName' no es participante del evento",
                        "debtors",
                    ),
                )
            }
        }

        // IV-08: Item name warning
        if (item.name.isBlank()) {
            warnings.add(
                ValidationError("El gasto no tiene nombre descriptivo", "name"),
            )
        }

        // IV-10: Only EUR supported for item currency (D6 currency standardization)
        if (
            item.itemCurrency != null &&
            item.itemCurrency.isNotBlank() &&
            item.itemCurrency != SUPPORTED_CURRENCY
        ) {
            errors.add(
                ValidationError("Solo se admite EUR como moneda por el momento", "itemCurrency"),
            )
        }

        // IV-09: Exchange rate required for cross-currency items
        if (
            item.itemCurrency != null &&
            item.itemCurrency.isNotBlank() &&
            item.itemCurrency != eventBaseCurrency &&
            (item.exchangeRate == null || item.exchangeRate <= 0)
        ) {
            errors.add(
                ValidationError("El tipo de cambio debe ser mayor a cero", "exchangeRate"),
            )
        }

        return buildResult(errors, warnings)
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
