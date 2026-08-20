package com.brifo.server.decision.dto.response

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.decision.entity.DecisionDirection
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class GetDecisionsResponse(
    val items: List<DecisionItem>,
) {
    data class DecisionItem(
        val decisionId: UUID,
        val direction: DecisionDirection,
        val confidenceLevel: Int,
        val isSettled: Boolean,
        val agent: DecisionListAgent,
        val stock: DecisionListStock,
    )

    data class DecisionListAgent(
        val agentId: UUID,
        val agentType: AgentType,
    )

    data class DecisionListStock(
        val stockId: UUID,
        val name: String,
        val logoUrl: String?,
        val price: Long?,
        val changeRate: BigDecimal?,
        val tradeDate: LocalDate?,
    )
}
