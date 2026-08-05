package com.brifo.server.badge.repository

import com.brifo.server.badge.dto.response.GetBadgesResponse
import com.brifo.server.badge.dto.response.GetOwnedBadgeResponse
import com.brifo.server.badge.entity.QBadge.Companion.badge
import com.brifo.server.badge.entity.QUserBadge.Companion.userBadge
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import java.util.UUID

class BadgeQueryRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : BadgeQueryRepository {
    override fun findAllWithOwnership(userPublicId: UUID): List<GetBadgesResponse.BadgeItem> =
        queryFactory
            .select(
                Projections.constructor(
                    GetBadgesResponse.BadgeItem::class.java,
                    badge.publicId,
                    badge.code,
                    badge.name,
                    userBadge.id.isNotNull,
                ),
            ).from(badge)
            .leftJoin(userBadge)
            .on(
                userBadge.badge.id.eq(badge.id),
                userBadge.user.publicId.eq(userPublicId),
            ).orderBy(badge.code.asc())
            .fetch()

    override fun findOwnedBadge(
        userPublicId: UUID,
        badgePublicId: UUID,
    ): GetOwnedBadgeResponse? =
        queryFactory
            .select(
                Projections.constructor(
                    GetOwnedBadgeResponse::class.java,
                    badge.publicId,
                    badge.code,
                    badge.name,
                    badge.description,
                    badge.rewardAp,
                ),
            ).from(userBadge)
            .join(userBadge.badge, badge)
            .where(
                userBadge.user.publicId.eq(userPublicId),
                badge.publicId.eq(badgePublicId),
            ).fetchOne()

}
