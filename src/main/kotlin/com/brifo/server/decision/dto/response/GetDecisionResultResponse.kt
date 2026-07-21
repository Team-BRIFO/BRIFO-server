package com.brifo.server.decision.dto.response

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.decision.entity.DecisionDirection
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class GetDecisionResultResponse(
    val isCorrect: Boolean,
    val apDelta: Int,
    val direction: DecisionDirection,
    val confidenceLevel: Int,
    val agent: Agent,
    val stock: Stock,
) {
    data class Agent(
        val agentId: UUID,
        val agentType: AgentType,
    )

    data class Stock(
        val name: String,
        val price: Long,
        val changeRate: BigDecimal,
        val tradeDate: LocalDate,
    )
}
