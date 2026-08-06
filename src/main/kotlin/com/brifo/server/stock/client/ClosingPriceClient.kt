package com.brifo.server.stock.client

import java.math.BigDecimal
import java.time.LocalDate

interface ClosingPriceClient {
    fun getClosingPrice(request: Request): Result

    data class Request(
        val stockCode: String,
        val tradeDate: LocalDate,
    )

    data class Result(
        val price: BigDecimal,
        val changeRate: BigDecimal,
    )
}
