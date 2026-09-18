package com.brifo.server.decision.service

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.decision.entity.DecisionDirection
import com.brifo.server.decision.exception.DecisionNotFoundException
import com.brifo.server.decision.exception.DecisionNotSettledException
import com.brifo.server.decision.repository.DecisionRepository
import com.brifo.server.decision.repository.DecisionResultRepository
import com.brifo.server.decision.repository.TodayDecisionRow
import com.brifo.server.stock.dto.response.PriceStatus
import com.brifo.server.stock.dto.response.StockPriceResult
import com.brifo.server.stock.service.StockPriceService
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DecisionQueryServiceTest {
    private val repository = mock(DecisionRepository::class.java)
    private val resultRepository = mock(DecisionResultRepository::class.java)
    private val stockPriceService = mock(StockPriceService::class.java)
    private val service = DecisionQueryService(
        repository,
        resultRepository,
        stockPriceService,
        Clock.fixed(Instant.parse("2026-07-21T05:00:00Z"), ZoneId.of("Asia/Seoul")),
    )

    @Test
    fun `오늘의 결정 목록은 서울 기준 오늘 날짜로 조회한다`() {
        val userId = UUID.randomUUID()
        val today = LocalDate.of(2026, 7, 21)
        `when`(repository.findTodayDecisions(userId, today)).thenReturn(emptyList())

        service.getDecisions(userId)

        verify(repository).findTodayDecisions(userId, today)
    }

    @Test
    fun `정산 전 결정은 장중 실시간 현재가로 덮어쓴다`() {
        val userId = UUID.randomUUID()
        val today = LocalDate.of(2026, 7, 21)
        val row = TodayDecisionRow(
            decisionId = UUID.randomUUID(),
            direction = DecisionDirection.UP,
            allocatedAp = 30_000,
            isSettled = false,
            agentId = UUID.randomUUID(),
            agentType = AgentType.ROOKIE,
            stockId = 1L,
            stockPublicId = UUID.randomUUID(),
            stockName = "삼성전자",
            stockCode = "005930",
            logoUrl = null,
            price = 71_000L,
            changeRate = BigDecimal("1.1"),
            tradeDate = today.minusDays(1),
        )
        `when`(repository.findTodayDecisions(userId, today)).thenReturn(listOf(row))
        `when`(stockPriceService.getCurrentPrice(1L, "005930")).thenReturn(
            StockPriceResult(
                stockCode = "005930",
                currentPrice = BigDecimal("72500"),
                priceChange = BigDecimal("1500"),
                changeRate = BigDecimal("2.1"),
                priceStatus = PriceStatus.DELAYED_CURRENT,
                tradeDate = today,
            ),
        )

        val result = service.getDecisions(userId)

        val stock = result.items.single().stock
        assertEquals(72_500L, stock.price)
        assertEquals(BigDecimal("2.1"), stock.changeRate)
        assertEquals(today, stock.tradeDate)
    }

    @Test
    fun `정산된 결정은 실시간 현재가를 조회하지 않고 정산 시점 시세를 유지한다`() {
        val userId = UUID.randomUUID()
        val today = LocalDate.of(2026, 7, 21)
        val row = TodayDecisionRow(
            decisionId = UUID.randomUUID(),
            direction = DecisionDirection.UP,
            allocatedAp = 30_000,
            isSettled = true,
            agentId = UUID.randomUUID(),
            agentType = AgentType.ROOKIE,
            stockId = 1L,
            stockPublicId = UUID.randomUUID(),
            stockName = "삼성전자",
            stockCode = "005930",
            logoUrl = null,
            price = 72_420L,
            changeRate = BigDecimal("2.06"),
            tradeDate = today,
        )
        `when`(repository.findTodayDecisions(userId, today)).thenReturn(listOf(row))

        val result = service.getDecisions(userId)

        val stock = result.items.single().stock
        assertEquals(72_420L, stock.price)
        verify(stockPriceService, never()).getCurrentPrice(anyLong(), anyString())
    }

    @Test
    fun `소유한 결정이 아니면 결과를 조회할 수 없다`() {
        val userId = UUID.randomUUID()
        val decisionId = UUID.randomUUID()
        `when`(repository.existsByPublicIdAndBriefingAgentUserPublicId(decisionId, userId)).thenReturn(false)

        assertFailsWith<DecisionNotFoundException> {
            service.getDecisionResult(userId, decisionId)
        }
    }

    @Test
    fun `소유한 결정이 정산 전이면 결과를 조회할 수 없다`() {
        val userId = UUID.randomUUID()
        val decisionId = UUID.randomUUID()
        `when`(repository.existsByPublicIdAndBriefingAgentUserPublicId(decisionId, userId)).thenReturn(true)
        `when`(resultRepository.existsByDecisionPublicId(decisionId)).thenReturn(false)

        assertFailsWith<DecisionNotSettledException> {
            service.getDecisionResult(userId, decisionId)
        }
    }
}
