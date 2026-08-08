package com.brifo.server.stock.client

import com.brifo.server.externalapi.ExternalApiCallContext
import com.brifo.server.externalapi.ExternalApiCallPolicy
import com.brifo.server.externalapi.ExternalApiCallService
import com.brifo.server.externalapi.dataserver.DataServerPriceResult
import com.brifo.server.externalapi.dataserver.DataServerResponse
import com.brifo.server.externalapi.idempotency.ExternalApiIdempotencyKeyGenerator
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.Clock
import java.time.LocalDate

@Component
@ConditionalOnProperty(
    prefix = "app.stock-price",
    name = ["provider"],
    havingValue = "data-server",
    matchIfMissing = true,
)
class DataServerStockPriceClient(
    @Qualifier("dataServerRestClient") private val restClient: RestClient,
    private val externalApiCallService: ExternalApiCallService,
    private val clock: Clock,
) : CurrentStockPriceClient, ClosingPriceClient {
    override fun getCurrentPrice(request: CurrentStockPriceClient.Request): CurrentStockPriceClient.Result {
        val price = getPrice(request.stockId, request.stockCode, LocalDate.now(clock))
        return CurrentStockPriceClient.Result(request.stockCode, price.price, null, price.changeRate)
    }

    override fun getClosingPrice(request: ClosingPriceClient.Request): ClosingPriceClient.Result {
        val price = getPrice(null, request.stockCode, request.tradeDate)
        return ClosingPriceClient.Result(price.price, price.changeRate)
    }

    private fun getPrice(stockId: Long?, stockCode: String, date: LocalDate): com.brifo.server.externalapi.dataserver.DataServerStockPrice {
        val response = externalApiCallService.execute(
            provider = "DATA_SERVER",
            apiName = "STOCK_PRICE",
            policy = ExternalApiCallPolicy.STOCK_PRICE,
            retryEnabled = true,
            context = ExternalApiCallContext(
                idempotencyKey = ExternalApiIdempotencyKeyGenerator.stockPrice(stockCode, date),
                stockId = stockId,
            ),
            requestPayload = mapOf("stockCode" to stockCode, "date" to date),
        ) {
            restClient.get()
                .uri { it.path("/api/stocks/{stockCode}/prices").queryParam("date", date).build(stockCode) }
                .retrieve()
                .toEntity(object : ParameterizedTypeReference<DataServerResponse<DataServerPriceResult>>() {})
                .also { entity ->
                    val body = checkNotNull(entity.body) { "데이터 서버 주가 응답 본문이 없습니다." }
                    check(body.success) { "${body.code}: ${body.message}" }
                    checkNotNull(body.result) { "${body.code}: 주가 조회 결과가 없습니다." }
                }
        }
        return checkNotNull(response.result).stockPrice
    }
}
