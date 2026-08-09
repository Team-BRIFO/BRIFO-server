package com.brifo.server.batch.collection

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NewsImportanceCalculatorTest {
    private val calculator = NewsImportanceCalculator()

    @Test
    fun `공시가 있고 마감 회차이면 최대 중요도다`() {
        val importance = calculator.calculate(CollectionRound.CLOSING, hasDisclosure = true)

        assertEquals("1.00", importance.toPlainString())
    }

    @Test
    fun `공시가 없으면 늦은 회차의 중요도가 높다`() {
        val morning = calculator.calculate(CollectionRound.MORNING, hasDisclosure = false)
        val closing = calculator.calculate(CollectionRound.CLOSING, hasDisclosure = false)

        assertTrue(closing > morning)
    }
}
