package com.brifo.server.agent.repository

import com.brifo.server.agent.entity.Agent
import java.util.UUID

interface AgentQueryRepository {
    fun findOwnedAgents(
        userPublicId: UUID,
        agentPublicIds: Collection<UUID>,
    ): List<Agent>
}
