package com.brifo.server.externalapi.kis

import com.brifo.server.externalapi.ExternalApiCallContext
import com.brifo.server.externalapi.ExternalApiCallPolicy
import com.brifo.server.externalapi.ExternalApiCallService
import com.brifo.server.externalapi.idempotency.ExternalApiIdempotencyKeyGenerator
import com.brifo.server.externalapi.kis.dto.KisCurrentPriceResponse
import com.brifo.server.externalapi.kis.dto.KisCurrentPriceResult
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class KisCurrentPriceClient(
    @Qualifier("kisCurrentPriceRestClient")
    private val restClient: RestClient,
    private val properties: KisProperties,
    private val tokenProvider: KisTokenProvider,
    private val externalApiCallService: ExternalApiCallService,
) {
    fun getCurrentPrice(
        stockId: Long,
        stockCode: String,
    ): KisCurrentPriceResult {
        val key =
            ExternalApiIdempotencyKeyGenerator.kisCurrentPrice(stockCode)

        val response = externalApiCallService.execute(
            provider = "KIS",
            apiName = "KIS_CURRENT_PRICE",
            policy = ExternalApiCallPolicy.KIS_CURRENT_PRICE,
            retryEnabled = true,
            context = ExternalApiCallContext(
                idempotencyKey = key,
                stockId = stockId,
            ),
            requestPayload = mapOf(
                "stockCode" to stockCode,
            ),
            refreshToken = {
                tokenProvider.refresh()
            },
            request = {
                val result = restClient
                    .get()
                    .uri { builder ->
                        builder
                            .path("/uapi/domestic-stock/v1/quotations/inquire-price")
                            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                            .queryParam("FID_INPUT_ISCD", stockCode)
                            .build()
                    }
                    .header(
                        "authorization",
                        "Bearer ${tokenProvider.getAccessToken()}",
                    )
                    .header("appkey", properties.appKey)
                    .header("appsecret", properties.appSecret)
                    .header("tr_id", "FHKST01010100")
                    .retrieve()
                    .toEntity(KisCurrentPriceResponse::class.java)

                val body = checkNotNull(result.body) {
                    "KIS current price response body is empty"
                }

                check(body.resultCode == "0" && body.output != null) {
                    body.message ?: "KIS current price request failed"
                }

                result
            },
        )

        val output = checkNotNull(response.output)

        return KisCurrentPriceResult(
            stockCode = stockCode,
            currentPrice = output.currentPrice.toBigDecimal(),
            priceChange = output.priceChange.toBigDecimal(),
            changeRate = output.changeRate.toBigDecimal(),
        )
    }
}
