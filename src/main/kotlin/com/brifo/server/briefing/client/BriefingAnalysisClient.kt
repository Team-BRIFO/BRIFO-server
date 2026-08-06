package com.brifo.server.briefing.client

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.briefing.entity.BriefingDirection
import com.brifo.server.decision.entity.DecisionDirection
import java.math.BigDecimal
import java.util.UUID

interface BriefingAnalysisClient {
    fun createBriefings(request: Request): Result

    data class Request(
        val userId: UUID,
        val newsCard: List<NewsCard>,
        val agentTypes: List<AgentType>,
        val levelRange: String,
        val recentDecisions: List<RecentDecision>,
    )

    data class NewsCard(
        val cardId: UUID,
        val newsId: UUID,
        val headline: String,
        val points: List<String>,
    )

    data class RecentDecision(
        val stockName: String,
        val direction: DecisionDirection,
        val confidence: Int,
        val isCorrect: Boolean,
        val actualChange: BigDecimal,
    )

    data class Result(
        val briefings: List<BriefingResult>,
    )

    data class BriefingResult(
        val agentType: AgentType,
        val direction: BriefingDirection,
        val confidenceRate: Int,
        val headline: String,
        val summary: String,
        val personalComment: String?,
        val contentText: String,
        val oneLiner: String,
        val modelName: String,
        val cached: Boolean,
        val personalCached: Boolean,
    )
}
