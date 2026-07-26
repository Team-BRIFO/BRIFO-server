package com.brifo.server.stock.repository

import com.brifo.server.stock.dto.response.GetStocksResponse
import java.util.UUID

interface StockQueryRepository {
    fun findPopularStocks(): List<GetStocksResponse.Item>

    fun searchStocks(
        keyword: String,
        cursor: UUID?,
        limit: Int,
    ): List<GetStocksResponse.Item>
}
