package com.brifo.server.auth.service

import com.brifo.server.auth.dto.LogoutRequest
import com.brifo.server.auth.entity.RevokedRefreshToken
import com.brifo.server.auth.repository.RevokedRefreshTokenRepository
import com.brifo.server.global.code.ErrorCode
import com.brifo.server.global.exception.BusinessException
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
                ?: throw BusinessException(ErrorCode.REFRESH_TOKEN_REQUIRED)

        val accessClaims = jwtTokenProvider.parseAccessToken(accessToken)
        val refreshClaims = jwtTokenProvider.parseRefreshTokenForLogout(refreshToken)

        if (accessClaims.userId != refreshClaims.userId) {
            throw BusinessException(ErrorCode.REFRESH_TOKEN_MISMATCH)
        }
        if (revokedRefreshTokenRepository.existsByTokenId(refreshClaims.tokenId)) {
            throw BusinessException(ErrorCode.REFRESH_TOKEN_UNUSABLE)
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
            throw BusinessException(ErrorCode.REFRESH_TOKEN_UNUSABLE)
        }
    }

    private fun extractAccessToken(authorizationHeader: String?): String {
        val header = authorizationHeader?.trim()
        if (header.isNullOrEmpty()) {
            throw BusinessException(ErrorCode.UNAUTHORIZED)
        }
        if (!header.startsWith(BEARER_PREFIX, ignoreCase = true)) {
            throw BusinessException(ErrorCode.OAUTH_INVALID_TOKEN)
        }

        return header.substring(BEARER_PREFIX.length).trim().takeIf { it.isNotEmpty() }
            ?: throw BusinessException(ErrorCode.OAUTH_INVALID_TOKEN)
    }

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }
}
