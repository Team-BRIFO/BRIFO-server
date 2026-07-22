package com.brifo.server.agent.repository

import com.brifo.server.agent.entity.Agent
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AgentRepository :
    JpaRepository<Agent, Long>,
    AgentQueryRepository {
    fun findAllByUserPublicIdAndPublicIdInOrderByIdAsc(
        userPublicId: UUID,
        publicIds: Collection<UUID>,
    ): List<Agent>
}
