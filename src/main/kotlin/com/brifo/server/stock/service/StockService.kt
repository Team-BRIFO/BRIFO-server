package com.brifo.server.stock.service

import com.brifo.server.global.common.CursorPage
import com.brifo.server.stock.dto.request.GetStocksRequest
import com.brifo.server.stock.dto.response.GetStocksResponse
import com.brifo.server.stock.repository.StockRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.RoundingMode

@Service
class StockService(
    private val stockRepository: StockRepository,
) {
    @Transactional(readOnly = true)
    fun getStocks(request: GetStocksRequest): GetStocksResponse {
        val keyword = request.keyword?.trim().orEmpty()

        if (keyword.isBlank()) {
            val items =
                stockRepository.findPopularStocks().mapIndexed { index, stock ->
                    stock.copy(
                        rank = index + 1,
                        changeRate = stock.changeRate?.setScale(1, RoundingMode.HALF_UP),
                    )
                }

            return GetStocksResponse(
                mode = GetStocksResponse.Mode.POPULAR,
                keyword = null,
                page =
                    CursorPage(
                        items = items,
                        nextCursor = null,
                        hasNext = false,
                    ),
            )
        }

        val stocks =
            stockRepository.searchStocks(
                keyword = keyword,
                cursor = request.cursor,
                limit = request.size + 1,
            )

        val hasNext = stocks.size > request.size
        val items =
            stocks
                .take(request.size)
                .map { stock ->
                    stock.copy(
                        changeRate = stock.changeRate?.setScale(1, RoundingMode.HALF_UP),
                    )
                }

        return GetStocksResponse(
            mode = GetStocksResponse.Mode.SEARCH,
            keyword = keyword,
            page =
                CursorPage(
                    items = items,
                    nextCursor = if (hasNext) items.last().stockId else null,
                    hasNext = hasNext,
                ),
        )
    }
}
