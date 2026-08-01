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
    val agent: DecisionResultAgent,
    val stock: DecisionResultStock,
) {
    data class DecisionResultAgent(
        val agentId: UUID,
        val agentType: AgentType,
    )

    data class DecisionResultStock(
        val name: String,
        val price: Long,
        val changeRate: BigDecimal,
        val tradeDate: LocalDate,
    )
}
