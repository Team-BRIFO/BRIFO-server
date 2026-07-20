package com.brifo.server.auth.service

import com.brifo.server.auth.dto.TokenInfo
import com.brifo.server.global.code.ErrorCode
import com.brifo.server.global.config.JwtProperties
import com.brifo.server.global.exception.BusinessException
import com.brifo.server.user.entity.OAuthProvider
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    private val properties: JwtProperties,
    private val clock: Clock,
) {
    private val signingKey: SecretKey = createSigningKey(properties.secretBase64)
    private val parser =
        Jwts
            .parser()
            .verifyWith(signingKey)
            .requireIssuer(properties.issuer)
            .clock { Date.from(Instant.now(clock)) }
            .build()

    fun issueLoginTokens(userId: UUID): TokenInfo =
        TokenInfo(
            accessToken = createToken(userId.toString(), TokenType.ACCESS, properties.accessTokenExpiration),
            refreshToken = createToken(userId.toString(), TokenType.REFRESH, properties.refreshTokenExpiration),
            accessTokenExpiresIn = properties.accessTokenExpiration.seconds,
            refreshTokenExpiresIn = properties.refreshTokenExpiration.seconds,
        )

    fun issueSignupToken(
        provider: OAuthProvider,
        socialId: String,
        email: String?,
    ): String =
        createToken(
            subject = socialId,
            tokenType = TokenType.SIGNUP,
            expiration = properties.signupTokenExpiration,
            additionalClaims = mapOf(PROVIDER_CLAIM to provider.name, EMAIL_CLAIM to email),
        )

    fun parseSignupToken(token: String): SignupTokenClaims {
        val claims =
            parseClaims(
                token = token,
                expectedType = TokenType.SIGNUP,
                invalidErrorCode = ErrorCode.OAUTH_INVALID_TOKEN,
                expiredErrorCode = ErrorCode.OAUTH_INVALID_TOKEN,
            )
        return SignupTokenClaims(
            socialId = claims.subject,
            provider = claims.get(PROVIDER_CLAIM, String::class.java),
            email = claims.get(EMAIL_CLAIM, String::class.java),
        )
    }

    fun parseAccessToken(token: String): AuthTokenClaims =
        parseAuthToken(
            token = token,
            expectedType = TokenType.ACCESS,
            invalidErrorCode = ErrorCode.OAUTH_INVALID_TOKEN,
            expiredErrorCode = ErrorCode.OAUTH_INVALID_TOKEN,
        )

    fun parseRefreshToken(token: String): AuthTokenClaims =
        parseAuthToken(
            token = token,
            expectedType = TokenType.REFRESH,
            invalidErrorCode = ErrorCode.REFRESH_TOKEN_INVALID,
            expiredErrorCode = ErrorCode.REFRESH_TOKEN_EXPIRED,
        )

    fun parseRefreshTokenForLogout(token: String): AuthTokenClaims =
        parseAuthToken(
            token = token,
            expectedType = TokenType.REFRESH,
            invalidErrorCode = ErrorCode.OAUTH_INVALID_TOKEN,
            expiredErrorCode = ErrorCode.OAUTH_INVALID_TOKEN,
        )

    private fun parseAuthToken(
        token: String,
        expectedType: TokenType,
        invalidErrorCode: ErrorCode,
        expiredErrorCode: ErrorCode,
    ): AuthTokenClaims {
        val claims = parseClaims(token, expectedType, invalidErrorCode, expiredErrorCode)
        return try {
            AuthTokenClaims(
                userId = UUID.fromString(claims.subject),
                tokenId = claims.id?.takeIf { it.isNotBlank() } ?: throw IllegalArgumentException(),
                expiresAt = requireNotNull(claims.expiration).toInstant(),
            )
        } catch (exception: IllegalArgumentException) {
            throw BusinessException(invalidErrorCode)
        }
    }

    private fun createToken(
        subject: String,
        tokenType: TokenType,
        expiration: Duration,
        additionalClaims: Map<String, Any?> = emptyMap(),
    ): String {
        val issuedAt = Instant.now(clock)
        val builder =
            Jwts
                .builder()
                .id(UUID.randomUUID().toString())
                .issuer(properties.issuer)
                .subject(subject)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plus(expiration)))
                .claim(TOKEN_TYPE_CLAIM, tokenType.name)

        additionalClaims.forEach { (name, value) ->
            if (value != null) builder.claim(name, value)
        }

        return builder.signWith(signingKey).compact()
    }

    private fun parseClaims(
        token: String,
        expectedType: TokenType,
        invalidErrorCode: ErrorCode,
        expiredErrorCode: ErrorCode,
    ): Claims {
        try {
            val claims = parser.parseSignedClaims(token).payload
            if (claims.get(TOKEN_TYPE_CLAIM, String::class.java) != expectedType.name) {
                throw BusinessException(invalidErrorCode)
            }
            return claims
        } catch (exception: BusinessException) {
            throw exception
        } catch (exception: ExpiredJwtException) {
            throw BusinessException(expiredErrorCode)
        } catch (exception: JwtException) {
            throw BusinessException(invalidErrorCode)
        } catch (exception: IllegalArgumentException) {
            throw BusinessException(invalidErrorCode)
        }
    }

    private fun createSigningKey(secretBase64: String): SecretKey {
        val keyBytes =
            try {
                Decoders.BASE64.decode(secretBase64)
            } catch (exception: IllegalArgumentException) {
                throw IllegalStateException("JWT_SECRET_BASE64 must be valid Base64.", exception)
            }

        require(keyBytes.size >= MINIMUM_KEY_SIZE_BYTES) {
            "JWT_SECRET_BASE64 must decode to at least $MINIMUM_KEY_SIZE_BYTES bytes."
        }
        return Keys.hmacShaKeyFor(keyBytes)
    }

    data class SignupTokenClaims(
        val socialId: String,
        val provider: String,
        val email: String?,
    )

    data class AuthTokenClaims(
        val userId: UUID,
        val tokenId: String,
        val expiresAt: Instant,
    )

    private enum class TokenType {
        ACCESS,
        REFRESH,
        SIGNUP,
    }

    companion object {
        private const val MINIMUM_KEY_SIZE_BYTES = 32
        private const val TOKEN_TYPE_CLAIM = "tokenType"
        private const val PROVIDER_CLAIM = "provider"
        private const val EMAIL_CLAIM = "email"
    }
}
