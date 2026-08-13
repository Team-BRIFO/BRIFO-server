package com.brifo.server.externalapi.kis

import com.brifo.server.externalapi.ExternalApiCallService
import com.brifo.server.externalapi.log.service.ExternalApiCallLogService
import com.brifo.server.stock.client.ClosingPriceClient
import com.brifo.server.stock.client.CurrentStockPriceClient
import com.brifo.server.stock.client.KisStockPriceClientAdapter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.mockito.Mockito.mock
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

@EnabledIfEnvironmentVariable(named = "KIS_APP_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "KIS_APP_SECRET", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KisRealApiIntegrationTest {
    private lateinit var stockPriceClient: KisStockPriceClientAdapter

    @BeforeAll
    fun setUp() {
        val properties =
            KisProperties(
                baseUrl = "https://openapi.koreainvestment.com:9443",
                appKey = requireNotNull(System.getenv("KIS_APP_KEY")),
                appSecret = requireNotNull(System.getenv("KIS_APP_SECRET")),
            )
        val config = ExternalRestClientConfig()
        val currentPriceRestClient = config.kisCurrentPriceRestClient(properties)
        val dailyPriceRestClient = config.kisDailyPriceRestClient(properties)
        val tokenProvider = KisTokenProvider(currentPriceRestClient, properties)
        val callService = ExternalApiCallService(mock(ExternalApiCallLogService::class.java))

        stockPriceClient =
            KisStockPriceClientAdapter(
                currentPriceClient =
                    KisCurrentPriceClient(
                        currentPriceRestClient,
                        properties,
                        tokenProvider,
                        callService,
                    ),
                dailyPriceClient =
                    KisDailyPriceClient(
                        dailyPriceRestClient,
                        properties,
                        tokenProvider,
                        callService,
                    ),
            )
    }

    @Test
    fun `삼성전자 현재가를 실제로 조회한다`() {
        val result = stockPriceClient.getCurrentPrice(CurrentStockPriceClient.Request(1L, STOCK_CODE))

        println("===== KIS 현재가 조회 결과 =====")
        println("종목 코드: ${result.stockCode}")
        println("현재가: ${result.currentPrice}")
        println("전일 대비: ${result.priceChange}")
        println("등락률: ${result.changeRate}%")
        println("=============================")

        assertThat(result.stockCode).isEqualTo(STOCK_CODE)
        assertThat(result.currentPrice).isGreaterThan(BigDecimal.ZERO)
    }

    @Test
    fun `삼성전자 최근 종가를 실제로 조회한다`() {
        val tradeDate = previousWeekday(LocalDate.now(ZoneId.of("Asia/Seoul")))
        val result = stockPriceClient.getClosingPrice(ClosingPriceClient.Request(1L, STOCK_CODE, tradeDate))

        println("===== KIS 종가 조회 결과 =====")
        println("종목 코드: $STOCK_CODE")
        println("거래일: $tradeDate")
        println("종가: ${result.price}")
        println("등락률: ${result.changeRate}%")
        println("============================")

        assertThat(result.price).isGreaterThan(BigDecimal.ZERO)
    }

    private fun previousWeekday(today: LocalDate): LocalDate {
        var date = today.minusDays(1)
        while (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) {
            date = date.minusDays(1)
        }
        return date
    }

    private companion object {
        const val STOCK_CODE = "005930"
    }
}
