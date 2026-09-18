package com.brifo.server.decision.repository

import com.brifo.server.decision.entity.DecisionResult
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID
import com.brifo.server.decision.entity.DecisionDirection

interface DecisionResultRepository : JpaRepository<DecisionResult, Long> {
    fun existsByDecisionId(decisionId: Long): Boolean

    fun existsByDecisionPublicId(decisionPublicId: UUID): Boolean

    fun findByDecisionPublicId(decisionPublicId: UUID): DecisionResult?

    fun countByDecisionBriefingAgentUserId(userId: Long): Long

    fun countByDecisionBriefingAgentUserIdAndIsCorrect(
        userId: Long,
        isCorrect: Boolean,
    ): Long

    fun countByDecisionBriefingAgentUserIdAndIsCorrectAndDecisionDirection(
        userId: Long,
        isCorrect: Boolean,
        direction: DecisionDirection,
    ): Long

    fun countByDecisionBriefingAgentUserIdAndIsCorrectAndDecisionAllocationRatePercentGreaterThanEqual(
        userId: Long,
        isCorrect: Boolean,
        allocationRatePercent: Short,
    ): Long

    fun findTop3ByDecisionBriefingAgentUserIdOrderByIdDesc(userId: Long): List<DecisionResult>

    fun findTop5ByDecisionBriefingAgentUserIdOrderByIdDesc(userId: Long): List<DecisionResult>
}
