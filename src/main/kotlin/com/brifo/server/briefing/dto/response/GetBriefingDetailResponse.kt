package com.brifo.server.briefing.dto.response

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.briefing.entity.BriefingDirection
import java.util.UUID

data class GetBriefingDetailResponse(
    val stock: BriefingStockResponse,
    val agent: BriefingDetailAgent,
    val newsCards: List<BriefingNewsCard>,
    val briefing: BriefingDetailContent,
) {
    data class BriefingDetailAgent(
        val agentId: UUID,
        val agentType: AgentType,
        val nickname: String,
        val modelName: String,
    )

    data class BriefingNewsCard(
        val cardId: UUID,
        val headline: String?,
    )

    data class BriefingDetailContent(
        val briefingId: UUID,
        val direction: BriefingDirection,
        val confidenceRate: Int,
        val summary: String,
        val personalComment: String?,
        val contentText: String,
        val oneLiner: String,
    )
}
