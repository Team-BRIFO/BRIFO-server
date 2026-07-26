package com.brifo.server.agent.repository

import com.brifo.server.agent.entity.AgentType
import java.time.LocalDate
import java.util.UUID

interface AgentQueryRepository {
    fun findAgentSummaries(userPublicId: UUID): List<AgentSummary>

    fun findAgentDetail(
        userPublicId: UUID,
        agentPublicId: UUID,
    ): AgentDetail?

    fun findCompletedWorkDates(agentId: Long): List<LocalDate>

    data class AgentSummary(
        val agentId: Long,
        val publicId: UUID,
        val nickname: String,
        val agentType: AgentType,
        val modelName: String,
        val level: Int,
        val exp: Int,
        val dailySalary: Int,
        val totalAnalyses: Long,
        val correctAnalyses: Long,
    )

    data class AgentDetail(
        val agentId: Long,
        val publicId: UUID,
        val nickname: String,
        val agentType: AgentType,
        val level: Int,
        val exp: Int,
        val modelName: String,
        val description: String,
        val dailySalary: Int,
        val totalAnalyses: Long,
        val correctAnalyses: Long,
        val contributedAp: Long,
    )
}
