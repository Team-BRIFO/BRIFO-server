package com.brifo.server.stock.service

import com.brifo.server.stock.client.ClosingPriceClient
import com.brifo.server.stock.client.CurrentStockPriceClient
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
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

@Service
class StockPriceService(
    private val currentStockPriceClient: CurrentStockPriceClient,
    private val closingPriceClient: ClosingPriceClient,
    private val currentPriceCacheRepository: StockCurrentPriceCacheRepository,
    private val dailyStockPriceRepository: DailyStockPriceRepository,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(StockPriceService::class.java)

    private val priceLocks = ConcurrentHashMap<String, Any>()

    fun getCurrentPrice(
        stockId: Long,
        stockCode: String,
    ): StockPriceResult {
        val today = LocalDate.now(clock)
        val cachedPrice =
            currentPriceCacheRepository.findByCode(stockCode)

        if (cachedPrice != null) {
            return StockPriceResult(
                stockCode = cachedPrice.code,
                currentPrice = cachedPrice.currentPrice,
                priceChange = cachedPrice.priceChange,
                changeRate = cachedPrice.changeRate,
                priceStatus = PriceStatus.DELAYED_CURRENT,
                fetchedAt = cachedPrice.fetchedAt,
                tradeDate = today,
            )
        }

        val lock =
            priceLocks.computeIfAbsent(stockCode) {
                Any()
            }

        synchronized(lock) {
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
                    tradeDate = today,
                )
            }

            val remotePrice =
                try {
                    currentStockPriceClient.getCurrentPrice(stockId, stockCode)
                } catch (exception: Exception) {
                    log.warn(
                        "현재가 조회 실패로 fallback을 수행합니다. stockId={}, stockCode={}",
                        stockId,
                        stockCode,
                        exception,
                    )

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
                            tradeDate = today,
                        )
                    }

                    val previousClose =
                        dailyStockPriceRepository
                            .findTopByStockIdAndTradeDateBeforeOrderByTradeDateDesc(
                                stockId = stockId,
                                tradeDate = today,
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

                    throw BusinessException(
                        StockErrorCode.STOCK_PRICE_UNAVAILABLE,
                    )
                }

            val fetchedAt = LocalDateTime.now(clock)

            val priceToCache =
                StockCurrentPriceCache(
                    code = remotePrice.stockCode,
                    currentPrice = remotePrice.currentPrice,
                    priceChange = remotePrice.priceChange,
                    changeRate = remotePrice.changeRate,
                    fetchedAt = fetchedAt,
                )

            currentPriceCacheRepository.save(priceToCache)

            currentPriceCacheRepository.saveLastSuccess(priceToCache)

            return StockPriceResult(
                stockCode = remotePrice.stockCode,
                currentPrice = remotePrice.currentPrice,
                priceChange = remotePrice.priceChange,
                changeRate = remotePrice.changeRate,
                priceStatus = PriceStatus.DELAYED_CURRENT,
                fetchedAt = fetchedAt,
                tradeDate = today,
            )
        }
    }

    @Transactional
    fun saveClosingPrice(
        stock: Stock,
        tradeDate: LocalDate,
    ): DailyStockPrice {
        val dailyPrice =
            closingPriceClient.getClosingPrice(
                ClosingPriceClient.Request(requireNotNull(stock.id), stock.code, tradeDate),
            )

        val entity =
            DailyStockPrice.create(
                stock = stock,
                tradeDate = tradeDate,
                price = dailyPrice.price,
                changeRate = dailyPrice.changeRate,
            )

        return dailyStockPriceRepository.save(entity)
    }
}
