package com.brifo.server.auth.entity

import com.brifo.server.global.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "revoked_refresh_tokens")
class RevokedRefreshToken private constructor(
    tokenId: String,
    userPublicId: UUID,
    expiresAt: Instant,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "revokedRefreshTokenIdGenerator")
    @SequenceGenerator(
        name = "revokedRefreshTokenIdGenerator",
        sequenceName = "revoked_refresh_tokens_id_seq",
        allocationSize = 50,
    )
    @Column(name = "id", nullable = false, updatable = false)
    var id: Long? = null
        protected set

    @Column(name = "token_id", nullable = false, updatable = false, length = 36)
    var tokenId: String = tokenId
        protected set

    @Column(name = "user_public_id", nullable = false, updatable = false)
    var userPublicId: UUID = userPublicId
        protected set

    @Column(name = "expires_at", nullable = false, updatable = false)
    var expiresAt: Instant = expiresAt
        protected set

    companion object {
        fun create(
            tokenId: String,
            userPublicId: UUID,
            expiresAt: Instant,
        ): RevokedRefreshToken =
            RevokedRefreshToken(
                tokenId = tokenId,
                userPublicId = userPublicId,
                expiresAt = expiresAt,
            )
    }
}
