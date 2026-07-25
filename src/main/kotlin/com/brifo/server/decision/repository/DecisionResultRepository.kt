package com.brifo.server.decision.repository

import com.brifo.server.decision.entity.DecisionResult
import org.springframework.data.jpa.repository.JpaRepository

interface DecisionResultRepository : JpaRepository<DecisionResult, Long> {
    fun countByDecisionBriefingAgentUserId(userId: Long): Long

    fun countByDecisionBriefingAgentUserIdAndIsCorrect(
        userId: Long,
        isCorrect: Boolean,
    ): Long
}
