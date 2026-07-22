package com.brifo.server.user.entity

import com.brifo.server.global.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.annotations.Generated
import org.hibernate.generator.EventType
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.SQLRestriction
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "users")
@SQLDelete(sql = "UPDATE users SET deleted_at = now() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
class User private constructor(
    provider: OAuthProvider,
    socialId: String,
    nickname: String?,
    email: String?,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "userIdGenerator")
    @SequenceGenerator(name = "userIdGenerator", sequenceName = "users_id_seq", allocationSize = 50)
    @Column(name = "id", nullable = false, updatable = false)
    var id: Long? = null
        protected set

    @Column(name = "public_id", nullable = false, insertable = false, updatable = false)
    @Generated(event = [EventType.INSERT])
    var publicId: UUID? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    var provider: OAuthProvider = provider
        protected set

    @Column(name = "social_id", nullable = false, length = 100)
    var socialId: String = socialId
        protected set

    @Column(name = "nickname", length = 50)
    var nickname: String? = nickname
        protected set

    @Column(name = "email", length = 255)
    var email: String? = email
        protected set

    @Column(name = "company_name", nullable = false, length = 100)
    var companyName: String = DEFAULT_COMPANY_NAME
        protected set

    @Column(name = "balance_ap", nullable = false)
    var balanceAp: Int = DEFAULT_BALANCE_AP
        protected set

    @Column(name = "onboarding_completed_at")
    var onboardingCompletedAt: LocalDateTime? = null
        protected set

    @Column(name = "last_login_at")
    var lastLoginAt: LocalDateTime? = null
        protected set

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null
        protected set

    fun changeAp(deltaAp: Int) {
        val changedBalance = balanceAp + deltaAp
        require(changedBalance >= 0) { "insufficient AP balance" }
        balanceAp = changedBalance
    }

    fun spendAp(amount: Int) {
        require(amount > 0) { "amount must be positive" }
        changeAp(-amount)
    }

    fun refundAp(amount: Int) {
        require(amount > 0) { "amount must be positive" }
        changeAp(amount)
    }

    companion object {
        private const val DEFAULT_COMPANY_NAME = "내 투자회사"
        private const val DEFAULT_BALANCE_AP = 0

        fun create(
            provider: OAuthProvider,
            socialId: String,
            email: String?,
            nickname: String? = null,
        ): User {
            return User(
                provider = provider,
                socialId = socialId,
                nickname = nickname,
                email = email,
            )
        }
    }
}
