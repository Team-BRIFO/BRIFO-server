package com.brifo.server.auth.service

import com.brifo.server.auth.code.AuthErrorCode
import com.brifo.server.auth.dto.request.RefreshTokenRequest
import com.brifo.server.auth.entity.RevokedRefreshToken
import com.brifo.server.auth.exception.AuthException
import com.brifo.server.auth.repository.RevokedRefreshTokenRepository
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
class LogoutServiceTest {
    @Mock
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Mock
    private lateinit var revokedRefreshTokenRepository: RevokedRefreshTokenRepository

    @Test
    fun `인증 사용자와 Refresh Token 사용자가 같으면 Refresh Token을 폐기한다`() {
        val userPublicId = UUID.randomUUID()
        val expiresAt = Instant.parse("2026-07-27T00:00:00Z")
        val service = service()
        `when`(jwtTokenProvider.parseRefreshToken(REFRESH_TOKEN))
            .thenReturn(JwtTokenProvider.AuthTokenClaims(userPublicId, REFRESH_TOKEN_ID, expiresAt))
        `when`(revokedRefreshTokenRepository.existsByTokenId(REFRESH_TOKEN_ID)).thenReturn(false)

        service.logout(userPublicId, RefreshTokenRequest(REFRESH_TOKEN))

        val savedToken = org.mockito.ArgumentCaptor.forClass(RevokedRefreshToken::class.java)
        verify(revokedRefreshTokenRepository).saveAndFlush(savedToken.capture())
        assertEquals(REFRESH_TOKEN_ID, savedToken.value.tokenId)
        assertEquals(userPublicId, savedToken.value.userPublicId)
        assertEquals(expiresAt, savedToken.value.expiresAt)
    }

    @Test
    fun `Refresh Token이 없으면 필수 값 오류를 반환한다`() {
        val service = service()

        val exception =
            assertThrows(AuthException::class.java) {
                service.logout(UUID.randomUUID(), null)
            }

        assertEquals(AuthErrorCode.REFRESH_TOKEN_REQUIRED, exception.errorCode)
        verifyNoInteractions(jwtTokenProvider, revokedRefreshTokenRepository)
    }

    @Test
    fun `두 토큰의 사용자가 다르면 불일치 오류를 반환한다`() {
        val service = service()
        val expiresAt = Instant.parse("2026-07-27T00:00:00Z")
        `when`(jwtTokenProvider.parseRefreshToken(REFRESH_TOKEN))
            .thenReturn(JwtTokenProvider.AuthTokenClaims(UUID.randomUUID(), REFRESH_TOKEN_ID, expiresAt))

        val exception =
            assertThrows(AuthException::class.java) {
                service.logout(UUID.randomUUID(), RefreshTokenRequest(REFRESH_TOKEN))
            }

        assertEquals(AuthErrorCode.REFRESH_TOKEN_MISMATCH, exception.errorCode)
        verifyNoInteractions(revokedRefreshTokenRepository)
    }

    @Test
    fun `이미 폐기된 Refresh Token이면 사용할 수 없는 토큰 오류를 반환한다`() {
        val userPublicId = UUID.randomUUID()
        val expiresAt = Instant.parse("2026-07-27T00:00:00Z")
        val service = service()
        `when`(jwtTokenProvider.parseRefreshToken(REFRESH_TOKEN))
            .thenReturn(JwtTokenProvider.AuthTokenClaims(userPublicId, REFRESH_TOKEN_ID, expiresAt))
        `when`(revokedRefreshTokenRepository.existsByTokenId(REFRESH_TOKEN_ID)).thenReturn(true)

        val exception =
            assertThrows(AuthException::class.java) {
                service.logout(userPublicId, RefreshTokenRequest(REFRESH_TOKEN))
            }

        assertEquals(AuthErrorCode.REFRESH_TOKEN_UNUSABLE, exception.errorCode)
        verify(revokedRefreshTokenRepository).existsByTokenId(REFRESH_TOKEN_ID)
        verifyNoMoreInteractions(revokedRefreshTokenRepository)
    }

    private fun service() = LogoutService(jwtTokenProvider, revokedRefreshTokenRepository)

    companion object {
        private const val REFRESH_TOKEN = "refresh-token"
        private const val REFRESH_TOKEN_ID = "00000000-0000-0000-0000-000000000001"
    }
}
