package com.brifo.server.stock.service

import com.brifo.server.briefing.support.BriefingTestConfiguration
import com.brifo.server.externalapi.log.entity.ExternalApiCallStatus
import com.brifo.server.externalapi.log.repository.ExternalApiCallLogRepository
import com.brifo.server.stock.dto.response.PriceStatus
import com.brifo.server.stock.entity.Stock
import com.brifo.server.stock.repository.StockRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@SpringBootTest
@ActiveProfiles("test")
@Import(BriefingTestConfiguration::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfEnvironmentVariable(
    named = "DB_URL",
    matches = ".+",
)
@EnabledIfEnvironmentVariable(
    named = "DB_USERNAME",
    matches = ".+",
)
@EnabledIfEnvironmentVariable(
    named = "DB_PASSWORD",
    matches = ".+",
)
@EnabledIfEnvironmentVariable(
    named = "KIS_APP_KEY",
    matches = ".+",
)
@EnabledIfEnvironmentVariable(
    named = "KIS_APP_SECRET",
    matches = ".+",
)
class StockPriceServiceRealKisIntegrationTest {
    @Autowired
    private lateinit var stockPriceService:
        StockPriceService

    @Autowired
    private lateinit var redisTemplate:
        StringRedisTemplate

    @Autowired
    private lateinit var stockRepository:
        StockRepository

    @Autowired
    private lateinit var logRepository:
        ExternalApiCallLogRepository

    private lateinit var stock: Stock

    @BeforeAll
    fun setUpStock() {
        // 기존 삼성전자 종목이 있으면 그대로 사용한다.
        stock =
            stockRepository.findAll()
                .firstOrNull {
                    it.code == STOCK_CODE
                }
                ?: stockRepository.saveAndFlush(
                    Stock.create(
                        code = STOCK_CODE,
                        name = "삼성전자 테스트",
                        sector = "반도체",
                    ),
                )
    }

    @BeforeEach
    fun clearCache() {
        // 첫 번째 요청이 실제 KIS를 호출하도록 비운다.
        redisTemplate.delete(CURRENT_PRICE_KEY)
        redisTemplate.delete(LAST_SUCCESS_KEY)
    }

    @AfterEach
    fun cleanCache() {
        // 테스트가 만든 캐시만 삭제한다.
        redisTemplate.delete(CURRENT_PRICE_KEY)
        redisTemplate.delete(LAST_SUCCESS_KEY)
    }

    @Test
    fun `실제 KIS 현재가를 조회하고 두 번째 요청은 캐시를 사용한다`() {
        val firstResult =
            stockPriceService.getCurrentPrice(
                stockId = requireNotNull(stock.id),
                stockCode = stock.code,
            )

        val savedLog =
            logRepository.findAll()
                .lastOrNull {
                    it.apiName == "KIS_CURRENT_PRICE" &&
                        it.stockId == stock.id
                }

        val firstLogCount =
            logRepository.findAll()
                .count {
                    it.apiName == "KIS_CURRENT_PRICE" &&
                        it.stockId == stock.id
                }

        // 첫 번째 요청은 실제 KIS 결과를 반환한다.
        assertThat(firstResult.priceStatus)
            .isEqualTo(
                PriceStatus.DELAYED_CURRENT,
            )
        assertThat(firstResult.currentPrice)
            .isPositive()

        // 실제 KIS 성공 로그가 DB에 저장된다.
        assertThat(savedLog)
            .isNotNull
        assertThat(savedLog?.provider)
            .isEqualTo("KIS")
        assertThat(savedLog?.status)
            .isEqualTo(
                ExternalApiCallStatus.SUCCESS,
            )
        assertThat(savedLog?.responseStatusCode)
            .isEqualTo(200)

        val secondResult =
            stockPriceService.getCurrentPrice(
                stockId = requireNotNull(stock.id),
                stockCode = stock.code,
            )

        val secondLogCount =
            logRepository.findAll()
                .count {
                    it.apiName == "KIS_CURRENT_PRICE" &&
                        it.stockId == stock.id
                }

        // 두 번째 요청은 첫 번째 요청과 같은 값을 반환한다.
        assertThat(secondResult.currentPrice)
            .isEqualByComparingTo(
                firstResult.currentPrice,
            )

        // 캐시 hit이면 KIS 호출 로그가 늘어나지 않는다.
        assertThat(secondLogCount)
            .isEqualTo(firstLogCount)
    }

    companion object {
        private const val STOCK_CODE =
            "005930"

        private const val CURRENT_PRICE_KEY =
            "stock:delayed-price:$STOCK_CODE"

        private const val LAST_SUCCESS_KEY =
            "stock:last-success-price:$STOCK_CODE"

        @JvmStatic
        @DynamicPropertySource
        fun properties(
            registry: DynamicPropertyRegistry,
        ) {
            // 실행 중인 PostgreSQL 연결 정보를 등록한다.
            registry.add(
                "spring.datasource.url",
            ) {
                requireNotNull(
                    System.getenv("DB_URL"),
                )
            }
            registry.add(
                "spring.datasource.username",
            ) {
                requireNotNull(
                    System.getenv("DB_USERNAME"),
                )
            }
            registry.add(
                "spring.datasource.password",
            ) {
                requireNotNull(
                    System.getenv("DB_PASSWORD"),
                )
            }

            // 테스트 프로필의 KIS 주소를 실제 주소로 바꾼다.
            registry.add(
                "external.kis.base-url",
            ) {
                "https://openapi.koreainvestment.com:9443"
            }
        }
    }
}
