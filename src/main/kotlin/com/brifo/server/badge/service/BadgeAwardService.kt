package com.brifo.server.badge.service

import com.brifo.server.badge.code.BadgeCode
import com.brifo.server.badge.dto.response.AwardBadgeResult
import com.brifo.server.badge.exception.BadgeNotFoundException
import com.brifo.server.badge.repository.BadgeRepository
import com.brifo.server.badge.repository.UserBadgeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BadgeAwardService(
    private val badgeRepository: BadgeRepository,
    private val userBadgeRepository: UserBadgeRepository,
) {
    @Transactional
    fun awardBadge(
        userId: UUID,
        badgeCode: BadgeCode,
    ): AwardBadgeResult {
        val badge = badgeRepository.findByCode(badgeCode.name) ?: throw BadgeNotFoundException()
        val inserted =
            userBadgeRepository.insertIfAbsent(
                userPublicId = userId,
                badgeId = requireNotNull(badge.id),
            )

        return AwardBadgeResult(
            badgeId = requireNotNull(badge.publicId),
            rewardAp = badge.rewardAp,
            awarded = inserted == 1,
        )
    }
}
