package com.brifo.server.batch.settlement

import com.brifo.server.agent.entity.Agent
import com.brifo.server.agent.repository.AgentRepository
import com.brifo.server.ap.service.ApTransactionService
import com.brifo.server.badge.code.BadgeCode
import com.brifo.server.badge.service.BadgeAwardService
import com.brifo.server.briefing.entity.Briefing
import com.brifo.server.decision.entity.Decision
import com.brifo.server.decision.entity.DecisionDirection
import com.brifo.server.decision.entity.DecisionResult
import com.brifo.server.decision.repository.DecisionRepository
import com.brifo.server.decision.repository.DecisionResultRepository
import com.brifo.server.diary.entity.DiaryEntry
import com.brifo.server.diary.repository.DiaryEntryRepository
import com.brifo.server.news.entity.News
import com.brifo.server.news.entity.NewsCard
import com.brifo.server.notification.entity.NotificationCode
import com.brifo.server.notification.entity.NotificationTargetType
import com.brifo.server.notification.service.NotificationCreationService
import com.brifo.server.stock.entity.DailyStockPrice
import com.brifo.server.stock.entity.Stock
import com.brifo.server.stock.repository.DailyStockPriceRepository
import com.brifo.server.user.entity.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals

class DecisionSettlementServiceTest {
    private val decisionRepository = mock(DecisionRepository::class.java)
    private val resultRepository = mock(DecisionResultRepository::class.java)
    private val priceRepository = mock(DailyStockPriceRepository::class.java)
    private val diaryRepository = mock(DiaryEntryRepository::class.java)
    private val agentRepository = mock(AgentRepository::class.java)
    private val apService = mock(ApTransactionService::class.java)
    private val badgeService = mock(BadgeAwardService::class.java)
    private val notificationService = mock(NotificationCreationService::class.java)
    private val service =
        DecisionSettlementService(
            decisionRepository,
            resultRepository,
            priceRepository,
            diaryRepository,
            agentRepository,
            apService,
            badgeService,
            notificationService,
            DecisionSettlementCalculator(),
        )

    private val decisionId = 11L
    private val priceId = 21L
    private val userId = 31L
    private val userPublicId = UUID.randomUUID()
    private val decisionPublicId = UUID.randomUUID()
    private val agentPublicId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        `when`(resultRepository.existsByDecisionId(decisionId)).thenReturn(false)
    }

    @Test
    fun `이미 결과가 있는 결정은 어떤 부가효과도 다시 처리하지 않는다`() {
        `when`(resultRepository.existsByDecisionId(decisionId)).thenReturn(true)

        service.settle(DecisionSettlementItem(decisionId, priceId, true))

        verify(decisionRepository, never()).findById(decisionId)
        org.mockito.Mockito.verifyNoInteractions(apService, notificationService)
    }

    @Test
    fun `적중 결정을 정산하면 결과 AP EXP 일기 뱃지 알림을 함께 반영한다`() {
        val fixture = fixture(direction = DecisionDirection.UP, confidence = 5, leveledUp = true)
        `when`(resultRepository.countByDecisionBriefingAgentUserIdAndIsCorrect(userId, true)).thenReturn(10L)
        `when`(
            resultRepository.countByDecisionBriefingAgentUserIdAndIsCorrectAndDecisionDirection(
                userId,
                true,
                DecisionDirection.NEUTRAL,
            ),
        ).thenReturn(0L)
        `when`(agentRepository.existsByUserIdAndLevelGreaterThanEqual(userId, 5)).thenReturn(true)

        service.settle(DecisionSettlementItem(decisionId, priceId, true))

        assertEquals(1, org.mockito.Mockito.mockingDetails(resultRepository).invocations.count { it.method.name == "save" })
        verify(apService).settleDecision(userPublicId, 100, com.brifo.server.ap.entity.ApTransactionReason.DECISION_WIN, decisionId)
        verify(fixture.agent).addExperience(50)
        assertEquals(1, org.mockito.Mockito.mockingDetails(diaryRepository).invocations.count { it.method.name == "save" })
        verify(badgeService).awardBadge(userPublicId, BadgeCode.B03)
        verify(badgeService).awardBadge(userPublicId, BadgeCode.B04)
        verify(badgeService).awardBadge(userPublicId, BadgeCode.B07)
        verify(badgeService).awardBadge(userPublicId, BadgeCode.B10)

        val notificationCalls = org.mockito.Mockito.mockingDetails(notificationService).invocations
            .filter { it.method.name == "create" }
        val codes = notificationCalls.map { it.arguments[1] as NotificationCode }
        val targets = notificationCalls.map { it.arguments[2] as NotificationCreationService.Target }
        assertEquals(listOf(NotificationCode.DECISION_RESULT, NotificationCode.AGENT_LEVEL_UP), codes)
        assertEquals(NotificationTargetType.DECISION, targets[0].type)
        assertEquals(NotificationTargetType.AGENT, targets[1].type)
    }

    private fun fixture(
        direction: DecisionDirection,
        confidence: Int,
        leveledUp: Boolean,
    ): Fixture {
        val decision = mock(Decision::class.java)
        val briefing = mock(Briefing::class.java)
        val agent = mock(Agent::class.java)
        val user = mock(User::class.java)
        val card = mock(NewsCard::class.java)
        val news = mock(News::class.java)
        val stock = mock(Stock::class.java)
        val price = mock(DailyStockPrice::class.java)

        `when`(decision.id).thenReturn(decisionId)
        `when`(decision.publicId).thenReturn(decisionPublicId)
        `when`(decision.direction).thenReturn(direction)
        `when`(decision.confidenceLevel).thenReturn(confidence.toShort())
        `when`(decision.briefing).thenReturn(briefing)
        `when`(briefing.agent).thenReturn(agent)
        `when`(briefing.newsCards).thenReturn(listOf(card))
        `when`(agent.user).thenReturn(user)
        `when`(agent.publicId).thenReturn(agentPublicId)
        `when`(agent.addExperience(org.mockito.ArgumentMatchers.anyInt())).thenReturn(leveledUp)
        `when`(user.id).thenReturn(userId)
        `when`(user.publicId).thenReturn(userPublicId)
        `when`(card.news).thenReturn(news)
        `when`(news.stock).thenReturn(stock)
        `when`(price.stock).thenReturn(stock)
        `when`(decisionRepository.findById(decisionId)).thenReturn(Optional.of(decision))
        `when`(priceRepository.findById(priceId)).thenReturn(Optional.of(price))
        return Fixture(agent)
    }

    private data class Fixture(
        val agent: Agent,
    )
}
