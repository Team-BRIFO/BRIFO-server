package com.brifo.server.auth.service

import com.brifo.server.auth.dto.request.RefreshTokenRequest
import com.brifo.server.auth.entity.RevokedRefreshToken
import com.brifo.server.auth.exception.RefreshTokenMismatchException
import com.brifo.server.auth.exception.RefreshTokenRequiredException
import com.brifo.server.auth.exception.UnusableRefreshTokenException
import com.brifo.server.auth.repository.RevokedRefreshTokenRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class LogoutService(
    private val jwtTokenProvider: JwtTokenProvider,
    private val revokedRefreshTokenRepository: RevokedRefreshTokenRepository,
) {
    @Transactional
    fun logout(
        authenticatedUserPublicId: UUID,
        request: RefreshTokenRequest?,
    ) {
        val refreshToken =
            request?.refreshToken?.takeIf { it.isNotBlank() }
                ?: throw RefreshTokenRequiredException()

        val refreshClaims = jwtTokenProvider.parseRefreshToken(refreshToken)

        if (authenticatedUserPublicId != refreshClaims.userPublicId) {
            throw RefreshTokenMismatchException()
        }
        if (revokedRefreshTokenRepository.existsByTokenId(refreshClaims.tokenId)) {
            throw UnusableRefreshTokenException()
        }

        try {
            revokedRefreshTokenRepository.saveAndFlush(
                RevokedRefreshToken.create(
                    tokenId = refreshClaims.tokenId,
                    userPublicId = refreshClaims.userPublicId,
                    expiresAt = refreshClaims.expiresAt,
                ),
            )
        } catch (_: DataIntegrityViolationException) {
            throw UnusableRefreshTokenException()
        }
    }
}
