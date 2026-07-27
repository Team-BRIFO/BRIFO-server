package com.brifo.server.auth.service

import com.brifo.server.auth.code.AuthErrorCode
import com.brifo.server.auth.exception.AuthException
import com.brifo.server.global.config.JwtProperties
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
    fun `Access Token과 Refresh Token을 용도별로 검증한다`() {
        val userId = UUID.randomUUID()
        val tokens = provider.issueLoginTokens(userId)

        val accessClaims = provider.parseAccessToken(tokens.accessToken)
        val refreshClaims = provider.parseRefreshToken(tokens.refreshToken)

        assertEquals(userId, accessClaims.userPublicId)
        assertEquals(userId, refreshClaims.userPublicId)
        assertEquals(Instant.parse("2026-07-08T01:00:00Z"), accessClaims.expiresAt)
        assertEquals(Instant.parse("2026-07-15T00:00:00Z"), refreshClaims.expiresAt)
    }

    @Test
    fun `인증 토큰은 한 번의 파싱으로 Access와 Signup 종류를 구분한다`() {
        val userId = UUID.randomUUID()
        val accessToken = provider.issueLoginTokens(userId).accessToken
        val signupToken = provider.issueSignupToken(userId)

        val accessClaims = provider.parseAuthenticationToken(accessToken)
        val signupClaims = provider.parseAuthenticationToken(signupToken)

        assertEquals(userId, accessClaims.userPublicId)
        assertEquals(JwtTokenProvider.AuthenticationTokenType.ACCESS, accessClaims.tokenType)
        assertEquals(userId, signupClaims.userPublicId)
        assertEquals(JwtTokenProvider.AuthenticationTokenType.SIGNUP, signupClaims.tokenType)
    }

    @Test
    fun `Refresh Token은 필터 인증 토큰으로 사용할 수 없다`() {
        val refreshToken = provider.issueLoginTokens(UUID.randomUUID()).refreshToken

        val exception =
            assertThrows(AuthException::class.java) {
                provider.parseAuthenticationToken(refreshToken)
            }

        assertEquals(AuthErrorCode.INVALID_TOKEN, exception.errorCode)
    }

    @Test
    fun `Access Token을 Refresh Token으로 사용하면 거부한다`() {
        val accessToken = provider.issueLoginTokens(UUID.randomUUID()).accessToken

        val exception =
            assertThrows(AuthException::class.java) {
                provider.parseRefreshToken(accessToken)
            }

        assertEquals(AuthErrorCode.REFRESH_TOKEN_INVALID, exception.errorCode)
    }

    @Test
    fun `만료된 Refresh Token은 만료 오류로 구분한다`() {
        val refreshToken = provider.issueLoginTokens(UUID.randomUUID()).refreshToken
        val expiredProvider =
            JwtTokenProvider(
                properties,
                Clock.fixed(Instant.parse("2026-07-16T00:00:00Z"), ZoneOffset.UTC),
            )

        val exception =
            assertThrows(AuthException::class.java) {
                expiredProvider.parseRefreshToken(refreshToken)
            }

        assertEquals(AuthErrorCode.REFRESH_TOKEN_EXPIRED, exception.errorCode)
    }

    @Test
    fun `회원가입 토큰은 사용자 공개 ID를 담는다`() {
        val userId = UUID.randomUUID()
        val token = provider.issueSignupToken(userId)

        val claims = provider.parseSignupToken(token)

        assertEquals(userId, claims.userPublicId)
    }

    @Test
    fun `회원가입 토큰에는 공급자와 개인정보를 포함하지 않는다`() {
        val token = provider.issueSignupToken(UUID.randomUUID())
        val key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET_BASE64))
        val claims =
            Jwts
                .parser()
                .verifyWith(key)
                .clock { Date.from(Instant.now(clock)) }
                .build()
                .parseSignedClaims(token)
                .payload

        assertEquals(null, claims["provider"])
        assertEquals(null, claims["email"])
    }

    @Test
    fun `위조된 회원가입 토큰은 거부한다`() {
        val token = provider.issueSignupToken(UUID.randomUUID())
        val parts = token.split(".").toMutableList()
        parts[2] = (if (parts[2].first() == 'a') "b" else "a") + parts[2].drop(1)
        val tamperedToken = parts.joinToString(".")

        val exception = assertThrows(AuthException::class.java) { provider.parseSignupToken(tamperedToken) }

        assertEquals(AuthErrorCode.INVALID_TOKEN, exception.errorCode)
    }

    @Test
    fun `subject가 UUID가 아닌 회원가입 토큰은 거부한다`() {
        val key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET_BASE64))
        val issuedAt = Instant.now(clock)
        val token =
            Jwts
                .builder()
                .id(UUID.randomUUID().toString())
                .issuer(properties.issuer)
                .subject("not-a-uuid")
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plus(properties.signupTokenExpiration)))
                .claim("tokenType", "SIGNUP")
                .signWith(key)
                .compact()

        val exception = assertThrows(AuthException::class.java) { provider.parseSignupToken(token) }

        assertEquals(AuthErrorCode.INVALID_TOKEN, exception.errorCode)
    }

    @Test
    fun `subject가 없는 Access Token은 유효하지 않은 토큰으로 처리한다`() {
        val key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET_BASE64))
        val issuedAt = Instant.now(clock)
        val token =
            Jwts
                .builder()
                .id(UUID.randomUUID().toString())
                .issuer(properties.issuer)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plus(properties.accessTokenExpiration)))
                .claim("tokenType", "ACCESS")
                .signWith(key)
                .compact()

        val exception = assertThrows(AuthException::class.java) { provider.parseAccessToken(token) }

        assertEquals(AuthErrorCode.INVALID_TOKEN, exception.errorCode)
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
