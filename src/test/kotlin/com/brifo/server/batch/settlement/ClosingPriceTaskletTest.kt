package com.brifo.server.batch.settlement

import com.brifo.server.decision.repository.DecisionRepository
import com.brifo.server.stock.client.ClosingPriceClient
import com.brifo.server.stock.entity.DailyStockPrice
import com.brifo.server.stock.entity.Stock
import com.brifo.server.stock.repository.DailyStockPriceRepository
import com.brifo.server.stock.repository.StockRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.StepContribution
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals

class ClosingPriceTaskletTest {
    private val decisionRepository = mock(DecisionRepository::class.java)
    private val stockRepository = mock(StockRepository::class.java)
    private val priceRepository = mock(DailyStockPriceRepository::class.java)
    private val client = mock(ClosingPriceClient::class.java)
    private val targetDate = LocalDate.of(2026, 8, 3)

    @Test
    fun `미정산 결정 종목의 종가를 한 번 저장한다`() {
        val stock = mock(Stock::class.java)
        `when`(decisionRepository.findUnsettledStockIds(targetDate, false)).thenReturn(listOf(2L))
        `when`(stockRepository.findAllById(listOf(2L))).thenReturn(listOf(stock))
        `when`(stock.id).thenReturn(2L)
        `when`(stock.code).thenReturn("BRF001")
        `when`(priceRepository.findByStockIdAndTradeDateAndIsClosingTrue(2L, targetDate)).thenReturn(null)
        `when`(client.getClosingPrice(ClosingPriceClient.Request(2L, "BRF001", targetDate))).thenReturn(
            ClosingPriceClient.Result(BigDecimal("1000.00"), BigDecimal("0.50")),
        )

        tasklet().execute(mock(StepContribution::class.java), mock(ChunkContext::class.java))

        val saved = mockingDetails(priceRepository).invocations.single { it.method.name == "save" }.arguments[0]
            as DailyStockPrice
        assertEquals(true, saved.isClosing)
        assertEquals(BigDecimal("1000.00"), saved.price)
    }

    @Test
    fun `이미 저장된 종가는 다시 조회하지 않는다`() {
        val stock = mock(Stock::class.java)
        `when`(decisionRepository.findUnsettledStockIds(targetDate, false)).thenReturn(listOf(2L))
        `when`(stockRepository.findAllById(listOf(2L))).thenReturn(listOf(stock))
        `when`(stock.id).thenReturn(2L)
        `when`(priceRepository.findByStockIdAndTradeDateAndIsClosingTrue(2L, targetDate))
            .thenReturn(mock(DailyStockPrice::class.java))

        tasklet().execute(mock(StepContribution::class.java), mock(ChunkContext::class.java))

        verifyNoInteractions(client)
        assertEquals(0, mockingDetails(priceRepository).invocations.count { it.method.name == "save" })
    }

    private fun tasklet() = ClosingPriceTasklet(targetDate, decisionRepository, stockRepository, priceRepository, client)
}
