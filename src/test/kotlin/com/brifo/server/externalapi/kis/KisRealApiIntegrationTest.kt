package com.brifo.server.externalapi.kis

import com.brifo.server.externalapi.ExternalApiCallService
import com.brifo.server.externalapi.log.service.ExternalApiCallLogService
import com.brifo.server.externalapi.log.service.ExternalApiCallLogWriter
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.mockito.Mockito.mock
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfEnvironmentVariable(
    named = "KIS_APP_KEY",
    matches = ".+",
)
@EnabledIfEnvironmentVariable(
    named = "KIS_APP_SECRET",
    matches = ".+",
)
class KisRealApiIntegrationTest {
    private val properties =
        KisProperties(
            baseUrl = "https://openapi.koreainvestment.com:9443",
            appKey = requireNotNull(System.getenv("KIS_APP_KEY")),
            appSecret = requireNotNull(System.getenv("KIS_APP_SECRET")),
        )

    private val config = ExternalRestClientConfig()

    private val currentPriceRestClient =
        config.kisCurrentPriceRestClient(properties)

    private val dailyPriceRestClient =
        config.kisDailyPriceRestClient(properties)

    private val tokenProvider =
        KisTokenProvider(
            restClient = currentPriceRestClient,
            properties = properties,
        )

    private val callService =
        ExternalApiCallService(
            logService =
                ExternalApiCallLogService(
                    externalApiCallLogWriter =
                        mock(ExternalApiCallLogWriter::class.java),
                    objectMapper = ObjectMapper(),
                ),
        )

    @Test
    fun `삼성전자 현재가를 실제로 조회한다`() {
        val client =
            KisCurrentPriceClient(
                restClient = currentPriceRestClient,
                properties = properties,
                tokenProvider = tokenProvider,
                externalApiCallService = callService,
            )

        val result =
            client.getCurrentPrice(
                stockId = 1L,
                stockCode = "005930",
            )

        println("종목코드: ${result.stockCode}")
        println("현재가: ${result.currentPrice}")
        println("전일 대비: ${result.priceChange}")
        println("등락률: ${result.changeRate}%")

        assertThat(result.stockCode).isEqualTo("005930")
        assertThat(result.currentPrice)
            .isGreaterThan(BigDecimal.ZERO)
    }

    @Test
    fun `삼성전자 오늘 종가를 실제로 조회한다`() {
        val client =
            KisDailyPriceClient(
                restClient = dailyPriceRestClient,
                properties = properties,
                tokenProvider = tokenProvider,
                externalApiCallService = callService,
            )

        val today = LocalDate.now(ZoneId.of("Asia/Seoul"))

        val result =
            client.getDailyPrice(
                stockId = 1L,
                stockCode = "005930",
                tradeDate = today,
            )

        println("종목코드: ${result.stockCode}")
        println("거래일: ${result.tradeDate}")
        println("종가: ${result.price}")
        println("등락률: ${result.changeRate}%")

        assertThat(result.stockCode).isEqualTo("005930")
        assertThat(result.price)
            .isGreaterThan(BigDecimal.ZERO)
    }
}
