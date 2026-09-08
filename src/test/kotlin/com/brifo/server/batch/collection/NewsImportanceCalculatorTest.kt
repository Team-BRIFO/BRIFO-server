package com.brifo.server.batch.collection

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NewsImportanceCalculatorTest {
    private val calculator = NewsImportanceCalculator()

    @Test
    fun `마감 회차의 중요도는 최대값이다`() {
        val importance = calculator.calculate(CollectionRound.CLOSING)

        assertEquals("1.00", importance.toPlainString())
    }

    @Test
    fun `늦은 회차일수록 중요도가 높다`() {
        val morning = calculator.calculate(CollectionRound.MORNING)
        val midday = calculator.calculate(CollectionRound.MIDDAY)
        val closing = calculator.calculate(CollectionRound.CLOSING)

        assertTrue(morning < midday)
        assertTrue(midday < closing)
    }
}
