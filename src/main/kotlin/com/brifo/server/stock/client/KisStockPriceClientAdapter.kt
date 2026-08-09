package com.brifo.server.stock.client

import com.brifo.server.externalapi.kis.KisCurrentPriceClient
import com.brifo.server.externalapi.kis.KisDailyPriceClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "app.data-provider",
    name = ["type"],
    havingValue = "kis",
)
class KisStockPriceClientAdapter(
    private val currentPriceClient: KisCurrentPriceClient,
    private val dailyPriceClient: KisDailyPriceClient,
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
        val result = dailyPriceClient.getDailyPrice(
            stockId = request.stockId,
            stockCode = request.stockCode,
            tradeDate = request.tradeDate,
        )
        return ClosingPriceClient.Result(
            price = result.price,
            changeRate = result.changeRate,
        )
    }
}
