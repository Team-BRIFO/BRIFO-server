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

    /**
     * 배분금(allocatedAp)은 등록 시점에 이미 에스크로로 차감된 상태다.
     * 여기서는 정산 결과에 따른 추가 지급/환급만 계산한다:
     * 오답이면 이미 잃은 배분금 외 추가 변동 없음(0), 관망 적중이면 원금만 환급,
     * 방향 적중이면 배분금의 2배를 지급한다(원금 + 순수익 배분금만큼).
     */
    fun apSettlement(
        predicted: DecisionDirection,
        isCorrect: Boolean,
        allocatedAp: Int,
    ): ApSettlement =
        when {
            predicted == DecisionDirection.NEUTRAL && isCorrect ->
                ApSettlement(allocatedAp, ApTransactionReason.NEUTRAL_HIT)
            !isCorrect ->
                ApSettlement(0, ApTransactionReason.DECISION_LOSE)
            else ->
                ApSettlement(allocatedAp * PAYOUT_MULTIPLIER, ApTransactionReason.DECISION_WIN)
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
        const val PAYOUT_MULTIPLIER = 2
    }
}
