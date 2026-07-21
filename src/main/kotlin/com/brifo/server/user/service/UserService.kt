package com.brifo.server.user.service

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.stock.entity.UserStock
import com.brifo.server.stock.exception.DuplicatedStockSelectionException
import com.brifo.server.stock.exception.StockNotFoundException
import com.brifo.server.stock.exception.StockSelectionMaximumExceededException
import com.brifo.server.stock.exception.StockSelectionMinimumNotMetException
import com.brifo.server.stock.repository.StockRepository
import com.brifo.server.stock.repository.UserStockRepository
import com.brifo.server.user.dto.request.UpdateOnboardingProfileRequest
import com.brifo.server.user.dto.request.UpdateUserProfileRequest
import com.brifo.server.user.dto.response.GetMyPageResponse
import com.brifo.server.user.dto.response.GetUserHomeResponse
import com.brifo.server.user.entity.OAuthProvider
import com.brifo.server.user.exception.InvalidCompanyNameException
import com.brifo.server.user.exception.InvalidNicknameException
import com.brifo.server.user.exception.OnboardingAlreadyCompletedException
import com.brifo.server.user.exception.UserNotFoundException
import com.brifo.server.user.repository.UserHomeData
import com.brifo.server.user.repository.UserHomeQueryRepository
import com.brifo.server.user.repository.UserMyPageQueryRepository
import com.brifo.server.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import kotlin.math.roundToInt

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userMyPageQueryRepository: UserMyPageQueryRepository,
    private val userHomeQueryRepository: UserHomeQueryRepository,
    private val stockRepository: StockRepository,
    private val userStockRepository: UserStockRepository,
    private val clock: Clock,
) {
    @Transactional
    fun updateOnboardingProfile(
        provider: OAuthProvider,
        socialId: String,
        request: UpdateOnboardingProfileRequest,
    ) {
        val nickname = validateNickname(request.nickname)
        val companyName = validateCompanyName(request.companyName)
        val user = userRepository.findByProviderAndSocialId(provider, socialId) ?: throw UserNotFoundException()

        if (user.onboardingCompletedAt != null) {
            throw OnboardingAlreadyCompletedException()
        }

        user.updateOnboardingProfile(nickname, companyName)
    }

    @Transactional(readOnly = true)
    fun getMyPage(userPublicId: UUID): GetMyPageResponse {
        val user = userRepository.findByPublicId(userPublicId) ?: throw UserNotFoundException()
        val userId = requireNotNull(user.id) { "Persisted user must have an id." }
        val now = LocalDateTime.now(clock)
        val today = LocalDate.now(clock)
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay()
        val stats = userMyPageQueryRepository.getMyPageStats(userId, weekStart, now)

        return GetMyPageResponse(
            nickname = requireNotNull(user.nickname) { "Onboarded user must have a nickname." },
            companyName = user.companyName,
            balanceAp = user.balanceAp,
            thisWeekEarnedAp = stats.thisWeekEarnedAp,
            decisionAccuracyRate = calculateAccuracyRate(stats.correctDecisionCount, stats.settledDecisionCount),
            totalDecision = Math.toIntExact(stats.totalDecisionCount),
            consecutiveDays = calculateConsecutiveDays(stats.latestConsecutiveDays, stats.latestAttendanceAt, today),
            learnedTermCount = Math.toIntExact(stats.learnedTermCount),
        )
    }

    @Transactional(readOnly = true)
    fun getUserHome(userPublicId: UUID): GetUserHomeResponse {
        val user = userRepository.findByPublicId(userPublicId) ?: throw UserNotFoundException()
        val userId = requireNotNull(user.id) { "Persisted user must have an id." }
        val todayStart = LocalDate.now(clock).atStartOfDay()
        val tomorrowStart = todayStart.plusDays(1)
        val homeData = userHomeQueryRepository.getUserHomeData(userId, todayStart, tomorrowStart)

        return GetUserHomeResponse(
            user =
                GetUserHomeResponse.User(
                    nickname = requireNotNull(user.nickname) { "Onboarded user must have a nickname." },
                    companyName = user.companyName,
                    balanceAp = user.balanceAp,
                ),
            agents = mapAgents(homeData),
            todayDecisions = GetUserHomeResponse.TodayDecisions(Math.toIntExact(homeData.todayDecisionCount)),
            todayNewsCards = mapTodayNewsCards(homeData),
        )
    }

    @Transactional
    fun updateUserProfile(
        userPublicId: UUID,
        request: UpdateUserProfileRequest,
    ) {
        val nickname = validateNickname(request.nickname)
        val companyName = requireNotNull(validateCompanyName(request.companyName))
        validateStockIds(request.stockIds)

        val user = userRepository.findByPublicId(userPublicId) ?: throw UserNotFoundException()
        val stocks = stockRepository.findAllByPublicIdInAndIsActiveTrue(request.stockIds)
        val stocksByPublicId = stocks.associateBy { requireNotNull(it.publicId) { "Persisted stock must have a public id." } }
        if (stocksByPublicId.keys != request.stockIds.toSet()) {
            throw StockNotFoundException()
        }

        val existingInterests = userStockRepository.findAllByUser(user)
        val existingStockIds = existingInterests.mapTo(mutableSetOf()) { requireNotNull(it.stock.publicId) }
        val requestedStockIds = request.stockIds.toSet()

        user.updateProfile(nickname, companyName)
        userStockRepository.deleteAllInBatch(
            existingInterests.filter { requireNotNull(it.stock.publicId) !in requestedStockIds },
        )
        userStockRepository.saveAll(
            request.stockIds
                .filterNot(existingStockIds::contains)
                .map { UserStock.create(user, requireNotNull(stocksByPublicId[it])) },
        )
    }

    @Transactional
    fun deleteUser(userPublicId: UUID) {
        val user = userRepository.findByPublicId(userPublicId) ?: throw UserNotFoundException()
        userRepository.delete(user)
    }

    private fun validateNickname(nickname: String): String =
        nickname.trim().takeIf { it.length in NICKNAME_LENGTH_RANGE }
            ?: throw InvalidNicknameException()

    private fun validateCompanyName(companyName: String?): String? {
        if (companyName == null) return null

        return companyName.trim().takeIf { it.length in COMPANY_NAME_LENGTH_RANGE }
            ?: throw InvalidCompanyNameException()
    }

    private fun validateStockIds(stockIds: List<UUID>) {
        when {
            stockIds.isEmpty() -> throw StockSelectionMinimumNotMetException()
            stockIds.size > MAX_STOCK_SELECTION_COUNT -> throw StockSelectionMaximumExceededException()
            stockIds.distinct().size != stockIds.size -> throw DuplicatedStockSelectionException()
        }
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

    private fun mapAgents(homeData: UserHomeData): List<GetUserHomeResponse.Agent> {
        val agentsByType = homeData.agents.associateBy { it.agentType }
        check(agentsByType.size == REQUIRED_AGENT_ORDER.size && agentsByType.keys.containsAll(REQUIRED_AGENT_ORDER)) {
            "User must have exactly one agent for every required agent type."
        }

        return REQUIRED_AGENT_ORDER.map { agentType ->
            val agent = requireNotNull(agentsByType[agentType])
            GetUserHomeResponse.Agent(agent.agentId, agent.agentType, agent.level)
        }
    }

    private fun mapTodayNewsCards(homeData: UserHomeData): GetUserHomeResponse.TodayNewsCards {
        val batchTime = homeData.batchTime
        if (batchTime == null) {
            return GetUserHomeResponse.TodayNewsCards(batchTime = null, items = emptyList())
        }

        return GetUserHomeResponse.TodayNewsCards(
            batchTime = batchTime,
            items =
                homeData.newsCards.map {
                    GetUserHomeResponse.TodayNewsCards.Item(
                        cardId = it.cardId,
                        headline = it.headline,
                        news = GetUserHomeResponse.TodayNewsCards.News(it.newsId, it.publishedAt, it.source),
                        stock = GetUserHomeResponse.TodayNewsCards.Stock(it.stockId, it.stockName, it.changeRate),
                    )
                },
        )
    }

    companion object {
        private val REQUIRED_AGENT_ORDER = listOf(AgentType.ROOKIE, AgentType.TANKER, AgentType.PRO)
        private val NICKNAME_LENGTH_RANGE = 1..50
        private val COMPANY_NAME_LENGTH_RANGE = 1..100
        private const val MAX_STOCK_SELECTION_COUNT = 3
    }
}
