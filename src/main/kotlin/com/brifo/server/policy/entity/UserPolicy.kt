package com.brifo.server.policy.entity

import com.brifo.server.user.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(name = "user_policies")
class UserPolicy private constructor(
    user: User,
    policy: Policy,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "userPolicyIdGenerator")
    @SequenceGenerator(name = "userPolicyIdGenerator", sequenceName = "user_policies_id_seq", allocationSize = 50)
    @Column(name = "id", nullable = false, updatable = false)
    var id: Long? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User = user
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    var policy: Policy = policy
        protected set

    @CreatedDate
    @Column(name = "agreed_at", nullable = false, updatable = false)
    var agreedAt: LocalDateTime? = null
        protected set

    @Column(name = "revoked_at")
    var revokedAt: LocalDateTime? = null
        protected set

    fun revoke(revokedAt: LocalDateTime) {
        this.revokedAt = revokedAt
    }

    companion object {
        fun create(
            user: User,
            policy: Policy,
        ): UserPolicy {
            return UserPolicy(
                user = user,
                policy = policy,
            )
        }
    }
}
