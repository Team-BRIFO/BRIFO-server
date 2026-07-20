package com.brifo.server.auth.service

import com.brifo.server.auth.dto.request.ReissueRequest
import com.brifo.server.auth.dto.response.ReissueResponse
import com.brifo.server.auth.entity.RevokedRefreshToken
import com.brifo.server.auth.exception.AuthUserNotFoundException
import com.brifo.server.auth.exception.RefreshTokenRequiredException
import com.brifo.server.auth.exception.UnusableRefreshTokenException
import com.brifo.server.auth.repository.RevokedRefreshTokenRepository
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
                ?: throw RefreshTokenRequiredException()
        val claims = jwtTokenProvider.parseRefreshToken(refreshToken)

        if (revokedRefreshTokenRepository.existsByTokenId(claims.tokenId)) {
            throw UnusableRefreshTokenException()
        }
        if (userRepository.findByPublicId(claims.userId) == null) {
            throw AuthUserNotFoundException()
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
            throw UnusableRefreshTokenException()
        }

        return ReissueResponse(token = jwtTokenProvider.issueLoginTokens(claims.userId))
    }
}
