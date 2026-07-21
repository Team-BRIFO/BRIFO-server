package com.brifo.server.decision.service

import com.brifo.server.briefing.entity.Briefing
import com.brifo.server.briefing.entity.BriefingStatus
import com.brifo.server.briefing.exception.BriefingNotCompletedException
import com.brifo.server.briefing.exception.BriefingNotFoundException
import com.brifo.server.briefing.repository.BriefingRepository
import com.brifo.server.decision.entity.Decision
import com.brifo.server.decision.entity.DecisionDirection
import com.brifo.server.decision.exception.DecisionAlreadyExistsException
import com.brifo.server.decision.exception.DecisionRequestClosedException
import com.brifo.server.decision.repository.DecisionRepository
import com.brifo.server.global.exception.BusinessException
import com.brifo.server.news.entity.News
import com.brifo.server.news.entity.NewsCard
import com.brifo.server.stock.entity.Stock
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.test.assertFailsWith

class DecisionRequestServiceTest {
    private val briefingRepository = mock(BriefingRepository::class.java)
    private val decisionRepository = mock(DecisionRepository::class.java)

    @Test
    fun `15시 30분부터는 DB를 조회하지 않고 결정 등록을 거절한다`() {
        val service = serviceAt("2026-07-21T06:30:00Z")

        assertFailsWith<DecisionRequestClosedException> {
            service.request(
                UUID.randomUUID(),
                UUID.randomUUID(),
                DecisionDirection.UP,
                3,
            )
        }

        verifyNoInteractions(briefingRepository, decisionRepository)
    }

    @Test
    fun `확신도가 범위를 벗어나면 DB를 조회하지 않고 요청을 거절한다`() {
        assertFailsWith<BusinessException> {
            serviceAt("2026-07-21T05:00:00Z").request(
                UUID.randomUUID(),
                UUID.randomUUID(),
                DecisionDirection.UP,
                6,
            )
        }

        verifyNoInteractions(briefingRepository, decisionRepository)
    }

    @Test
    fun `소유한 브리핑을 찾을 수 없으면 등록을 거절한다`() {
        val userId = UUID.randomUUID()
        val briefingId = UUID.randomUUID()
        `when`(briefingRepository.findOwnedBriefing(userId, briefingId)).thenReturn(null)

        assertFailsWith<BriefingNotFoundException> {
            serviceAt("2026-07-21T05:00:00Z").request(
                userId,
                briefingId,
                DecisionDirection.UP,
                3,
            )
        }
    }

    @Test
    fun `완료되지 않은 브리핑이면 등록을 거절한다`() {
        val userId = UUID.randomUUID()
        val briefingId = UUID.randomUUID()
        val context = briefingContext(LocalDate.of(2026, 7, 21), BriefingStatus.ANALYZING)
        `when`(briefingRepository.findOwnedBriefing(userId, briefingId)).thenReturn(context.briefing)

        assertFailsWith<BriefingNotCompletedException> {
            serviceAt("2026-07-21T05:00:00Z").request(
                userId,
                briefingId,
                DecisionDirection.UP,
                3,
            )
        }
    }

    @Test
    fun `오늘 브리핑이 아니면 찾을 수 없는 브리핑으로 처리한다`() {
        val userId = UUID.randomUUID()
        val briefingId = UUID.randomUUID()
        val context = briefingContext(LocalDate.of(2026, 7, 20))
        `when`(briefingRepository.findOwnedBriefing(userId, briefingId)).thenReturn(context.briefing)

        assertFailsWith<BriefingNotFoundException> {
            serviceAt("2026-07-21T05:00:00Z").request(
                userId,
                briefingId,
                DecisionDirection.UP,
                3,
            )
        }
    }

    @Test
    fun `같은 사용자 종목 displayDate의 결정이 있으면 중복으로 처리한다`() {
        val userId = UUID.randomUUID()
        val briefingId = UUID.randomUUID()
        val date = LocalDate.of(2026, 7, 21)
        val context = briefingContext(date)
        `when`(briefingRepository.findOwnedBriefing(userId, briefingId)).thenReturn(context.briefing)
        `when`(decisionRepository.existsDailyDecision(userId, context.stockId, date)).thenReturn(true)

        assertFailsWith<DecisionAlreadyExistsException> {
            serviceAt("2026-07-21T05:00:00Z").request(
                userId,
                briefingId,
                DecisionDirection.DOWN,
                4,
            )
        }
    }

    @Test
    fun `완료된 오늘 브리핑으로 결정을 생성한다`() {
        val userId = UUID.randomUUID()
        val briefingId = UUID.randomUUID()
        val date = LocalDate.of(2026, 7, 21)
        val context = briefingContext(date)
        val decision = mock(Decision::class.java)
        val decisionId = UUID.randomUUID()
        `when`(briefingRepository.findOwnedBriefing(userId, briefingId)).thenReturn(context.briefing)
        `when`(decisionRepository.existsDailyDecision(userId, context.stockId, date)).thenReturn(false)
        `when`(decision.publicId).thenReturn(decisionId)
        `when`(decision.direction).thenReturn(DecisionDirection.NEUTRAL)
        `when`(decision.confidenceLevel).thenReturn(2.toShort())
        `when`(decisionRepository.saveAndFlush(any(Decision::class.java))).thenReturn(decision)

        val response = serviceAt("2026-07-21T05:00:00Z").request(
            userId,
            briefingId,
            DecisionDirection.NEUTRAL,
            2,
        )

        kotlin.test.assertEquals(decisionId, response.decisionId)
        kotlin.test.assertEquals(context.stockId, response.stock.stockId)
    }

    private fun serviceAt(instant: String) =
        DecisionRequestService(
            briefingRepository,
            decisionRepository,
            Clock.fixed(Instant.parse(instant), ZoneId.of("Asia/Seoul")),
        )

    private fun briefingContext(
        displayDate: LocalDate,
        status: BriefingStatus = BriefingStatus.COMPLETED,
    ): BriefingContext {
        val stockId = UUID.randomUUID()
        val stock = mock(Stock::class.java)
        val news = mock(News::class.java)
        val newsCard = mock(NewsCard::class.java)
        val briefing = mock(Briefing::class.java)
        `when`(stock.publicId).thenReturn(stockId)
        `when`(stock.name).thenReturn("삼성전자")
        `when`(news.stock).thenReturn(stock)
        `when`(newsCard.news).thenReturn(news)
        `when`(newsCard.displayDate).thenReturn(displayDate)
        `when`(briefing.newsCards).thenReturn(listOf(newsCard))
        `when`(briefing.status).thenReturn(status)
        return BriefingContext(briefing, stockId)
    }

    private data class BriefingContext(
        val briefing: Briefing,
        val stockId: UUID,
    )
}
