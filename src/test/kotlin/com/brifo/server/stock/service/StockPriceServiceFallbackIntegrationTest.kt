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

    // Keyval(캐시)이 Redis 프로토콜을 사용하므로
    // Spring의 StringRedisTemplate을 사용한다.
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

    // 토큰 발급은 fallback 검증 대상이 아니므로 mock 처리한다.
    @MockitoBean
    private lateinit var tokenProvider:
        KisTokenProvider

    private lateinit var stock: Stock

    @BeforeAll
    fun setUpStock() {
        // 여러 번 실행해도 같은 테스트 종목을 사용한다.
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
        // 실제 KIS 토큰을 발급하지 않는다.
        `when`(tokenProvider.getAccessToken())
            .thenReturn("test-token")

        // 항상 Keyval(캐시) 미스부터 시작한다.
        redisTemplate.delete(CURRENT_PRICE_KEY)
        redisTemplate.delete(LAST_SUCCESS_KEY)

        // 이전 테스트의 종가가 fallback에 사용되지 않게 한다.
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

        // fallback 값을 실제 Keyval(캐시)에 저장한다.
        cacheRepository.saveLastSuccess(lastSuccess)

        // 최초 KIS 요청을 실패시킨다.
        mockKisServer.enqueue(
            MockResponse()
                .setResponseCode(500),
        )

        // 재시도 요청도 실패시킨다.
        mockKisServer.enqueue(
            MockResponse()
                .setResponseCode(500),
        )

        val result =
            stockPriceService.getCurrentPrice(
                stockId = requireNotNull(stock.id),
                stockCode = STOCK_CODE,
            )

        // Keyval(캐시)의 마지막 성공값을 반환했는지 확인한다.
        assertThat(result.priceStatus)
            .isEqualTo(PriceStatus.LAST_SUCCESS)
        assertThat(result.currentPrice)
            .isEqualByComparingTo("79700")
        assertThat(result.fetchedAt)
            .isEqualTo(lastSuccess.fetchedAt)

        // KIS 실패 로그가 실제 DB에 저장됐는지 확인한다.
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

        // 최초 요청 후 한 번 재시도했는지 확인한다.
        assertThat(failureLog.retryCount)
            .isEqualTo(1)
    }

    @Test
    fun `마지막 성공값이 없으면 직전 종가를 반환한다`() {
        // fallback으로 반환할 종가를 실제 DB에 저장한다.
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

        // 최초 KIS 요청과 재시도를 모두 실패시킨다.
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

        // DB의 직전 종가를 반환했는지 확인한다.
        assertThat(result.priceStatus)
            .isEqualTo(PriceStatus.PREVIOUS_CLOSE)
        assertThat(result.currentPrice)
            .isEqualByComparingTo("73900")
        assertThat(result.tradeDate)
            .isEqualTo(previousClose.tradeDate)

        // KIS 실패 로그가 실제 DB에 저장됐는지 확인한다.
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
        // 최초 KIS 요청과 재시도를 모두 실패시킨다.
        mockKisServer.enqueue(
            MockResponse()
                .setResponseCode(500),
        )
        mockKisServer.enqueue(
            MockResponse()
                .setResponseCode(500),
        )

        // Keyval(캐시)과 DB에 fallback 값이 없으므로 예외가 발생한다.
        val exception =
            assertThrows<BusinessException> {
                stockPriceService.getCurrentPrice(
                    stockId = requireNotNull(stock.id),
                    stockCode = STOCK_CODE,
                )
            }

        // 이 오류 코드는 API 계층에서 503으로 변환된다.
        assertThat(exception.errorCode)
            .isEqualTo(
                StockErrorCode.STOCK_PRICE_UNAVAILABLE,
            )

        // fallback 값이 없어도 KIS 실패 로그는 저장돼야 한다.
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

        // 실제 KIS 대신 실패 응답을 반환할 테스트 서버다.
        private val mockKisServer =
            MockWebServer().apply {
                start()
            }

        @JvmStatic
        @DynamicPropertySource
        fun properties(
            registry: DynamicPropertyRegistry,
        ) {
            // KIS 현재가 요청만 테스트 서버로 보낸다.
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
            // 모든 테스트가 끝나면 테스트 서버를 종료한다.
            mockKisServer.shutdown()
        }
    }
}
