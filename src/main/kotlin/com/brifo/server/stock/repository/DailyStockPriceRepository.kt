package com.brifo.server.stock.repository

import com.brifo.server.stock.entity.DailyStockPrice
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface DailyStockPriceRepository : JpaRepository<DailyStockPrice, Long> {
    // 기준일 이전의 가장 최근 종가를 조회한다.
    fun findTopByStockIdAndTradeDateBeforeOrderByTradeDateDesc(
        stockId: Long,
        tradeDate: LocalDate,
    ): DailyStockPrice?
}
