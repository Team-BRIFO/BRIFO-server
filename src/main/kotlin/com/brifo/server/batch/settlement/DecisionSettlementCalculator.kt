package com.brifo.server.batch.settlement

import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.decision.entity.DecisionDirection
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class DecisionSettlementCalculator {
    data class ApSettlement(
        val amount: Int,
        val reason: ApTransactionReason,
    )

    fun actualDirection(changeRate: BigDecimal): DecisionDirection =
        when {
            changeRate >= THRESHOLD -> DecisionDirection.UP
            changeRate <= THRESHOLD.negate() -> DecisionDirection.DOWN
            else -> DecisionDirection.NEUTRAL
        }

    fun apSettlement(
        predicted: DecisionDirection,
        isCorrect: Boolean,
        confidenceLevel: Int,
    ): ApSettlement =
        when {
            predicted == DecisionDirection.NEUTRAL && isCorrect ->
                ApSettlement(10, ApTransactionReason.NEUTRAL_HIT)
            predicted == DecisionDirection.NEUTRAL ->
                ApSettlement(0, ApTransactionReason.DECISION_LOSE)
            isCorrect ->
                ApSettlement(confidenceLevel * 20, ApTransactionReason.DECISION_WIN)
            else ->
                ApSettlement(-confidenceLevel * 10, ApTransactionReason.DECISION_LOSE)
        }

    fun experience(
        predicted: DecisionDirection,
        isCorrect: Boolean,
    ): Int =
        when {
            !isCorrect -> 10
            predicted == DecisionDirection.NEUTRAL -> 20
            else -> 50
        }

    private companion object {
        val THRESHOLD = BigDecimal("0.5")
    }
}
