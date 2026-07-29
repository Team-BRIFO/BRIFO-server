package com.brifo.server.stock.cache

import java.math.BigDecimal
import java.time.LocalDateTime

// KIS에서 정상 조회한 현재가를 저장한다.
data class StockCurrentPriceCache(
    val code: String,
    val currentPrice: BigDecimal,
    val priceChange: BigDecimal,
    val changeRate: BigDecimal,
    val fetchedAt: LocalDateTime,
)
