package com.brifo.server.briefing.dto.response

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.briefing.entity.BriefingStatus
import java.util.UUID

data class GetOfficeBriefingsResponse(
    val items: List<Item>,
) {
    data class Item(
        val stockName: String,
        val agents: List<Agent>,
    )

    data class Agent(
        val briefingId: UUID,
        val agentId: UUID,
        val nickname: String,
        val agentType: AgentType,
        val status: BriefingStatus,
    )
}
