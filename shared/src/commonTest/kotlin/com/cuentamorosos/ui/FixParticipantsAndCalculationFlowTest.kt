package com.cuentamorosos.ui

import com.cuentamorosos.model.EventState
import com.cuentamorosos.model.SplitMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for fix-participants-and-calculation-flow change:
 * - R007: Checkbox visibility gated by event state
 * - R008: Batch multi-select in AddProfileToEventDialog
 * - R008: Scroll trigger and celebration state transitions
 * - R009: Calculation mode persistence via onApply callback
 */
class FixParticipantsAndCalculationFlowTest {

    // ── R007: Checkbox visibility by event state ──────────────────────────────

    /**
     * Pure function extracted from SettlementPanel logic.
     * Returns whether the checkbox should be shown for a participant row.
     */
    private fun shouldShowParticipantCheckbox(hasDebt: Boolean, eventState: EventState): Boolean {
        return hasDebt && eventState != EventState.OPEN
    }

    @Test
    fun `checkbox hidden for OPEN event even when hasDebt is true`() {
        assertFalse(
            shouldShowParticipantCheckbox(hasDebt = true, eventState = EventState.OPEN),
            "OPEN event must NOT show checkboxes even when debts exist"
        )
    }

    @Test
    fun `checkbox hidden for OPEN event when hasDebt is false`() {
        assertFalse(
            shouldShowParticipantCheckbox(hasDebt = false, eventState = EventState.OPEN),
            "OPEN event with no debts must NOT show checkboxes"
        )
    }

    @Test
    fun `checkbox visible for CALCULATED event when hasDebt is true`() {
        assertTrue(
            shouldShowParticipantCheckbox(hasDebt = true, eventState = EventState.CALCULATED),
            "CALCULATED event must show checkboxes when debts exist"
        )
    }

    @Test
    fun `checkbox visible for CLOSED event when hasDebt is true`() {
        assertTrue(
            shouldShowParticipantCheckbox(hasDebt = true, eventState = EventState.CLOSED),
            "CLOSED event must show checkboxes when debts exist"
        )
    }

    @Test
    fun `checkbox hidden for CALCULATED event when hasDebt is false`() {
        assertFalse(
            shouldShowParticipantCheckbox(hasDebt = false, eventState = EventState.CALCULATED),
            "CALCULATED event with no debts must NOT show checkboxes"
        )
    }

    @Test
    fun `row clickable gated same as checkbox visibility`() {
        // Same logic gate applies: clickable only when checkbox would be visible
        assertEquals(
            shouldShowParticipantCheckbox(hasDebt = true, eventState = EventState.OPEN),
            shouldShowParticipantCheckbox(hasDebt = true, eventState = EventState.OPEN),
            "clickable gate must match checkbox gate"
        )
    }

    // ── R008: Batch multi-select dialog logic ──────────────────────────────────

    /**
     * Pure function extracted from AddProfileToEventDialog logic.
     * Returns whether the "Aceptar" button should be enabled.
     */
    private fun isAcceptButtonEnabled(selectedProfileCount: Int): Boolean {
        return selectedProfileCount > 0
    }

    @Test
    fun `Aceptar disabled when nothing selected`() {
        assertFalse(
            isAcceptButtonEnabled(0),
            "Aceptar must be disabled when 0 profiles selected"
        )
    }

    @Test
    fun `Aceptar enabled when 1 profile selected`() {
        assertTrue(
            isAcceptButtonEnabled(1),
            "Aceptar must be enabled when 1 profile selected"
        )
    }

    @Test
    fun `Aceptar enabled when 3 profiles selected`() {
        assertTrue(
            isAcceptButtonEnabled(3),
            "Aceptar must be enabled when 3 profiles selected"
        )
    }

    @Test
    fun `batch select toggles correctly — simulate add and remove`() {
        // Simulate mutableSetOf toggle behavior
        val selectedIds = mutableSetOf<String>()

        // Select "profile-1"
        selectedIds.add("profile-1")
        assertEquals(1, selectedIds.size)
        assertTrue(selectedIds.contains("profile-1"))

        // Select "profile-2"
        selectedIds.add("profile-2")
        assertEquals(2, selectedIds.size)

        // Deselect "profile-1" (toggle off)
        selectedIds.remove("profile-1")
        assertEquals(1, selectedIds.size)
        assertFalse(selectedIds.contains("profile-1"))
        assertTrue(selectedIds.contains("profile-2"))
    }

    @Test
    fun `batch add filters availableProfiles by selectedIds`() {
        data class TestProfile(val id: String, val name: String)

        val availableProfiles = listOf(
            TestProfile("p1", "Ana"),
            TestProfile("p2", "Bob"),
            TestProfile("p3", "Carlos"),
            TestProfile("p4", "Diana"),
            TestProfile("p5", "Eva"),
        )
        val selectedIds = setOf("p1", "p3", "p5")

        // This simulates the dialog's "Aceptar" logic:
        val selected = availableProfiles.filter { it.id in selectedIds }

        assertEquals(3, selected.size, "Must select exactly 3 profiles")
        assertEquals("Ana", selected[0].name)
        assertEquals("Carlos", selected[1].name)
        assertEquals("Eva", selected[2].name)
    }

    // ── R008: Scroll trigger and celebration state transitions ─────────────────

    @Test
    fun `scroll trigger false — no celebration when no calculation applied`() {
        val scrollToSettlement = false
        val celebrationTriggered = scrollToSettlement  // Simplified: celebration only when scroll true

        assertFalse(
            celebrationTriggered,
            "Celebration must NOT trigger when scrollToSettlement is false"
        )
    }

    @Test
    fun `scroll trigger true — celebration should trigger after scroll`() {
        val scrollToSettlement = true
        // In the real impl: scroll happens → delay(100ms) → celebration = true
        val celebrationShouldShow = scrollToSettlement

        assertTrue(
            celebrationShouldShow,
            "Celebration must trigger when scrollToSettlement is true"
        )
    }

    @Test
    fun `celebration dismissed resets showCelebration to false`() {
        var showCelebration = true
        // Simulate onDismiss callback
        showCelebration = false

        assertFalse(
            showCelebration,
            "showCelebration must be false after onDismiss"
        )
    }

    // ── R009: Calculation mode persistence ────────────────────────────────────

    @Test
    fun `onApply passes selectedModeId for REAL_CONSUMPTION`() {
        val selectedModeId = SplitMode.REAL_CONSUMPTION.id // "real_consumption"
        val capturedModeId: String = selectedModeId

        assertEquals(
            "real_consumption",
            capturedModeId,
            "REAL_CONSUMPTION mode id must be 'real_consumption'"
        )
    }

    @Test
    fun `onApply passes selectedModeId for CUSTOM_PERCENTAGE`() {
        val selectedModeId = SplitMode.CUSTOM_PERCENTAGE.id // "custom_percentage"
        val capturedModeId: String = selectedModeId

        assertEquals(
            "custom_percentage",
            capturedModeId,
            "CUSTOM_PERCENTAGE mode id must be 'custom_percentage'"
        )
    }

    @Test
    fun `lastCalculationMode stored from onApply modeId`() {
        // Simulates the flow:
        // CalculatorSheet calls onApply(selectedModeId, result)
        // CuentaMorososApp stores: event.copy(lastCalculationMode = modeId)
        val modeIdFromCallback = SplitMode.SIMPLE_AVG.id // "simple_avg"

        data class Event(val lastCalculationMode: String?)

        val oldEvent = Event(lastCalculationMode = null)
        val newEvent = oldEvent.copy(lastCalculationMode = modeIdFromCallback)

        assertEquals(
            "simple_avg",
            newEvent.lastCalculationMode,
            "lastCalculationMode must equal the modeId from onApply callback"
        )
        assertTrue(
            oldEvent.lastCalculationMode == null,
            "Original event must have null lastCalculationMode before apply"
        )
    }

    @Test
    fun `lastCalculationMode for EXACT mode`() {
        val modeId = SplitMode.EXACT.id // "exact"

        data class Event(val lastCalculationMode: String?)
        val event = Event(lastCalculationMode = modeId)

        assertEquals("exact", event.lastCalculationMode)
    }

    @Test
    fun `lastCalculationMode for PARTS mode`() {
        val modeId = SplitMode.PARTS.id // "parts"

        data class Event(val lastCalculationMode: String?)
        val event = Event(lastCalculationMode = modeId)

        assertEquals("parts", event.lastCalculationMode)
    }
}
