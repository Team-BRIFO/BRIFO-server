package com.brifo.server.stock.repository

import com.brifo.server.stock.entity.DailyStockPrice
import org.springframework.data.jpa.repository.JpaRepository

interface DailyStockPriceRepository : JpaRepository<DailyStockPrice, Long> {
    fun findTopByStockIdOrderByTradeDateDescFetchedAtDescIdDesc(stockId: Long): DailyStockPrice?
}
