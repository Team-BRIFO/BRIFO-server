package com.brifo.server.decision.dto.response

import com.brifo.server.decision.entity.DecisionDirection
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class GetDecisionsResponse(
    val items: List<Item>,
) {
    data class Item(
        val decisionId: UUID,
        val direction: DecisionDirection,
        val confidenceLevel: Int,
        val isSettled: Boolean,
        val stock: Stock,
    )

    data class Stock(
        val stockId: UUID,
        val name: String,
        val changeRate: BigDecimal,
        val tradeDate: LocalDate,
    )
}
