package com.brifo.server.news.repository

import com.brifo.server.stock.entity.DailyStockPrice
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface NewsDailyStockPriceRepository : JpaRepository<DailyStockPrice, Long> {
    fun findTopByStockIdAndTradeDateLessThanEqualOrderByTradeDateDescFetchedAtDescIdDesc(
        stockId: Long,
        tradeDate: LocalDate,
    ): DailyStockPrice?
}
