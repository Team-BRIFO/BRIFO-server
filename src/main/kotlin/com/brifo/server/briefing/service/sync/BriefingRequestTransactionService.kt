package com.brifo.server.briefing.service.sync

import com.brifo.server.agent.entity.Agent
import com.brifo.server.agent.exception.AgentNotFoundException
import com.brifo.server.agent.repository.AgentRepository
import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.ap.entity.ApTransactionTargetType
import com.brifo.server.ap.exception.InsufficientApBalanceException
import com.brifo.server.ap.service.ApTransactionService
import com.brifo.server.briefing.entity.Briefing
import com.brifo.server.briefing.entity.BriefingStatus
import com.brifo.server.briefing.exception.BriefingAgentNotInInitialRequestException
import com.brifo.server.briefing.exception.BriefingAlreadyRequestedException
import com.brifo.server.briefing.exception.BriefingRetryCooldownException
import com.brifo.server.briefing.repository.BriefingRepository
import com.brifo.server.news.entity.NewsCard
import com.brifo.server.news.exception.NewsCardNotFoundException
import com.brifo.server.news.repository.NewsCardRepository
import com.brifo.server.stock.exception.StockNotFoundException
import com.brifo.server.stock.repository.UserStockRepository
import com.brifo.server.user.entity.User
import com.brifo.server.user.exception.UserNotFoundException
import com.brifo.server.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import kotlin.math.ceil

/** 브리핑 요청에 필요한 잠금, DB 검증과 상태 변경을 하나의 트랜잭션으로 처리한다. */
@Service
class BriefingRequestTransactionService(
    private val userRepository: UserRepository,
    private val userStockRepository: UserStockRepository,
    private val agentRepository: AgentRepository,
    private val newsCardRepository: NewsCardRepository,
    private val briefingRepository: BriefingRepository,
    private val apTransactionService: ApTransactionService,
) {
    @Transactional
    fun request(command: BriefingRequestTask.Command): BriefingRequestTask.Result {
        // 락 전 검증 수행
        val newsCards = validateBeforeLock(command)
        // 락 이후 검증 수행
        val lockedRequest = lockAndValidate(command)

        // 사용자가 오늘 요청한 브리핑 조회
        val dailyBriefings = findDailyBriefings(command)
        val briefings = prepareBriefings(
            dailyBriefings = dailyBriefings,
            command = command,
            agents = lockedRequest.agents,
            newsCards = newsCards,
        )
        // 각종 데이터 저장(브리핑, AP 로그) 및 balance_ap 차감
        val totalSalaryCost = chargeAndSave(
            user = lockedRequest.user,
            agents = lockedRequest.agents,
            briefings = briefings,
        )

        return createResult(briefings, totalSalaryCost)
    }

    private fun validateBeforeLock(command: BriefingRequestTask.Command): List<NewsCard> {
        val newsCards = newsCardRepository.findAnalysisCards(
            command.stockPublicId,
            command.requestedAt.toLocalDate(),
        )
        // 카드뉴스가 존재하는지 검증한다.
        if (newsCards.size != REQUIRED_NEWS_CARD_COUNT) {
            throw NewsCardNotFoundException()
        }

        return newsCards
    }

    private fun lockAndValidate(command: BriefingRequestTask.Command): LockedRequest {
        // 요청 사용자가 존재하는지 검증한다.
        // 해당 사용자 행(users 테이블) 비관적 락 설정
        val user = userRepository.findForUpdateByPublicId(command.userPublicId) ?: throw UserNotFoundException()
        // 요청 종목이 사용자의 현재 관심 종목인지 검증한다.
        if (!userStockRepository.existsInterestStock(command.userPublicId, command.stockPublicId)) {
            throw StockNotFoundException()
        }

        val agents = agentRepository.findAllByUserPublicIdAndPublicIdInOrderByIdAsc(
            command.userPublicId,
            command.agentPublicIds,
        )
        // 요청한 모든 에이전트를 사용자가 소유하고 있는지 검증한다.
        if (agents.size != command.agentPublicIds.size) {
            throw AgentNotFoundException()
        }

        return LockedRequest(user = user, agents = agents)
    }

    private fun findDailyBriefings(command: BriefingRequestTask.Command): List<Briefing> =
        briefingRepository.findDailyBriefings(
            command.userPublicId,
            command.stockPublicId,
            command.requestedAt.toLocalDate(),
        )

    private fun validateRetry(
        briefings: List<Briefing>,
        command: BriefingRequestTask.Command,
    ) {
        // 최초 요청에 없던 에이전트를 재시도로 새로 추가하지 않는지 검증한다.
        if (briefings.size != command.agentPublicIds.size) {
            throw BriefingAgentNotInInitialRequestException()
        }
        // 요청 대상 브리핑이 모두 실패 상태여서 재시도 가능한지 검증한다.
        if (briefings.any { it.status != BriefingStatus.FAILED }) {
            throw BriefingAlreadyRequestedException()
        }

        val retryAfterSeconds = briefings.maxOf { briefing ->
            val retryAt = briefing.updatedAt!!.plusSeconds(RETRY_COOLDOWN_SECONDS)
            ceil(
                Duration.between(command.requestedAt, retryAt)
                    .toMillis()
                    .coerceAtLeast(0) / 1000.0,
            ).toLong()
        }
        // 가장 늦게 실패한 브리핑을 기준으로 재시도 대기 시간이 지났는지 검증한다.
        if (retryAfterSeconds > 0) {
            throw BriefingRetryCooldownException(retryAfterSeconds)
        }
    }

    private fun prepareBriefings(
        dailyBriefings: List<Briefing>,
        command: BriefingRequestTask.Command,
        agents: List<Agent>,
        newsCards: List<NewsCard>,
    ): List<Briefing> {
        if (dailyBriefings.isEmpty()) {
            return createBriefings(agents, newsCards)
        }

        val requestedBriefings = dailyBriefings.filter { it.agent.publicId in command.agentPublicIds }
        validateRetry(requestedBriefings, command)
        return requestedBriefings.onEach { it.retry(newsCards) }
    }

    private fun createBriefings(
        agents: List<Agent>,
        newsCards: List<NewsCard>,
    ): List<Briefing> =
        agents.map { agent -> Briefing.create(newsCards = newsCards, agent = agent) }

    private fun chargeAndSave(
        user: User,
        agents: List<Agent>,
        briefings: List<Briefing>,
    ): Int {
        val totalSalaryCost = agents.sumOf { it.dailySalary }
        // 잠근 사용자의 현재 AP가 전체 에이전트 급여를 지불하기에 충분한지 검증한다.
        if (user.balanceAp < totalSalaryCost) {
            throw InsufficientApBalanceException()
        }
        briefingRepository.saveAll(briefings)
        briefingRepository.flush()

        briefings.forEach { briefing ->
            apTransactionService.change(
                userId = requireNotNull(user.publicId),
                deltaAp = -briefing.agent.dailySalary,
                reason = ApTransactionReason.SALARY,
                target =
                    ApTransactionService.Target(
                        type = ApTransactionTargetType.BRIEFING,
                        id = requireNotNull(briefing.id),
                    ),
            )
        }

        return totalSalaryCost
    }

    private fun createResult(
        briefings: List<Briefing>,
        totalSalaryCost: Int,
    ): BriefingRequestTask.Result =
        BriefingRequestTask.Result(
            requestedCount = briefings.size,
            totalSalaryCost = totalSalaryCost,
            requestedAgents = briefings.map { it.toRequestedAgent() },
        )

    private fun Briefing.toRequestedAgent(): BriefingRequestTask.Result.RequestedAgent =
        BriefingRequestTask.Result.RequestedAgent(
            briefingId = publicId!!,
            agentId = agent.publicId!!,
            agentType = agent.agentType,
            salaryCost = agent.dailySalary,
        )

    private companion object {
        const val REQUIRED_NEWS_CARD_COUNT = 2
        const val RETRY_COOLDOWN_SECONDS = 30L
    }

    private data class LockedRequest(
        val user: User,
        val agents: List<Agent>,
    )
}
