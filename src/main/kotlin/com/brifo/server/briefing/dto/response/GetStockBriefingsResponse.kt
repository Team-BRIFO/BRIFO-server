package com.brifo.server.briefing.dto.response

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.briefing.entity.BriefingDirection
import com.brifo.server.briefing.entity.BriefingStatus
import java.util.UUID

data class GetStockBriefingsResponse(
    val stock: BriefingStockResponse,
    val items: List<Item>,
) {
    data class Item(
        val briefingId: UUID,
        val status: BriefingStatus,
        val oneLiner: String?,
        val direction: BriefingDirection?,
        val agentId: UUID,
        val nickname: String,
        val agentType: AgentType,
    )
}
