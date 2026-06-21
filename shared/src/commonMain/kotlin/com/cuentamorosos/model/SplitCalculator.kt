package com.cuentamorosos.model

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Pure split calculation functions — all work in integer cents internally
 * to avoid floating-point drift.
 */
object SplitCalculator {

    /**
     * SM-01: IGUAL — Equal split among debtors.
     * Remainder cents are distributed via round-robin starting from
     * [seed] % debtorCount offset. seed=0 (default) starts at the
     * first debtor, preserving backward compatibility with tests.
     */
    fun calculateEqual(
        total: Double,
        debtorIds: List<String>,
        seed: Int = 0,
    ): Map<String, Double> {
        require(debtorIds.isNotEmpty()) { "Debe haber al menos un deudor para repartir" }
        val totalCents = (total * 100).roundToInt()
        val baseCents = totalCents / debtorIds.size
        val remainderCents = totalCents % debtorIds.size

        val startIndex = seed.mod(debtorIds.size)

        return debtorIds.mapIndexed { index, id ->
            val offset = (index - startIndex).mod(debtorIds.size)
            val getsExtra = offset < remainderCents
            val assignedCents = baseCents + if (getsExtra) 1 else 0
            id to assignedCents / 100.0
        }.toMap()
    }

    /**
     * SM-02: EXACTO — Each debtor has a specific amount.
     * Validates sum ≈ total (±0.01) and each amount >= 0.01.
     */
    fun calculateExact(
        total: Double,
        amounts: Map<String, Double>,
    ): Map<String, Double> {
        require(amounts.isNotEmpty()) { "Debe haber al menos un deudor para repartir" }

        val totalCents = (total * 100).roundToInt()
        val sumCents = amounts.values.sumOf { (it * 100).roundToInt() }

        require(abs(sumCents - totalCents) <= 1) {
            "La suma de los importes exactos no coincide con el total"
        }

        amounts.forEach { (_, amount) ->
            require(amount >= 0.01) { "Cada importe debe ser al menos 0.01" }
        }

        return amounts
    }

    /**
     * SM-03: PORCENTAJE — Each debtor has a percentage.
     * Validates sum = 100% (±0.01), each pct in 0-100.
     * Remainder cents go to debtor with highest percentage.
     */
    fun calculatePercentage(
        total: Double,
        percentages: Map<String, Double>,
    ): Map<String, Double> {
        require(percentages.isNotEmpty()) { "Debe haber al menos un deudor para repartir" }

        val sumPct = percentages.values.sum()
        require(abs(sumPct - 100.0) <= 0.01) {
            "Los porcentajes deben sumar 100%"
        }

        percentages.forEach { (_, pct) ->
            require(pct in 0.0..100.0) {
                "Cada porcentaje debe estar entre 0 y 100"
            }
        }

        val totalCents = (total * 100).roundToInt()
        val amountsCents = percentages.mapValues { (_, pct) ->
            ((total * pct / 100.0) * 100).roundToInt()
        }

        val sumCents = amountsCents.values.sum()
        val remainderCents = totalCents - sumCents

        // Distribute remainder to debtor with highest percentage
        val highestPctId = percentages.maxByOrNull { it.value }?.key
        val adjusted = amountsCents.mapValues { (id, cents) ->
            val adjustedCents = if (id == highestPctId) cents + remainderCents else cents
            adjustedCents / 100.0
        }

        return adjusted
    }

    /**
     * SM-04: PARTES — Each debtor has integer parts (1-100).
     * Part value = total_cents / sum_of_parts. Remainder to most parts.
     */
    fun calculateParts(
        total: Double,
        parts: Map<String, Int>,
    ): Map<String, Double> {
        require(parts.isNotEmpty()) { "Debe haber al menos un deudor para repartir" }

        val totalParts = parts.values.sum()
        require(totalParts > 0) { "Debe haber al menos una parte asignada" }

        parts.forEach { (_, p) ->
            require(p in 1..100) {
                "Cada parte debe ser un número entre 1 y 100"
            }
        }

        val totalCents = (total * 100).roundToInt()
        val partValueCents = totalCents / totalParts
        val remainderCents = totalCents % totalParts

        // Distribute remainder to debtor with most parts
        val mostPartsId = parts.maxByOrNull { it.value }?.key

        val amountsCents = parts.mapValues { (id, p) ->
            val cents = p * partValueCents
            val adjusted = if (id == mostPartsId) cents + remainderCents else cents
            adjusted / 100.0
        }

        return amountsCents
    }

    /**
     * SM-07: Verify that amounts sum to total within ±0.01 tolerance.
     */
    fun verifySum(amounts: Map<String, Double>, total: Double): Boolean {
        val sumCents = amounts.values.sumOf { (it * 100).roundToInt() }
        val totalCents = (total * 100).roundToInt()
        return abs(sumCents - totalCents) <= 1
    }
}
