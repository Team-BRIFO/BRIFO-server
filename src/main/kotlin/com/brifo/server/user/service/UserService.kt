package com.brifo.server.user.service

import com.brifo.server.agent.entity.Agent
import com.brifo.server.agent.entity.AgentType
import com.brifo.server.agent.repository.AgentRepository
import com.brifo.server.policy.repository.PolicyRepository
import com.brifo.server.policy.repository.UserPolicyRepository
import com.brifo.server.stock.entity.PendingUserStock
import com.brifo.server.stock.exception.StockNotFoundException
import com.brifo.server.stock.repository.PendingUserStockRepository
import com.brifo.server.stock.repository.StockRepository
import com.brifo.server.stock.repository.UserStockRepository
import com.brifo.server.user.dto.request.UpdateOnboardingProfileRequest
import com.brifo.server.user.dto.request.UpdateUserProfileRequest
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
    private val clock: Clock,
) {
    @Transactional
    fun updateOnboardingProfile(
        userPublicId: UUID,
        request: UpdateOnboardingProfileRequest,
    ) {
        val nickname = userValidationService.normalizeNickname(request.nickname)
        val companyName = userValidationService.normalizeCompanyName(request.companyName)
        val user = userRepository.findForUpdateByPublicId(userPublicId) ?: throw UserNotFoundException()
        userValidationService.requireOnboardingPending(user)
        user.updateOnboardingProfile(nickname, companyName)
    }

    @Transactional
    fun updateUserProfile(
        userPublicId: UUID,
        request: UpdateUserProfileRequest,
    ) {
        val nickname = userValidationService.normalizeNickname(request.nickname)
        val companyName = requireNotNull(userValidationService.normalizeCompanyName(request.companyName))
        userValidationService.validateStockIds(request.stockIds)

        val stocks = stockRepository.findAllByPublicIdInAndIsActiveTrue(request.stockIds)
        val stocksByPublicId = stocks.associateBy { requireNotNull(it.publicId) { "Persisted stock must have a public id." } }
        if (stocksByPublicId.keys != request.stockIds.toSet()) {
            throw StockNotFoundException()
        }

        val user = userRepository.findForUpdateByPublicId(userPublicId) ?: throw UserNotFoundException()
        user.updateProfile(nickname, companyName)
        pendingUserStockRepository.deleteAllByUser(user)
        val effectiveAt = LocalDate.now(clock).plusDays(1).atStartOfDay()
        pendingUserStockRepository.saveAll(
            request.stockIds.map {
                PendingUserStock.create(user, requireNotNull(stocksByPublicId[it]), effectiveAt)
            },
        )
    }

    @Transactional
    fun deleteUser(userPublicId: UUID) {
        val user = userRepository.findForUpdateByPublicId(userPublicId) ?: throw UserNotFoundException()
        userRepository.delete(user)
    }

    @Transactional
    fun completeOnboarding(userPublicId: UUID) {
        val user = userRepository.findForUpdateByPublicId(userPublicId) ?: throw UserNotFoundException()
        completeOnboarding(user)
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
