package com.brifo.server.user.service

import com.brifo.server.agent.entity.Agent
import com.brifo.server.agent.entity.AgentType
import com.brifo.server.agent.repository.AgentRepository
import com.brifo.server.auth.service.JwtTokenProvider
import com.brifo.server.global.config.DevBehaviorProperties
import com.brifo.server.policy.repository.PolicyRepository
import com.brifo.server.policy.repository.UserPolicyRepository
import com.brifo.server.stock.entity.PendingUserStock
import com.brifo.server.stock.entity.UserStock
import com.brifo.server.stock.exception.StockNotFoundException
import com.brifo.server.stock.repository.PendingUserStockRepository
import com.brifo.server.stock.repository.StockRepository
import com.brifo.server.stock.repository.UserStockRepository
import com.brifo.server.user.dto.request.UpdateOnboardingProfileRequest
import com.brifo.server.user.dto.request.UpdateUserProfileRequest
import com.brifo.server.user.dto.response.CompleteOnboardingResponse
import com.brifo.server.user.entity.User
import com.brifo.server.user.exception.OnboardingProfileNotCompletedException
import com.brifo.server.user.exception.OnboardingStocksNotSelectedException
import com.brifo.server.user.exception.RequiredPoliciesNotAgreedException
import com.brifo.server.user.exception.UserNotFoundException
import com.brifo.server.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
    private val stockRepository: StockRepository,
    private val userStockRepository: UserStockRepository,
    private val pendingUserStockRepository: PendingUserStockRepository,
    private val policyRepository: PolicyRepository,
    private val userPolicyRepository: UserPolicyRepository,
    private val agentRepository: AgentRepository,
    private val userValidationService: UserValidationService,
    private val jwtTokenProvider: JwtTokenProvider,
    private val clock: Clock,
    private val devBehaviorProperties: DevBehaviorProperties = DevBehaviorProperties(),
) {
    @Transactional
    fun updateOnboardingProfile(
        userPublicId: UUID,
        request: UpdateOnboardingProfileRequest,
    ) {
        val nickname = userValidationService.normalizeNickname(request.nickname)
        val companyName = userValidationService.normalizeCompanyName(request.companyName)
        userValidationService.validateStockIds(request.stockIds)

        val user = userRepository.findForUpdateByPublicId(userPublicId) ?: throw UserNotFoundException()
        userValidationService.requireOnboardingPending(user)
        val stocks = findRequestedStocks(request.stockIds)

        user.updateOnboardingProfile(nickname, companyName)
        userStockRepository.deleteAllByUser(user)
        userStockRepository.saveAll(
            request.stockIds.map { stockId ->
                UserStock.create(user, requireNotNull(stocks[stockId]))
            },
        )
    }

    @Transactional
    fun updateUserProfile(
        userPublicId: UUID,
        request: UpdateUserProfileRequest,
    ) {
        val nickname = userValidationService.normalizeNickname(request.nickname)
        val companyName = requireNotNull(userValidationService.normalizeCompanyName(request.companyName))
        userValidationService.validateStockIds(request.stockIds)

        val stocksByPublicId = findRequestedStocks(request.stockIds)

        val user = userRepository.findForUpdateByPublicId(userPublicId) ?: throw UserNotFoundException()
        user.updateProfile(nickname, companyName)
        pendingUserStockRepository.deleteAllByUser(user)
        if (devBehaviorProperties.immediateInterestStockUpdates) {
            userStockRepository.deleteAllByUser(user)
            userStockRepository.saveAll(
                request.stockIds.map { UserStock.create(user, requireNotNull(stocksByPublicId[it])) },
            )
            return
        }
        val effectiveAt = LocalDate.now(clock).plusDays(1).atStartOfDay()
        pendingUserStockRepository.saveAll(
            request.stockIds.map {
                PendingUserStock.create(user, requireNotNull(stocksByPublicId[it]), effectiveAt)
            },
        )
    }

    private fun findRequestedStocks(stockIds: List<UUID>) =
        stockRepository
            .findAllByPublicIdInAndIsActiveTrue(stockIds)
            .associateBy { requireNotNull(it.publicId) { "Persisted stock must have a public id." } }
            .also {
                if (it.keys != stockIds.toSet()) {
                    throw StockNotFoundException()
                }
            }

    @Transactional
    fun deleteUser(userPublicId: UUID) {
        val user = userRepository.findForUpdateByPublicId(userPublicId) ?: throw UserNotFoundException()
        userRepository.delete(user)
    }

    @Transactional
    fun completeOnboarding(userPublicId: UUID): CompleteOnboardingResponse {
        val user = userRepository.findForUpdateByPublicId(userPublicId) ?: throw UserNotFoundException()
        completeOnboarding(user)
        val token = jwtTokenProvider.issueLoginTokens(userPublicId)
        return CompleteOnboardingResponse(
            token =
                CompleteOnboardingResponse.Token(
                    accessToken = token.accessToken,
                    refreshToken = token.refreshToken,
                    accessTokenExpiresIn = token.accessTokenExpiresIn,
                    refreshTokenExpiresIn = token.refreshTokenExpiresIn,
                ),
        )
    }

    private fun completeOnboarding(user: User) {
        userValidationService.requireOnboardingPending(user)

        val requiredPolicyCount = policyRepository.countByIsRequiredTrueAndIsActiveTrue()
        val agreedRequiredPolicyCount = userPolicyRepository.countActiveRequiredAgreements(user)
        if (agreedRequiredPolicyCount != requiredPolicyCount) {
            throw RequiredPoliciesNotAgreedException()
        }
        if (user.nickname.isNullOrBlank()) {
            throw OnboardingProfileNotCompletedException()
        }

        val interestStockCount = userStockRepository.countByUser(user)
        if (interestStockCount !in MIN_STOCK_SELECTION_COUNT..MAX_STOCK_SELECTION_COUNT.toLong()) {
            throw OnboardingStocksNotSelectedException()
        }

        check(!agentRepository.existsByUser(user)) { "Onboarding user must not already have agents." }
        user.completeOnboarding(LocalDateTime.now(clock))
        agentRepository.saveAll(DEFAULT_AGENT_PROFILES.map { it.createAgent(user) })
    }

    companion object {
        private const val MIN_STOCK_SELECTION_COUNT = 1L
        private const val MAX_STOCK_SELECTION_COUNT = 3
        private val DEFAULT_AGENT_PROFILES =
            listOf(
                DefaultAgentProfile(AgentType.ROOKIE, "Claude Haiku 4.5", "루키", 10),
                DefaultAgentProfile(AgentType.PRO, "Claude Sonnet 4.6", "프로", 15),
                DefaultAgentProfile(AgentType.TANKER, "Claude Opus 5", "탱커", 25),
            )
    }

    private data class DefaultAgentProfile(
        val agentType: AgentType,
        val modelName: String,
        val nickname: String,
        val dailySalary: Int,
    ) {
        fun createAgent(user: User): Agent =
            Agent.create(user, agentType, modelName, nickname, null, dailySalary)
    }
}
