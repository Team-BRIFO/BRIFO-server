package com.brifo.server.stock.service

import com.brifo.server.externalapi.kis.KisCurrentPriceClient
import com.brifo.server.externalapi.kis.KisDailyPriceClient
import com.brifo.server.externalapi.kis.dto.KisCurrentPriceResult
import com.brifo.server.global.exception.BusinessException
import com.brifo.server.stock.cache.StockCurrentPriceCache
import com.brifo.server.stock.cache.StockCurrentPriceCacheRepository
import com.brifo.server.stock.code.StockErrorCode
import com.brifo.server.stock.dto.response.PriceStatus
import com.brifo.server.stock.entity.DailyStockPrice
import com.brifo.server.stock.repository.DailyStockPriceRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class StockPriceServiceTest {
    private lateinit var kisCurrentPriceClient: KisCurrentPriceClient
    private lateinit var cacheRepository: StockCurrentPriceCacheRepository
    private lateinit var dailyPriceRepository: DailyStockPriceRepository
    private lateinit var service: StockPriceService

    @BeforeEach
    fun setUp() {
        kisCurrentPriceClient =
            mock(KisCurrentPriceClient::class.java)

        cacheRepository =
            mock(StockCurrentPriceCacheRepository::class.java)

        dailyPriceRepository =
            mock(DailyStockPriceRepository::class.java)

        service =
            StockPriceService(
                kisCurrentPriceClient = kisCurrentPriceClient,
                kisDailyPriceClient =
                    mock(KisDailyPriceClient::class.java),
                currentPriceCacheRepository = cacheRepository,
                dailyStockPriceRepository = dailyPriceRepository,
            )
    }

    @Test
    fun `현재가 캐시가 있으면 KIS를 호출하지 않는다`() {
        val cachedPrice =
            StockCurrentPriceCache(
                code = "005930",
                currentPrice = BigDecimal("79800"),
                priceChange = BigDecimal("5900"),
                changeRate = BigDecimal("8.1"),
                fetchedAt =
                    LocalDateTime.of(
                        2026,
                        7,
                        29,
                        10,
                        0,
                    ),
            )

        `when`(cacheRepository.findByCode("005930"))
            .thenReturn(cachedPrice)

        val result =
            service.getCurrentPrice(
                stockId = 1L,
                stockCode = "005930",
            )

        assertThat(result.priceStatus)
            .isEqualTo(PriceStatus.DELAYED_CURRENT)
        assertThat(result.currentPrice)
            .isEqualByComparingTo("79800")

        // 캐시가 있으므로 KIS를 호출하지 않는다.
        verifyNoInteractions(kisCurrentPriceClient)
    }

    @Test
    fun `KIS 성공값을 두 캐시에 저장한다`() {
        `when`(cacheRepository.findByCode("005930"))
            .thenReturn(null)

        `when`(
            kisCurrentPriceClient.getCurrentPrice(
                stockId = 1L,
                stockCode = "005930",
            ),
        ).thenReturn(
            KisCurrentPriceResult(
                stockCode = "005930",
                currentPrice = BigDecimal("79800"),
                priceChange = BigDecimal("5900"),
                changeRate = BigDecimal("8.1"),
            ),
        )

        val result =
            service.getCurrentPrice(
                stockId = 1L,
                stockCode = "005930",
            )

        assertThat(result.priceStatus)
            .isEqualTo(PriceStatus.DELAYED_CURRENT)
        assertThat(result.currentPrice)
            .isEqualByComparingTo("79800")
        assertThat(result.fetchedAt)
            .isNotNull()

        val savedPrice =
            StockCurrentPriceCache(
                code = "005930",
                currentPrice = BigDecimal("79800"),
                priceChange = BigDecimal("5900"),
                changeRate = BigDecimal("8.1"),
                fetchedAt = requireNotNull(result.fetchedAt),
            )

        // 정상 응답을 두 캐시에 저장한다.
        verify(cacheRepository)
            .save(savedPrice)
        verify(cacheRepository)
            .saveLastSuccess(savedPrice)
    }

    @Test
    fun `KIS 실패 시 마지막 성공값을 반환한다`() {
        val fetchedAt =
            LocalDateTime.of(
                2026,
                7,
                29,
                9,
                59,
            )

        val lastSuccess =
            StockCurrentPriceCache(
                code = "005930",
                currentPrice = BigDecimal("79700"),
                priceChange = BigDecimal("5800"),
                changeRate = BigDecimal("7.9"),
                fetchedAt = fetchedAt,
            )

        `when`(cacheRepository.findByCode("005930"))
            .thenReturn(null)

        `when`(
            kisCurrentPriceClient.getCurrentPrice(
                1L,
                "005930",
            ),
        ).thenThrow(
            IllegalStateException("KIS failed"),
        )

        `when`(
            cacheRepository.findLastSuccessByCode("005930"),
        ).thenReturn(lastSuccess)

        val result =
            service.getCurrentPrice(
                stockId = 1L,
                stockCode = "005930",
            )

        assertThat(result.priceStatus)
            .isEqualTo(PriceStatus.LAST_SUCCESS)
        assertThat(result.currentPrice)
            .isEqualByComparingTo("79700")
        assertThat(result.fetchedAt)
            .isEqualTo(fetchedAt)

        // 마지막 성공값이 있어 DB는 조회하지 않는다.
        verifyNoInteractions(dailyPriceRepository)
    }

    @Test
    fun `마지막 성공값이 없으면 직전 종가를 반환한다`() {
        val previousClose =
            mock(DailyStockPrice::class.java)

        `when`(previousClose.price)
            .thenReturn(BigDecimal("73900"))
        `when`(previousClose.changeRate)
            .thenReturn(BigDecimal("1.2"))
        `when`(previousClose.tradeDate)
            .thenReturn(LocalDate.of(2026, 7, 28))

        `when`(cacheRepository.findByCode("005930"))
            .thenReturn(null)

        `when`(
            kisCurrentPriceClient.getCurrentPrice(
                1L,
                "005930",
            ),
        ).thenThrow(
            IllegalStateException("KIS failed"),
        )

        `when`(
            cacheRepository.findLastSuccessByCode("005930"),
        ).thenReturn(null)

        `when`(
            dailyPriceRepository
                .findTopByStockIdAndTradeDateBeforeOrderByTradeDateDesc(
                    stockId = 1L,
                    tradeDate = LocalDate.now(),
                ),
        ).thenReturn(previousClose)

        val result =
            service.getCurrentPrice(
                stockId = 1L,
                stockCode = "005930",
            )

        assertThat(result.priceStatus)
            .isEqualTo(PriceStatus.PREVIOUS_CLOSE)
        assertThat(result.currentPrice)
            .isEqualByComparingTo("73900")
        assertThat(result.tradeDate)
            .isEqualTo(LocalDate.of(2026, 7, 28))
    }

    @Test
    fun `조회 가능한 가격이 없으면 503 예외를 던진다`() {
        `when`(cacheRepository.findByCode("005930"))
            .thenReturn(null)

        `when`(
            kisCurrentPriceClient.getCurrentPrice(
                1L,
                "005930",
            ),
        ).thenThrow(
            IllegalStateException("KIS failed"),
        )

        `when`(
            cacheRepository.findLastSuccessByCode("005930"),
        ).thenReturn(null)

        `when`(
            dailyPriceRepository
                .findTopByStockIdAndTradeDateBeforeOrderByTradeDateDesc(
                    stockId = 1L,
                    tradeDate = LocalDate.now(),
                ),
        ).thenReturn(null)

        val exception =
            assertThrows<BusinessException> {
                service.getCurrentPrice(
                    stockId = 1L,
                    stockCode = "005930",
                )
            }

        assertThat(exception.errorCode)
            .isEqualTo(
                StockErrorCode.STOCK_PRICE_UNAVAILABLE,
            )
    }

    @Test
    fun `같은 종목의 동시 요청은 KIS를 한 번만 호출한다`() {
        val savedCache =
            AtomicReference<StockCurrentPriceCache?>()

        val start = CountDownLatch(1)
        val kisStarted = CountDownLatch(1)
        val finishKis = CountDownLatch(1)

        `when`(cacheRepository.findByCode("005930"))
            .thenAnswer {
                savedCache.get()
            }

        `when`(
            kisCurrentPriceClient.getCurrentPrice(
                1L,
                "005930",
            ),
        ).thenAnswer {
            kisStarted.countDown()
            finishKis.await(3, TimeUnit.SECONDS)

            val kisPrice =
                KisCurrentPriceResult(
                    stockCode = "005930",
                    currentPrice = BigDecimal("79800"),
                    priceChange = BigDecimal("5900"),
                    changeRate = BigDecimal("8.1"),
                )

            savedCache.set(
                StockCurrentPriceCache(
                    code = kisPrice.stockCode,
                    currentPrice = kisPrice.currentPrice,
                    priceChange = kisPrice.priceChange,
                    changeRate = kisPrice.changeRate,
                    fetchedAt = LocalDateTime.now(),
                ),
            )

            kisPrice
        }

        val executor =
            Executors.newFixedThreadPool(2)

        try {
            val first =
                executor.submit {
                    start.await()
                    service.getCurrentPrice(1L, "005930")
                }

            val second =
                executor.submit {
                    start.await()
                    service.getCurrentPrice(1L, "005930")
                }

            // 두 요청을 함께 시작한다.
            start.countDown()

            assertThat(
                kisStarted.await(3, TimeUnit.SECONDS),
            ).isTrue()

            finishKis.countDown()

            first.get(3, TimeUnit.SECONDS)
            second.get(3, TimeUnit.SECONDS)

            // 같은 종목은 KIS를 한 번만 호출한다.
            verify(
                kisCurrentPriceClient,
                times(1),
            ).getCurrentPrice(
                1L,
                "005930",
            )
        } finally {
            executor.shutdownNow()
        }
    }
}
