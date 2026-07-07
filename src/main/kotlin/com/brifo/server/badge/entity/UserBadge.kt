package com.brifo.server.badge.entity

import com.brifo.server.user.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "user_badges")
class UserBadge private constructor(
    user: User,
    badge: Badge,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "userBadgeIdGenerator")
    @SequenceGenerator(name = "userBadgeIdGenerator", sequenceName = "user_badges_id_seq", allocationSize = 50)
    @Column(name = "id", nullable = false, updatable = false)
    var id: Long? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User = user
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "badge_id", nullable = false)
    var badge: Badge = badge
        protected set

    @Column(name = "awarded_at", nullable = false, insertable = false, updatable = false)
    var awardedAt: LocalDateTime? = null
        protected set

    companion object {
        fun create(
            user: User,
            badge: Badge,
        ): UserBadge {
            return UserBadge(
                user = user,
                badge = badge,
            )
        }
    }
}
