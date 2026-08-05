package com.brifo.server.user.service

import com.brifo.server.agent.entity.Agent
import com.brifo.server.agent.entity.AgentType
import com.brifo.server.agent.repository.AgentRepository
import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.ap.entity.AttendanceReward
import com.brifo.server.ap.repository.ApTransactionRepository
import com.brifo.server.ap.repository.AttendanceRewardRepository
import com.brifo.server.decision.repository.DecisionRepository
import com.brifo.server.decision.repository.DecisionResultRepository
import com.brifo.server.news.entity.NewsSource
import com.brifo.server.stock.entity.PendingUserStock
import com.brifo.server.stock.entity.Stock
import com.brifo.server.stock.entity.UserStock
import com.brifo.server.stock.repository.PendingUserStockRepository
import com.brifo.server.stock.repository.UserStockRepository
import com.brifo.server.term.repository.UserLearnedTermRepository
import com.brifo.server.user.entity.User
import com.brifo.server.user.exception.UserNotFoundException
import com.brifo.server.user.repository.UserHomeNewsCard
import com.brifo.server.user.repository.UserHomeQueryRepository
import com.brifo.server.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

class UserQueryServiceTest {
    private val userRepository = mock(UserRepository::class.java)
    private val userStockRepository = mock(UserStockRepository::class.java)
    private val pendingUserStockRepository = mock(PendingUserStockRepository::class.java)
    private val agentRepository = mock(AgentRepository::class.java)
    private val apTransactionRepository = mock(ApTransactionRepository::class.java)
    private val attendanceRewardRepository = mock(AttendanceRewardRepository::class.java)
    private val decisionRepository = mock(DecisionRepository::class.java)
    private val decisionResultRepository = mock(DecisionResultRepository::class.java)
    private val userLearnedTermRepository = mock(UserLearnedTermRepository::class.java)
    private val userHomeQueryRepository = mock(UserHomeQueryRepository::class.java)
    private val validationService = mock(UserValidationService::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-07-21T09:00:00Z"), ZoneId.of("Asia/Seoul"))
    private lateinit var queryService: UserQueryService

    @BeforeEach
    fun setUp() {
        queryService =
            UserQueryService(
                userRepository,
                userStockRepository,
                pendingUserStockRepository,
                agentRepository,
                apTransactionRepository,
                attendanceRewardRepository,
                decisionRepository,
                decisionResultRepository,
                userLearnedTermRepository,
                userHomeQueryRepository,
                validationService,
                clock,
            )
    }

    @Test
    fun `마이페이지 통계는 주간 범위와 비율 및 연속 출석 정책을 반영한다`() {
        val userId = UUID.randomUUID()
        val user = completedUser()
        val attendance = mock(AttendanceReward::class.java)
        `when`(userRepository.findByPublicId(userId)).thenReturn(user)
        `when`(apTransactionRepository.sumEarnedAmount(
            7L,
            ApTransactionReason.SALARY_REFUND,
            LocalDateTime.of(2026, 7, 20, 0, 0),
            LocalDateTime.of(2026, 7, 21, 18, 0),
        )).thenReturn(450)
        `when`(decisionResultRepository.countByDecisionBriefingAgentUserIdAndIsCorrect(7L, true)).thenReturn(2)
        `when`(decisionResultRepository.countByDecisionBriefingAgentUserId(7L)).thenReturn(3)
        `when`(decisionRepository.countByBriefingAgentUserId(7L)).thenReturn(48)
        `when`(attendanceRewardRepository.findTopByUserIdOrderByCreatedAtDescIdDesc(7L)).thenReturn(attendance)
        `when`(attendance.consecutiveDays).thenReturn(5)
        `when`(attendance.createdAt).thenReturn(LocalDateTime.of(2026, 7, 20, 9, 0))
        `when`(userLearnedTermRepository.countByUserId(7L)).thenReturn(24)

        val response = queryService.getMyPage(userId)

        assertEquals(450, response.thisWeekEarnedAp)
        assertEquals(67, response.decisionAccuracyRate)
        assertEquals(48, response.totalDecision)
        assertEquals(5, response.consecutiveDays)
        assertEquals(24, response.learnedTermCount)
        verify(validationService).requireOnboardingCompleted(user)
    }

    @Test
    fun `정산 결정이나 유효한 최근 출석이 없으면 비율과 연속 출석은 0이다`() {
        val userId = UUID.randomUUID()
        val user = completedUser()
        val attendance = mock(AttendanceReward::class.java)
        `when`(userRepository.findByPublicId(userId)).thenReturn(user)
        `when`(attendanceRewardRepository.findTopByUserIdOrderByCreatedAtDescIdDesc(7L)).thenReturn(attendance)
        `when`(attendance.consecutiveDays).thenReturn(7)
        `when`(attendance.createdAt).thenReturn(LocalDateTime.of(2026, 7, 19, 9, 0))

        val response = queryService.getMyPage(userId)

        assertEquals(0, response.decisionAccuracyRate)
        assertEquals(0, response.consecutiveDays)
    }

    @Test
    fun `홈 조회는 에이전트 순서와 출석 현황 및 오늘 범위와 카드뉴스를 반영한다`() {
        val userId = UUID.randomUUID()
        val user = completedUser()
        val card = newsCard()
        val mondayAttendance = attendanceAt(LocalDateTime.of(2026, 7, 20, 9, 0))
        val sundayAttendance = attendanceAt(LocalDateTime.of(2026, 7, 26, 23, 59, 59))
        val agents =
            listOf(
                agent(AgentType.PRO),
                agent(AgentType.ROOKIE),
                agent(AgentType.TANKER),
            )
        `when`(userRepository.findByPublicId(userId)).thenReturn(user)
        `when`(agentRepository.findAllByUserId(7L)).thenReturn(agents)
        `when`(
            attendanceRewardRepository.existsByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                7L,
                LocalDateTime.of(2026, 7, 21, 0, 0),
                LocalDateTime.of(2026, 7, 22, 0, 0),
            ),
        ).thenReturn(true)
        `when`(
            attendanceRewardRepository.findAllByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(
                7L,
                LocalDateTime.of(2026, 7, 20, 0, 0),
                LocalDateTime.of(2026, 7, 27, 0, 0),
            ),
        ).thenReturn(listOf(mondayAttendance, sundayAttendance))
        `when`(decisionRepository.countByUserIdWithinPeriod(
            7L,
            LocalDateTime.of(2026, 7, 21, 0, 0),
            LocalDateTime.of(2026, 7, 22, 0, 0),
        )).thenReturn(2)
        `when`(userHomeQueryRepository.findTodayNewsCards(
            7L,
            LocalDate.of(2026, 7, 21),
        )).thenReturn(listOf(card))

        val response = queryService.getUserHome(userId)

        assertEquals(listOf(AgentType.ROOKIE, AgentType.TANKER, AgentType.PRO), response.agents.map { it.agentType })
        assertEquals(true, response.attendedToday)
        assertEquals(2, response.weeklyAttendanceDays)
        assertEquals(listOf(LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 26)), response.dates)
        assertEquals(2, response.todayDecisions.count)
        assertEquals(null, response.todayNewsCards.batchTime)
        assertEquals(card.cardId, response.todayNewsCards.items.single().cardId)
        assertEquals(BigDecimal("1.3"), response.todayNewsCards.items.single().stock.changeRate)
    }

    @Test
    fun `출석과 배치 시각이 없으면 기본 출석 현황과 오늘 카드뉴스를 반환한다`() {
        val userId = UUID.randomUUID()
        val user = completedUser()
        val agents = requiredAgents()
        `when`(userRepository.findByPublicId(userId)).thenReturn(user)
        `when`(agentRepository.findAllByUserId(7L)).thenReturn(agents)
        `when`(
            attendanceRewardRepository.existsByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                7L,
                LocalDateTime.of(2026, 7, 21, 0, 0),
                LocalDateTime.of(2026, 7, 22, 0, 0),
            ),
        ).thenReturn(false)
        `when`(
            attendanceRewardRepository.findAllByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(
                7L,
                LocalDateTime.of(2026, 7, 20, 0, 0),
                LocalDateTime.of(2026, 7, 27, 0, 0),
            ),
        ).thenReturn(emptyList())
        `when`(userHomeQueryRepository.findTodayNewsCards(7L, LocalDate.of(2026, 7, 21)))
            .thenReturn(emptyList())

        val response = queryService.getUserHome(userId)

        assertEquals(false, response.attendedToday)
        assertEquals(0, response.weeklyAttendanceDays)
        assertEquals(emptyList<LocalDate>(), response.dates)
        assertEquals(null, response.todayNewsCards.batchTime)
        assertEquals(emptyList<Any>(), response.todayNewsCards.items)
        verify(attendanceRewardRepository).existsByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            7L,
            LocalDateTime.of(2026, 7, 21, 0, 0),
            LocalDateTime.of(2026, 7, 22, 0, 0),
        )
        verify(attendanceRewardRepository).findAllByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(
            7L,
            LocalDateTime.of(2026, 7, 20, 0, 0),
            LocalDateTime.of(2026, 7, 27, 0, 0),
        )
        verify(userHomeQueryRepository).findTodayNewsCards(7L, LocalDate.of(2026, 7, 21))
    }

    @Test
    fun `적용 예정 관심 종목이 없으면 프로필 조회는 현재 관심 종목을 반환한다`() {
        val userId = UUID.randomUUID()
        val user = completedUser()
        val stockId = UUID.randomUUID()
        val stock = mock(Stock::class.java)
        val userStock = mock(UserStock::class.java)
        `when`(userRepository.findByPublicId(userId)).thenReturn(user)
        `when`(userStockRepository.findAllByUser(user)).thenReturn(listOf(userStock))
        `when`(userStock.stock).thenReturn(stock)
        `when`(stock.publicId).thenReturn(stockId)
        `when`(stock.name).thenReturn("삼성전자")

        val response = queryService.getUserProfile(userId)

        assertEquals("닉네임", response.nickname)
        assertEquals(listOf(stockId), response.stocks.map { it.stockId })
        assertEquals(listOf("삼성전자"), response.stocks.map { it.name })
    }

    @Test
    fun `프로필 조회는 적용 예정 관심 종목을 우선 반환한다`() {
        val userId = UUID.randomUUID()
        val user = completedUser()
        val currentStock = mock(Stock::class.java)
        val currentUserStock = mock(UserStock::class.java)
        val pendingStockId = UUID.randomUUID()
        val pendingStock = mock(Stock::class.java)
        val pendingUserStock = mock(PendingUserStock::class.java)
        `when`(userRepository.findByPublicId(userId)).thenReturn(user)
        `when`(pendingUserStockRepository.findAllByUser(user)).thenReturn(listOf(pendingUserStock))
        `when`(pendingUserStock.stock).thenReturn(pendingStock)
        `when`(pendingStock.publicId).thenReturn(pendingStockId)
        `when`(pendingStock.name).thenReturn("SK하이닉스")
        `when`(currentUserStock.stock).thenReturn(currentStock)
        `when`(userStockRepository.findAllByUser(user)).thenReturn(listOf(currentUserStock))

        val response = queryService.getUserProfile(userId)

        assertEquals(listOf(pendingStockId), response.stocks.map { it.stockId })
        assertEquals(listOf("SK하이닉스"), response.stocks.map { it.name })
        verify(userStockRepository, never()).findAllByUser(user)
    }

    @Test
    fun `존재하지 않는 사용자는 조회 의존성을 호출하지 않는다`() {
        val userId = UUID.randomUUID()
        `when`(userRepository.findByPublicId(userId)).thenReturn(null)

        assertThrows(UserNotFoundException::class.java) {
            queryService.getUserProfile(userId)
        }

        verifyNoInteractions(userStockRepository, pendingUserStockRepository, validationService)
    }

    private fun completedUser(): User =
        mock(User::class.java).also {
            `when`(it.id).thenReturn(7L)
            `when`(it.nickname).thenReturn("닉네임")
            `when`(it.companyName).thenReturn("회사")
            `when`(it.balanceAp).thenReturn(1_250)
        }

    private fun attendanceAt(createdAt: LocalDateTime): AttendanceReward =
        mock(AttendanceReward::class.java).also {
            `when`(it.createdAt).thenReturn(createdAt)
        }

    private fun agent(type: AgentType): Agent =
        mock(Agent::class.java).also {
            `when`(it.publicId).thenReturn(UUID.randomUUID())
            `when`(it.agentType).thenReturn(type)
            `when`(it.level).thenReturn(1)
        }

    private fun requiredAgents(): List<Agent> =
        listOf(agent(AgentType.ROOKIE), agent(AgentType.TANKER), agent(AgentType.PRO))

    private fun newsCard(): UserHomeNewsCard =
        UserHomeNewsCard(
            cardId = UUID.randomUUID(),
            headline = "헤드라인",
            newsId = UUID.randomUUID(),
            publishedAt = LocalDateTime.of(2026, 7, 21, 7, 30),
            source = NewsSource.NAVER,
            stockId = UUID.randomUUID(),
            stockName = "삼성전자",
            changeRate = BigDecimal("1.34"),
        )
}
