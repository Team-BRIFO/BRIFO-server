package com.brifo.server.stock.service

import com.brifo.server.TestcontainersConfiguration
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
@Import(
    BriefingTestConfiguration::class,
    TestcontainersConfiguration::class,
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
        redisTemplate.delete(CURRENT_PRICE_KEY)
        redisTemplate.delete(LAST_SUCCESS_KEY)
    }

    @AfterEach
    fun cleanCache() {
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

        assertThat(firstResult.priceStatus)
            .isEqualTo(
                PriceStatus.DELAYED_CURRENT,
            )
        assertThat(firstResult.currentPrice)
            .isPositive()

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

        assertThat(secondResult.currentPrice)
            .isEqualByComparingTo(
                firstResult.currentPrice,
            )

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
            registry.add(
                "external.kis.base-url",
            ) {
                "https://openapi.koreainvestment.com:9443"
            }
        }
    }
}
