package com.brifo.server.agent.repository

import com.brifo.server.agent.entity.Agent
import com.brifo.server.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AgentRepository :
    JpaRepository<Agent, Long>,
    AgentQueryRepository {
    fun findAllByUserPublicIdAndPublicIdInOrderByIdAsc(
        userPublicId: UUID,
        publicIds: Collection<UUID>,
    ): List<Agent>

    fun findByPublicId(publicId: UUID): Agent?

    fun findAllByUserPublicIdOrderByAgentTypeAsc(userPublicId: UUID): List<Agent>

    fun findByPublicIdAndUserPublicId(
        publicId: UUID,
        userPublicId: UUID,
    ): Agent?

    fun existsByUser(user: User): Boolean

    fun findAllByUserId(userId: Long): List<Agent>

    fun existsByUserIdAndLevelGreaterThanEqual(
        userId: Long,
        level: Int,
    ): Boolean
}
