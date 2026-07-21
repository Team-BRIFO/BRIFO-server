package com.brifo.server.decision.repository

import com.brifo.server.decision.entity.DecisionResult
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DecisionResultRepository : JpaRepository<DecisionResult, Long> {
    fun existsByDecisionPublicId(decisionPublicId: UUID): Boolean
}
