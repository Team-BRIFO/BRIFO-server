package com.brifo.server.auth.repository

import com.brifo.server.auth.entity.RevokedRefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface RevokedRefreshTokenRepository : JpaRepository<RevokedRefreshToken, Long> {
    fun existsByTokenId(tokenId: String): Boolean

    @Modifying
    @Query("delete from RevokedRefreshToken token where token.expiresAt <= :expiredAt")
    fun deleteExpired(
        @Param("expiredAt") expiredAt: Instant,
    ): Int
}
