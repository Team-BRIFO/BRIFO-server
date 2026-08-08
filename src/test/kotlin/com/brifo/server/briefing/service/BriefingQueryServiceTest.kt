package com.brifo.server.briefing.service

import com.brifo.server.briefing.repository.BriefingRepository
import com.brifo.server.briefing.service.BriefingQueryService
import com.brifo.server.briefing.entity.Briefing
import com.brifo.server.briefing.entity.BriefingStatus
import com.brifo.server.briefing.exception.BriefingNotCompletedException
import com.brifo.server.briefing.exception.BriefingNotFoundException
import com.brifo.server.briefing.exception.BriefingProcessingFailedException
import com.brifo.server.stock.exception.StockNotFoundException
import com.brifo.server.stock.repository.StockRepository
import com.brifo.server.stock.repository.UserStockRepository
import com.brifo.server.stock.service.StockPriceService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
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
}
