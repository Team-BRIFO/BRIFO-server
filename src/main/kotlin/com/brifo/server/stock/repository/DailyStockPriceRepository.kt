package com.brifo.server.stock.repository

import com.brifo.server.stock.entity.DailyStockPrice
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface DailyStockPriceRepository : JpaRepository<DailyStockPrice, Long> {
    fun findTopByStockIdAndTradeDateBeforeOrderByTradeDateDesc(
        stockId: Long,
        tradeDate: LocalDate,
    ): DailyStockPrice?
  
    fun findTopByStockIdOrderByTradeDateDescFetchedAtDescIdDesc(stockId: Long): DailyStockPrice?
}
