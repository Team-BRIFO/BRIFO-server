package com.brifo.server.auth.repository

import com.brifo.server.auth.entity.RevokedRefreshToken
import org.springframework.data.jpa.repository.JpaRepository

interface RevokedRefreshTokenRepository : JpaRepository<RevokedRefreshToken, Long> {
    fun existsByTokenId(tokenId: String): Boolean
}
