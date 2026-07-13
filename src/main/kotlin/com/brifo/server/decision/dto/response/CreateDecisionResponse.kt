package com.brifo.server.decision.dto.response

import com.brifo.server.decision.entity.DecisionDirection
import java.util.UUID

data class CreateDecisionResponse(
    val decisionId: UUID,
    val direction: DecisionDirection,
    val confidenceLevel: Int,
    val stock: Stock,
) {
    data class Stock(
        val stockId: UUID,
        val name: String,
    )
}
