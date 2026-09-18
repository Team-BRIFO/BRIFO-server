package com.brifo.server.batch.settlement

import com.brifo.server.agent.repository.AgentRepository
import com.brifo.server.ap.service.ApTransactionService
import com.brifo.server.badge.code.BadgeCode
import com.brifo.server.badge.service.BadgeAwardService
import com.brifo.server.decision.entity.DecisionDirection
import com.brifo.server.decision.entity.DecisionResult
import com.brifo.server.decision.repository.DecisionRepository
import com.brifo.server.decision.repository.DecisionResultRepository
import com.brifo.server.diary.entity.DiaryEntry
import com.brifo.server.diary.repository.DiaryEntryRepository
import com.brifo.server.notification.entity.NotificationCode
import com.brifo.server.notification.entity.NotificationTargetType
import com.brifo.server.notification.service.NotificationCreationService
import com.brifo.server.stock.repository.DailyStockPriceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DecisionSettlementService(
    private val decisionRepository: DecisionRepository,
    private val decisionResultRepository: DecisionResultRepository,
    private val dailyStockPriceRepository: DailyStockPriceRepository,
    private val diaryEntryRepository: DiaryEntryRepository,
    private val agentRepository: AgentRepository,
    private val apTransactionService: ApTransactionService,
    private val badgeAwardService: BadgeAwardService,
    private val notificationCreationService: NotificationCreationService,
    private val calculator: DecisionSettlementCalculator,
) {
    @Transactional
    fun settle(item: DecisionSettlementItem) {
        if (decisionResultRepository.existsByDecisionId(item.decisionId)) return

        val decision = decisionRepository.findById(item.decisionId).orElseThrow {
            IllegalStateException("결정을 찾을 수 없습니다: ${item.decisionId}")
        }
        val price = dailyStockPriceRepository.findById(item.dailyStockPriceId).orElseThrow {
            IllegalStateException("일별 주가를 찾을 수 없습니다: ${item.dailyStockPriceId}")
        }
        val decisionId = requireNotNull(decision.id)
        val user = decision.briefing.agent.user
        val userId = requireNotNull(user.id)
        val userPublicId = requireNotNull(user.publicId)
        val agent = decision.briefing.agent

        decisionResultRepository.save(DecisionResult.create(decision, price, item.isCorrect))

        val ap = calculator.apSettlement(decision.direction, item.isCorrect, decision.confidenceLevel.toInt())
        val settleResult = apTransactionService.settleDecision(userPublicId, ap.amount, ap.reason, decisionId)

        val leveledUp = agent.addExperience(calculator.experience(decision.direction, item.isCorrect))
        diaryEntryRepository.save(DiaryEntry.create(decision))

        awardSettlementBadges(userId, userPublicId, decision.confidenceLevel.toInt(), item.isCorrect)
        badgeAwardService.awardBalanceMilestones(userPublicId, settleResult.balanceAp)

        notificationCreationService.create(
            userId = userPublicId,
            code = NotificationCode.DECISION_RESULT,
            target = NotificationCreationService.Target(
                type = NotificationTargetType.DECISION,
                id = requireNotNull(decision.publicId),
            ),
        )
        if (leveledUp) {
            notificationCreationService.create(
                userId = userPublicId,
                code = NotificationCode.AGENT_LEVEL_UP,
                target = NotificationCreationService.Target(
                    type = NotificationTargetType.AGENT,
                    id = requireNotNull(agent.publicId),
                ),
            )
        }
    }

    private fun awardSettlementBadges(
        userId: Long,
        userPublicId: java.util.UUID,
        confidenceLevel: Int,
        isCorrect: Boolean,
    ) {
        if (isCorrect) badgeAwardService.awardBadge(userPublicId, BadgeCode.B03)
        if (isCorrect && confidenceLevel == 5) badgeAwardService.awardBadge(userPublicId, BadgeCode.B04)

        val correctCount = decisionResultRepository.countByDecisionBriefingAgentUserIdAndIsCorrect(userId, true)
        if (correctCount >= 10) badgeAwardService.awardBadge(userPublicId, BadgeCode.B07)
        if (correctCount >= 50) badgeAwardService.awardBadge(userPublicId, BadgeCode.B08)
        if (correctCount >= 100) badgeAwardService.awardBadge(userPublicId, BadgeCode.B23)
        if (correctCount >= 200) badgeAwardService.awardBadge(userPublicId, BadgeCode.B24)
        if (correctCount >= 300) badgeAwardService.awardBadge(userPublicId, BadgeCode.B25)

        val neutralHitCount =
            decisionResultRepository.countByDecisionBriefingAgentUserIdAndIsCorrectAndDecisionDirection(
                userId,
                true,
                DecisionDirection.NEUTRAL,
            )
        if (neutralHitCount >= 5) badgeAwardService.awardBadge(userPublicId, BadgeCode.B09)
        if (neutralHitCount >= 15) badgeAwardService.awardBadge(userPublicId, BadgeCode.B26)
        if (neutralHitCount >= 30) badgeAwardService.awardBadge(userPublicId, BadgeCode.B27)

        val confidence5CorrectCount =
            decisionResultRepository.countByDecisionBriefingAgentUserIdAndIsCorrectAndDecisionConfidenceLevel(
                userId,
                true,
                5.toShort(),
            )
        if (confidence5CorrectCount >= 5) badgeAwardService.awardBadge(userPublicId, BadgeCode.B28)
        if (confidence5CorrectCount >= 10) badgeAwardService.awardBadge(userPublicId, BadgeCode.B29)

        if (agentRepository.existsByUserIdAndLevelGreaterThanEqual(userId, 5)) {
            badgeAwardService.awardBadge(userPublicId, BadgeCode.B10)
        }
        if (agentRepository.existsByUserIdAndLevelGreaterThanEqual(userId, 10)) {
            badgeAwardService.awardBadge(userPublicId, BadgeCode.B30)
        }
        val agents = agentRepository.findAllByUserId(userId)
        if (agents.isNotEmpty() && agents.all { it.level >= 5 }) {
            badgeAwardService.awardBadge(userPublicId, BadgeCode.B31)
        }
        if (agents.isNotEmpty() && agents.all { it.level >= 10 }) {
            badgeAwardService.awardBadge(userPublicId, BadgeCode.B32)
        }

        val last3Results = decisionResultRepository.findTop3ByDecisionBriefingAgentUserIdOrderByIdDesc(userId)
        if (last3Results.size == 3 && last3Results.all { it.isCorrect }) {
            badgeAwardService.awardBadge(userPublicId, BadgeCode.B49)
        }
        val last5Results = decisionResultRepository.findTop5ByDecisionBriefingAgentUserIdOrderByIdDesc(userId)
        if (last5Results.size == 5 && last5Results.all { it.isCorrect }) {
            badgeAwardService.awardBadge(userPublicId, BadgeCode.B50)
        }
    }
}
