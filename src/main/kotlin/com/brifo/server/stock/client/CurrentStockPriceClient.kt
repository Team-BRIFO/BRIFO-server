package com.brifo.server.stock.client

import java.math.BigDecimal

interface CurrentStockPriceClient {
    fun getCurrentPrice(request: Request): Result

    fun getCurrentPrice(stockId: Long, stockCode: String): Result = getCurrentPrice(Request(stockId, stockCode))

    data class Request(val stockId: Long, val stockCode: String)
    data class Result(
        val stockCode: String,
        val currentPrice: BigDecimal,
        val priceChange: BigDecimal?,
        val changeRate: BigDecimal,
    )
}
