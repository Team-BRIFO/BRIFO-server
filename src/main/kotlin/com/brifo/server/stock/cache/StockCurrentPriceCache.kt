package com.brifo.server.stock.cache

import java.math.BigDecimal
import java.time.LocalDateTime

data class StockCurrentPriceCache(
    val code: String,
    val currentPrice: BigDecimal,
    val priceChange: BigDecimal?,
    val changeRate: BigDecimal,
    val fetchedAt: LocalDateTime,
)
