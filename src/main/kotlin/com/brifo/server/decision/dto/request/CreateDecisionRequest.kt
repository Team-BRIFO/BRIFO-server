package com.brifo.server.decision.dto.request

import com.brifo.server.decision.entity.DecisionDirection
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class CreateDecisionRequest(
    val direction: DecisionDirection,
    @field:Min(1)
    @field:Max(5)
    val confidenceLevel: Int,
)
