package com.brifo.server.badge.repository

import com.brifo.server.badge.dto.response.GetBadgesResponse
import com.brifo.server.badge.dto.response.GetOwnedBadgeResponse
import java.util.UUID

interface BadgeQueryRepository {
    fun findAllWithOwnership(userPublicId: UUID): List<GetBadgesResponse.BadgeItem>

    fun findOwnedBadge(
        userPublicId: UUID,
        badgePublicId: UUID,
    ): GetOwnedBadgeResponse?
}
