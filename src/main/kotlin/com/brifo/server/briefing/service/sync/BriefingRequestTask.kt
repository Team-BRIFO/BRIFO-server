package com.brifo.server.briefing.service.sync

import com.brifo.server.agent.entity.AgentType
import java.time.LocalDateTime
import java.util.UUID

object BriefingRequestTask {
    data class Command(
        val userPublicId: UUID,
        val stockPublicId: UUID,
        val agentPublicIds: List<UUID>,
        val requestedAt: LocalDateTime,
    )

    data class Result(
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
}
