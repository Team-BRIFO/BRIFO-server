package com.brifo.server.stock.service

import com.brifo.server.TestcontainersConfiguration
import com.brifo.server.briefing.support.BriefingTestConfiguration
import com.brifo.server.externalapi.kis.KisTokenProvider
import com.brifo.server.externalapi.log.entity.ExternalApiCallStatus
import com.brifo.server.externalapi.log.repository.ExternalApiCallLogRepository
import com.brifo.server.global.exception.BusinessException
import com.brifo.server.stock.cache.StockCurrentPriceCache
import com.brifo.server.stock.cache.StockCurrentPriceCacheRepository
import com.brifo.server.stock.code.StockErrorCode
import com.brifo.server.stock.dto.response.PriceStatus
import com.brifo.server.stock.entity.DailyStockPrice
import com.brifo.server.stock.entity.Stock
import com.brifo.server.stock.repository.DailyStockPriceRepository
import com.brifo.server.stock.repository.StockRepository
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles("test")
@Import(
    BriefingTestConfiguration::class,
    TestcontainersConfiguration::class,
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StockPriceServiceFallbackIntegrationTest {
    @Autowired
    private lateinit var stockPriceService:
        StockPriceService

    @Autowired
    private lateinit var cacheRepository:
        StockCurrentPriceCacheRepository

    @Autowired
    private lateinit var redisTemplate:
        StringRedisTemplate

    @Autowired
    private lateinit var stockRepository:
        StockRepository

    @Autowired
    private lateinit var dailyPriceRepository:
        DailyStockPriceRepository

    @Autowired
    private lateinit var logRepository:
        ExternalApiCallLogRepository

    @MockitoBean
    private lateinit var tokenProvider:
        KisTokenProvider

    private lateinit var stock: Stock

    @BeforeAll
    fun setUpStock() {
        stock =
            stockRepository.findAll()
                .firstOrNull {
                    it.code == STOCK_CODE
                }
                ?: stockRepository.saveAndFlush(
                    Stock.create(
                        code = STOCK_CODE,
                        name = "Fallback 테스트 종목",
                        sector = "테스트",
                    ),
                )
    }

    @BeforeEach
    fun setUp() {
        `when`(tokenProvider.getAccessToken())
            .thenReturn("test-token")

        redisTemplate.delete(CURRENT_PRICE_KEY)
        redisTemplate.delete(LAST_SUCCESS_KEY)

        dailyPriceRepository.deleteAll(
            dailyPriceRepository.findAll()
                .filter {
                    it.stock.id == stock.id
                },
        )
    }

    @Test
    fun `KIS 실패 시 마지막 성공값을 반환한다`() {
        val lastSuccess =
            StockCurrentPriceCache(
                code = STOCK_CODE,
                currentPrice = BigDecimal("79700"),
                priceChange = BigDecimal("5800"),
                changeRate = BigDecimal("7.9"),
                fetchedAt =
                    LocalDateTime.of(
                        2026,
                        7,
                        29,
                        10,
                        0,
                    ),
            )

        cacheRepository.saveLastSuccess(lastSuccess)

        mockKisServer.enqueue(
            MockResponse()
                .setResponseCode(500),
        )

        mockKisServer.enqueue(
            MockResponse()
                .setResponseCode(500),
        )

        val result =
            stockPriceService.getCurrentPrice(
                stockId = requireNotNull(stock.id),
                stockCode = STOCK_CODE,
            )

        assertThat(result.priceStatus)
            .isEqualTo(PriceStatus.LAST_SUCCESS)
        assertThat(result.currentPrice)
            .isEqualByComparingTo("79700")
        assertThat(result.fetchedAt)
            .isEqualTo(lastSuccess.fetchedAt)

        val failureLog =
            logRepository.findAll()
                .last {
                    it.apiName == "KIS_CURRENT_PRICE" &&
                        it.stockId == stock.id
                }

        assertThat(failureLog.status)
            .isEqualTo(ExternalApiCallStatus.FAIL)
        assertThat(failureLog.responseStatusCode)
            .isEqualTo(500)

        assertThat(failureLog.retryCount)
            .isEqualTo(1)
    }

    @Test
    fun `마지막 성공값이 없으면 직전 종가를 반환한다`() {
        val previousClose =
            dailyPriceRepository.saveAndFlush(
                DailyStockPrice.create(
                    stock = stock,
                    tradeDate =
                        LocalDate.now()
                            .minusDays(1),
                    price = BigDecimal("73900"),
                    changeRate = BigDecimal("1.2"),
                ),
            )

        mockKisServer.enqueue(
            MockResponse()
                .setResponseCode(500),
        )
        mockKisServer.enqueue(
            MockResponse()
                .setResponseCode(500),
        )

        val result =
            stockPriceService.getCurrentPrice(
                stockId = requireNotNull(stock.id),
                stockCode = STOCK_CODE,
            )

        assertThat(result.priceStatus)
            .isEqualTo(PriceStatus.PREVIOUS_CLOSE)
        assertThat(result.currentPrice)
            .isEqualByComparingTo("73900")
        assertThat(result.tradeDate)
            .isEqualTo(previousClose.tradeDate)

        val failureLog =
            logRepository.findAll()
                .last {
                    it.apiName == "KIS_CURRENT_PRICE" &&
                        it.stockId == stock.id
                }

        assertThat(failureLog.status)
            .isEqualTo(ExternalApiCallStatus.FAIL)
        assertThat(failureLog.responseStatusCode)
            .isEqualTo(500)
    }

    @Test
    fun `사용 가능한 fallback이 없으면 503 예외를 던진다`() {
        mockKisServer.enqueue(
            MockResponse()
                .setResponseCode(500),
        )
        mockKisServer.enqueue(
            MockResponse()
                .setResponseCode(500),
        )

        val exception =
            assertThrows<BusinessException> {
                stockPriceService.getCurrentPrice(
                    stockId = requireNotNull(stock.id),
                    stockCode = STOCK_CODE,
                )
            }

        assertThat(exception.errorCode)
            .isEqualTo(
                StockErrorCode.STOCK_PRICE_UNAVAILABLE,
            )

        val failureLog =
            logRepository.findAll()
                .last {
                    it.apiName == "KIS_CURRENT_PRICE" &&
                        it.stockId == stock.id
                }

        assertThat(failureLog.status)
            .isEqualTo(ExternalApiCallStatus.FAIL)
        assertThat(failureLog.responseStatusCode)
            .isEqualTo(500)
    }

    companion object {
        private const val STOCK_CODE =
            "FALLBACK"

        private const val CURRENT_PRICE_KEY =
            "stock:delayed-price:$STOCK_CODE"

        private const val LAST_SUCCESS_KEY =
            "stock:last-success-price:$STOCK_CODE"

        private val mockKisServer =
            MockWebServer().apply {
                start()
            }

        @JvmStatic
        @DynamicPropertySource
        fun properties(
            registry: DynamicPropertyRegistry,
        ) {
            registry.add(
                "external.kis.base-url",
            ) {
                mockKisServer.url("/")
                    .toString()
            }
        }

        @JvmStatic
        @AfterAll
        fun stopServer() {
            mockKisServer.shutdown()
        }
    }
}
