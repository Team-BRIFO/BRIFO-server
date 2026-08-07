package com.brifo.server.briefing.dto.response

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class BriefingStockResponse(
    val stockId: UUID,
    val name: String,
    val logoUrl: String?,
    val price: BigDecimal,
    val changeRate: BigDecimal,
    val tradeDate: LocalDate,
)
