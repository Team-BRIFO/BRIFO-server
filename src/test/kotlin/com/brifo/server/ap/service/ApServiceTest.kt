package com.brifo.server.ap.service

import com.brifo.server.agent.entity.Agent
import com.brifo.server.agent.exception.AgentNotFoundException
import com.brifo.server.agent.repository.AgentRepository
import com.brifo.server.ap.dto.request.CreateCreditLoanRequest
import com.brifo.server.ap.dto.request.GetApTransactionsRequest
import com.brifo.server.ap.dto.response.GetApTransactionsResponse
import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.ap.entity.ApTransactionTargetType
import com.brifo.server.ap.entity.AttendanceReward
import com.brifo.server.ap.exception.AttendanceRewardAlreadyClaimedException
import com.brifo.server.ap.exception.CreditLoanAlreadyClaimedException
import com.brifo.server.ap.exception.CreditLoanNotEligibleException
import com.brifo.server.ap.exception.TutorialRewardAlreadyClaimedException
import com.brifo.server.ap.repository.ApTransactionRepository
import com.brifo.server.ap.repository.AttendanceRewardRepository
import com.brifo.server.badge.code.BadgeCode
import com.brifo.server.badge.service.BadgeAwardService
import com.brifo.server.notification.service.NotificationCreationService
import com.brifo.server.user.entity.User
import com.brifo.server.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApServiceTest {
    private val zoneId = ZoneId.of("Asia/Seoul")
    private val clock = Clock.fixed(Instant.parse("2026-07-22T03:00:00Z"), zoneId)
    private lateinit var transactionService: ApTransactionService
    private lateinit var transactionRepository: ApTransactionRepository
    private lateinit var attendanceRepository: AttendanceRewardRepository
    private lateinit var agentRepository: AgentRepository
    private lateinit var userRepository: UserRepository
    private lateinit var badgeAwardService: BadgeAwardService
    private lateinit var notificationCreationService: NotificationCreationService
    private lateinit var service: ApService

    @BeforeEach
    fun setUp() {
        transactionService = mock(ApTransactionService::class.java)
        transactionRepository = mock(ApTransactionRepository::class.java)
        attendanceRepository = mock(AttendanceRewardRepository::class.java)
        agentRepository = mock(AgentRepository::class.java)
        userRepository = mock(UserRepository::class.java)
        badgeAwardService = mock(BadgeAwardService::class.java)
        notificationCreationService = mock(NotificationCreationService::class.java)
        service =
            ApService(
                transactionService,
                transactionRepository,
                attendanceRepository,
                agentRepository,
                userRepository,
                badgeAwardService,
                notificationCreationService,
                clock,
            )
    }

    @Test
    fun `연속 출석이 끊기면 1일차 기본 보상 50 AP를 지급한다`() {
        val userId = UUID.randomUUID()
        val user = user(id = 1L, balance = 0)
        val lastReward = attendance(id = 10L, consecutiveDays = 3, createdAt = LocalDateTime.of(2026, 7, 20, 9, 0))
        givenLockedUser(userId, user)
        `when`(attendanceRepository.findTopByUserIdOrderByCreatedAtDesc(1L)).thenReturn(lastReward)
        givenSavedAttendance(id = 11L)
        `when`(
            transactionService.change(
                userId,
                50,
                ApTransactionReason.ATTENDANCE,
                ApTransactionService.Target(ApTransactionTargetType.ATTENDANCE_REWARD, 11L),
            ),
        ).thenReturn(50)

        val response = service.createAttendanceReward(userId)

        assertEquals(50, response.rewardedAp)
        assertEquals(1, response.consecutiveDays)
        assertFalse(response.bonusRewarded)
        assertEquals(50, response.balanceAp)
    }

    @Test
    fun `전날이 6일차면 7일차 보너스를 포함해 250 AP를 지급한다`() {
        val userId = UUID.randomUUID()
        val user = user(id = 1L, balance = 100)
        val lastReward = attendance(id = 10L, consecutiveDays = 6, createdAt = LocalDateTime.of(2026, 7, 21, 9, 0))
        givenLockedUser(userId, user)
        `when`(attendanceRepository.findTopByUserIdOrderByCreatedAtDesc(1L)).thenReturn(lastReward)
        givenSavedAttendance(id = 11L)
        `when`(
            transactionService.change(
                userId,
                250,
                ApTransactionReason.ATTENDANCE,
                ApTransactionService.Target(ApTransactionTargetType.ATTENDANCE_REWARD, 11L),
            ),
        ).thenReturn(350)

        val response = service.createAttendanceReward(userId)

        assertEquals(250, response.rewardedAp)
        assertEquals(7, response.consecutiveDays)
        assertTrue(response.bonusRewarded)
        assertEquals(350, response.balanceAp)
        verify(badgeAwardService).awardBadge(userId, BadgeCode.B05)
        verify(badgeAwardService).awardBadge(userId, BadgeCode.B06)
    }

    @Test
    fun `전날이 7일차면 새로운 주기의 1일차가 된다`() {
        val userId = UUID.randomUUID()
        val user = user(id = 1L, balance = 250)
        val lastReward = attendance(id = 10L, consecutiveDays = 7, createdAt = LocalDateTime.of(2026, 7, 21, 9, 0))
        givenLockedUser(userId, user)
        `when`(attendanceRepository.findTopByUserIdOrderByCreatedAtDesc(1L)).thenReturn(lastReward)
        givenSavedAttendance(id = 11L)
        `when`(
            transactionService.change(
                userId,
                50,
                ApTransactionReason.ATTENDANCE,
                ApTransactionService.Target(ApTransactionTargetType.ATTENDANCE_REWARD, 11L),
            ),
        ).thenReturn(300)

        val response = service.createAttendanceReward(userId)

        assertEquals(1, response.consecutiveDays)
        assertEquals(50, response.rewardedAp)
        assertFalse(response.bonusRewarded)
    }

    @Test
    fun `오늘 이미 출석했으면 새 출석과 AP 원장을 만들지 않는다`() {
        val userId = UUID.randomUUID()
        val user = user(id = 1L, balance = 50)
        val todayReward = attendance(id = 10L, consecutiveDays = 1, createdAt = LocalDateTime.of(2026, 7, 22, 9, 0))
        givenLockedUser(userId, user)
        `when`(attendanceRepository.findTopByUserIdOrderByCreatedAtDesc(1L)).thenReturn(todayReward)

        assertFailsWith<AttendanceRewardAlreadyClaimedException> { service.createAttendanceReward(userId) }

        verify(attendanceRepository, never()).save(any(AttendanceReward::class.java))
        verifyNoMoreInteractions(transactionService)
    }

    @Test
    fun `튜토리얼 보상은 최초 한 번만 200 AP를 지급한다`() {
        val userId = UUID.randomUUID()
        val user = user(id = 1L, balance = 0)
        givenLockedUser(userId, user)
        `when`(transactionService.change(userId, 200, ApTransactionReason.TUTORIAL)).thenReturn(200)

        assertEquals(200, service.createTutorialReward(userId).balanceAp)
        verify(badgeAwardService).awardBadge(userId, BadgeCode.B01)
        inOrder(userRepository, transactionRepository, transactionService).apply {
            verify(userRepository).findForUpdateByPublicId(userId)
            verify(transactionRepository).existsByUserIdAndReason(1L, ApTransactionReason.TUTORIAL)
            verify(transactionService).change(userId, 200, ApTransactionReason.TUTORIAL)
        }

        `when`(transactionRepository.existsByUserIdAndReason(1L, ApTransactionReason.TUTORIAL)).thenReturn(true)
        assertFailsWith<TutorialRewardAlreadyClaimedException> { service.createTutorialReward(userId) }
    }

    @Test
    fun `거래 조회는 한 건을 더 조회해 다음 커서를 결정한다`() {
        val userId = UUID.randomUUID()
        val user = user(id = 1L, balance = 300)
        val ids = (1..3).map(::uuid)
        val items = ids.map { transactionItem(it) }
        `when`(userRepository.findByPublicId(userId)).thenReturn(user)
        `when`(
            transactionRepository.findMonthlyAmountsByUserId(
                1L,
                LocalDateTime.of(2026, 7, 1, 0, 0),
                LocalDateTime.of(2026, 8, 1, 0, 0),
            ),
        ).thenReturn(GetApTransactionsResponse.MonthlyAmounts(200, 50))
        `when`(transactionRepository.findPageByUserId(1L, null, 3)).thenReturn(items)

        val response = service.getApTransactions(userId, GetApTransactionsRequest(size = 2))

        assertEquals(300, response.summary.balanceAp)
        assertEquals(200, response.summary.monthlyEarnedAp)
        assertEquals(50, response.summary.monthlyLostAp)
        assertEquals(ids.take(2), response.page.items.map { it.apTransactionId })
        assertTrue(response.page.hasNext)
        assertEquals(ids[1], response.page.nextCursor)
    }

    @Test
    fun `신용대출은 잔액이 급여보다 1 적을 때 허용하고 같은 금액이면 거부한다`() {
        val userId = UUID.randomUUID()
        val agentId = UUID.randomUUID()
        val user = user(id = 1L, balance = 99)
        val agent = agent(id = 2L, owner = user, dailySalary = 100)
        givenLockedUser(userId, user)
        `when`(agentRepository.findByPublicId(agentId)).thenReturn(agent)
        `when`(transactionService.change(userId, 200, ApTransactionReason.CREDIT_LOAN)).thenReturn(299)

        assertEquals(299, service.createCreditLoan(userId, CreateCreditLoanRequest(agentId)).balanceAp)

        val boundaryUser = user(id = 3L, balance = 100)
        val boundaryAgent = agent(id = 4L, owner = boundaryUser, dailySalary = 100)
        givenLockedUser(userId, boundaryUser)
        `when`(agentRepository.findByPublicId(agentId)).thenReturn(boundaryAgent)
        assertFailsWith<CreditLoanNotEligibleException> {
            service.createCreditLoan(userId, CreateCreditLoanRequest(agentId))
        }
    }

    @Test
    fun `타인 사원과 이미 사용한 신용대출은 각각 정의된 예외를 던진다`() {
        val userId = UUID.randomUUID()
        val agentId = UUID.randomUUID()
        val user = user(id = 1L, balance = 0)
        val otherUser = user(id = 2L, balance = 0)
        givenLockedUser(userId, user)
        val otherAgent = agent(3L, otherUser, 100)
        `when`(agentRepository.findByPublicId(agentId)).thenReturn(otherAgent)
        assertFailsWith<AgentNotFoundException> {
            service.createCreditLoan(userId, CreateCreditLoanRequest(agentId))
        }

        val ownedAgent = agent(3L, user, 100)
        `when`(agentRepository.findByPublicId(agentId)).thenReturn(ownedAgent)
        `when`(transactionRepository.existsByUserIdAndReason(1L, ApTransactionReason.CREDIT_LOAN)).thenReturn(true)
        assertFailsWith<CreditLoanAlreadyClaimedException> {
            service.createCreditLoan(userId, CreateCreditLoanRequest(agentId))
        }
    }

    private fun givenLockedUser(
        userId: UUID,
        user: User,
    ) {
        `when`(userRepository.findForUpdateByPublicId(userId)).thenReturn(user)
    }

    private fun givenSavedAttendance(id: Long) {
        val savedAttendance = attendance(id, 1, null)
        `when`(attendanceRepository.save(any(AttendanceReward::class.java))).thenReturn(savedAttendance)
    }

    private fun user(
        id: Long,
        balance: Int,
    ): User = mock(User::class.java).also {
        `when`(it.id).thenReturn(id)
        `when`(it.balanceAp).thenReturn(balance)
    }

    private fun attendance(
        id: Long,
        consecutiveDays: Int,
        createdAt: LocalDateTime?,
    ): AttendanceReward = mock(AttendanceReward::class.java).also {
        `when`(it.id).thenReturn(id)
        `when`(it.consecutiveDays).thenReturn(consecutiveDays)
        `when`(it.createdAt).thenReturn(createdAt)
    }

    private fun agent(
        id: Long,
        owner: User,
        dailySalary: Int,
    ): Agent = mock(Agent::class.java).also {
        `when`(it.id).thenReturn(id)
        `when`(it.user).thenReturn(owner)
        `when`(it.dailySalary).thenReturn(dailySalary)
    }

    private fun transactionItem(id: UUID): GetApTransactionsResponse.ApTransactionItem =
        GetApTransactionsResponse.ApTransactionItem(
            apTransactionId = id,
            reason = ApTransactionReason.TUTORIAL,
            amount = 200,
            createdAt = LocalDateTime.of(2026, 7, 22, 12, 0),
        )

    private fun uuid(value: Int): UUID =
        UUID.fromString("00000000-0000-0000-0000-${value.toString().padStart(12, '0')}")
}
