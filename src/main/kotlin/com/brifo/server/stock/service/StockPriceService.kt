package com.brifo.server.stock.service

import com.brifo.server.externalapi.kis.KisCurrentPriceClient
import com.brifo.server.externalapi.kis.KisDailyPriceClient
import com.brifo.server.global.exception.BusinessException
import com.brifo.server.stock.cache.StockCurrentPriceCache
import com.brifo.server.stock.cache.StockCurrentPriceCacheRepository
import com.brifo.server.stock.code.StockErrorCode
import com.brifo.server.stock.dto.response.PriceStatus
import com.brifo.server.stock.dto.response.StockPriceResult
import com.brifo.server.stock.entity.DailyStockPrice
import com.brifo.server.stock.entity.Stock
import com.brifo.server.stock.repository.DailyStockPriceRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

@Service
class StockPriceService(
    private val kisCurrentPriceClient: KisCurrentPriceClient,
    private val kisDailyPriceClient: KisDailyPriceClient,
    private val currentPriceCacheRepository: StockCurrentPriceCacheRepository,
    private val dailyStockPriceRepository: DailyStockPriceRepository,
) {
    private val log = LoggerFactory.getLogger(StockPriceService::class.java)

    // 종목별로 잠금 객체를 따로 보관한다.
    // 삼성전자 요청과 카카오 요청은 서로 막지 않고,
    // 삼성전자 요청끼리만 한 번에 하나씩 처리한다.
    private val priceLocks = ConcurrentHashMap<String, Any>()

    fun getCurrentPrice(
        stockId: Long,
        stockCode: String,
    ): StockPriceResult {
        // 1. 60초 현재가 캐시를 가장 먼저 확인한다.
        val cachedPrice =
            currentPriceCacheRepository.findByCode(stockCode)

        // 캐시값이 있으면 KIS를 호출하지 않고 바로 반환한다.
        if (cachedPrice != null) {
            return StockPriceResult(
                stockCode = cachedPrice.code,
                currentPrice = cachedPrice.currentPrice,
                priceChange = cachedPrice.priceChange,
                changeRate = cachedPrice.changeRate,
                priceStatus = PriceStatus.DELAYED_CURRENT,
                fetchedAt = cachedPrice.fetchedAt,
            )
        }

        // 종목 코드마다 서로 다른 잠금 객체를 사용한다.
        val lock =
            priceLocks.computeIfAbsent(stockCode) {
                Any()
            }

        synchronized(lock) {
            // 락을 기다리는 사이 앞 요청이 KIS 조회를 끝내고
            // 캐시에 저장했을 수 있으므로 캐시를 다시 확인한다.
            val cachedPriceAfterLock =
                currentPriceCacheRepository.findByCode(stockCode)

            if (cachedPriceAfterLock != null) {
                return StockPriceResult(
                    stockCode = cachedPriceAfterLock.code,
                    currentPrice = cachedPriceAfterLock.currentPrice,
                    priceChange = cachedPriceAfterLock.priceChange,
                    changeRate = cachedPriceAfterLock.changeRate,
                    priceStatus = PriceStatus.DELAYED_CURRENT,
                    fetchedAt = cachedPriceAfterLock.fetchedAt,
                )
            }

            val kisPrice =
                try {
                    // 2. 캐시에 값이 없으면 KIS 현재가를 조회한다.
                    kisCurrentPriceClient.getCurrentPrice(
                        stockId = stockId,
                        stockCode = stockCode,
                    )
                } catch (exception: Exception) {
                    log.warn(
                        "KIS 현재가 조회 실패로 fallback을 수행합니다. stockId={}, stockCode={}",
                        stockId,
                        stockCode,
                        exception,
                    )

                    // 3. KIS 실패 시 3분 마지막 성공값을 확인한다.
                    val lastSuccessPrice =
                        currentPriceCacheRepository
                            .findLastSuccessByCode(stockCode)

                    if (lastSuccessPrice != null) {
                        return StockPriceResult(
                            stockCode = lastSuccessPrice.code,
                            currentPrice = lastSuccessPrice.currentPrice,
                            priceChange = lastSuccessPrice.priceChange,
                            changeRate = lastSuccessPrice.changeRate,
                            priceStatus = PriceStatus.LAST_SUCCESS,
                            fetchedAt = lastSuccessPrice.fetchedAt,
                        )
                    }

                    // 4. 마지막 성공값도 없으면 DB의 가장 최근 종가를 조회한다.
                    val previousClose =
                        dailyStockPriceRepository
                            .findTopByStockIdAndTradeDateBeforeOrderByTradeDateDesc(
                                stockId = stockId,
                                tradeDate = LocalDate.now(),
                            )

                    if (previousClose != null) {
                        return StockPriceResult(
                            stockCode = stockCode,
                            currentPrice = previousClose.price,
                            priceChange = null,
                            changeRate = previousClose.changeRate,
                            priceStatus = PriceStatus.PREVIOUS_CLOSE,
                            tradeDate = previousClose.tradeDate,
                        )
                    }

                    // 5. 캐시와 DB에 모두 값이 없으면 503을 반환한다.
                    throw BusinessException(
                        StockErrorCode.STOCK_PRICE_UNAVAILABLE,
                    )
                }

            val fetchedAt = LocalDateTime.now()

            val priceToCache =
                StockCurrentPriceCache(
                    code = kisPrice.stockCode,
                    currentPrice = kisPrice.currentPrice,
                    priceChange = kisPrice.priceChange,
                    changeRate = kisPrice.changeRate,
                    fetchedAt = fetchedAt,
                )

            // KIS 성공값을 60초 현재가 캐시에 저장한다.
            currentPriceCacheRepository.save(priceToCache)

            // 같은 값을 3분 마지막 성공값 캐시에도 저장한다.
            currentPriceCacheRepository.saveLastSuccess(priceToCache)

            return StockPriceResult(
                stockCode = kisPrice.stockCode,
                currentPrice = kisPrice.currentPrice,
                priceChange = kisPrice.priceChange,
                changeRate = kisPrice.changeRate,
                priceStatus = PriceStatus.DELAYED_CURRENT,
                fetchedAt = fetchedAt,
            )
        }
    }

    @Transactional
    fun saveClosingPrice(
        stock: Stock,
        tradeDate: LocalDate,
    ): DailyStockPrice {
        // 종가 정산은 현재가 캐시나 fallback을 사용하지 않는다.
        // KIS 종가 API를 직접 호출한 뒤 DB에 저장한다.
        val dailyPrice =
            kisDailyPriceClient.getDailyPrice(
                stockId = requireNotNull(stock.id),
                stockCode = stock.code,
                tradeDate = tradeDate,
            )

        val entity =
            DailyStockPrice.create(
                stock = stock,
                tradeDate = dailyPrice.tradeDate,
                price = dailyPrice.price,
                changeRate = dailyPrice.changeRate,
            )

        return dailyStockPriceRepository.save(entity)
    }
}
