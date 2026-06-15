package com.cuentamorosos.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Escenario:
 *  - Evento con 3 participantes: Alice, Bob y Carol.
 *  - Gasto 1: "Cena" — 60 €, categoría SHARED  → se reparte a partes iguales entre los 3.
 *  - Gasto 2: "Taxi" — 30 €, categoría PERSONAL → lo paga solo Bob (primer perfil asignado).
 *
 * Resultado esperado con modo BY_CATEGORY:
 *   Alice = 60/3        =  20,00 €
 *   Bob   = 60/3 + 30   =  50,00 €
 *   Carol = 60/3        =  20,00 €
 *   Total calculado     =  90,00 €
 *
 * Liquidación mínima esperada:
 *   Alice → Bob: 20,00 €   (Alice debe su parte de la cena a Bob, que pagó todo)
 *   Carol → Bob: 20,00 €   (Carol también)
 */
class EventExpenseCalculationTest {

    // IDs fijos para que los tests sean deterministas
    private val aliceId = "profile-alice"
    private val bobId   = "profile-bob"
    private val carolId = "profile-carol"

    private val eventId = "event-test"

    private val participantIds = listOf(aliceId, bobId, carolId)

    private val sharedDinner = EventExpenseItem(
        id = "expense-dinner",
        eventId = eventId,
        name = "Cena",
        amountEuros = 60.0,
        category = ExpenseCategory.SHARED.id,
        assignedProfileIds = emptyList(), // SHARED ignora esta lista
    )

    private val personalTaxi = EventExpenseItem(
        id = "expense-taxi",
        eventId = eventId,
        name = "Taxi",
        amountEuros = 30.0,
        category = ExpenseCategory.OTHER.id,
        assignedProfileIds = listOf(bobId), // Solo Bob
    )

    private val expenses = listOf(sharedDinner, personalTaxi)

    // -------------------------------------------------------------------------
    // 1. Cálculo de importes con modo BY_CATEGORY
    // -------------------------------------------------------------------------

    @Test
    fun `modo BY_CATEGORY no produce mensaje de validacion`() {
        val preview = buildCalculationPreview(
            total = 90.0,
            mode = SplitMode.BY_CATEGORY,
            inputs = emptyList(),
            participantIds = participantIds,
            expenses = expenses,
        )
        assertNull("No debería haber error de validación", preview.validationMessage)
    }

    @Test
    fun `modo BY_CATEGORY asigna 20 euros a Alice`() {
        val preview = buildCalculationPreview(
            total = 90.0,
            mode = SplitMode.BY_CATEGORY,
            inputs = emptyList(),
            participantIds = participantIds,
            expenses = expenses,
        )
        val aliceIndex = participantIds.indexOf(aliceId)
        assertEquals("Alice debe 20,00 €", 20.0, preview.amounts[aliceIndex], 0.001)
    }

    @Test
    fun `modo BY_CATEGORY asigna 50 euros a Bob (cena + taxi personal)`() {
        val preview = buildCalculationPreview(
            total = 90.0,
            mode = SplitMode.BY_CATEGORY,
            inputs = emptyList(),
            participantIds = participantIds,
            expenses = expenses,
        )
        val bobIndex = participantIds.indexOf(bobId)
        assertEquals("Bob debe 50,00 € (20 cena + 30 taxi)", 50.0, preview.amounts[bobIndex], 0.001)
    }

    @Test
    fun `modo BY_CATEGORY asigna 20 euros a Carol`() {
        val preview = buildCalculationPreview(
            total = 90.0,
            mode = SplitMode.BY_CATEGORY,
            inputs = emptyList(),
            participantIds = participantIds,
            expenses = expenses,
        )
        val carolIndex = participantIds.indexOf(carolId)
        assertEquals("Carol debe 20,00 €", 20.0, preview.amounts[carolIndex], 0.001)
    }

    @Test
    fun `modo BY_CATEGORY calcula el total correcto de 90 euros`() {
        val preview = buildCalculationPreview(
            total = 90.0,
            mode = SplitMode.BY_CATEGORY,
            inputs = emptyList(),
            participantIds = participantIds,
            expenses = expenses,
        )
        assertEquals("Total calculado debe ser 90,00 €", 90.0, preview.calculatedTotal, 0.001)
    }

    @Test
    fun `la suma de todos los importes es igual al total`() {
        val preview = buildCalculationPreview(
            total = 90.0,
            mode = SplitMode.BY_CATEGORY,
            inputs = emptyList(),
            participantIds = participantIds,
            expenses = expenses,
        )
        val sumOfAmounts = preview.amounts.sum()
        assertEquals("La suma de importes debe ser igual al total", 90.0, sumOfAmounts, 0.001)
    }

    // -------------------------------------------------------------------------
    // 2. Deuda por participante (deuda neta = importe_asignado - parte_ya_pagada)
    //    Asumimos que Bob pagó los 90 € y quiere recuperar su dinero.
    //    Saldo neto:
    //      Alice = 0 pagado - 20 asignado = debe 20 a Bob
    //      Bob   = 90 pagado - 50 asignado = le deben 40
    //      Carol = 0 pagado - 20 asignado = debe 20 a Bob
    //
    //    Para buildSettlementTransfers: amounts = lo que cada uno DEBE pagar.
    //    Si Bob ya pagó todo, su "deuda" es negativa (es acreedor):
    //      amounts = [20.0, -40.0, 20.0]  (Alice, Bob, Carol)
    // -------------------------------------------------------------------------

    @Test
    fun `liquidacion minima genera exactamente dos transferencias`() {
        val profileNames = listOf("Alice", "Bob", "Carol")
        val debtAmounts = listOf(20.0, -40.0, 20.0)

        val transfers = buildSettlementTransfers(profileNames, debtAmounts)

        assertEquals("Deben generarse exactamente 2 transferencias", 2, transfers.size)
    }

    @Test
    fun `Alice debe pagar 20 euros a Bob`() {
        val profileNames = listOf("Alice", "Bob", "Carol")
        val debtAmounts = listOf(20.0, -40.0, 20.0)

        val transfers = buildSettlementTransfers(profileNames, debtAmounts)

        val aliceToBob = transfers.find { it.fromProfileId == "Alice" && it.toProfileId == "Bob" }
        assertNull("No debe haber null en la búsqueda de Alice→Bob", null) // placeholder
        assertEquals("Alice debe pagar 20,00 € a Bob", 20.0, aliceToBob!!.amount, 0.001)
    }

    @Test
    fun `Carol debe pagar 20 euros a Bob`() {
        val profileNames = listOf("Alice", "Bob", "Carol")
        val debtAmounts = listOf(20.0, -40.0, 20.0)

        val transfers = buildSettlementTransfers(profileNames, debtAmounts)

        val carolToBob = transfers.find { it.fromProfileId == "Carol" && it.toProfileId == "Bob" }
        assertEquals("Carol debe pagar 20,00 € a Bob", 20.0, carolToBob!!.amount, 0.001)
    }

    @Test
    fun `nadie paga a Alice ni a Carol`() {
        val profileNames = listOf("Alice", "Bob", "Carol")
        val debtAmounts = listOf(20.0, -40.0, 20.0)

        val transfers = buildSettlementTransfers(profileNames, debtAmounts)

        val invalidTransfer = transfers.find { it.toProfileId == "Alice" || it.toProfileId == "Carol" }
        assertEquals("Nadie debería pagar a Alice ni a Carol", null, invalidTransfer)
    }

    // -------------------------------------------------------------------------
    // 3. Caso extremo: gasto personal sin perfil asignado debe fallar con validación
    // -------------------------------------------------------------------------

    @Test
    fun `gasto PERSONAL sin perfil asignado genera error de validacion`() {
        val taxiSinAsignar = EventExpenseItem(
            id = "expense-taxi-empty",
            eventId = eventId,
            name = "Taxi sin asignar",
            amountEuros = 30.0,
            category = ExpenseCategory.OTHER.id,
            assignedProfileIds = emptyList(),
        )

        val preview = buildCalculationPreview(
            total = 90.0,
            mode = SplitMode.BY_CATEGORY,
            inputs = emptyList(),
            participantIds = participantIds,
            expenses = listOf(sharedDinner, taxiSinAsignar),
        )

        assertNull("Debería existir un mensaje de validación", preview.validationMessage?.let { null })
        // El validationMessage no debe ser null
        assertEquals(
            "Debe haber error de validación cuando PERSONAL no tiene perfil",
            true,
            preview.validationMessage != null
        )
    }
}
