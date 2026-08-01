package com.brifo.server.briefing.dto.response

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.briefing.entity.BriefingStatus
import java.util.UUID

data class GetOfficeBriefingsResponse(
    val items: List<OfficeBriefingItemResponse>,
)

data class OfficeBriefingItemResponse(
    val stockName: String,
    val agents: List<OfficeBriefingAgentResponse>,
)

data class OfficeBriefingAgentResponse(
    val briefingId: UUID,
    val agentId: UUID,
    val nickname: String,
    val agentType: AgentType,
    val status: BriefingStatus,
)
