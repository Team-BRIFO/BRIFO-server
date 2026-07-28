package com.brifo.server.auth.service

import com.brifo.server.auth.dto.response.TokenInfo
import com.brifo.server.auth.exception.AuthException
import com.brifo.server.auth.exception.InvalidJwtTokenException
import com.brifo.server.auth.exception.InvalidRefreshTokenException
import com.brifo.server.auth.exception.RefreshTokenExpiredException
import com.brifo.server.global.config.JwtProperties
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

    fun issueLoginTokens(userPublicId: UUID): TokenInfo =
        TokenInfo(
            accessToken = createToken(userPublicId.toString(), TokenType.ACCESS, properties.accessTokenExpiration),
            refreshToken = createToken(userPublicId.toString(), TokenType.REFRESH, properties.refreshTokenExpiration),
            accessTokenExpiresIn = properties.accessTokenExpiration.seconds,
            refreshTokenExpiresIn = properties.refreshTokenExpiration.seconds,
        )

    fun issueSignupToken(userPublicId: UUID): String =
        createToken(
            subject = userPublicId.toString(),
            tokenType = TokenType.SIGNUP,
            expiration = properties.signupTokenExpiration,
        )

    fun parseSignupToken(token: String): SignupTokenClaims {
        val claims =
            parseClaims(
                token = token,
                expectedType = TokenType.SIGNUP,
                invalidException = { InvalidJwtTokenException() },
                expiredException = { InvalidJwtTokenException() },
            )
        val subject = claims.subject?.takeIf { it.isNotBlank() } ?: throw InvalidJwtTokenException()
        val userPublicId =
            try {
                UUID.fromString(subject)
            } catch (exception: IllegalArgumentException) {
                throw InvalidJwtTokenException()
            }
        return SignupTokenClaims(userPublicId)
    }

    fun parseAccessToken(token: String): AuthTokenClaims =
        parseAuthToken(
            token = token,
            expectedType = TokenType.ACCESS,
            invalidException = { InvalidJwtTokenException() },
            expiredException = { InvalidJwtTokenException() },
        )

    fun parseAuthenticationToken(token: String): AuthenticationTokenClaims {
        val claims =
            parseVerifiedClaims(
                token = token,
                invalidException = { InvalidJwtTokenException() },
                expiredException = { InvalidJwtTokenException() },
            )

        val tokenType =
            when (claims.get(TOKEN_TYPE_CLAIM, String::class.java)) {
                TokenType.ACCESS.name -> AuthenticationTokenType.ACCESS
                TokenType.SIGNUP.name -> AuthenticationTokenType.SIGNUP
                else -> throw InvalidJwtTokenException()
            }
        val userPublicId =
            try {
                UUID.fromString(claims.subject?.takeIf { it.isNotBlank() } ?: throw IllegalArgumentException())
            } catch (_: IllegalArgumentException) {
                throw InvalidJwtTokenException()
            }

        return AuthenticationTokenClaims(userPublicId, tokenType)
    }

    fun parseRefreshToken(token: String): AuthTokenClaims =
        parseAuthToken(
            token = token,
            expectedType = TokenType.REFRESH,
            invalidException = { InvalidRefreshTokenException() },
            expiredException = { RefreshTokenExpiredException() },
        )

    private fun parseAuthToken(
        token: String,
        expectedType: TokenType,
        invalidException: () -> AuthException,
        expiredException: () -> AuthException,
    ): AuthTokenClaims {
        val claims = parseClaims(token, expectedType, invalidException, expiredException)
        val subject = claims.subject?.takeIf { it.isNotBlank() } ?: throw invalidException()
        return try {
            AuthTokenClaims(
                userPublicId = UUID.fromString(subject),
                tokenId = claims.id?.takeIf { it.isNotBlank() } ?: throw IllegalArgumentException(),
                expiresAt = requireNotNull(claims.expiration).toInstant(),
            )
        } catch (_: IllegalArgumentException) {
            throw invalidException()
        }
    }

    private fun createToken(
        subject: String,
        tokenType: TokenType,
        expiration: Duration,
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

        return builder.signWith(signingKey).compact()
    }

    private fun parseClaims(
        token: String,
        expectedType: TokenType,
        invalidException: () -> AuthException,
        expiredException: () -> AuthException,
    ): Claims {
        val claims = parseVerifiedClaims(token, invalidException, expiredException)
        if (claims.get(TOKEN_TYPE_CLAIM, String::class.java) != expectedType.name) {
            throw invalidException()
        }
        return claims
    }

    private fun parseVerifiedClaims(
        token: String,
        invalidException: () -> AuthException,
        expiredException: () -> AuthException,
    ): Claims {
        try {
            return parser.parseSignedClaims(token).payload
        } catch (_: ExpiredJwtException) {
            throw expiredException()
        } catch (_: JwtException) {
            throw invalidException()
        } catch (_: IllegalArgumentException) {
            throw invalidException()
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
        val userPublicId: UUID,
    )

    data class AuthTokenClaims(
        val userPublicId: UUID,
        val tokenId: String,
        val expiresAt: Instant,
    )

    data class AuthenticationTokenClaims(
        val userPublicId: UUID,
        val tokenType: AuthenticationTokenType,
    )

    enum class AuthenticationTokenType {
        ACCESS,
        SIGNUP,
    }

    private enum class TokenType {
        ACCESS,
        REFRESH,
        SIGNUP,
    }

    companion object {
        private const val MINIMUM_KEY_SIZE_BYTES = 32
        private const val TOKEN_TYPE_CLAIM = "tokenType"
    }
}
