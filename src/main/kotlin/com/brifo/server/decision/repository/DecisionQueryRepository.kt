package com.brifo.server.decision.repository

import com.brifo.server.decision.dto.response.GetDecisionResultResponse
import com.brifo.server.decision.dto.response.GetDecisionsResponse
import java.time.LocalDate
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
    ): List<GetDecisionsResponse.Item>

    fun findDecisionResults(
        userPublicId: UUID,
        decisionPublicId: UUID,
    ): List<GetDecisionResultResponse>

    fun findRecentSettledDecisionIds(
        userPublicId: UUID,
        limit: Long,
    ): List<UUID>
}
