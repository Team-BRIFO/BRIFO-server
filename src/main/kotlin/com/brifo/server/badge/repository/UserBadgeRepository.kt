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
}
