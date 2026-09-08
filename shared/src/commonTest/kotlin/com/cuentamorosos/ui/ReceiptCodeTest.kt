package com.cuentamorosos.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReceiptCodeTest {

    @Test
    fun sameSnapshotAlwaysPrintsTheSameCode() {
        assertEquals(receiptCode(1756000000000L), receiptCode(1756000000000L))
    }

    @Test
    fun codeHasStableShape() {
        val code = receiptCode(1756000000000L)
        assertTrue(code.matches(Regex("CM-[0-9A-F]{4}")), "unexpected code: $code")
    }

    @Test
    fun differentMillisUsuallyDiffer() {
        assertTrue(receiptCode(1L) != receiptCode(999999999L))
    }
}
