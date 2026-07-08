package com.brifo.server.auth.service

import com.brifo.server.auth.dto.KakaoInfo
import com.brifo.server.global.code.ErrorCode
import com.brifo.server.global.config.JwtProperties
import com.brifo.server.global.exception.BusinessException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.Date
import java.util.UUID

class JwtTokenProviderTest {
    private val clock = Clock.fixed(Instant.parse("2026-07-08T00:00:00Z"), ZoneOffset.UTC)
    private val properties =
        JwtProperties(
            secretBase64 = TEST_SECRET_BASE64,
            accessTokenExpiration = Duration.ofHours(1),
            refreshTokenExpiration = Duration.ofDays(7),
            signupTokenExpiration = Duration.ofMinutes(10),
        )
    private val provider = JwtTokenProvider(properties, clock)

    @Test
    fun `로그인 토큰은 사용자 공개 ID와 종류 및 만료 시간을 담는다`() {
        val userId = UUID.randomUUID()

        val tokens = provider.issueLoginTokens(userId)
        val key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET_BASE64))
        val parser =
            Jwts
                .parser()
                .verifyWith(key)
                .clock { Date.from(Instant.now(clock)) }
                .build()
        val accessClaims = parser.parseSignedClaims(tokens.accessToken).payload
        val refreshClaims = parser.parseSignedClaims(tokens.refreshToken).payload

        assertEquals(userId.toString(), accessClaims.subject)
        assertEquals("ACCESS", accessClaims["tokenType"])
        assertEquals("REFRESH", refreshClaims["tokenType"])
        assertEquals(3600, tokens.accessTokenExpiresIn)
        assertEquals(604800, tokens.refreshTokenExpiresIn)
    }

    @Test
    fun `회원가입 토큰은 카카오 정보와 SIGNUP 종류를 검증한다`() {
        val token = provider.issueSignupToken(KakaoInfo(id = "1234567890", email = "user@kakao.com"))

        val claims = provider.parseSignupToken(token)

        assertEquals("1234567890", claims.socialId)
        assertEquals("KAKAO", claims.provider)
        assertEquals("user@kakao.com", claims.email)
    }

    @Test
    fun `위조된 회원가입 토큰은 거부한다`() {
        val token = provider.issueSignupToken(KakaoInfo(id = "1234567890", email = null))
        val parts = token.split(".").toMutableList()
        parts[2] = (if (parts[2].first() == 'a') "b" else "a") + parts[2].drop(1)
        val tamperedToken = parts.joinToString(".")

        val exception = assertThrows(BusinessException::class.java) { provider.parseSignupToken(tamperedToken) }

        assertEquals(ErrorCode.KAKAO_INVALID_TOKEN, exception.errorCode)
    }

    @Test
    fun `32바이트보다 짧은 JWT 키는 거부한다`() {
        val shortProperties = properties.copy(secretBase64 = "c2hvcnQ=")

        assertThrows(IllegalArgumentException::class.java) {
            JwtTokenProvider(shortProperties, clock)
        }
    }

    companion object {
        private const val TEST_SECRET_BASE64 =
            "dGVzdC1qd3Qtc2VjcmV0LWtleS1tdXN0LWJlLWF0LWxlYXN0LTMyLWJ5dGVz"
    }
}
