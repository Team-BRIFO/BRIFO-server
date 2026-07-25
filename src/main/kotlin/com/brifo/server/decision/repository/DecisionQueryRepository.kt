package com.brifo.server.decision.repository

import java.time.LocalDateTime
import java.util.UUID

interface DecisionQueryRepository {
    fun findRecentSettledDecisionIds(
        userPublicId: UUID,
        limit: Long,
    ): List<UUID>

    fun countByUserIdWithinPeriod(
        userId: Long,
        from: LocalDateTime,
        to: LocalDateTime,
    ): Long
}
