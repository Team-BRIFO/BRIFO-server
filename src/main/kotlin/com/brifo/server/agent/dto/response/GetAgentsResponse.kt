package com.brifo.server.agent.dto.response

import com.brifo.server.agent.entity.AgentType
import java.util.UUID

data class GetAgentsResponse(
    val items: List<Item>,
) {
    data class Item(
        val agentId: UUID,
        val nickname: String,
        val agentType: AgentType,
        val modelName: String,
        val level: Int,
        val exp: Int,
        val dailySalary: Int,
        val accuracyRate: Int,
    )
}
