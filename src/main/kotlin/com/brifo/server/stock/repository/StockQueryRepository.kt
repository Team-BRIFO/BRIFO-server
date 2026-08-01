package com.brifo.server.stock.repository

import com.brifo.server.stock.dto.response.GetStocksResponse
import java.util.UUID

interface StockQueryRepository {
    fun findPopularStocks(): List<GetStocksResponse.StockItem>

    fun searchStocks(
        keyword: String,
        cursor: UUID?,
        limit: Int,
    ): List<GetStocksResponse.StockItem>
}
