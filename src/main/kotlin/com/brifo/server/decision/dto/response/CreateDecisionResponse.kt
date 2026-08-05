package com.brifo.server.decision.dto.response

import com.brifo.server.decision.entity.DecisionDirection
import java.util.UUID

data class CreateDecisionResponse(
    val decisionId: UUID,
    val direction: DecisionDirection,
    val confidenceLevel: Int,
    val stock: CreatedDecisionStock,
) {
    data class CreatedDecisionStock(
        val stockId: UUID,
        val name: String,
    )
}
