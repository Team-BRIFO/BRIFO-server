package com.brifo.server.decision.repository

import java.util.UUID

interface DecisionQueryRepository {
    fun findRecentSettledDecisionIds(
        userPublicId: UUID,
        limit: Long,
    ): List<UUID>
}
