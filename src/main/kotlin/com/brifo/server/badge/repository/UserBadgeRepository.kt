package com.brifo.server.badge.repository

import com.brifo.server.badge.entity.UserBadge
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface UserBadgeRepository : JpaRepository<UserBadge, Long> {
    @Modifying
    @Query(
        value =
            """
            INSERT INTO user_badges (user_id, badge_id)
            SELECT users.id, :badgeId
            FROM users
            WHERE users.public_id = :userPublicId
            ON CONFLICT (user_id, badge_id) DO NOTHING
            """,
        nativeQuery = true,
    )
    fun insertIfAbsent(
        @Param("userPublicId") userPublicId: UUID,
        @Param("badgeId") badgeId: Long,
    ): Int

    @Query(
        value =
            """
            SELECT user_badges.id
            FROM user_badges
            JOIN users ON users.id = user_badges.user_id
            WHERE users.public_id = :userPublicId
              AND user_badges.badge_id = :badgeId
            """,
        nativeQuery = true,
    )
    fun findIdByUserPublicIdAndBadgeId(
        @Param("userPublicId") userPublicId: UUID,
        @Param("badgeId") badgeId: Long,
    ): Long?

    fun findByIdAndUserPublicId(
        id: Long,
        userPublicId: UUID,
    ): UserBadge?
}
