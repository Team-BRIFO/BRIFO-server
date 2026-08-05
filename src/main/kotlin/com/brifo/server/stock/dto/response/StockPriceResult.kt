package com.brifo.server.stock.dto.response

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

enum class PriceStatus {
    DELAYED_CURRENT,
    LAST_SUCCESS,
    PREVIOUS_CLOSE,
}

data class StockPriceResult(
    val stockCode: String,
    val currentPrice: BigDecimal,
    val priceChange: BigDecimal?,
    val changeRate: BigDecimal?,
    val priceStatus: PriceStatus,

    val fetchedAt: LocalDateTime? = null,

    val tradeDate: LocalDate? = null,
)
