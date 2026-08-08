package com.brifo.server.batch.settlement

import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.decision.entity.DecisionDirection
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals

class DecisionSettlementCalculatorTest {
    private val calculator = DecisionSettlementCalculator()

    @Test
    fun `변동률 경계값을 포함해 실제 방향을 판정한다`() {
        assertEquals(DecisionDirection.UP, calculator.actualDirection(BigDecimal("0.5")))
        assertEquals(DecisionDirection.DOWN, calculator.actualDirection(BigDecimal("-0.5")))
        assertEquals(DecisionDirection.NEUTRAL, calculator.actualDirection(BigDecimal("0.49")))
    }

    @Test
    fun `상승 하락 적중과 오답 AP를 계산한다`() {
        assertEquals(
            DecisionSettlementCalculator.ApSettlement(100, ApTransactionReason.DECISION_WIN),
            calculator.apSettlement(DecisionDirection.UP, true, 5),
        )
        assertEquals(
            DecisionSettlementCalculator.ApSettlement(-50, ApTransactionReason.DECISION_LOSE),
            calculator.apSettlement(DecisionDirection.DOWN, false, 5),
        )
    }

    @Test
    fun `관망 오답도 0원 거래로 계산한다`() {
        assertEquals(
            DecisionSettlementCalculator.ApSettlement(0, ApTransactionReason.DECISION_LOSE),
            calculator.apSettlement(DecisionDirection.NEUTRAL, false, 3),
        )
    }
}
