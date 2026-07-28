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
        }

        return AwardBadgeResult(
            badgeId = requireNotNull(badge.publicId),
            rewardAp = badge.rewardAp,
            awarded = awarded,
        )
    }
}
