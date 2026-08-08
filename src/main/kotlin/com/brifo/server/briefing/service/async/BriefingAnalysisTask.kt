package com.brifo.server.briefing.service.async

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.briefing.entity.BriefingDirection
import com.brifo.server.decision.entity.DecisionDirection
import java.math.BigDecimal
import java.util.UUID

object BriefingAnalysisTask {
    data class Command(
        val userPublicId: UUID,
        val briefingPublicIds: List<UUID>,
    )

    data class Context(
        val userPublicId: UUID,
        val newsCards: List<NewsCard>,
        val targets: List<Target>,
        val recentDecisions: List<RecentDecision>,
    ) {
        data class NewsCard(
            val cardPublicId: UUID,
            val newsPublicId: UUID,
            val headline: String,
            val points: List<String>,
        )

        data class Target(
            val briefingPublicId: UUID,
            val agentType: AgentType,
            val modelName: String,
            val level: Int,
        )

        data class RecentDecision(
            val stockName: String,
            val direction: DecisionDirection,
            val confidence: Int,
            val isCorrect: Boolean,
            val actualChange: BigDecimal,
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
