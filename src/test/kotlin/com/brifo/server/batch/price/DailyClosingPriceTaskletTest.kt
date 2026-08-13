package com.brifo.server.batch.price

import com.brifo.server.stock.entity.DailyStockPrice
import com.brifo.server.stock.entity.Stock
import com.brifo.server.stock.repository.DailyStockPriceRepository
import com.brifo.server.stock.repository.StockRepository
import com.brifo.server.stock.service.StockPriceService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.StepContribution
import java.time.Duration
import java.time.LocalDate
import kotlin.test.assertEquals

class DailyClosingPriceTaskletTest {
    private val stockRepository = mock(StockRepository::class.java)
    private val priceRepository = mock(DailyStockPriceRepository::class.java)
    private val stockPriceService = mock(StockPriceService::class.java)
    private val targetDate = LocalDate.of(2026, 8, 3)

    @Test
    fun `활성 종목의 종가를 백필한다`() {
        val stock = mock(Stock::class.java)
        `when`(stock.id).thenReturn(1L)
        `when`(stockRepository.findAllByIsActiveTrueOrderByCode()).thenReturn(listOf(stock))
        `when`(priceRepository.findByStockIdAndTradeDateAndIsClosingFalse(1L, targetDate)).thenReturn(null)

        tasklet().execute(mock(StepContribution::class.java), mock(ChunkContext::class.java))

        verify(stockPriceService).saveClosingPrice(stock, targetDate)
    }

    @Test
    fun `이미 백필된 종목은 다시 조회하지 않는다`() {
        val stock = mock(Stock::class.java)
        `when`(stock.id).thenReturn(1L)
        `when`(stockRepository.findAllByIsActiveTrueOrderByCode()).thenReturn(listOf(stock))
        `when`(priceRepository.findByStockIdAndTradeDateAndIsClosingFalse(1L, targetDate))
            .thenReturn(mock(DailyStockPrice::class.java))

        tasklet().execute(mock(StepContribution::class.java), mock(ChunkContext::class.java))

        verify(stockPriceService, never()).saveClosingPrice(stock, targetDate)
    }

    @Test
    fun `한 종목이 실패해도 나머지 종목은 계속 처리한다`() {
        val failingStock = mock(Stock::class.java)
        `when`(failingStock.id).thenReturn(1L)
        `when`(failingStock.code).thenReturn("000001")
        val succeedingStock = mock(Stock::class.java)
        `when`(succeedingStock.id).thenReturn(2L)

        `when`(stockRepository.findAllByIsActiveTrueOrderByCode()).thenReturn(listOf(failingStock, succeedingStock))
        `when`(priceRepository.findByStockIdAndTradeDateAndIsClosingFalse(1L, targetDate)).thenReturn(null)
        `when`(priceRepository.findByStockIdAndTradeDateAndIsClosingFalse(2L, targetDate)).thenReturn(null)
        `when`(stockPriceService.saveClosingPrice(failingStock, targetDate))
            .thenThrow(RuntimeException("KIS 조회 실패"))

        tasklet().execute(mock(StepContribution::class.java), mock(ChunkContext::class.java))

        verify(stockPriceService, times(1)).saveClosingPrice(failingStock, targetDate)
        verify(stockPriceService).saveClosingPrice(succeedingStock, targetDate)
    }

    @Test
    fun `KIS 호출을 한 종목마다 지정된 간격만큼 스로틀링한다`() {
        // KIS 실전 계좌는 초당 20건 제한(EGW00201)이라, 종목을 쉬지 않고 순회하면
        // 초과해서 실패한다. 실패 여부와 무관하게 호출마다 대기해야 한다.
        val failingStock = mock(Stock::class.java)
        `when`(failingStock.id).thenReturn(1L)
        `when`(failingStock.code).thenReturn("000001")
        val succeedingStock = mock(Stock::class.java)
        `when`(succeedingStock.id).thenReturn(2L)
        val skippedStock = mock(Stock::class.java)
        `when`(skippedStock.id).thenReturn(3L)

        `when`(stockRepository.findAllByIsActiveTrueOrderByCode())
            .thenReturn(listOf(failingStock, succeedingStock, skippedStock))
        `when`(priceRepository.findByStockIdAndTradeDateAndIsClosingFalse(1L, targetDate)).thenReturn(null)
        `when`(priceRepository.findByStockIdAndTradeDateAndIsClosingFalse(2L, targetDate)).thenReturn(null)
        // 이미 백필된 종목은 KIS를 아예 호출하지 않으므로 대기도 없어야 한다.
        `when`(priceRepository.findByStockIdAndTradeDateAndIsClosingFalse(3L, targetDate))
            .thenReturn(mock(DailyStockPrice::class.java))
        `when`(stockPriceService.saveClosingPrice(failingStock, targetDate))
            .thenThrow(RuntimeException("KIS 조회 실패"))

        val sleptDurations = mutableListOf<Duration>()
        val interval = Duration.ofMillis(150)
        val taskletWithSleeper =
            DailyClosingPriceTasklet(
                targetDate = targetDate,
                stockRepository = stockRepository,
                dailyStockPriceRepository = priceRepository,
                stockPriceService = stockPriceService,
                requestInterval = interval,
                sleeper = sleptDurations::add,
            )

        taskletWithSleeper.execute(mock(StepContribution::class.java), mock(ChunkContext::class.java))

        assertEquals(listOf(interval, interval), sleptDurations)
    }

    private fun tasklet() =
        DailyClosingPriceTasklet(
            targetDate = targetDate,
            stockRepository = stockRepository,
            dailyStockPriceRepository = priceRepository,
            stockPriceService = stockPriceService,
            sleeper = {}, // 테스트에서는 실제로 기다리지 않는다.
        )
}
