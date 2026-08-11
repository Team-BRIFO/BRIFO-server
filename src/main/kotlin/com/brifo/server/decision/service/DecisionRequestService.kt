package com.brifo.server.decision.service

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

        val decision = decisionRepository.saveAndFlush(
            Decision.create(
                briefing = briefing,
                direction = direction,
                confidenceLevel = confidenceLevel,
            ),
        )
        badgeAwardService.awardBadge(userPublicId, BadgeCode.B02)
        if (devBehaviorProperties.immediateDecisionSettlement) {
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
            stock = CreateDecisionResponse.CreatedDecisionStock(
                stockId = stock.publicId!!,
                name = stock.name,
            ),
        )
    }

    private fun validateRequest(
        requestedAt: LocalDateTime,
        confidenceLevel: Int,
    ) {
        if (confidenceLevel !in 1..5) {
            throw BusinessException(ErrorCode.INVALID_REQUEST)
        }
        if (
            devBehaviorProperties.decisionRequestCutoffEnabled &&
            !DecisionMarketPolicy.isRegistrationOpen(requestedAt)
        ) {
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
}

data class DecisionCreatedEvent(
    val decisionPublicId: UUID,
    val targetDate: java.time.LocalDate,
)
