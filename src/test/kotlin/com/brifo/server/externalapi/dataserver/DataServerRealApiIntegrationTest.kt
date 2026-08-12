package com.brifo.server.externalapi.dataserver

import com.brifo.server.externalapi.ExternalApiCallService
import com.brifo.server.externalapi.log.service.ExternalApiCallLogService
import com.brifo.server.news.client.DataServerDisclosureClient
import com.brifo.server.news.client.DisclosureClient
import com.brifo.server.stock.client.ClosingPriceClient
import com.brifo.server.stock.client.CurrentStockPriceClient
import com.brifo.server.stock.client.DataServerStockPriceClient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.mockito.Mockito.mock
import org.springframework.web.client.RestClient
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@EnabledIfEnvironmentVariable(named = "DATA_SERVER_REAL_API_TEST", matches = "true")
class DataServerRealApiIntegrationTest {
    private lateinit var stockPriceClient: DataServerStockPriceClient
    private lateinit var disclosureClient: DataServerDisclosureClient

    private val stockCode = requiredEnvironment("DATA_SERVER_TEST_STOCK_CODE")
    private val testDate = LocalDate.parse(requiredEnvironment("DATA_SERVER_TEST_DATE"))

    @BeforeEach
    fun setUp() {
        val restClient = RestClient.builder()
            .baseUrl(requiredEnvironment("DATA_SERVER_BASE_URL"))
            .build()
        val callService = ExternalApiCallService(mock(ExternalApiCallLogService::class.java))
        val clock = Clock.fixed(
            testDate.atTime(LocalTime.NOON).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
            ZoneId.of("Asia/Seoul"),
        )

        stockPriceClient = DataServerStockPriceClient(restClient, callService, clock)
        disclosureClient = DataServerDisclosureClient(restClient, callService)
    }

    @Test
    fun `데이터 서버의 현재가와 종가를 조회한다`() {
        val current = stockPriceClient.getCurrentPrice(CurrentStockPriceClient.Request(1L, stockCode))
        val closing = stockPriceClient.getClosingPrice(ClosingPriceClient.Request(1L, stockCode, testDate))

        assertEquals(stockCode, current.stockCode)
        assertTrue(current.currentPrice.signum() > 0)
        assertNotNull(current.changeRate)
        assertTrue(closing.price.signum() > 0)
    }

    @Test
    fun `데이터 서버의 공시 여부를 조회한다`() {
        disclosureClient.exists(
            DisclosureClient.Request(
                stockCode = stockCode,
                date = testDate,
            ),
        )
    }

    private fun requiredEnvironment(name: String): String =
        requireNotNull(System.getenv(name)) { "$name 환경변수가 필요합니다." }
}
