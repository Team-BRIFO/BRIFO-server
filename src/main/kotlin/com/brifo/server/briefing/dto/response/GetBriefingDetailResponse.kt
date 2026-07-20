package com.brifo.server.briefing.dto.response

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.briefing.entity.BriefingDirection
import java.util.UUID

data class GetBriefingDetailResponse(
    val stock: BriefingStockResponse,
    val agent: Agent,
    val newsCards: List<NewsCard>,
    val briefing: Briefing,
) {
    data class Agent(
        val agentId: UUID,
        val agentType: AgentType,
        val nickname: String,
        val modelName: String,
    )

    data class NewsCard(
        val cardId: UUID,
        val headline: String?,
    )

    data class Briefing(
        val briefingId: UUID,
        val direction: BriefingDirection,
        val confidenceRate: Int,
        val summary: String,
        val personalComment: String?,
        val contentText: String,
        val oneLiner: String,
    )
}
