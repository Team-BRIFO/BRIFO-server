package com.brifo.server.stock.dto.response

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

// 현재가 조회 결과를 구분한다.
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

    // 마지막 성공값의 조회 시각이다.
    val fetchedAt: LocalDateTime? = null,

    // 직전 종가의 거래일이다.
    val tradeDate: LocalDate? = null,
)
