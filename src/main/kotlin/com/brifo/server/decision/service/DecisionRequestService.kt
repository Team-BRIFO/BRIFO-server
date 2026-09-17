package com.brifo.server.decision.service

import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.ap.entity.ApTransactionTargetType
import com.brifo.server.ap.exception.InsufficientApBalanceException
import com.brifo.server.ap.service.ApTransactionService
import com.brifo.server.briefing.entity.Briefing
import com.brifo.server.briefing.entity.BriefingStatus
import com.brifo.server.briefing.exception.BriefingNotCompletedException
import com.brifo.server.briefing.exception.BriefingNotFoundException
import com.brifo.server.briefing.repository.BriefingRepository
import com.brifo.server.badge.code.BadgeCode
import com.brifo.server.badge.service.BadgeAwardService
import com.brifo.server.decision.DecisionMarketPolicy
import com.brifo.server.decision.dto.response.CreateDecisionResponse
import com.brifo.server.decision.entity.Decision
import com.brifo.server.decision.entity.DecisionDirection
import com.brifo.server.decision.exception.DecisionAlreadyExistsException
import com.brifo.server.decision.exception.DecisionRequestClosedException
import com.brifo.server.decision.repository.DecisionRepository
import com.brifo.server.global.code.ErrorCode
import com.brifo.server.global.config.DevBehaviorProperties
import com.brifo.server.global.exception.BusinessException
import com.brifo.server.news.entity.NewsCard
import com.brifo.server.user.exception.UserNotFoundException
import com.brifo.server.user.repository.UserRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

/** 오늘 완료된 브리핑 하나를 채택하여 결정을 등록한다. */
@Service
class DecisionRequestService(
    private val briefingRepository: BriefingRepository,
    private val decisionRepository: DecisionRepository,
    private val badgeAwardService: BadgeAwardService,
    private val apTransactionService: ApTransactionService,
    private val userRepository: UserRepository,
    private val clock: Clock,
    private val devBehaviorProperties: DevBehaviorProperties = DevBehaviorProperties(),
    private val eventPublisher: ApplicationEventPublisher? = null,
) {
    @Transactional
    fun request(
        userPublicId: UUID,
        briefingPublicId: UUID,
        direction: DecisionDirection,
        confidenceLevel: Int,
    ): CreateDecisionResponse {
        val requestedAt = LocalDateTime.now(clock)

        validateRequest(requestedAt, confidenceLevel)
        val briefing = briefingRepository.findOwnedBriefing(userPublicId, briefingPublicId)
            ?: throw BriefingNotFoundException()
        val newsCard = validateDatabaseState(
            userPublicId = userPublicId,
            briefing = briefing,
            requestedAt = requestedAt,
        )
        val stock = newsCard.news.stock

        val user = userRepository.findForUpdateByPublicId(userPublicId) ?: throw UserNotFoundException()
        if (user.balanceAp < DECISION_ENTRY_FEE_AP) {
            throw InsufficientApBalanceException()
        }

        val decision = decisionRepository.saveAndFlush(
            Decision.create(
                briefing = briefing,
                direction = direction,
                confidenceLevel = confidenceLevel,
            ),
        )
        val balanceAp = apTransactionService.change(
            userId = userPublicId,
            deltaAp = -DECISION_ENTRY_FEE_AP,
            reason = ApTransactionReason.DECISION_ENTRY_FEE,
            target = ApTransactionService.Target(
                type = ApTransactionTargetType.DECISION,
                id = requireNotNull(decision.id),
            ),
        )
        badgeAwardService.awardBadge(userPublicId, BadgeCode.B02)
        if (needsImmediateSettlement(requestedAt)) {
            eventPublisher?.publishEvent(
                DecisionCreatedEvent(
                    decisionPublicId = requireNotNull(decision.publicId),
                    targetDate = newsCard.displayDate,
                ),
            )
        }

        return CreateDecisionResponse(
            decisionId = decision.publicId!!,
            direction = decision.direction,
            confidenceLevel = decision.confidenceLevel.toInt(),
            entryFeeAp = DECISION_ENTRY_FEE_AP,
            balanceAp = balanceAp,
            stock = CreateDecisionResponse.CreatedDecisionStock(
                stockId = stock.publicId!!,
                name = stock.name,
            ),
        )
    }

    /**
     * 휴장일 예측은 즉시 정산이 유일한 정산 경로다. 정산 스케줄러 cron이 `MON-FRI`라
     * 주말에 등록된 예측을 나중에 거둬갈 배치가 없기 때문에, 개발용 즉시 정산 설정과
     * 무관하게 항상 정산 이벤트를 발행한다.
     */
    private fun needsImmediateSettlement(requestedAt: LocalDateTime): Boolean =
        devBehaviorProperties.immediateDecisionSettlement ||
            (devBehaviorProperties.weekendMarketEnabled && !DecisionMarketPolicy.isBusinessDay(requestedAt))

    private fun validateRequest(
        requestedAt: LocalDateTime,
        confidenceLevel: Int,
    ) {
        if (confidenceLevel !in 1..5) {
            throw BusinessException(ErrorCode.INVALID_REQUEST)
        }
        if (!devBehaviorProperties.weekendMarketEnabled && !DecisionMarketPolicy.isBusinessDay(requestedAt)) {
            throw DecisionRequestClosedException()
        }
        if (devBehaviorProperties.decisionRequestCutoffEnabled && !DecisionMarketPolicy.isRegistrationOpen(requestedAt)) {
            throw DecisionRequestClosedException()
        }
    }

    private fun validateDatabaseState(
        userPublicId: UUID,
        briefing: Briefing,
        requestedAt: LocalDateTime,
    ): NewsCard {
        if (briefing.status != BriefingStatus.COMPLETED) {
            throw BriefingNotCompletedException()
        }
        val newsCard = briefing.newsCards.firstOrNull() ?: throw BriefingNotFoundException()
        if (newsCard.displayDate != requestedAt.toLocalDate()) {
            throw BriefingNotFoundException()
        }
        if (
            decisionRepository.existsDailyDecision(
                userPublicId = userPublicId,
                stockPublicId = newsCard.news.stock.publicId!!,
                displayDate = newsCard.displayDate,
            )
        ) {
            throw DecisionAlreadyExistsException()
        }
        return newsCard
    }

    private companion object {
        /** 예측 등록 시 즉시 차감되는 참가비. 의뢰비(사원 배치 비용)와는 별개의 비용이다. */
        const val DECISION_ENTRY_FEE_AP = 1_000
    }
}

data class DecisionCreatedEvent(
    val decisionPublicId: UUID,
    val targetDate: java.time.LocalDate,
)
