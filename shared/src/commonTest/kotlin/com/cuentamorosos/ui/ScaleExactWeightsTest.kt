package com.cuentamorosos.ui

import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regresión del crash de la calculadora en modo EXACTO: el motor valida los
 * pesos GASTO a GASTO, así que los importes exactos del evento deben escalarse
 * a cada gasto sumando exactamente su importe.
 */
class ScaleExactWeightsTest {

    private fun sumCents(weights: Map<String, Double>): Int =
        weights.values.sumOf { (it * 100).roundToInt() }

    @Test
    fun scenarioThatCrashedTheApp() {
        // 4 personas a 20 € en un evento de 80 € con gastos de 60 € y 20 €.
        val exact = mapOf("a" to 20.0, "b" to 20.0, "c" to 20.0, "d" to 20.0)

        val cena = scaleExactWeightsForExpense(exact, 60.0, 80.0)
        val taxi = scaleExactWeightsForExpense(exact, 20.0, 80.0)

        assertEquals(6000, sumCents(cena), "La cena debe sumar exactamente 60,00 €")
        assertEquals(2000, sumCents(taxi), "El taxi debe sumar exactamente 20,00 €")
        assertEquals(15.0, cena.getValue("a"))
        assertEquals(5.0, taxi.getValue("a"))
    }

    @Test
    fun roundingRemainderStaysWithinTheExpense() {
        // 33,33 / 33,33 / 33,34 sobre un gasto de 10 € en un total de 100 €.
        val exact = mapOf("a" to 33.33, "b" to 33.33, "c" to 33.34)

        val weights = scaleExactWeightsForExpense(exact, 10.0, 100.0)

        assertEquals(1000, sumCents(weights), "Los céntimos de resto no pueden desviar la suma")
    }

    @Test
    fun overshootByOneCentIsAbsorbed() {
        // El motor tolera ±1 céntimo en la suma tecleada; el escalado no debe
        // dejar que ese céntimo desvíe la suma por gasto.
        val exact = mapOf("a" to 40.01, "b" to 40.0)

        val weights = scaleExactWeightsForExpense(exact, 30.0, 80.0)

        assertEquals(3000, sumCents(weights))
    }

    @Test
    fun zeroAllocationsAreExcluded() {
        // El motor exige >= 0,01 por deudor listado: quien queda a 0 sale del mapa.
        val exact = mapOf("a" to 80.0, "b" to 0.0)

        val weights = scaleExactWeightsForExpense(exact, 20.0, 80.0)

        assertTrue("b" !in weights, "Un perfil con 0 asignado no debe aparecer")
        assertEquals(2000, sumCents(weights))
    }

    @Test
    fun singleExpenseKeepsTypedAmounts() {
        val exact = mapOf("a" to 25.5, "b" to 54.5)

        val weights = scaleExactWeightsForExpense(exact, 80.0, 80.0)

        assertEquals(25.5, weights.getValue("a"))
        assertEquals(54.5, weights.getValue("b"))
    }
}
