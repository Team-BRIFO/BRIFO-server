package com.brifo.server.briefing.dto.response

import com.brifo.server.agent.entity.AgentType
import java.util.UUID

data class CreateBriefingResponse(
    val requestedCount: Int,
    val totalSalaryCost: Int,
    val requestedAgents: List<RequestedAgent>,
) {
    data class RequestedAgent(
        val briefingId: UUID,
        val agentId: UUID,
        val agentType: AgentType,
        val salaryCost: Int,
    )
}
