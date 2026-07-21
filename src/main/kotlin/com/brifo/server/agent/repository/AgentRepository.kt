package com.brifo.server.agent.repository

import com.brifo.server.agent.entity.Agent
import com.brifo.server.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AgentRepository :
    JpaRepository<Agent, Long>,
    AgentQueryRepository {
    fun findByPublicId(publicId: UUID): Agent?

    fun existsByUser(user: User): Boolean
}
