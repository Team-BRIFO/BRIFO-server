package com.brifo.server.auth.service

import com.brifo.server.auth.code.AuthErrorCode
import com.brifo.server.auth.dto.request.RefreshTokenRequest
import com.brifo.server.auth.dto.response.TokenInfo
import com.brifo.server.auth.entity.RevokedRefreshToken
import com.brifo.server.auth.exception.AuthException
import com.brifo.server.auth.repository.RevokedRefreshTokenRepository
import com.brifo.server.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class TokenReissueServiceTest {
    @Mock
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Mock
    private lateinit var revokedRefreshTokenRepository: RevokedRefreshTokenRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Test
    fun `유효한 Refresh Token을 폐기하고 새 토큰 쌍을 발급한다`() {
        val userId = UUID.randomUUID()
        val expiresAt = Instant.parse("2026-07-27T00:00:00Z")
        val claims = JwtTokenProvider.AuthTokenClaims(userId, REFRESH_TOKEN_ID, expiresAt)
        val newTokens = TokenInfo("new-access-token", "new-refresh-token", 3600, 604800)
        `when`(jwtTokenProvider.parseRefreshToken(REFRESH_TOKEN)).thenReturn(claims)
        `when`(revokedRefreshTokenRepository.existsByTokenId(REFRESH_TOKEN_ID)).thenReturn(false)
        `when`(userRepository.existsByPublicId(userId)).thenReturn(true)
        `when`(jwtTokenProvider.issueLoginTokens(userId)).thenReturn(newTokens)

        val response = service().reissue(RefreshTokenRequest(REFRESH_TOKEN))

        assertEquals(newTokens, response.token)
        val savedToken = org.mockito.ArgumentCaptor.forClass(RevokedRefreshToken::class.java)
        verify(revokedRefreshTokenRepository).saveAndFlush(savedToken.capture())
        assertEquals(REFRESH_TOKEN_ID, savedToken.value.tokenId)
        assertEquals(userId, savedToken.value.userPublicId)
        assertEquals(expiresAt, savedToken.value.expiresAt)
    }

    @Test
    fun `Refresh Token이 없으면 필수 값 오류를 반환한다`() {
        val exception =
            assertThrows(AuthException::class.java) {
                service().reissue(null)
            }

        assertEquals(AuthErrorCode.REFRESH_TOKEN_REQUIRED, exception.errorCode)
        verifyNoInteractions(jwtTokenProvider, revokedRefreshTokenRepository, userRepository)
    }

    @Test
    fun `이미 사용했거나 로그아웃된 Refresh Token은 재발급할 수 없다`() {
        val claims =
            JwtTokenProvider.AuthTokenClaims(
                UUID.randomUUID(),
                REFRESH_TOKEN_ID,
                Instant.parse("2026-07-27T00:00:00Z"),
            )
        `when`(jwtTokenProvider.parseRefreshToken(REFRESH_TOKEN)).thenReturn(claims)
        `when`(revokedRefreshTokenRepository.existsByTokenId(REFRESH_TOKEN_ID)).thenReturn(true)

        val exception =
            assertThrows(AuthException::class.java) {
                service().reissue(RefreshTokenRequest(REFRESH_TOKEN))
            }

        assertEquals(AuthErrorCode.REFRESH_TOKEN_UNUSABLE, exception.errorCode)
        verifyNoInteractions(userRepository)
        verify(revokedRefreshTokenRepository).existsByTokenId(REFRESH_TOKEN_ID)
        verifyNoMoreInteractions(revokedRefreshTokenRepository)
    }

    @Test
    fun `Refresh Token의 사용자가 없으면 사용자 없음 오류를 반환한다`() {
        val userId = UUID.randomUUID()
        val claims =
            JwtTokenProvider.AuthTokenClaims(
                userId,
                REFRESH_TOKEN_ID,
                Instant.parse("2026-07-27T00:00:00Z"),
            )
        `when`(jwtTokenProvider.parseRefreshToken(REFRESH_TOKEN)).thenReturn(claims)
        `when`(revokedRefreshTokenRepository.existsByTokenId(REFRESH_TOKEN_ID)).thenReturn(false)
        `when`(userRepository.existsByPublicId(userId)).thenReturn(false)

        val exception =
            assertThrows(AuthException::class.java) {
                service().reissue(RefreshTokenRequest(REFRESH_TOKEN))
            }

        assertEquals(AuthErrorCode.AUTH_USER_NOT_FOUND, exception.errorCode)
        verify(revokedRefreshTokenRepository).existsByTokenId(REFRESH_TOKEN_ID)
        verifyNoMoreInteractions(revokedRefreshTokenRepository)
    }

    private fun service() = TokenReissueService(jwtTokenProvider, revokedRefreshTokenRepository, userRepository)

    companion object {
        private const val REFRESH_TOKEN = "refresh-token"
        private const val REFRESH_TOKEN_ID = "00000000-0000-0000-0000-000000000001"
    }
}
