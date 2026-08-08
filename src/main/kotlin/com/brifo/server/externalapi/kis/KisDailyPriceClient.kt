package com.brifo.server.externalapi.kis

import com.brifo.server.externalapi.ExternalApiCallContext
import com.brifo.server.externalapi.ExternalApiCallPolicy
import com.brifo.server.externalapi.ExternalApiCallService
import com.brifo.server.externalapi.idempotency.ExternalApiIdempotencyKeyGenerator
import com.brifo.server.externalapi.kis.dto.KisDailyPriceResponse
import com.brifo.server.externalapi.kis.dto.KisDailyPriceResult
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
@ConditionalOnProperty(prefix = "app.stock-price", name = ["provider"], havingValue = "kis")
class KisDailyPriceClient(
    @Qualifier("kisDailyPriceRestClient")
    private val restClient: RestClient,
    private val properties: KisProperties,
    private val tokenProvider: KisTokenProvider,
    private val externalApiCallService: ExternalApiCallService,
) {
    fun getDailyPrice(
        stockId: Long,
        stockCode: String,
        tradeDate: LocalDate,
    ): KisDailyPriceResult {
        val key =
            ExternalApiIdempotencyKeyGenerator.stockPrice(
                stockCode = stockCode,
                tradeDate = tradeDate,
            )

        val date = tradeDate.format(DateTimeFormatter.BASIC_ISO_DATE)

        val response = externalApiCallService.execute(
            provider = "KIS",
            apiName = "KIS_DAILY_PRICE",
            policy = ExternalApiCallPolicy.STOCK_PRICE,
            retryEnabled = true,
            context = ExternalApiCallContext(
                idempotencyKey = key,
                stockId = stockId,
            ),
            requestPayload = mapOf(
                "stockCode" to stockCode,
                "tradeDate" to tradeDate,
            ),
            refreshToken = {
                tokenProvider.refresh()
            },
            request = {
                val result = restClient
                    .get()
                    .uri { builder ->
                        builder
                            .path("/uapi/domestic-stock/v1/quotations/inquire-daily-price")
                            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                            .queryParam("FID_INPUT_ISCD", stockCode)
                            .queryParam("FID_PERIOD_DIV_CODE", "D")
                            .queryParam("FID_ORG_ADJ_PRC", "0")
                            .build()
                    }
                    .header(
                        "authorization",
                        "Bearer ${tokenProvider.getAccessToken()}",
                    )
                    .header("appkey", properties.appKey)
                    .header("appsecret", properties.appSecret)
                    .header("tr_id", "FHKST01010400")
                    .retrieve()
                    .toEntity<KisDailyPriceResponse>()

                val body = checkNotNull(result.body) {
                    "KIS daily price response body is empty"
                }

                check(body.resultCode == "0") {
                    body.message ?: "KIS daily price request failed"
                }

                result
            },
        )

        val output = response.output.firstOrNull {
            it.tradeDate == date
        } ?: error("KIS daily price was not found: $tradeDate")

        return KisDailyPriceResult(
            stockCode = stockCode,
            tradeDate = tradeDate,
            price = output.closingPrice.toBigDecimal(),
            changeRate = output.changeRate.toBigDecimal(),
        )
    }
}
