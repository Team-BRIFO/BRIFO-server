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
import java.time.LocalDate

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

    private fun tasklet() = DailyClosingPriceTasklet(targetDate, stockRepository, priceRepository, stockPriceService)
}
