package com.brifo.server.agent.repository

import com.brifo.server.agent.entity.Agent
import com.brifo.server.agent.entity.QAgent.Companion.agent
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class AgentQueryRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : AgentQueryRepository {
    override fun findOwnedAgents(
        userPublicId: UUID,
        agentPublicIds: Collection<UUID>,
    ): List<Agent> {
        if (agentPublicIds.isEmpty()) {
            return emptyList()
        }

        return queryFactory
            .selectFrom(agent)
            .where(
                agent.user.publicId.eq(userPublicId),
                agent.publicId.`in`(agentPublicIds),
            ).orderBy(agent.id.asc())
            .fetch()
    }
}
