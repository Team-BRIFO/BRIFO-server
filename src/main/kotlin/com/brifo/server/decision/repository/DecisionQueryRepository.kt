package com.brifo.server.decision.repository

import com.brifo.server.decision.dto.response.GetDecisionResultResponse
import com.brifo.server.decision.dto.response.GetDecisionsResponse
import com.brifo.server.decision.entity.DecisionDirection
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

interface DecisionQueryRepository {
    fun existsDailyDecision(
        userPublicId: UUID,
        stockPublicId: UUID,
        displayDate: LocalDate,
    ): Boolean

    fun findTodayDecisions(
        userPublicId: UUID,
        displayDate: LocalDate,
    ): List<TodayDecisionRow>

    fun findDecisionResults(
        userPublicId: UUID,
        decisionPublicId: UUID,
    ): List<GetDecisionResultResponse>

    fun findRecentSettledDecisions(
        userPublicId: UUID,
        limit: Long,
    ): List<RecentSettledDecision>

    fun countByUserIdWithinPeriod(
        userId: Long,
        from: LocalDateTime,
        to: LocalDateTime,
    ): Long

    fun findUnsettledIds(
        targetDate: LocalDate,
        ignoreSettlementCutoff: Boolean = false,
    ): List<Long>

    fun findUnsettledStockIds(
        targetDate: LocalDate,
        ignoreSettlementCutoff: Boolean = false,
    ): List<Long>

    fun findSettlementCandidate(decisionId: Long): SettlementDecision?
}

/**
 * "오늘의 예측" 목록 조회용 내부 행. 정산 전 종목은 서비스 계층에서 이 행의
 * stockId(Long)/stockCode로 [com.brifo.server.stock.service.StockPriceService]를 호출해
 * 장중 실시간 현재가로 price/changeRate를 덮어쓴다.
 */
data class TodayDecisionRow(
    val decisionId: UUID,
    val direction: DecisionDirection,
    val allocatedAp: Int,
    val isSettled: Boolean,
    val agentId: UUID,
    val agentType: com.brifo.server.agent.entity.AgentType,
    val stockId: Long,
    val stockPublicId: UUID,
    val stockName: String,
    val stockCode: String,
    val logoUrl: String?,
    val price: Long?,
    val changeRate: BigDecimal?,
    val tradeDate: LocalDate?,
)

data class RecentSettledDecision(
    val stockName: String,
    val direction: DecisionDirection,
    val allocationRatePercent: Int,
    val isCorrect: Boolean,
    val actualChange: BigDecimal,
)

data class SettlementDecision(
    val direction: DecisionDirection,
    val stockId: Long,
)
