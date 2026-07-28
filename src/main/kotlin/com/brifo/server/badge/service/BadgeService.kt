package com.brifo.server.badge.service

import com.brifo.server.badge.dto.response.GetBadgesResponse
import com.brifo.server.badge.dto.response.GetOwnedBadgeResponse
import com.brifo.server.badge.exception.BadgeNotFoundException
import com.brifo.server.badge.repository.BadgeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BadgeService(
    private val badgeRepository: BadgeRepository,
) {
    @Transactional(readOnly = true)
    fun getBadges(userId: UUID): GetBadgesResponse =
        GetBadgesResponse(
            items = badgeRepository.findAllWithOwnership(userId),
        )

    @Transactional(readOnly = true)
    fun getOwnedBadge(
        userId: UUID,
        badgeId: UUID,
    ): GetOwnedBadgeResponse =
        badgeRepository.findOwnedBadge(userId, badgeId)
            ?: throw BadgeNotFoundException()
}
