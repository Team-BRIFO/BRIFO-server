package com.brifo.server.briefing.service

import com.brifo.server.agent.entity.Agent
import com.brifo.server.agent.entity.AgentType
import com.brifo.server.briefing.entity.BriefingDirection
import com.brifo.server.briefing.repository.BriefingRepository
import com.brifo.server.briefing.service.BriefingQueryService
import com.brifo.server.briefing.entity.Briefing
import com.brifo.server.briefing.entity.BriefingStatus
import com.brifo.server.briefing.exception.BriefingNotCompletedException
import com.brifo.server.briefing.exception.BriefingNotFoundException
import com.brifo.server.briefing.exception.BriefingProcessingFailedException
import com.brifo.server.news.entity.News
import com.brifo.server.news.entity.NewsCard
import com.brifo.server.stock.dto.response.PriceStatus
import com.brifo.server.stock.dto.response.StockPriceResult
import com.brifo.server.stock.entity.Stock
import com.brifo.server.stock.exception.StockNotFoundException
import com.brifo.server.stock.repository.StockRepository
import com.brifo.server.stock.repository.UserStockRepository
import com.brifo.server.stock.service.StockPriceService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BriefingQueryServiceTest {
    private val briefingRepository = mock(BriefingRepository::class.java)
    private val userStockRepository = mock(UserStockRepository::class.java)
    private val stockRepository = mock(StockRepository::class.java)
    private val stockPriceService = mock(StockPriceService::class.java)
    private val service = BriefingQueryService(
        briefingRepository,
        userStockRepository,
        stockRepository,
        stockPriceService,
        Clock.fixed(Instant.parse("2026-07-18T06:20:00Z"), ZoneId.of("Asia/Seoul")),
    )

    @Test
    fun `현재 관심 종목이 아니면 종목 브리핑을 조회하지 않는다`() {
        val userId = UUID.randomUUID()
        val stockId = UUID.randomUUID()
        `when`(userStockRepository.existsInterestStock(userId, stockId)).thenReturn(false)

        assertFailsWith<StockNotFoundException> {
            service.getStockBriefings(userId, stockId)
        }

        verifyNoInteractions(briefingRepository)
    }

    @Test
    fun `관심 종목에 오늘 브리핑이 없으면 조회할 수 없다`() {
        val userId = UUID.randomUUID()
        val stockId = UUID.randomUUID()
        `when`(userStockRepository.existsInterestStock(userId, stockId)).thenReturn(true)
        `when`(briefingRepository.findStockBriefingItems(userId, stockId, java.time.LocalDate.of(2026, 7, 18)))
            .thenReturn(emptyList())

        assertFailsWith<BriefingNotFoundException> {
            service.getStockBriefings(userId, stockId)
        }
    }

    @Test
    fun `소유한 브리핑이 아니면 상세를 조회할 수 없다`() {
        val userId = UUID.randomUUID()
        val briefingId = UUID.randomUUID()
        `when`(briefingRepository.findOwnedBriefing(userId, briefingId)).thenReturn(null)

        assertFailsWith<BriefingNotFoundException> {
            service.getBriefingDetail(userId, briefingId)
        }
    }

    @Test
    fun `완료되지 않은 브리핑은 상태에 맞는 예외로 상세 조회를 거절한다`() {
        val userId = UUID.randomUUID()
        val briefingId = UUID.randomUUID()
        val briefing = mock(Briefing::class.java)
        `when`(briefingRepository.findOwnedBriefing(userId, briefingId)).thenReturn(briefing)

        listOf(BriefingStatus.PENDING, BriefingStatus.ANALYZING).forEach { status ->
            `when`(briefing.status).thenReturn(status)
            assertFailsWith<BriefingNotCompletedException> {
                service.getBriefingDetail(userId, briefingId)
            }
        }

        `when`(briefing.status).thenReturn(BriefingStatus.FAILED)
        assertFailsWith<BriefingProcessingFailedException> {
            service.getBriefingDetail(userId, briefingId)
        }
    }

    @Test
    fun `상세 조회는 raw DB 등락률 대신 StockPriceService의 현재가를 사용한다`() {
        val userId = UUID.randomUUID()
        val briefingId = UUID.randomUUID()
        val stockPublicId = UUID.randomUUID()
        val stockId = 5L
        val stock = mock(Stock::class.java)
        `when`(stock.id).thenReturn(stockId)
        `when`(stock.publicId).thenReturn(stockPublicId)
        `when`(stock.name).thenReturn("삼성전자")
        `when`(stock.logoUrl).thenReturn(null)
        `when`(stock.code).thenReturn("005930")

        val news = mock(News::class.java)
        `when`(news.stock).thenReturn(stock)
        val newsCard = mock(NewsCard::class.java)
        `when`(newsCard.news).thenReturn(news)
        `when`(newsCard.publicId).thenReturn(UUID.randomUUID())
        `when`(newsCard.headline).thenReturn("헤드라인")

        val agent = mock(Agent::class.java)
        `when`(agent.publicId).thenReturn(UUID.randomUUID())
        `when`(agent.agentType).thenReturn(AgentType.ROOKIE)
        `when`(agent.nickname).thenReturn("루키")
        `when`(agent.modelName).thenReturn("gpt")

        val briefing = mock(Briefing::class.java)
        `when`(briefing.status).thenReturn(BriefingStatus.COMPLETED)
        `when`(briefing.newsCards).thenReturn(listOf(newsCard))
        `when`(briefing.agent).thenReturn(agent)
        `when`(briefing.publicId).thenReturn(briefingId)
        `when`(briefing.direction).thenReturn(BriefingDirection.UP)
        `when`(briefing.confidenceRate).thenReturn(70.toShort())
        `when`(briefing.summary).thenReturn("요약")
        `when`(briefing.personalComment).thenReturn(null)
        `when`(briefing.contentText).thenReturn("본문")
        `when`(briefing.oneLiner).thenReturn("한줄평")
        `when`(briefingRepository.findOwnedBriefing(userId, briefingId)).thenReturn(briefing)

        // 데이터 서버가 아직 등락률을 확정하지 못해 0을 내려주는 상황을 StockPriceService가
        // 이미 저장된 종가로 대체한 결과를 그대로 전달해야 한다.
        `when`(stockPriceService.getCurrentPrice(stockId, "005930")).thenReturn(
            StockPriceResult(
                stockCode = "005930",
                currentPrice = BigDecimal("70000"),
                priceChange = BigDecimal("500"),
                changeRate = BigDecimal("0.7"),
                priceStatus = PriceStatus.DELAYED_CURRENT,
                tradeDate = LocalDate.of(2026, 7, 18),
            ),
        )

        val response = service.getBriefingDetail(userId, briefingId)

        assertEquals(BigDecimal("0.7"), response.stock.changeRate)
        assertEquals(BigDecimal("70000"), response.stock.price)
        assertEquals(stockPublicId, response.stock.stockId)
    }
}
