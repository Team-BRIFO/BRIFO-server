package com.brifo.server.notification.service

import com.brifo.server.agent.entity.Agent
import com.brifo.server.agent.entity.AgentType
import com.brifo.server.agent.repository.AgentRepository
import com.brifo.server.ap.entity.ApTransaction
import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.ap.entity.ApTransactionTargetType
import com.brifo.server.ap.entity.AttendanceReward
import com.brifo.server.ap.repository.ApTransactionRepository
import com.brifo.server.ap.repository.AttendanceRewardRepository
import com.brifo.server.badge.entity.Badge
import com.brifo.server.badge.entity.UserBadge
import com.brifo.server.badge.repository.UserBadgeRepository
import com.brifo.server.briefing.entity.BriefingDirection
import com.brifo.server.decision.entity.DecisionDirection
import com.brifo.server.notification.entity.Notification
import com.brifo.server.notification.entity.NotificationCode
import com.brifo.server.notification.entity.NotificationTargetType
import com.brifo.server.notification.entity.NotificationType
import com.brifo.server.notification.repository.NotificationContentProjection
import com.brifo.server.notification.repository.NotificationRepository
import com.brifo.server.notification.repository.NotificationTypeRepository
import com.brifo.server.user.entity.User
import com.brifo.server.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class NotificationCreationServiceUnitTest {
    private lateinit var userRepository: UserRepository
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var notificationTypeRepository: NotificationTypeRepository
    private lateinit var userBadgeRepository: UserBadgeRepository
    private lateinit var attendanceRewardRepository: AttendanceRewardRepository
    private lateinit var apTransactionRepository: ApTransactionRepository
    private lateinit var agentRepository: AgentRepository
    private lateinit var service: NotificationCreationService

    @BeforeEach
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        notificationRepository = mock(NotificationRepository::class.java)
        notificationTypeRepository = mock(NotificationTypeRepository::class.java)
        userBadgeRepository = mock(UserBadgeRepository::class.java)
        attendanceRewardRepository = mock(AttendanceRewardRepository::class.java)
        apTransactionRepository = mock(ApTransactionRepository::class.java)
        agentRepository = mock(AgentRepository::class.java)
        service =
            NotificationCreationService(
                userRepository,
                notificationRepository,
                notificationTypeRepository,
                userBadgeRepository,
                attendanceRewardRepository,
                apTransactionRepository,
                agentRepository,
            )
    }

    @Test
    fun `모든 알림 코드는 정해진 대상 타입 외의 입력을 거부한다`() {
        val expectedTypes =
            mapOf(
                NotificationCode.DECISION_RESULT to NotificationTargetType.DECISION,
                NotificationCode.BRIEFING_READY to NotificationTargetType.BRIEFING,
                NotificationCode.NEWS_CARD_ARRIVED to NotificationTargetType.NEWS_CARD_LIST,
                NotificationCode.BADGE_AWARDED to NotificationTargetType.BADGE,
                NotificationCode.ATTENDANCE_REWARDED to NotificationTargetType.NONE,
                NotificationCode.AGENT_SALARY_PAID to NotificationTargetType.STOCK_BRIEFINGS,
                NotificationCode.AGENT_LEVEL_UP to NotificationTargetType.AGENT,
            )

        expectedTypes.forEach { (code, expectedType) ->
            val wrongType = NotificationTargetType.entries.first { it != expectedType }
            assertThrows(IllegalArgumentException::class.java) {
                service.create(
                    UUID.randomUUID(),
                    code,
                    NotificationCreationService.Target(wrongType, null),
                )
            }
        }
        verifyNoInteractions(userRepository, notificationRepository)
    }

    @Test
    fun `DB에 알림 코드가 없으면 저장하지 않고 실패한다`() {
        val userId = UUID.randomUUID()
        val stockId = UUID.randomUUID()
        `when`(userRepository.findByPublicId(userId)).thenReturn(mock(com.brifo.server.user.entity.User::class.java))
        `when`(notificationTypeRepository.findByCode(NotificationCode.NEWS_CARD_ARRIVED.name)).thenReturn(null)

        assertThrows(IllegalStateException::class.java) {
            service.create(
                userId,
                NotificationCode.NEWS_CARD_ARRIVED,
                NotificationCreationService.Target(NotificationTargetType.NEWS_CARD_LIST, stockId),
            )
        }
        verifyNoInteractions(notificationRepository)
    }

    @Test
    fun `결정 결과와 브리핑 템플릿은 projection 값을 표시 형식으로 변환한다`() {
        val userId = UUID.randomUUID()
        val decisionId = UUID.randomUUID()
        val briefingId = UUID.randomUUID()
        `when`(notificationRepository.findDecisionResultContent(userId, decisionId)).thenReturn(
            NotificationContentProjection.DecisionResult("삼성전자", BigDecimal("1.90"), DecisionDirection.UP, true, 100, 1380),
        )
        `when`(notificationRepository.findBriefingReadyContent(userId, briefingId)).thenReturn(
            NotificationContentProjection.BriefingReady(
                AgentType.PRO,
                "SK하이닉스",
                BriefingDirection.DOWN,
                65.toShort(),
            ),
        )

        assertCreatedContent(
            userId = userId,
            code = NotificationCode.DECISION_RESULT,
            target = NotificationCreationService.Target(NotificationTargetType.DECISION, decisionId),
            expectedTitle = "오늘의 정산이 끝났어요",
            expectedBody = "삼성전자 ▲+1.9% · 상승 예측 적중 · AP +100 (잔액 1,380)",
        )
        `when`(notificationRepository.findDecisionResultContent(userId, decisionId)).thenReturn(
            NotificationContentProjection.DecisionResult(
                "삼성전자",
                BigDecimal("-1.90"),
                DecisionDirection.DOWN,
                false,
                -100,
                1280,
            ),
        )
        assertCreatedContent(
            userId = userId,
            code = NotificationCode.DECISION_RESULT,
            target = NotificationCreationService.Target(NotificationTargetType.DECISION, decisionId),
            expectedTitle = "오늘의 정산이 끝났어요",
            expectedBody = "삼성전자 ▼1.9% · 하락 예측 실패 · AP -100 (잔액 1,280)",
        )
        assertCreatedContent(
            userId = userId,
            code = NotificationCode.BRIEFING_READY,
            target = NotificationCreationService.Target(NotificationTargetType.BRIEFING, briefingId),
            expectedTitle = "프로의 브리핑이 도착했어요",
            expectedBody = "SK하이닉스 · 하락 65% 확신 · 브리핑룸에서 확인하세요",
        )
    }

    @Test
    fun `카드뉴스와 사원 의뢰비 템플릿은 사용자 단위 목록을 집계한다`() {
        val userId = UUID.randomUUID()
        val stockId = UUID.randomUUID()
        `when`(notificationRepository.findNewsCardContents(userId, stockId, LocalDate.now(SEOUL_ZONE))).thenReturn(
            listOf(
                NotificationContentProjection.NewsCard("삼성전자"),
                NotificationContentProjection.NewsCard("삼성전자"),
                NotificationContentProjection.NewsCard("NAVER"),
            ),
        )
        `when`(notificationRepository.findAgentSalaryContents(userId, stockId, LocalDate.now(SEOUL_ZONE))).thenReturn(
            listOf(
                NotificationContentProjection.AgentSalary(AgentType.ROOKIE, -10, 1280),
                NotificationContentProjection.AgentSalary(AgentType.PRO, -20, 1280),
                NotificationContentProjection.AgentSalary(AgentType.TANKER, -20, 1280),
            ),
        )

        assertCreatedContent(
            userId = userId,
            code = NotificationCode.NEWS_CARD_ARRIVED,
            target = NotificationCreationService.Target(NotificationTargetType.NEWS_CARD_LIST, stockId),
            expectedTitle = "새 카드뉴스 3건이 도착했어요",
            expectedBody = "관심 종목 삼성전자 · NAVER 관련 새 소식이 올라왔어요",
        )
        assertCreatedContent(
            userId = userId,
            code = NotificationCode.AGENT_SALARY_PAID,
            target = NotificationCreationService.Target(NotificationTargetType.STOCK_BRIEFINGS, stockId),
            expectedTitle = "분석 의뢰비가 지급됐어요",
            expectedBody = "루키 · 프로 · 탱커 3명에게 의뢰 · -50 AP (잔액 1,280)",
        )
    }

    @Test
    fun `배지 출석 템플릿은 전달된 이벤트를 조회하고 레벨업 템플릿은 최신 상태를 반영한다`() {
        val userId = UUID.randomUUID()
        val badgeId = UUID.randomUUID()
        val agentId = UUID.randomUUID()
        val userBadgeId = 21L
        val attendanceRewardId = 31L
        val badge = mock(Badge::class.java)
        val userBadge = mock(UserBadge::class.java)
        val reward = mock(AttendanceReward::class.java)
        val transaction = mock(ApTransaction::class.java)
        val agent = mock(Agent::class.java)
        `when`(badge.name).thenReturn("첫 적중")
        `when`(badge.description).thenReturn("첫 예측을 맞혔어요!")
        `when`(badge.rewardAp).thenReturn(50)
        `when`(userBadge.badge).thenReturn(badge)
        `when`(userBadgeRepository.findByIdAndUserPublicId(userBadgeId, userId)).thenReturn(userBadge)
        `when`(reward.id).thenReturn(attendanceRewardId)
        `when`(reward.consecutiveDays).thenReturn(3)
        `when`(attendanceRewardRepository.findByIdAndUserPublicId(attendanceRewardId, userId)).thenReturn(reward)
        `when`(
            apTransactionRepository.findTopByUserPublicIdAndTargetTypeAndTargetIdAndReasonInOrderByIdDesc(
                userId,
                ApTransactionTargetType.ATTENDANCE_REWARD,
                attendanceRewardId,
                listOf(ApTransactionReason.ATTENDANCE),
            ),
        ).thenReturn(transaction)
        `when`(transaction.amount).thenReturn(50)
        `when`(agent.agentType).thenReturn(AgentType.ROOKIE)
        `when`(agent.level).thenReturn(3)
        `when`(agentRepository.findByPublicIdAndUserPublicId(agentId, userId)).thenReturn(agent)

        assertCreatedContent(
            userId,
            NotificationCode.BADGE_AWARDED,
            NotificationCreationService.Target(NotificationTargetType.BADGE, badgeId),
            "뱃지를 획득했어요 · 첫 적중",
            "첫 예측을 맞혔어요! 보상 +50 AP를 지급했어요",
            userBadgeId,
        )
        assertCreatedContent(
            userId,
            NotificationCode.ATTENDANCE_REWARDED,
            NotificationCreationService.Target(NotificationTargetType.NONE, null),
            "출석 보너스 +50 AP",
            "3일 연속 출석 중이에요. 내일도 만나요!",
            attendanceRewardId,
        )
        assertCreatedContent(
            userId,
            NotificationCode.AGENT_LEVEL_UP,
            NotificationCreationService.Target(NotificationTargetType.AGENT, agentId),
            "루키의 레벨이 올랐어요",
            "루키가 3레벨이 되었어요",
        )
    }

    private fun assertCreatedContent(
        userId: UUID,
        code: NotificationCode,
        target: NotificationCreationService.Target,
        expectedTitle: String,
        expectedBody: String,
        eventId: Long? = null,
    ) {
        `when`(userRepository.findByPublicId(userId)).thenReturn(mock(User::class.java))
        `when`(notificationTypeRepository.findByCode(code.name)).thenReturn(mock(NotificationType::class.java))

        service.create(userId, code, target, eventId)

        val captor = ArgumentCaptor.forClass(Notification::class.java)
        verify(notificationRepository).save(captor.capture())
        assertEquals(expectedTitle, captor.value.title)
        assertEquals(expectedBody, captor.value.body)
        clearInvocations(notificationRepository)
    }

    companion object {
        private val SEOUL_ZONE = java.time.ZoneId.of("Asia/Seoul")
    }
}
