package com.brifo.server.batch.collection

import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NewsImportanceCalculatorTest {
    private val calculator = NewsImportanceCalculator()

    @Test
    fun `키워드는 최대 3개까지만 중요도에 반영한다`() {
        val three = calculator.calculate(3, CollectionRound.CLOSING)
        val five = calculator.calculate(5, CollectionRound.CLOSING)

        assertEquals(BigDecimal("1.00"), three)
        assertEquals(three, five)
    }

    @Test
    fun `같은 키워드 점수라면 늦은 회차의 중요도가 높다`() {
        val morning = calculator.calculate(1, CollectionRound.MORNING)
        val closing = calculator.calculate(1, CollectionRound.CLOSING)

        assertTrue(closing > morning)
    }
}
