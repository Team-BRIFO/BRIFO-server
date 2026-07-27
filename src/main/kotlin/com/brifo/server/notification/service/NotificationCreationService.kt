package com.brifo.server.notification.service

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.agent.repository.AgentRepository
import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.ap.entity.ApTransactionTargetType
import com.brifo.server.ap.repository.ApTransactionRepository
import com.brifo.server.ap.repository.AttendanceRewardRepository
import com.brifo.server.badge.repository.UserBadgeRepository
import com.brifo.server.briefing.entity.BriefingDirection
import com.brifo.server.decision.entity.DecisionDirection
import com.brifo.server.notification.entity.Notification
import com.brifo.server.notification.entity.NotificationCode
import com.brifo.server.notification.entity.NotificationTargetType
import com.brifo.server.notification.repository.NotificationRepository
import com.brifo.server.notification.repository.NotificationTypeRepository
import com.brifo.server.user.exception.UserNotFoundException
import com.brifo.server.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.math.absoluteValue

@Service
class NotificationCreationService(
    private val userRepository: UserRepository,
    private val notificationRepository: NotificationRepository,
    private val notificationTypeRepository: NotificationTypeRepository,
    private val userBadgeRepository: UserBadgeRepository,
    private val attendanceRewardRepository: AttendanceRewardRepository,
    private val apTransactionRepository: ApTransactionRepository,
    private val agentRepository: AgentRepository,
) {
    data class Target(
        val type: NotificationTargetType,
        val id: UUID?,
    )

    private data class Content(
        val title: String,
        val body: String,
    )

    @Transactional
    fun create(
        userId: UUID,
        code: NotificationCode,
        target: Target,
        eventId: Long? = null,
    ) {
        require(target.type == TARGET_TYPES.getValue(code)) { "Invalid target type for $code" }

        val user = userRepository.findByPublicId(userId) ?: throw UserNotFoundException()
        val type =
            notificationTypeRepository.findByCode(code.name)
                ?: error("Unknown notification type: ${code.name}")
        val content = resolveContent(userId, code, target.id, eventId)

        notificationRepository.save(
            Notification.create(
                user = user,
                notificationType = type,
                title = content.title,
                body = content.body,
                targetType = target.type,
                targetPublicId = target.id,
            ),
        )
    }

    private fun resolveContent(
        userId: UUID,
        code: NotificationCode,
        targetId: UUID?,
        eventId: Long?,
    ): Content =
        when (code) {
            NotificationCode.DECISION_RESULT -> decisionResultContent(userId, requireNotNull(targetId))
            NotificationCode.BRIEFING_READY -> briefingReadyContent(userId, requireNotNull(targetId))
            NotificationCode.NEWS_CARD_ARRIVED -> newsCardArrivedContent(userId, requireNotNull(targetId))
            NotificationCode.BADGE_AWARDED -> badgeAwardedContent(userId, requireNotNull(eventId))
            NotificationCode.ATTENDANCE_REWARDED -> attendanceRewardedContent(userId, requireNotNull(eventId))
            NotificationCode.AGENT_SALARY_PAID -> agentSalaryPaidContent(userId, requireNotNull(targetId))
            NotificationCode.AGENT_LEVEL_UP -> agentLevelUpContent(userId, requireNotNull(targetId))
        }

    private fun decisionResultContent(
        userId: UUID,
        decisionId: UUID,
    ): Content {
        val data = notificationRepository.findDecisionResultContent(userId, decisionId) ?: invalidTarget(decisionId)
        return Content(
            title = "오늘의 정산이 끝났어요",
            body =
                "${data.stockName} ${formatChangeRate(data.changeRate)} · " +
                    "${data.direction.label()} 예측 ${if (data.isCorrect) "적중" else "실패"} · " +
                    "AP ${formatSigned(data.apAmount)} (잔액 ${formatNumber(data.balanceAp)})",
        )
    }

    private fun briefingReadyContent(
        userId: UUID,
        briefingId: UUID,
    ): Content {
        val data = notificationRepository.findBriefingReadyContent(userId, briefingId) ?: invalidTarget(briefingId)
        return Content(
            title = "${data.agentType.label()}의 브리핑이 도착했어요",
            body = "${data.stockName} · ${data.direction.label()} ${data.confidenceRate}% 확신 · 브리핑룸에서 확인하세요",
        )
    }

    private fun newsCardArrivedContent(
        userId: UUID,
        stockId: UUID,
    ): Content {
        val cards = notificationRepository.findNewsCardContents(userId, stockId, today())
        require(cards.isNotEmpty()) { "No news cards found for notification" }
        val stockNames = cards.map { it.stockName }.distinct().joinToString(" · ")
        return Content(
            title = "새 카드뉴스 ${cards.size}건이 도착했어요",
            body = "관심 종목 $stockNames 관련 새 소식이 올라왔어요",
        )
    }

    private fun badgeAwardedContent(
        userId: UUID,
        eventId: Long,
    ): Content {
        val badge = userBadgeRepository.findByIdAndUserPublicId(eventId, userId)?.badge ?: invalidTarget(eventId)
        return Content(
            title = "뱃지를 획득했어요 · ${badge.name}",
            body = "${badge.description ?: badge.name} 보상 +${formatNumber(badge.rewardAp)} AP를 지급했어요",
        )
    }

    private fun attendanceRewardedContent(
        userId: UUID,
        eventId: Long,
    ): Content {
        val reward = attendanceRewardRepository.findByIdAndUserPublicId(eventId, userId) ?: invalidTarget(eventId)
        val transaction =
            apTransactionRepository
                .findTopByUserPublicIdAndTargetTypeAndTargetIdAndReasonInOrderByIdDesc(
                    userPublicId = userId,
                    targetType = ApTransactionTargetType.ATTENDANCE_REWARD,
                    targetId = requireNotNull(reward.id),
                    reasons = listOf(ApTransactionReason.ATTENDANCE),
                ) ?: invalidTarget(userId)
        return Content(
            title = "출석 보너스 +${formatNumber(transaction.amount)} AP",
            body = "${reward.consecutiveDays}일 연속 출석 중이에요. 내일도 만나요!",
        )
    }

    private fun agentSalaryPaidContent(
        userId: UUID,
        stockId: UUID,
    ): Content {
        val salaries = notificationRepository.findAgentSalaryContents(userId, stockId, today())
        require(salaries.isNotEmpty()) { "No briefings found for stock notification" }
        val agentTypes = salaries.map { it.agentType }.distinct()
        return Content(
            title = "분석 의뢰비가 지급됐어요",
            body =
                "${agentTypes.joinToString(" · ") { it.label() }} ${salaries.size}명에게 의뢰 · " +
                    "${formatSigned(salaries.sumOf { it.salaryAmount })} AP " +
                    "(잔액 ${formatNumber(salaries.first().balanceAp)})",
        )
    }

    private fun agentLevelUpContent(
        userId: UUID,
        agentId: UUID,
    ): Content {
        val agent = agentRepository.findByPublicIdAndUserPublicId(agentId, userId) ?: invalidTarget(agentId)
        val label = agent.agentType.label()
        return Content(
            title = "${label}의 레벨이 올랐어요",
            body = "${label}가 ${agent.level}레벨이 되었어요",
        )
    }

    private fun today(): LocalDate = LocalDate.now(SEOUL_ZONE)

    private fun invalidTarget(id: Any): Nothing = throw IllegalArgumentException("Invalid notification target: $id")

    private fun AgentType.label(): String =
        when (this) {
            AgentType.ROOKIE -> "루키"
            AgentType.PRO -> "프로"
            AgentType.TANKER -> "탱커"
        }

    private fun DecisionDirection.label(): String =
        when (this) {
            DecisionDirection.UP -> "상승"
            DecisionDirection.DOWN -> "하락"
            DecisionDirection.NEUTRAL -> "중립"
        }

    private fun BriefingDirection.label(): String =
        when (this) {
            BriefingDirection.UP -> "상승"
            BriefingDirection.DOWN -> "하락"
            BriefingDirection.NEUTRAL -> "중립"
        }

    private fun formatChangeRate(rate: BigDecimal): String {
        val value = rate.stripTrailingZeros().toPlainString()
        return when {
            rate.signum() > 0 -> "▲+$value%"
            rate.signum() < 0 -> "▼${rate.abs().stripTrailingZeros().toPlainString()}%"
            else -> "0%"
        }
    }

    private fun formatSigned(value: Int): String = if (value > 0) "+${formatNumber(value)}" else formatNumber(value)

    private fun formatNumber(value: Int): String =
        java.text.NumberFormat
            .getIntegerInstance(java.util.Locale.KOREA)
            .format(value.absoluteValue)
            .let { formatted -> if (value < 0) "-$formatted" else formatted }

    companion object {
        private val SEOUL_ZONE: ZoneId = ZoneId.of("Asia/Seoul")
        private val TARGET_TYPES =
            mapOf(
                NotificationCode.DECISION_RESULT to NotificationTargetType.DECISION,
                NotificationCode.BRIEFING_READY to NotificationTargetType.BRIEFING,
                NotificationCode.NEWS_CARD_ARRIVED to NotificationTargetType.NEWS_CARD_LIST,
                NotificationCode.BADGE_AWARDED to NotificationTargetType.BADGE,
                NotificationCode.ATTENDANCE_REWARDED to NotificationTargetType.NONE,
                NotificationCode.AGENT_SALARY_PAID to NotificationTargetType.STOCK_BRIEFINGS,
                NotificationCode.AGENT_LEVEL_UP to NotificationTargetType.AGENT,
            )
    }
}
