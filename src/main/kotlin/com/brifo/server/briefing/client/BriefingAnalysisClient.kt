package com.brifo.server.briefing.client

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.briefing.entity.BriefingDirection
import java.math.BigDecimal
import java.util.UUID

interface BriefingAnalysisClient {
    fun createBriefings(request: Request): Result

    data class Request(
        val userId: UUID,
        val newsCardIds: List<UUID>,
        val targets: List<Target>,
        val recentDecisionIds: List<UUID>,
    )

    data class Target(
        val briefingId: UUID,
        val agentId: UUID,
    )

    data class Result(
        val briefings: List<BriefingResult>,
    )

    data class BriefingResult(
        val briefingId: UUID,
        val agentId: UUID,
        val agentType: AgentType,
        val direction: BriefingDirection,
        val probability: BigDecimal,
        val headline: String,
        val summary: String,
        val personalComment: String?,
        val commonAnalysis: String,
        val closingComment: String,
        val modelName: String,
        val cached: Boolean,
        val personalCached: Boolean,
    )
}
