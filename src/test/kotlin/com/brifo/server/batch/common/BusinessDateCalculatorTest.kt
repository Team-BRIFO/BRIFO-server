package com.brifo.server.batch.common

import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class BusinessDateCalculatorTest {
    private val calculator = BusinessDateCalculator()

    @Test
    fun `월요일의 직전 영업일은 금요일이다`() {
        assertEquals(
            LocalDate.of(2026, 7, 31),
            calculator.previousBusinessDay(LocalDate.of(2026, 8, 3)),
        )
    }

    @Test
    fun `금요일의 다음 영업일은 월요일이다`() {
        assertEquals(
            LocalDate.of(2026, 8, 3),
            calculator.nextBusinessDay(LocalDate.of(2026, 7, 31)),
        )
    }
}
