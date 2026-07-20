package com.brifo.server.briefing.service.async

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.briefing.entity.BriefingDirection
import java.math.BigDecimal
import java.util.UUID

object BriefingAnalysisTask {
    data class Command(
        val userPublicId: UUID,
        val briefingPublicIds: List<UUID>,
    )

    data class Context(
        val userPublicId: UUID,
        val newsCardPublicIds: List<UUID>,
        val targets: List<Target>,
        val recentDecisionPublicIds: List<UUID>,
    ) {
        data class Target(
            val briefingPublicId: UUID,
            val agentPublicId: UUID,
            val agentType: AgentType,
            val modelName: String,
        )
    }

    data class Completion(
        val briefingPublicId: UUID,
        val direction: BriefingDirection,
        val probability: BigDecimal,
        val headline: String,
        val summary: String,
        val personalComment: String?,
        val commonAnalysis: String,
        val closingComment: String,
    )

    data class ValidationResult(
        val completions: List<Completion>,
        val failedBriefingPublicIds: List<UUID>,
    )
}
