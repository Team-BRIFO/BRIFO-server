package com.brifo.server.stock.client

import com.brifo.server.externalapi.kis.KisCurrentPriceClient
import com.brifo.server.externalapi.kis.KisDailyPriceClient
import com.brifo.server.stock.repository.StockRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "app.stock-price",
    name = ["provider"],
    havingValue = "kis",
)
class KisStockPriceClientAdapter(
    private val currentPriceClient: KisCurrentPriceClient,
    private val dailyPriceClient: KisDailyPriceClient,
    private val stockRepository: StockRepository,
) : CurrentStockPriceClient, ClosingPriceClient {
    override fun getCurrentPrice(request: CurrentStockPriceClient.Request): CurrentStockPriceClient.Result {
        val result = currentPriceClient.getCurrentPrice(request.stockId, request.stockCode)
        return CurrentStockPriceClient.Result(
            stockCode = result.stockCode,
            currentPrice = result.currentPrice,
            priceChange = result.priceChange,
            changeRate = result.changeRate,
        )
    }

    override fun getClosingPrice(request: ClosingPriceClient.Request): ClosingPriceClient.Result {
        val stockId = checkNotNull(stockRepository.findByCode(request.stockCode)?.id) {
            "KIS 종가 조회 대상 종목이 없습니다: ${request.stockCode}"
        }
        val result = dailyPriceClient.getDailyPrice(
            stockId = stockId,
            stockCode = request.stockCode,
            tradeDate = request.tradeDate,
        )
        return ClosingPriceClient.Result(
            price = result.price,
            changeRate = result.changeRate,
        )
    }
}
