package com.brifo.server.decision.service

import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.ap.entity.ApTransactionTargetType
import com.brifo.server.ap.exception.InsufficientApBalanceException
import com.brifo.server.ap.service.ApTransactionService
import com.brifo.server.badge.code.BadgeCode
import com.brifo.server.badge.service.BadgeAwardService
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
import com.brifo.server.global.config.DevBehaviorProperties
import com.brifo.server.global.exception.BusinessException
import com.brifo.server.news.entity.News
import com.brifo.server.news.entity.NewsCard
import com.brifo.server.stock.entity.Stock
import com.brifo.server.user.entity.User
import com.brifo.server.user.repository.UserRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.context.ApplicationEventPublisher
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.test.assertFailsWith

class DecisionRequestServiceTest {
    private val briefingRepository = mock(BriefingRepository::class.java)
    private val decisionRepository = mock(DecisionRepository::class.java)
    private val badgeAwardService = mock(BadgeAwardService::class.java)
    private val apTransactionService = mock(ApTransactionService::class.java)
    private val userRepository = mock(UserRepository::class.java)

    @Test
    fun `15시 30분부터는 DB를 조회하지 않고 결정 등록을 거절한다`() {
        val service = serviceAt("2026-07-21T06:30:00Z")

        assertFailsWith<DecisionRequestClosedException> {
            service.request(
                UUID.randomUUID(),
                UUID.randomUUID(),
                DecisionDirection.UP,
                1_000,
            )
        }

        verifyNoInteractions(briefingRepository, decisionRepository)
    }

    @ParameterizedTest
    @ValueSource(strings = ["2026-07-18T01:00:00Z", "2026-07-19T01:00:00Z"])
    fun `주말에는 DB를 조회하지 않고 결정 등록을 거절한다`(instant: String) {
        val service = serviceAt(instant)

        assertFailsWith<DecisionRequestClosedException> {
            service.request(
                UUID.randomUUID(),
                UUID.randomUUID(),
                DecisionDirection.UP,
                1_000,
            )
        }

        verifyNoInteractions(briefingRepository, decisionRepository)
    }

    @ParameterizedTest
    @ValueSource(strings = ["2026-07-18T01:00:00Z", "2026-07-19T01:00:00Z"])
    fun `주말 시장 모드에서는 주말 결정을 등록하고 즉시 정산 이벤트를 발행한다`(instant: String) {
        val userId = UUID.randomUUID()
        val briefingId = UUID.randomUUID()
        val displayDate = LocalDate.ofInstant(Instant.parse(instant), ZoneId.of("Asia/Seoul"))
        val context = briefingContext(displayDate)
        val decision = mock(Decision::class.java)
        val eventPublisher = mock(ApplicationEventPublisher::class.java)
        `when`(briefingRepository.findOwnedBriefing(userId, briefingId)).thenReturn(context.briefing)
        `when`(decisionRepository.existsDailyDecision(userId, context.stockId, displayDate)).thenReturn(false)
        val user = mockUser(99_000)
        `when`(userRepository.findForUpdateByPublicId(userId)).thenReturn(user)
        `when`(decision.publicId).thenReturn(UUID.randomUUID())
        `when`(decision.id).thenReturn(1L)
        `when`(decision.direction).thenReturn(DecisionDirection.UP)
        `when`(decision.allocatedAp).thenReturn(1_000)
        `when`(decisionRepository.saveAndFlush(any(Decision::class.java))).thenReturn(decision)

        serviceAt(
            instant = instant,
            // 휴장일 정산은 개발용 즉시 정산 설정과 무관하게 동작해야 한다.
            devBehaviorProperties = DevBehaviorProperties(
                decisionRequestCutoffEnabled = false,
                immediateDecisionSettlement = false,
                weekendMarketEnabled = true,
            ),
            eventPublisher = eventPublisher,
        ).request(userId, briefingId, DecisionDirection.UP, 1_000)

        verify(eventPublisher).publishEvent(
            DecisionCreatedEvent(
                decisionPublicId = requireNotNull(decision.publicId),
                targetDate = displayDate,
            ),
        )
    }

    @Test
    fun `평일에는 즉시 정산 설정이 꺼져 있으면 정산 이벤트를 발행하지 않는다`() {
        val userId = UUID.randomUUID()
        val briefingId = UUID.randomUUID()
        val date = LocalDate.of(2026, 7, 21)
        val context = briefingContext(date)
        val decision = mock(Decision::class.java)
        val eventPublisher = mock(ApplicationEventPublisher::class.java)
        `when`(briefingRepository.findOwnedBriefing(userId, briefingId)).thenReturn(context.briefing)
        `when`(decisionRepository.existsDailyDecision(userId, context.stockId, date)).thenReturn(false)
        val user = mockUser(99_000)
        `when`(userRepository.findForUpdateByPublicId(userId)).thenReturn(user)
        `when`(decision.publicId).thenReturn(UUID.randomUUID())
        `when`(decision.id).thenReturn(1L)
        `when`(decision.direction).thenReturn(DecisionDirection.UP)
        `when`(decision.allocatedAp).thenReturn(1_000)
        `when`(decisionRepository.saveAndFlush(any(Decision::class.java))).thenReturn(decision)

        serviceAt(
            instant = "2026-07-21T05:00:00Z",
            devBehaviorProperties = DevBehaviorProperties(weekendMarketEnabled = true),
            eventPublisher = eventPublisher,
        ).request(userId, briefingId, DecisionDirection.UP, 1_000)

        verifyNoInteractions(eventPublisher)
    }

    @Test
    fun `배분 금액이 1 미만이면 DB를 조회하지 않고 요청을 거절한다`() {
        assertFailsWith<BusinessException> {
            serviceAt("2026-07-21T05:00:00Z").request(
                UUID.randomUUID(),
                UUID.randomUUID(),
                DecisionDirection.UP,
                0,
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
                1_000,
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
                1_000,
            )
        }
    }

    @Test
    fun `뉴스 카드가 없는 브리핑이면 찾을 수 없는 브리핑으로 처리한다`() {
        val userId = UUID.randomUUID()
        val briefingId = UUID.randomUUID()
        val briefing = mock(Briefing::class.java)
        `when`(briefing.newsCards).thenReturn(emptyList())
        `when`(briefing.status).thenReturn(BriefingStatus.COMPLETED)
        `when`(briefingRepository.findOwnedBriefing(userId, briefingId)).thenReturn(briefing)

        assertFailsWith<BriefingNotFoundException> {
            serviceAt("2026-07-21T05:00:00Z").request(
                userId,
                briefingId,
                DecisionDirection.UP,
                1_000,
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
                1_000,
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
                1_000,
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
        val user = mockUser(99_000)
        `when`(userRepository.findForUpdateByPublicId(userId)).thenReturn(user)
        `when`(decision.publicId).thenReturn(decisionId)
        `when`(decision.id).thenReturn(42L)
        `when`(decision.direction).thenReturn(DecisionDirection.NEUTRAL)
        `when`(decision.allocatedAp).thenReturn(1_000)
        `when`(decisionRepository.saveAndFlush(any(Decision::class.java))).thenReturn(decision)
        `when`(
            apTransactionService.change(
                userId,
                -1_000,
                ApTransactionReason.DECISION_ENTRY_FEE,
                ApTransactionService.Target(ApTransactionTargetType.DECISION, 42L),
            ),
        ).thenReturn(99_000)

        val response = serviceAt("2026-07-21T05:00:00Z").request(
            userId,
            briefingId,
            DecisionDirection.NEUTRAL,
            1_000,
        )

        kotlin.test.assertEquals(decisionId, response.decisionId)
        kotlin.test.assertEquals(context.stockId, response.stock.stockId)
        kotlin.test.assertEquals(1_000, response.allocatedAp)
        kotlin.test.assertEquals(99_000, response.balanceAp)
        verify(context.briefing, times(1)).newsCards
        verify(badgeAwardService).awardBadge(userId, BadgeCode.B02)
    }

    @Test
    fun `누적 예측 등록 횟수가 기준을 넘으면 단계별 뱃지를 함께 지급한다`() {
        val userId = UUID.randomUUID()
        val briefingId = UUID.randomUUID()
        val date = LocalDate.of(2026, 7, 21)
        val context = briefingContext(date)
        val decision = mock(Decision::class.java)
        `when`(briefingRepository.findOwnedBriefing(userId, briefingId)).thenReturn(context.briefing)
        `when`(decisionRepository.existsDailyDecision(userId, context.stockId, date)).thenReturn(false)
        val user = mockUser(99_000)
        `when`(user.id).thenReturn(9L)
        `when`(userRepository.findForUpdateByPublicId(userId)).thenReturn(user)
        `when`(decision.publicId).thenReturn(UUID.randomUUID())
        `when`(decision.id).thenReturn(1L)
        `when`(decision.direction).thenReturn(DecisionDirection.UP)
        `when`(decision.allocatedAp).thenReturn(1_000)
        `when`(decisionRepository.saveAndFlush(any(Decision::class.java))).thenReturn(decision)
        `when`(decisionRepository.countByBriefingAgentUserId(9L)).thenReturn(100L)

        serviceAt("2026-07-21T05:00:00Z").request(userId, briefingId, DecisionDirection.UP, 1_000)

        verify(badgeAwardService).awardBadge(userId, BadgeCode.B17)
        verify(badgeAwardService).awardBadge(userId, BadgeCode.B18)
        verify(badgeAwardService).awardBadge(userId, BadgeCode.B19)
        verify(badgeAwardService, never()).awardBadge(userId, BadgeCode.B20)
    }

    @Test
    fun `참가비를 낼 자금이 부족하면 결정을 저장하지 않고 등록을 거절한다`() {
        val userId = UUID.randomUUID()
        val briefingId = UUID.randomUUID()
        val date = LocalDate.of(2026, 7, 21)
        val context = briefingContext(date)
        `when`(briefingRepository.findOwnedBriefing(userId, briefingId)).thenReturn(context.briefing)
        `when`(decisionRepository.existsDailyDecision(userId, context.stockId, date)).thenReturn(false)
        val user = mockUser(999)
        `when`(userRepository.findForUpdateByPublicId(userId)).thenReturn(user)

        assertFailsWith<InsufficientApBalanceException> {
            serviceAt("2026-07-21T05:00:00Z").request(
                userId,
                briefingId,
                DecisionDirection.UP,
                1_000,
            )
        }

        verifyNoInteractions(badgeAwardService, apTransactionService)
        verify(decisionRepository, never()).saveAndFlush(any(Decision::class.java))
    }

    private fun serviceAt(
        instant: String,
        devBehaviorProperties: DevBehaviorProperties = DevBehaviorProperties(),
        eventPublisher: ApplicationEventPublisher? = null,
    ) = DecisionRequestService(
        briefingRepository,
        decisionRepository,
        badgeAwardService,
        apTransactionService,
        userRepository,
        Clock.fixed(Instant.parse(instant), ZoneId.of("Asia/Seoul")),
        devBehaviorProperties,
        eventPublisher,
    )

    private fun mockUser(balanceAp: Int): User =
        mock(User::class.java).also { `when`(it.balanceAp).thenReturn(balanceAp) }

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
