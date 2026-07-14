package com.brifo.server.briefing.dto.response

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.briefing.entity.BriefingDirection
import java.util.UUID

data class GetCardBriefingsResponse(
    val stock: BriefingStockResponse,
    val items: List<Item>,
) {
    data class Item(
        val briefingId: UUID,
        val oneLiner: String,
        val direction: BriefingDirection,
        val agentId: UUID,
        val nickname: String,
        val agentType: AgentType,
    )
}
