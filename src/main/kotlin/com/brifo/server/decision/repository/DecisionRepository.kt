package com.brifo.server.decision.repository

import com.brifo.server.decision.entity.Decision
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DecisionRepository :
    JpaRepository<Decision, Long>,
    DecisionQueryRepository {
    fun existsByPublicIdAndBriefingAgentUserPublicId(
        publicId: UUID,
        userPublicId: UUID,
    ): Boolean

    fun findByPublicId(publicId: UUID): Decision?

    fun countByBriefingAgentUserId(userId: Long): Long
}
