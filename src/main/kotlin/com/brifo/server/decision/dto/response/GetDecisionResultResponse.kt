package com.brifo.server.decision.dto.response

import com.brifo.server.decision.entity.DecisionDirection
import java.math.BigDecimal
import java.time.LocalDate

data class GetDecisionResultResponse(
    val isCorrect: Boolean?,
    val apDelta: Int?,
    val direction: DecisionDirection,
    val confidenceLevel: Int,
    val stock: Stock,
) {
    data class Stock(
        val name: String,
        val price: BigDecimal?,
        val changeRate: BigDecimal?,
        val tradeDate: LocalDate?,
    )
}
