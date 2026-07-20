package com.brifo.server.auth.service

import com.brifo.server.auth.dto.ReissueRequest
import com.brifo.server.auth.dto.ReissueResponse
import com.brifo.server.auth.entity.RevokedRefreshToken
import com.brifo.server.auth.repository.RevokedRefreshTokenRepository
import com.brifo.server.global.code.ErrorCode
import com.brifo.server.global.exception.BusinessException
import com.brifo.server.user.repository.UserRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TokenReissueService(
    private val jwtTokenProvider: JwtTokenProvider,
    private val revokedRefreshTokenRepository: RevokedRefreshTokenRepository,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun reissue(request: ReissueRequest?): ReissueResponse {
        val refreshToken =
            request?.refreshToken?.takeIf { it.isNotBlank() }
                ?: throw BusinessException(ErrorCode.REFRESH_TOKEN_REQUIRED)
        val claims = jwtTokenProvider.parseRefreshToken(refreshToken)

        if (revokedRefreshTokenRepository.existsByTokenId(claims.tokenId)) {
            throw BusinessException(ErrorCode.REFRESH_TOKEN_UNUSABLE)
        }
        if (userRepository.findByPublicId(claims.userId) == null) {
            throw BusinessException(ErrorCode.AUTH_USER_NOT_FOUND)
        }

        try {
            revokedRefreshTokenRepository.saveAndFlush(
                RevokedRefreshToken.create(
                    tokenId = claims.tokenId,
                    userPublicId = claims.userId,
                    expiresAt = claims.expiresAt,
                ),
            )
        } catch (exception: DataIntegrityViolationException) {
            throw BusinessException(ErrorCode.REFRESH_TOKEN_UNUSABLE)
        }

        return ReissueResponse(token = jwtTokenProvider.issueLoginTokens(claims.userId))
    }
}
