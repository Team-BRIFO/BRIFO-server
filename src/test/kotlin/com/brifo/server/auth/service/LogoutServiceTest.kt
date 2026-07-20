package com.brifo.server.auth.service

import com.brifo.server.auth.dto.LogoutRequest
import com.brifo.server.auth.entity.RevokedRefreshToken
import com.brifo.server.auth.repository.RevokedRefreshTokenRepository
import com.brifo.server.global.code.ErrorCode
import com.brifo.server.global.exception.BusinessException
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
    fun `같은 사용자의 Access Token과 Refresh Token이면 Refresh Token을 폐기한다`() {
        val userId = UUID.randomUUID()
        val expiresAt = Instant.parse("2026-07-27T00:00:00Z")
        val service = service()
        `when`(jwtTokenProvider.parseAccessToken(ACCESS_TOKEN))
            .thenReturn(JwtTokenProvider.AuthTokenClaims(userId, "access-jti", expiresAt))
        `when`(jwtTokenProvider.parseRefreshTokenForLogout(REFRESH_TOKEN))
            .thenReturn(JwtTokenProvider.AuthTokenClaims(userId, REFRESH_TOKEN_ID, expiresAt))
        `when`(revokedRefreshTokenRepository.existsByTokenId(REFRESH_TOKEN_ID)).thenReturn(false)

        service.logout("Bearer $ACCESS_TOKEN", LogoutRequest(REFRESH_TOKEN))

        val savedToken = org.mockito.ArgumentCaptor.forClass(RevokedRefreshToken::class.java)
        verify(revokedRefreshTokenRepository).saveAndFlush(savedToken.capture())
        assertEquals(REFRESH_TOKEN_ID, savedToken.value.tokenId)
        assertEquals(userId, savedToken.value.userPublicId)
        assertEquals(expiresAt, savedToken.value.expiresAt)
    }

    @Test
    fun `Authorization 헤더가 없으면 인증 필요 오류를 반환한다`() {
        val service = service()

        val exception =
            assertThrows(BusinessException::class.java) {
                service.logout(null, LogoutRequest(REFRESH_TOKEN))
            }

        assertEquals(ErrorCode.UNAUTHORIZED, exception.errorCode)
        verifyNoInteractions(jwtTokenProvider, revokedRefreshTokenRepository)
    }

    @Test
    fun `Refresh Token이 없으면 필수 값 오류를 반환한다`() {
        val service = service()

        val exception =
            assertThrows(BusinessException::class.java) {
                service.logout("Bearer $ACCESS_TOKEN", null)
            }

        assertEquals(ErrorCode.REFRESH_TOKEN_REQUIRED, exception.errorCode)
        verifyNoInteractions(jwtTokenProvider, revokedRefreshTokenRepository)
    }

    @Test
    fun `두 토큰의 사용자가 다르면 불일치 오류를 반환한다`() {
        val service = service()
        val expiresAt = Instant.parse("2026-07-27T00:00:00Z")
        `when`(jwtTokenProvider.parseAccessToken(ACCESS_TOKEN))
            .thenReturn(JwtTokenProvider.AuthTokenClaims(UUID.randomUUID(), "access-jti", expiresAt))
        `when`(jwtTokenProvider.parseRefreshTokenForLogout(REFRESH_TOKEN))
            .thenReturn(JwtTokenProvider.AuthTokenClaims(UUID.randomUUID(), REFRESH_TOKEN_ID, expiresAt))

        val exception =
            assertThrows(BusinessException::class.java) {
                service.logout("Bearer $ACCESS_TOKEN", LogoutRequest(REFRESH_TOKEN))
            }

        assertEquals(ErrorCode.REFRESH_TOKEN_MISMATCH, exception.errorCode)
        verifyNoInteractions(revokedRefreshTokenRepository)
    }

    @Test
    fun `이미 폐기된 Refresh Token이면 사용할 수 없는 토큰 오류를 반환한다`() {
        val userId = UUID.randomUUID()
        val expiresAt = Instant.parse("2026-07-27T00:00:00Z")
        val service = service()
        `when`(jwtTokenProvider.parseAccessToken(ACCESS_TOKEN))
            .thenReturn(JwtTokenProvider.AuthTokenClaims(userId, "access-jti", expiresAt))
        `when`(jwtTokenProvider.parseRefreshTokenForLogout(REFRESH_TOKEN))
            .thenReturn(JwtTokenProvider.AuthTokenClaims(userId, REFRESH_TOKEN_ID, expiresAt))
        `when`(revokedRefreshTokenRepository.existsByTokenId(REFRESH_TOKEN_ID)).thenReturn(true)

        val exception =
            assertThrows(BusinessException::class.java) {
                service.logout("Bearer $ACCESS_TOKEN", LogoutRequest(REFRESH_TOKEN))
            }

        assertEquals(ErrorCode.REFRESH_TOKEN_UNUSABLE, exception.errorCode)
        verify(revokedRefreshTokenRepository).existsByTokenId(REFRESH_TOKEN_ID)
        verifyNoMoreInteractions(revokedRefreshTokenRepository)
    }

    private fun service() = LogoutService(jwtTokenProvider, revokedRefreshTokenRepository)

    companion object {
        private const val ACCESS_TOKEN = "access-token"
        private const val REFRESH_TOKEN = "refresh-token"
        private const val REFRESH_TOKEN_ID = "00000000-0000-0000-0000-000000000001"
    }
}
