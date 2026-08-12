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

    fun findTodayUnsettledDecisions(
        userPublicId: UUID,
        displayDate: LocalDate,
    ): List<GetDecisionsResponse.DecisionItem>

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

data class RecentSettledDecision(
    val stockName: String,
    val direction: DecisionDirection,
    val confidence: Int,
    val isCorrect: Boolean,
    val actualChange: BigDecimal,
)

data class SettlementDecision(
    val direction: DecisionDirection,
    val stockId: Long,
)
