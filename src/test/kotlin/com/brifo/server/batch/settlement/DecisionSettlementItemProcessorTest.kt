package com.brifo.server.batch.settlement

import com.brifo.server.decision.entity.DecisionDirection
import com.brifo.server.decision.repository.DecisionRepository
import com.brifo.server.decision.repository.SettlementDecision
import com.brifo.server.stock.entity.DailyStockPrice
import com.brifo.server.stock.repository.DailyStockPriceRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DecisionSettlementItemProcessorTest {
    private val decisionRepository = mock(DecisionRepository::class.java)
    private val priceRepository = mock(DailyStockPriceRepository::class.java)
    private val processor =
        DecisionSettlementItemProcessor(
            targetDate = TARGET_DATE,
            decisionRepository = decisionRepository,
            dailyStockPriceRepository = priceRepository,
            calculator = DecisionSettlementCalculator(),
        )

    @Test
    fun `정산 projection으로 결과를 계산해 지연 연관관계에 접근하지 않는다`() {
        val price = mock(DailyStockPrice::class.java)
        `when`(decisionRepository.findSettlementCandidate(DECISION_ID)).thenReturn(
            SettlementDecision(DecisionDirection.UP, STOCK_ID),
        )
        `when`(priceRepository.findByStockIdAndTradeDateAndIsClosingTrue(STOCK_ID, TARGET_DATE)).thenReturn(price)
        `when`(price.id).thenReturn(PRICE_ID)
        `when`(price.changeRate).thenReturn(BigDecimal("1.25"))

        val result = processor.process(DECISION_ID)

        assertEquals(DecisionSettlementItem(DECISION_ID, PRICE_ID, isCorrect = true), result)
        verify(decisionRepository).findSettlementCandidate(DECISION_ID)
    }

    @Test
    fun `정산 대상을 찾을 수 없으면 실패한다`() {
        `when`(decisionRepository.findSettlementCandidate(DECISION_ID)).thenReturn(null)

        assertFailsWith<IllegalStateException> { processor.process(DECISION_ID) }
    }

    companion object {
        private val TARGET_DATE = LocalDate.of(2026, 8, 11)
        private const val DECISION_ID = 1L
        private const val STOCK_ID = 2L
        private const val PRICE_ID = 3L
    }
}
