package com.brifo.server.agent.repository

import com.brifo.server.agent.entity.Agent
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AgentRepository : JpaRepository<Agent, Long> {
    fun findByPublicId(publicId: UUID): Agent?
}
