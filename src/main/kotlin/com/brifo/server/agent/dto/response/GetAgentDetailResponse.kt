package com.brifo.server.agent.dto.response

import com.brifo.server.agent.entity.AgentType
import java.util.UUID

data class GetAgentDetailResponse(
    val agentId: UUID,
    val nickname: String,
    val agentType: AgentType,
    val level: Int,
    val exp: Int,
    val modelName: String,
    val description: String?,
    val accuracyRate: Int,
    val totalAnalyses: Int,
    val contributedAp: Int,
    val consecutiveWorkDays: Int,
    val dailySalary: Int,
)
