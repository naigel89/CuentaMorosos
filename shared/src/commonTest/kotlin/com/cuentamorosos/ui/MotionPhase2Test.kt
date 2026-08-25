package com.cuentamorosos.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.EventState
import com.cuentamorosos.model.SettlementTransfer
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Lógica pura de la fase 2 de movimiento. Nada de esto necesita Compose: son las
 * decisiones que toma cada animación antes de dibujar nada.
 */
class MotionPhase2Test {

    private fun day(epochDay: Int): Long = epochDay * DAY_MS

    private fun event(
        id: String,
        startDay: Int,
        endDay: Int = startDay,
        state: EventState = EventState.OPEN,
    ) = EventItem(
        id = id,
        name = id,
        dateMillis = day(startDay),
        ownerId = "owner",
        startDateMillis = day(startDay),
        endDateMillis = day(endDay),
        state = state,
    )

    // ── 01 · Dígitos que ruedan ───────────────────────────────────────

    @Test
    fun `una columna que no cambia no rueda`() {
        val delays = rollDelays("1.284,50€", "1.310,00€", staggerMs = 55)
        val from = "1.284,50€"
        val to = "1.310,00€"
        for (i in from.indices) {
            if (from[i] == to[i]) {
                assertEquals(NO_ROLL, delays[i], "la columna $i ('${from[i]}') no debería moverse")
            }
        }
    }

    @Test
    fun `las columnas que cambian se escalonan de izquierda a derecha`() {
        val delays = rollDelays("999", "111", staggerMs = 55)
        assertEquals(intArrayOf(0, 55, 110).toList(), delays.toList())
    }

    @Test
    fun `importes de distinta longitud se alinean por la derecha`() {
        // "31,80" contra "121,80": las unidades tienen que seguir siendo las
        // mismas columnas, o el número entero parecería otro.
        val delays = rollDelays("31,80", "121,80", staggerMs = 10)
        assertEquals(6, delays.size)
        // Los cuatro últimos caracteres coinciden ("1,80") y no se mueven.
        assertEquals(NO_ROLL, delays[2])
        assertEquals(NO_ROLL, delays[3])
        assertEquals(NO_ROLL, delays[4])
        assertEquals(NO_ROLL, delays[5])
    }

    @Test
    fun `sin cambios no rueda ninguna columna`() {
        val delays = rollDelays("50,00€", "50,00€", staggerMs = 55)
        assertTrue(delays.all { it == NO_ROLL })
    }

    @Test
    fun `el roller recuerda el importe anterior entre cambios`() {
        val roller = AmountRoller("0,00€", 0.0)
        roller.advance("10,00€", 10.0, staggerMs = 10)
        assertEquals("10,00€", roller.current)
        assertTrue(roller.goingUp)
        roller.advance("5,00€", 5.0, staggerMs = 10)
        assertFalse(roller.goingUp)
    }

    // ── 02 · El check ─────────────────────────────────────────────────

    @Test
    fun `la marca traza primero el trazo corto y luego el largo`() {
        val (firstAtQuarter, secondAtQuarter) = tickProgress(0.25f)
        assertTrue(firstAtQuarter > 0f, "el trazo corto ya debería estar empezado")
        assertEquals(0f, secondAtQuarter, "el trazo largo aún no debería haber empezado")

        val (firstAtEnd, secondAtEnd) = tickProgress(1f)
        assertEquals(1f, firstAtEnd)
        assertEquals(1f, secondAtEnd)
    }

    @Test
    fun `el progreso de la marca se recorta fuera de rango`() {
        assertEquals(0f to 0f, tickProgress(-3f))
        assertEquals(1f to 1f, tickProgress(4f))
    }

    @Test
    fun `el alcance distingue singular de plural`() {
        assertEquals("1 deuda · 90.00 €", settlementScopeLabel(1, 90.0))
        assertEquals("3 deudas · 30.00 €", settlementScopeLabel(3, 30.0))
    }

    // ── 05 · Estado del evento ────────────────────────────────────────

    @Test
    fun `el raíl ordena los estados como la máquina de estados`() {
        assertEquals(0, eventStateIndex(EventState.OPEN))
        assertEquals(1, eventStateIndex(EventState.CALCULATED))
        assertEquals(2, eventStateIndex(EventState.CLOSED))
        assertEquals(EVENT_STATE_COUNT, EventState.entries.size)
    }

    // ── 06 · Calendario ───────────────────────────────────────────────

    @Test
    fun `dos eventos que se solapan no comparten carril`() {
        val lanes = assignEventLanes(
            listOf(
                event("a", startDay = 10, endDay = 14),
                event("b", startDay = 12, endDay = 13),
            ),
        )
        assertTrue(lanes["a"] != lanes["b"], "una barra se partiría si compartieran carril")
    }

    @Test
    fun `dos eventos que no se solapan reutilizan el mismo carril`() {
        val lanes = assignEventLanes(
            listOf(
                event("a", startDay = 10, endDay = 12),
                event("b", startDay = 20, endDay = 22),
            ),
        )
        assertEquals(lanes["a"], lanes["b"])
    }

    @Test
    fun `el carril de un evento es el mismo todos sus días`() {
        // Es lo que mantiene recta la barra: si se eligiera celda a celda, el
        // evento suelto del día 12 empujaría la barra a otra altura.
        val range = event("largo", startDay = 10, endDay = 14)
        val spot = event("suelto", startDay = 12)
        val lanes = assignEventLanes(listOf(range, spot))
        val laneOnEachDay = (10..14).map { markerFor(range, it, lanes["largo"]!!).lane }
        assertEquals(1, laneOnEachDay.distinct().size)
    }

    @Test
    fun `solo los extremos reales del rango se redondean`() {
        val range = event("viaje", startDay = 10, endDay = 13)
        assertTrue(markerFor(range, 10, 0).roundedStart)
        assertFalse(markerFor(range, 10, 0).roundedEnd)
        assertFalse(markerFor(range, 11, 0).roundedStart)
        assertFalse(markerFor(range, 11, 0).roundedEnd)
        assertTrue(markerFor(range, 13, 0).roundedEnd)
    }

    @Test
    fun `un evento de un solo día es punto y no barra`() {
        assertFalse(markerFor(event("cena", startDay = 5), 5, 0).isRange)
        assertTrue(markerFor(event("viaje", startDay = 5, endDay = 7), 5, 0).isRange)
    }

    @Test
    fun `las marcas que no caben se resumen en un contador`() {
        val events = (1..5).map { event("e$it", startDay = 9) }
        val lanes = assignEventLanes(events)
        val markers = markersForDay(events, 9, lanes)
        assertEquals(MAX_MARKER_LANES, markers.visible.size)
        assertEquals(2, markers.overflow)
        assertEquals(listOf(0, 1, 2), markers.visible.map { it.lane })
    }

    // ── 04 · El cálculo se resuelve ───────────────────────────────────

    @Test
    fun `los roles salen de las transferencias, no de los saldos`() {
        val transfers = listOf(
            SettlementTransfer("marta", "ana", 90.0),
            SettlementTransfer("iker", "luis", 40.0),
            SettlementTransfer("sofia", "luis", 20.0),
        )
        val (debtors, creditors) = settlementRoles(transfers)
        assertEquals(listOf("marta", "iker", "sofia"), debtors)
        assertEquals(listOf("ana", "luis"), creditors)
    }

    @Test
    fun `un acreedor que recibe varias veces aparece una sola vez`() {
        val transfers = listOf(
            SettlementTransfer("a", "natalia", 16.81),
            SettlementTransfer("b", "natalia", 4.49),
            SettlementTransfer("c", "natalia", 16.81),
        )
        val (debtors, creditors) = settlementRoles(transfers)
        assertEquals(3, debtors.size)
        assertEquals(listOf("natalia"), creditors)
    }

    @Test
    fun `la columna corta se centra frente a la larga`() {
        // Tres deudores y un acreedor: el acreedor cae a la altura del bloque,
        // no pegado arriba.
        val debtors = columnRowCenters(count = 3, totalRows = 3, rowHeight = 100f)
        val creditors = columnRowCenters(count = 1, totalRows = 3, rowHeight = 100f)
        assertEquals(listOf(50f, 150f, 250f), debtors)
        assertEquals(listOf(150f), creditors)
    }

    @Test
    fun `una columna vacía no produce filas`() {
        assertTrue(columnRowCenters(count = 0, totalRows = 3, rowHeight = 100f).isEmpty())
    }

    @Test
    fun `la última flecha termina justo al final del recorrido`() {
        for (count in 1..6) {
            val last = arrowProgress(1f, count - 1, count)
            assertEquals(1f, last, "con $count transferencias la última se queda a medias")
        }
    }

    @Test
    fun `las flechas se trazan en cascada`() {
        val count = 3
        val mid = 0.5f
        val first = arrowProgress(mid, 0, count)
        val second = arrowProgress(mid, 1, count)
        val third = arrowProgress(mid, 2, count)
        assertTrue(first > second, "la primera va por delante de la segunda")
        assertTrue(second > third, "la segunda va por delante de la tercera")
    }

    @Test
    fun `la punta de flecha se abre a ambos lados del trazo`() {
        val tip = Offset(100f, 0f)
        val from = Offset(0f, 0f)
        val (left, right) = arrowHead(tip, from, length = 10f)
        assertTrue(left.x < tip.x, "la punta apunta hacia adelante")
        assertTrue(right.x < tip.x)
        assertTrue(abs(left.y + right.y) < 0.01f, "los dos lados son simétricos")
    }

    // ── 06 · Apertura del calendario ──────────────────────────────────

    @Test
    fun `el rectángulo interpola entre origen y pantalla completa`() {
        val from = Rect(344f, 16f, 384f, 56f)
        val to = Rect(0f, 0f, 400f, 880f)
        assertEquals(from, lerpRect(from, to, 0f))
        assertEquals(to, lerpRect(from, to, 1f))
        val mid = lerpRect(from, to, 0.5f)
        assertEquals(172f, mid.left)
        assertEquals(8f, mid.top)
    }

    @Test
    fun `un muelle que sobrepasa no dibuja fuera de la pantalla`() {
        val from = Rect(0f, 0f, 40f, 40f)
        val to = Rect(0f, 0f, 400f, 880f)
        assertEquals(to, lerpRect(from, to, 1.08f))
        assertEquals(from, lerpRect(from, to, -0.2f))
    }

    @Test
    fun `el color del origen se retira antes de que acabe el crecimiento`() {
        assertEquals(0f, originColorFraction(0f))
        assertEquals(1f, originColorFraction(0.34f))
        assertEquals(1f, originColorFraction(1f))
    }
}
