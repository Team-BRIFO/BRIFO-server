package com.brifo.server.user.service

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.agent.repository.AgentRepository
import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.ap.repository.ApTransactionRepository
import com.brifo.server.ap.repository.AttendanceRewardRepository
import com.brifo.server.decision.repository.DecisionRepository
import com.brifo.server.decision.repository.DecisionResultRepository
import com.brifo.server.stock.repository.PendingUserStockRepository
import com.brifo.server.stock.repository.UserStockRepository
import com.brifo.server.term.repository.UserLearnedTermRepository
import com.brifo.server.user.dto.response.GetMyPageResponse
import com.brifo.server.user.dto.response.GetUserHomeResponse
import com.brifo.server.user.dto.response.GetUserProfileResponse
import com.brifo.server.user.entity.User
import com.brifo.server.user.exception.UserNotFoundException
import com.brifo.server.user.repository.UserHomeNewsCard
import com.brifo.server.user.repository.UserHomeQueryRepository
import com.brifo.server.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.RoundingMode
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import kotlin.math.roundToInt

@Service
class UserQueryService(
    private val userRepository: UserRepository,
    private val userStockRepository: UserStockRepository,
    private val pendingUserStockRepository: PendingUserStockRepository,
    private val agentRepository: AgentRepository,
    private val apTransactionRepository: ApTransactionRepository,
    private val attendanceRewardRepository: AttendanceRewardRepository,
    private val decisionRepository: DecisionRepository,
    private val decisionResultRepository: DecisionResultRepository,
    private val userLearnedTermRepository: UserLearnedTermRepository,
    private val userHomeQueryRepository: UserHomeQueryRepository,
    private val userValidationService: UserValidationService,
    private val clock: Clock,
) {
    @Transactional(readOnly = true)
    fun getMyPage(userPublicId: UUID): GetMyPageResponse {
        val user = findCompletedUser(userPublicId)
        val userId = requireNotNull(user.id) { "Persisted user must have an id." }
        val now = LocalDateTime.now(clock)
        val today = LocalDate.now(clock)
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay()
        val latestAttendance = attendanceRewardRepository.findTopByUserIdOrderByCreatedAtDescIdDesc(userId)
        val correctDecisionCount =
            decisionResultRepository.countByDecisionBriefingAgentUserIdAndIsCorrect(userId, true)
        val settledDecisionCount = decisionResultRepository.countByDecisionBriefingAgentUserId(userId)

        return GetMyPageResponse(
            nickname = requireNotNull(user.nickname) { "Onboarded user must have a nickname." },
            companyName = user.companyName,
            balanceAp = user.balanceAp,
            thisWeekEarnedAp =
                Math.toIntExact(
                    apTransactionRepository.sumEarnedAmount(
                        userId,
                        ApTransactionReason.SALARY_REFUND,
                        weekStart,
                        now,
                    ),
                ),
            decisionAccuracyRate = calculateAccuracyRate(correctDecisionCount, settledDecisionCount),
            totalDecision = Math.toIntExact(decisionRepository.countByBriefingAgentUserId(userId)),
            consecutiveDays =
                calculateConsecutiveDays(
                    latestAttendance?.consecutiveDays ?: 0,
                    latestAttendance?.createdAt,
                    today,
                ),
            learnedTermCount = Math.toIntExact(userLearnedTermRepository.countByUserId(userId)),
            stocks =
                userStockRepository.findAllByUser(user).map {
                    val stock = it.stock
                    GetMyPageResponse.Stock(
                        stockId = requireNotNull(stock.publicId) { "Persisted stock must have a public id." },
                        name = stock.name,
                    )
                },
        )
    }

    @Transactional(readOnly = true)
    fun getUserHome(userPublicId: UUID): GetUserHomeResponse {
        val user = findCompletedUser(userPublicId)
        val userId = requireNotNull(user.id) { "Persisted user must have an id." }
        val today = LocalDate.now(clock)
        val todayStart = today.atStartOfDay()
        val tomorrowStart = todayStart.plusDays(1)
        val newsCards = userHomeQueryRepository.findTodayNewsCards(userId, today)

        return GetUserHomeResponse(
            user =
                GetUserHomeResponse.User(
                    nickname = requireNotNull(user.nickname) { "Onboarded user must have a nickname." },
                    companyName = user.companyName,
                    balanceAp = user.balanceAp,
                ),
            agents = mapAgents(userId),
            todayDecisions =
                GetUserHomeResponse.TodayDecisions(
                    Math.toIntExact(
                        decisionRepository
                            .countByUserIdWithinPeriod(
                                userId,
                                todayStart,
                                tomorrowStart,
                            ),
                    ),
                ),
            todayNewsCards = mapTodayNewsCards(newsCards),
        )
    }

    @Transactional(readOnly = true)
    fun getUserProfile(userPublicId: UUID): GetUserProfileResponse {
        val user = findCompletedUser(userPublicId)
        val pendingStocks = pendingUserStockRepository.findAllByUser(user)
        val stocks =
            if (pendingStocks.isNotEmpty()) {
                pendingStocks.map { it.stock }
            } else {
                userStockRepository.findAllByUser(user).map { it.stock }
            }

        return GetUserProfileResponse(
            nickname = requireNotNull(user.nickname) { "Onboarded user must have a nickname." },
            companyName = user.companyName,
            stocks =
                stocks.map { stock ->
                    GetUserProfileResponse.Stock(
                        stockId = requireNotNull(stock.publicId) { "Persisted stock must have a public id." },
                        name = stock.name,
                    )
                },
        )
    }

    private fun findCompletedUser(userPublicId: UUID): User {
        val user = userRepository.findByPublicId(userPublicId) ?: throw UserNotFoundException()
        userValidationService.requireOnboardingCompleted(user)
        return user
    }

    private fun calculateAccuracyRate(
        correctDecisionCount: Long,
        settledDecisionCount: Long,
    ): Int {
        if (settledDecisionCount == 0L) return 0
        return (correctDecisionCount.toDouble() * 100 / settledDecisionCount).roundToInt()
    }

    private fun calculateConsecutiveDays(
        latestConsecutiveDays: Int,
        latestAttendanceAt: LocalDateTime?,
        today: LocalDate,
    ): Int {
        val latestAttendanceDate = latestAttendanceAt?.toLocalDate() ?: return 0
        return if (latestAttendanceDate == today || latestAttendanceDate == today.minusDays(1)) {
            latestConsecutiveDays
        } else {
            0
        }
    }

    private fun mapAgents(userId: Long): List<GetUserHomeResponse.Agent> {
        val agentsByType = agentRepository.findAllByUserId(userId).associateBy { it.agentType }
        check(agentsByType.size == REQUIRED_AGENT_ORDER.size && agentsByType.keys.containsAll(REQUIRED_AGENT_ORDER)) {
            "User must have exactly one agent for every required agent type."
        }
        return REQUIRED_AGENT_ORDER.map { agentType ->
            val agent = requireNotNull(agentsByType[agentType])
            GetUserHomeResponse.Agent(requireNotNull(agent.publicId), agent.agentType, agent.level)
        }
    }

    private fun mapTodayNewsCards(
        newsCards: List<UserHomeNewsCard>,
    ): GetUserHomeResponse.TodayNewsCards =
        GetUserHomeResponse.TodayNewsCards(
            batchTime = null,
            items =
                newsCards.map {
                    GetUserHomeResponse.TodayNewsCards.Item(
                        cardId = it.cardId,
                        headline = it.headline,
                        news = GetUserHomeResponse.TodayNewsCards.News(it.newsId, it.publishedAt, it.source),
                        stock =
                            GetUserHomeResponse.TodayNewsCards.Stock(
                                it.stockId,
                                it.stockName,
                                it.changeRate?.setScale(1, RoundingMode.HALF_UP),
                            ),
                    )
                },
        )

    companion object {
        private val REQUIRED_AGENT_ORDER = listOf(AgentType.ROOKIE, AgentType.TANKER, AgentType.PRO)
    }
}
