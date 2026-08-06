package com.brifo.server.decision.repository

import com.brifo.server.decision.entity.DecisionResult
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID
import com.brifo.server.decision.entity.DecisionDirection

interface DecisionResultRepository : JpaRepository<DecisionResult, Long> {
    fun existsByDecisionId(decisionId: Long): Boolean

    fun existsByDecisionPublicId(decisionPublicId: UUID): Boolean

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
}
