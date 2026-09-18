package com.brifo.server.badge.service

import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.ap.entity.ApTransactionTargetType
import com.brifo.server.ap.service.ApTransactionService
import com.brifo.server.badge.code.BadgeCode
import com.brifo.server.badge.dto.response.AwardBadgeResult
import com.brifo.server.badge.exception.BadgeNotFoundException
import com.brifo.server.badge.repository.BadgeRepository
import com.brifo.server.badge.repository.UserBadgeRepository
import com.brifo.server.notification.entity.NotificationCode
import com.brifo.server.notification.entity.NotificationTargetType
import com.brifo.server.notification.service.NotificationCreationService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BadgeAwardService(
    private val badgeRepository: BadgeRepository,
    private val userBadgeRepository: UserBadgeRepository,
    private val apTransactionService: ApTransactionService,
    private val notificationCreationService: NotificationCreationService,
) {
    @Transactional
    fun awardBadge(
        userId: UUID,
        badgeCode: BadgeCode,
    ): AwardBadgeResult {
        val badge = badgeRepository.findByCode(badgeCode.name) ?: throw BadgeNotFoundException()
        val badgeId = requireNotNull(badge.id)
        val inserted =
            userBadgeRepository.insertIfAbsent(
                userPublicId = userId,
                badgeId = badgeId,
            )
        val awarded = inserted == 1

        if (awarded) {
            val userBadgeId =
                requireNotNull(
                    userBadgeRepository.findIdByUserPublicIdAndBadgeId(
                        userPublicId = userId,
                        badgeId = badgeId,
                    ),
                ) { "Awarded user badge must exist." }

            apTransactionService.change(
                userId = userId,
                deltaAp = badge.rewardAp,
                reason = ApTransactionReason.BADGE,
                target =
                    ApTransactionService.Target(
                        type = ApTransactionTargetType.USER_BADGE,
                        id = userBadgeId,
                    ),
            )
            notificationCreationService.create(
                userId = userId,
                code = NotificationCode.BADGE_AWARDED,
                target =
                    NotificationCreationService.Target(
                        type = NotificationTargetType.BADGE,
                        id = requireNotNull(badge.publicId),
                    ),
                eventId = userBadgeId,
            )
            if (badgeCode !in COLLECTION_MILESTONE_CODES) {
                awardCollectionMilestones(userId)
            }
        }

        return AwardBadgeResult(
            badgeId = requireNotNull(badge.publicId),
            rewardAp = badge.rewardAp,
            awarded = awarded,
        )
    }

    /** 잔액이 특정 금액을 넘기는 순간마다 호출해 자금 마일스톤 배지를 지급한다. */
    @Transactional
    fun awardBalanceMilestones(
        userId: UUID,
        balanceAp: Int,
    ) {
        if (balanceAp >= 500_000) awardBadge(userId, BadgeCode.B36)
        if (balanceAp >= 1_000_000) awardBadge(userId, BadgeCode.B37)
        if (balanceAp >= 3_000_000) awardBadge(userId, BadgeCode.B38)
        if (balanceAp >= 5_000_000) awardBadge(userId, BadgeCode.B39)
    }

    private fun awardCollectionMilestones(userId: UUID) {
        val ownedCount = userBadgeRepository.countByUserPublicId(userId)
        if (ownedCount >= 10) awardBadge(userId, BadgeCode.B45)
        if (ownedCount >= 25) awardBadge(userId, BadgeCode.B46)
        if (ownedCount >= 40) awardBadge(userId, BadgeCode.B47)
    }

    private companion object {
        val COLLECTION_MILESTONE_CODES = setOf(BadgeCode.B45, BadgeCode.B46, BadgeCode.B47)
    }
}
