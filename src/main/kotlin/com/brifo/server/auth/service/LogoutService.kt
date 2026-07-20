package com.brifo.server.auth.service

import com.brifo.server.auth.dto.request.LogoutRequest
import com.brifo.server.auth.entity.RevokedRefreshToken
import com.brifo.server.auth.exception.InvalidTokenException
import com.brifo.server.auth.exception.RefreshTokenMismatchException
import com.brifo.server.auth.exception.RefreshTokenRequiredException
import com.brifo.server.auth.exception.UnauthorizedException
import com.brifo.server.auth.exception.UnusableRefreshTokenException
import com.brifo.server.auth.repository.RevokedRefreshTokenRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LogoutService(
    private val jwtTokenProvider: JwtTokenProvider,
    private val revokedRefreshTokenRepository: RevokedRefreshTokenRepository,
) {
    @Transactional
    fun logout(
        authorizationHeader: String?,
        request: LogoutRequest?,
    ) {
        val accessToken = extractAccessToken(authorizationHeader)
        val refreshToken =
            request?.refreshToken?.takeIf { it.isNotBlank() }
                ?: throw RefreshTokenRequiredException()

        val accessClaims = jwtTokenProvider.parseAccessToken(accessToken)
        val refreshClaims = jwtTokenProvider.parseRefreshTokenForLogout(refreshToken)

        if (accessClaims.userId != refreshClaims.userId) {
            throw RefreshTokenMismatchException()
        }
        if (revokedRefreshTokenRepository.existsByTokenId(refreshClaims.tokenId)) {
            throw UnusableRefreshTokenException()
        }

        try {
            revokedRefreshTokenRepository.saveAndFlush(
                RevokedRefreshToken.create(
                    tokenId = refreshClaims.tokenId,
                    userPublicId = refreshClaims.userId,
                    expiresAt = refreshClaims.expiresAt,
                ),
            )
        } catch (exception: DataIntegrityViolationException) {
            throw UnusableRefreshTokenException()
        }
    }

    private fun extractAccessToken(authorizationHeader: String?): String {
        val header = authorizationHeader?.trim()
        if (header.isNullOrEmpty()) {
            throw UnauthorizedException()
        }
        if (!header.startsWith(BEARER_PREFIX, ignoreCase = true)) {
            throw InvalidTokenException()
        }

        return header.substring(BEARER_PREFIX.length).trim().takeIf { it.isNotEmpty() }
            ?: throw InvalidTokenException()
    }

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }
}
