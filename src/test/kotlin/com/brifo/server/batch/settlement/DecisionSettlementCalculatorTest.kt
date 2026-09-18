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
    fun `방향 적중은 배분금의 2배를 지급하고 오답은 추가 변동이 없다`() {
        assertEquals(
            DecisionSettlementCalculator.ApSettlement(100_000, ApTransactionReason.DECISION_WIN),
            calculator.apSettlement(DecisionDirection.UP, true, 50_000),
        )
        assertEquals(
            DecisionSettlementCalculator.ApSettlement(0, ApTransactionReason.DECISION_LOSE),
            calculator.apSettlement(DecisionDirection.DOWN, false, 50_000),
        )
    }

    @Test
    fun `관망 적중은 배분금 원금을 환급하고 오답은 0원 거래로 계산한다`() {
        assertEquals(
            DecisionSettlementCalculator.ApSettlement(30_000, ApTransactionReason.NEUTRAL_HIT),
            calculator.apSettlement(DecisionDirection.NEUTRAL, true, 30_000),
        )
        assertEquals(
            DecisionSettlementCalculator.ApSettlement(0, ApTransactionReason.DECISION_LOSE),
            calculator.apSettlement(DecisionDirection.NEUTRAL, false, 30_000),
        )
    }
}
